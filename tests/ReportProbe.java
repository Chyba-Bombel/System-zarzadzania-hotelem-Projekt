import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import pl.hotel.gui.HotelGUI;
import pl.hotel.serwis.GuiRepository;
import pl.hotel.raporty.Raporty;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;

public class ReportProbe {
    static void check(boolean ok,String m) { if(!ok)throw new AssertionError(m); }
    static String wsk(Raporty.Raport r,String key) { return r.tabele().get(0).wiersze().stream().filter(w->w.get(0).equals(key)).findFirst().orElseThrow().get(1); }
    static Pokoj pokoj(String n) throws Exception { return BazaDanych.pobierzPokoje().stream().filter(p->p.getNumer().equals(n)).findFirst().orElseThrow(); }
    static void platnosc(int rez,String data,String rodzaj,String kwota) throws Exception {
        try(var c=TestDatabase.polacz();var p=c.prepareStatement("INSERT INTO platnosci(rezerwacja_id,pracownik_id,kwota,rodzaj,metoda,data_platnosci) VALUES (?,1,?,?,'KARTA',?)")) {
            p.setInt(1,rez);p.setBigDecimal(2,new BigDecimal(kwota));p.setString(3,rodzaj);p.setTimestamp(4,Timestamp.valueOf(data));p.executeUpdate();
        }
    }
    public static void main(String[] args) throws Exception {
        TestDatabase.wymagajTestowejBazy();
        BazaDanych.zaloguj("admin","Admin123!");
        LocalDate d=LocalDate.of(2040,2,10);
        Gosc g=BazaDanych.zapiszGoscia("Łukasz","Raportowy","111222333","raport@example.com");
        for(String n:List.of("R100","R101","R102"))BazaDanych.dodajPokoj(n,"Test raportu",4,new BigDecimal("100"));
        var u=List.of(new Uczestnik("Anna","Nowak"),new Uczestnik("Jan","O'Connor"));
        var a=BazaDanych.zapiszRezerwacje(g,pokoj("R100"),d.minusDays(2),d.plusDays(1),3,1,u);
        var b=BazaDanych.zapiszRezerwacje(g,pokoj("R100"),d.plusDays(2),d.plusDays(5),2,1,u.subList(0,1));
        var an=BazaDanych.zapiszRezerwacje(g,pokoj("R101"),d,d.plusDays(2),1,1,List.of());
        BazaDanych.anulujRezerwacje(an.getId());
        var wyjazd=BazaDanych.zapiszRezerwacje(g,pokoj("R102"),d.minusDays(1),d,1,1,List.of());
        var po=BazaDanych.zapiszRezerwacje(g,pokoj("R102"),d.plusDays(4),d.plusDays(5),1,1,List.of());
        platnosc(a.getId(),"2040-02-10 00:00:00","WPLATA","50");
        platnosc(a.getId(),"2040-02-13 23:59:59","ZWROT","10");
        platnosc(a.getId(),"2040-02-14 00:00:00","WPLATA","200");
        var raport=Raporty.generuj(d,d.plusDays(3));
        check(wsk(raport,"Zarezerwowane pokojonoce").equals("3"),"Przecięcie pobytów z okresem");
        check(wsk(raport,"Osobonoce").equals("7"),"Liczba osób i noce");
        check(wsk(raport,"Wartość noclegów w okresie (zł)").equals("300,00"),"Wycena części pobytów");
        check(wsk(raport,"Wpłaty minus zwroty (zł)").equals("40,00"),"Granice dat płatności i brak mnożenia przez gości");
        var rows=raport.tabele().get(1).wiersze();
        check(rows.size()==3,"Wyjazd pierwszego dnia i przyjazd po końcu wykluczone");
        check(rows.stream().anyMatch(w->w.get(2).contains("O'Connor")),"Nazwiska dodatkowych osób w raporcie");
        var r100=raport.tabele().get(2).wiersze().stream().filter(w->w.get(0).equals("R100")).findFirst().orElseThrow();
        check(r100.get(5).equals("75,00"),"75% obłożenia pokoju");
        var r101=raport.tabele().get(2).wiersze().stream().filter(w->w.get(0).equals("R101")).findFirst().orElseThrow();
        check(r101.get(3).equals("0"),"Anulowanie nie zajmuje nocy");
        check(Raporty.generuj(d,d).tabele().get(1).wiersze().size()==2,"Raport jednodniowy");
        check(Raporty.generuj(d.plusYears(1),d.plusYears(1)).tabele().get(3).wiersze().isEmpty(),"Pusty okres");
        try{Raporty.generuj(d,d.minusDays(1));throw new AssertionError("Odwrócony okres przyjęty");}catch(IllegalArgumentException ok){}
        try{Raporty.generuj(d,d.plusDays(366));throw new AssertionError("Za długi okres przyjęty");}catch(IllegalArgumentException ok){}
        Path dir=Files.createTempDirectory("hotel-report-test-");
        try {
            var tabela=new Raporty.Tabela("Eksport",List.of("Nazwa","Kwota"),List.of(List.of("=HYPERLINK(\"x\")","-10,00"),List.of("\uFEFF=1+1","1,00"),List.of("\t+SUM(1;1)","2,00"),List.of("Łódź; \"test\"\n<script>alert(1)</script>","20,50")));
            Path csv=dir.resolve("raport.csv"),html=dir.resolve("raport.html");
            Raporty.zapiszCsv(tabela,csv);Raporty.zapiszHtml(new Raporty.Raport(d,d,LocalDateTime.now(),List.of(tabela)),html);
            String tekst=Files.readString(csv,StandardCharsets.UTF_8);
            check(tekst.startsWith("\uFEFF"),"BOM UTF-8");
            check(tekst.contains("\"'\uFEFF=1+1\"") && tekst.contains("\"'\t+SUM(1;1)\""),"Formuły ukryte za BOM i tabulatorem");check(tekst.contains("\"'=HYPERLINK(\"\"x\"\")\""),"Neutralizacja formuł CSV");
            check(tekst.contains("\"-10,00\""),"Ujemne kwoty pozostają liczbami");
            check(tekst.contains("Łódź; \"\"test\"\"\n"),"Cytowanie separatora, cudzysłowu i nowej linii");
            String h=Files.readString(html);check(!h.contains("<script>"),"Kod użytkownika nie jest HTML");check(h.contains("&lt;script&gt;"),"Kodowanie HTML");
            Raporty.zapiszCsv(raport.tabele().get(0),csv); check(!Files.readString(csv).contains("HYPERLINK"),"Zastępowanie istniejącego eksportu");
            try(var fs=Files.list(dir)) { check(fs.count()==2,"Brak tymczasowych plików po zapisie"); }
        } finally {try(var fs=Files.list(dir)){for(Path f:fs.toList())Files.delete(f);}Files.delete(dir);}
        System.out.println("REPORTS_OK: okres, obłożenie, ceny, płatności, uczestnicy, CSV i HTML.");
    }
}
