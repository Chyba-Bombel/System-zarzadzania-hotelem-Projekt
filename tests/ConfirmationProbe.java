import pl.hotel.baza.BazaDanych;
import pl.hotel.model.*;
import pl.hotel.potwierdzenia.Potwierdzenia;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

public final class ConfirmationProbe {
    private static int assertions;
    private static void check(boolean ok, String opis) {
        assertions++;
        if (!ok) throw new AssertionError(opis);
    }
    @FunctionalInterface interface Akcja { void run() throws Exception; }
    private static void odmowa(Akcja akcja) throws Exception {
        try { akcja.run(); throw new AssertionError("Przyjęto niedozwoloną operację"); }
        catch (SQLException ok) { assertions++; }
    }
    private static void rowne(BigDecimal kwota, String tekst, String opis) {
        check(kwota.compareTo(new BigDecimal(tekst)) == 0, opis);
    }
    public static void main(String[] args) throws Exception {
        TestDatabase.wymagajTestowejBazy();
        BazaDanych.wyloguj();
        try { BazaDanych.pobierzPotwierdzenieRezerwacji(1); throw new AssertionError("Brak ochrony sesji"); }
        catch (SecurityException ok) { assertions++; }
        var admin = BazaDanych.zaloguj("admin","Admin123!");
        odmowa(() -> BazaDanych.pobierzPotwierdzenieRezerwacji(0));
        odmowa(() -> BazaDanych.pobierzPotwierdzenieRezerwacji(Integer.MAX_VALUE));
        var g = BazaDanych.zapiszGoscia("Łukasz <b>","O'Connor & Syn","500100200","private@example.com");
        BazaDanych.dodajPokoj("C100","<img src=x onerror=alert(1)>",4,new BigDecimal("123.45"));
        BazaDanych.dodajPokoj("C101","Test",4,new BigDecimal("888.88"));
        var a = IntegrationProbe.pokoj("C100"); var b = IntegrationProbe.pokoj("C101");
        var osoby = List.of(new Uczestnik("Anna","<script>"),new Uczestnik("Jan","O'Connor"));
        var od = LocalDate.now().plusDays(5);
        var r = BazaDanych.zapiszRezerwacje(g,a,od,od.plusDays(2),3,admin.getId(),osoby);
        BazaDanych.zapiszWplate(r.getId(),new BigDecimal("100.10"),"GOTOWKA",admin.getId());
        BazaDanych.zapiszWplate(r.getId(),new BigDecimal("20.20"),"KARTA",admin.getId());
        BazaDanych.zapiszZwrot(r.getId(),new BigDecimal("10.10"),"KARTA",admin.getId());
        var p = BazaDanych.pobierzPotwierdzenieRezerwacji(r.getId());
        check(p.noce()==2 && p.liczbaOsob()==3 && p.uczestnicy().size()==2,"Termin i osoby");
        rowne(p.wartoscPobytu(),"246.90","Cena bez mnożenia przez liczbę gości");
        rowne(p.wplaty(),"120.30","Suma wpłat"); rowne(p.zwroty(),"10.10","Suma zwrotów");
        rowne(p.netto(),"110.20","Wpłaty netto"); rowne(p.saldo(),"136.70","Precyzyjne saldo");
        check(p.brakujaceOsoby()==0 && p.utworzono()!=null,"Kompletna lista i czas serwera");
        try { p.uczestnicy().add("Zmiana"); throw new AssertionError("Modyfikowalny dokument"); }
        catch (UnsupportedOperationException ok) { assertions++; }
        String html = Potwierdzenia.html(p);
        check(html.contains("&lt;b&gt;") && html.contains("O&#39;Connor &amp; Syn"),"Kodowanie nazwiska i znaków specjalnych");
        check(html.contains("&lt;script&gt;") && !html.contains("<script>"),"Kodowanie uczestnika");
        check(html.contains("&lt;img src=x onerror=alert(1)&gt;") && !html.contains("<img"),"Kodowanie typu pokoju");
        check(html.contains("Content-Security-Policy") && html.contains("default-src 'none'"),"Polityka zasobów dokumentu");
        check(!html.contains("private@example.com") && !html.contains("500100200") && !html.contains("hash_hasla"),"Zakres danych dokumentu");
        check(Potwierdzenia.nazwaPliku(p).equals("rezerwacja_"+r.getId()+"_"+od+".html"),"Nazwa pliku bez nazwisk");
        int historia = BazaDanych.pobierzHistorieRezerwacji(r.getId(),0).wpisy().size();
        Path folder = Files.createTempDirectory("hotel-confirmation-test-");
        try {
            Path plik = folder.resolve("potwierdzenie.html");
            Potwierdzenia.zapiszHtml(p,plik);
            check(Files.readString(plik,StandardCharsets.UTF_8).equals(html),"Zapis UTF-8");
            Files.writeString(plik,"stara zawartość"); Potwierdzenia.zapiszHtml(p,plik);
            check(Files.readString(plik).equals(html),"Zastąpienie istniejącego pliku");
            Path katalog = Files.createDirectory(folder.resolve("katalog"));
            Path wazny = katalog.resolve("zachowaj.txt"); Files.writeString(wazny,"ważne dane");
            try { Potwierdzenia.zapiszHtml(p,katalog); throw new AssertionError("Zastąpiono katalog plikiem"); }
            catch (java.io.IOException ok) { assertions++; }
            check(Files.readString(wazny).equals("ważne dane"),"Błąd zapisu zachowuje dane");
            try(var pliki=Files.list(folder)) {
                check(pliki.noneMatch(x->x.getFileName().toString().startsWith(".hotel-eksport-")),"Sprzątanie plików tymczasowych");
            }
            if(System.getProperty("hotel.confirmation.sample")!=null)
                Potwierdzenia.zapiszHtml(p,Path.of(System.getProperty("hotel.confirmation.sample")));
            Files.delete(wazny); Files.delete(katalog); Files.delete(plik);
        } finally {
            try(var pozostale=Files.walk(folder)) {
                for(Path plik:pozostale.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(plik);
            }
        }
        check(BazaDanych.pobierzHistorieRezerwacji(r.getId(),0).wpisy().size()==historia,"Odczyt i eksport nie zmieniają historii");
        BazaDanych.edytujPokoj(a.getId(),"C100","Nowy typ",new BigDecimal("999.99"));
        rowne(BazaDanych.pobierzPotwierdzenieRezerwacji(r.getId()).cenaZaNoc(),"123.45","Cena historyczna");
        BazaDanych.zmienPokojRezerwacji(r.getId(),a.getId(),b.getId());
        var nowe = BazaDanych.pobierzPotwierdzenieRezerwacji(r.getId());
        check(nowe.pokoj().equals("C101") && p.pokoj().equals("C100"),"Nowy pokój i niezmienność poprzedniego podglądu");
        rowne(nowe.cenaZaNoc(),"123.45","Przeniesienie nie zmienia ceny");
        var recepcja = BazaDanych.zaloguj("recepcja","Recepcja123!");
        check(recepcja!=null && BazaDanych.pobierzPotwierdzenieRezerwacji(r.getId()).id()==r.getId(),"Dostęp recepcji");
        BazaDanych.zaloguj("admin","Admin123!");
        BazaDanych.zapiszZwrot(r.getId(),new BigDecimal("110.20"),"KARTA",admin.getId());
        BazaDanych.anulujRezerwacje(r.getId());
        var anulowane = BazaDanych.pobierzPotwierdzenieRezerwacji(r.getId());
        rowne(anulowane.naleznosc(),"0","Należność anulowanej rezerwacji");
        rowne(anulowane.netto(),"0","Zwrot całej wpłaty");
        check(Potwierdzenia.html(anulowane).contains("REZERWACJA ANULOWANA") && Potwierdzenia.html(anulowane).contains("Nie potwierdza aktywnego pobytu."),
                "Oznaczenie anulowania");
        var stare = BazaDanych.zapiszRezerwacje(g,a,od.plusDays(10),od.plusDays(11),3,admin.getId());
        check(BazaDanych.pobierzPotwierdzenieRezerwacji(stare.getId()).brakujaceOsoby()==2,"Brakujące dane starszej rezerwacji");
        check(Potwierdzenia.html(BazaDanych.pobierzPotwierdzenieRezerwacji(stare.getId())).contains("Brak danych dodatkowych osób: 2"),"Brakujące nazwiska nie są wymyślane");
        spojnosc(admin.getId());
        System.out.println("CONFIRMATION_OK: "+assertions+" asercji; cena, płatności, anulowanie, HTML, zapis pliku, role i spójność odczytu.");
    }

    private static void spojnosc(long pracownik) throws Exception {
        var g = BazaDanych.zapiszGoscia("Wersja A","Spójna","123456789","snapshot@example.com");
        BazaDanych.dodajPokoj("C110","Spójność",2,new BigDecimal("100"));
        var r = BazaDanych.zapiszRezerwacje(g,IntegrationProbe.pokoj("C110"),LocalDate.now().plusDays(15),
                LocalDate.now().plusDays(16),2,pracownik,List.of(new Uczestnik("Wersja A","Dodatkowy")));
        BazaDanych.zapiszWplate(r.getId(),new BigDecimal("10"),"KARTA",pracownik);
        ExecutorService pool=Executors.newSingleThreadExecutor();
        CountDownLatch start=new CountDownLatch(1);
        try {
            Future<?> writer=pool.submit(()->{
                start.await();
                try(Connection c=TestDatabase.polacz();
                        PreparedStatement q=c.prepareStatement("UPDATE goscie SET imie=? WHERE id=?");
                        PreparedStatement u=c.prepareStatement("UPDATE uczestnicy_rezerwacji SET imie=? WHERE rezerwacja_id=?");
                        PreparedStatement w=c.prepareStatement("UPDATE platnosci SET kwota=? WHERE rezerwacja_id=?")) {
                    c.setAutoCommit(false);
                    for(int i=0;i<100;i++) {
                        String imie=i%2==0?"Wersja B":"Wersja A";
                        q.setString(1,imie);q.setInt(2,g.getId());q.executeUpdate();
                        u.setString(1,imie);u.setInt(2,r.getId());u.executeUpdate();
                        w.setBigDecimal(1,new BigDecimal(i%2==0?"20":"10"));w.setInt(2,r.getId());w.executeUpdate();
                        c.commit();
                    }
                }
                return null;
            });
            start.countDown();
            for(int i=0;i<20;i++) {
                var p=BazaDanych.pobierzPotwierdzenieRezerwacji(r.getId());
                boolean b=p.glownyGosc().startsWith("Wersja B");
                check(p.uczestnicy().get(0).startsWith(b?"Wersja B":"Wersja A")
                        && p.wplaty().compareTo(new BigDecimal(b?"20":"10"))==0,"Spójny obraz trzech tabel podczas równoczesnych zmian");
            }
            writer.get(30,TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); }
    }
}
