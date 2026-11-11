import pl.hotel.baza.BazaDanych;
import pl.hotel.model.*;
import pl.hotel.potwierdzenia.Potwierdzenia;
import pl.hotel.raporty.Raporty;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.concurrent.*;

public final class NotesProbe {
    private static int assertions;
    private static void check(boolean ok, String opis) {
        assertions++;
        if (!ok) throw new AssertionError(opis);
    }
    @FunctionalInterface interface Akcja { void run() throws Exception; }
    private static void odmowa(Akcja akcja) throws Exception {
        try { akcja.run(); throw new AssertionError("Przyjęto niedozwoloną notatkę"); }
        catch (SQLException | IllegalArgumentException ok) { assertions++; }
    }
    private static String tresc(int id) throws Exception { return BazaDanych.pobierzNotatkeRezerwacji(id).tresc(); }
    public static void main(String[] args) throws Exception {
        TestDatabase.wymagajTestowejBazy();
        BazaDanych.wyloguj();
        try { BazaDanych.pobierzNotatkeRezerwacji(1); throw new AssertionError("Odczyt bez sesji"); }
        catch (SecurityException ok) { assertions++; }
        try { BazaDanych.zapiszNotatkeRezerwacji(1,"","x"); throw new AssertionError("Zapis bez sesji"); }
        catch (SecurityException ok) { assertions++; }
        var admin = BazaDanych.zaloguj("admin","Admin123!");
        check(!BazaDanych.pobierzNotatkeRezerwacji(1).tresc().isEmpty(),"Odczyt istniejących uwag demonstracyjnych");
        var g = BazaDanych.zapiszGoscia("Jan","Notatka","500123456","notes@example.com");
        BazaDanych.dodajPokoj("N100","Notatki",2,new BigDecimal("100"));
        BazaDanych.dodajPokoj("N101","Notatki",2,new BigDecimal("100"));
        var pokoj = IntegrationProbe.pokoj("N100");
        var od = LocalDate.now().plusDays(7);
        var r = BazaDanych.zapiszRezerwacje(g,pokoj,od,od.plusDays(2),1,admin.getId());
        check(tresc(r.getId()).equals("") && BazaDanych.pobierzNotatkeRezerwacji(r.getId()).edytowalna(),"Nowa rezerwacja bez notatki");
        odmowa(() -> BazaDanych.pobierzNotatkeRezerwacji(0));
        odmowa(() -> BazaDanych.pobierzNotatkeRezerwacji(Integer.MAX_VALUE));
        odmowa(() -> BazaDanych.zapiszNotatkeRezerwacji(r.getId(),null,"x"));
        odmowa(() -> BazaDanych.zapiszNotatkeRezerwacji(r.getId(),"",null));
        odmowa(() -> BazaDanych.zapiszNotatkeRezerwacji(r.getId(),"","a".repeat(501)));
        odmowa(() -> BazaDanych.zapiszNotatkeRezerwacji(r.getId(),"","😀".repeat(501)));
        for (int cp : new int[]{0,27,127,133,0xD800,0xDC00}) {
            String zly = String.valueOf((char)cp);
            odmowa(() -> BazaDanych.zapiszNotatkeRezerwacji(r.getId(),"",zly));
        }
        check(tresc(r.getId()).isEmpty(),"Walidacja nie zmienia danych");
        String tekst = "Późny przyjazd o 23:00\nO'Connor\t<script>alert('x')</script> '); DROP TABLE goscie; --";
        BazaDanych.zapiszNotatkeRezerwacji(r.getId(),"",tekst);
        check(tresc(r.getId()).equals(tekst),"Unicode, nowe linie, tabulator i tekst SQL/HTML bez wykonania");
        check(BazaDanych.pobierzGosci().stream().anyMatch(x->x.getId()==g.getId()),"Tabela gości zachowana");
        BazaDanych.zapiszNotatkeRezerwacji(r.getId(),tekst,"Linia 1\r\nLinia 2\rLinia 3");
        check(tresc(r.getId()).equals("Linia 1\nLinia 2\nLinia 3"),"Ujednolicenie końców linii");
        String emoji = "😀".repeat(500);
        BazaDanych.zapiszNotatkeRezerwacji(r.getId(),tresc(r.getId()),emoji);
        check(tresc(r.getId()).equals(emoji),"500 znaków spoza BMP bez obcinania");
        try(Connection c=TestDatabase.polacz();PreparedStatement q=c.prepareStatement("SELECT CHAR_LENGTH(uwagi) FROM rezerwacje WHERE id=?")) {
            q.setInt(1,r.getId());try(ResultSet wynik=q.executeQuery()){wynik.next();check(wynik.getInt(1)==500,"Zgodność limitu z MySQL VARCHAR(500)");}
        }
        BazaDanych.zapiszNotatkeRezerwacji(r.getId(),emoji,"  \n\t");
        check(tresc(r.getId()).isEmpty(),"Pusta notatka usuwa treść");
        BazaDanych.zapiszNotatkeRezerwacji(r.getId(),"","Pierwsza");
        odmowa(() -> BazaDanych.zapiszNotatkeRezerwacji(r.getId(),"","Druga"));
        check(tresc(r.getId()).equals("Pierwsza"),"Nieaktualny edytor nie nadpisuje danych");
        odmowa(() -> BazaDanych.zapiszNotatkeRezerwacji(r.getId(),"pierwsza","Druga"));
        check(tresc(r.getId()).equals("Pierwsza"),"Porównanie wersji rozróżnia wielkość liter mimo collation SQL");
        BazaDanych.zapiszNotatkeRezerwacji(r.getId(),"Pierwsza","Wspólna");
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try {
            Future<Boolean> a=pool.submit(()->wyscig(r.getId(),"Wariant A","admin","Admin123!",start));
            Future<Boolean> b=pool.submit(()->wyscig(r.getId(),"Wariant B","recepcja","Recepcja123!",start));
            start.countDown();
            check((a.get(20,TimeUnit.SECONDS)?1:0)+(b.get(20,TimeUnit.SECONDS)?1:0)==1,"Jedna z równoczesnych edycji wygrywa");
            check(tresc(r.getId()).equals("Wariant A") || tresc(r.getId()).equals("Wariant B"),"Pełna treść zwycięskiej edycji");
        } finally { pool.shutdownNow(); }
        BazaDanych.zaloguj("recepcja","Recepcja123!");
        String poufna="NOTATKA_WEWNETRZNA_91827 — preferencja recepcji";
        BazaDanych.zapiszNotatkeRezerwacji(r.getId(),tresc(r.getId()),poufna);
        check(tresc(r.getId()).equals(poufna),"Edycja przez recepcję");
        int historia=BazaDanych.pobierzHistorieRezerwacji(r.getId(),0).wpisy().size();
        BazaDanych.zapiszNotatkeRezerwacji(r.getId(),poufna,poufna);
        check(BazaDanych.pobierzHistorieRezerwacji(r.getId(),0).wpisy().size()==historia,"Notatki nie zmieniają dziennika zdarzeń pobytu");
        var po=IntegrationProbe.rezerwacja(r.getId());
        check(po.getOd().equals(od) && po.getPokoj().getId()==pokoj.getId() && po.obliczCene().compareTo(new BigDecimal("200"))==0,"Notatka nie zmienia warunków pobytu");
        check(!Potwierdzenia.html(BazaDanych.pobierzPotwierdzenieRezerwacji(r.getId())).contains("NOTATKA_WEWNETRZNA_91827"),"Notatka wykluczona z potwierdzenia gościa");
        var raport=Raporty.generuj(od,od.plusDays(2));
        Path folder=Files.createTempDirectory("hotel-notes-test-");
        try {
            Path html=folder.resolve("raport.html");Raporty.zapiszHtml(raport,html);
            check(!Files.readString(html).contains("NOTATKA_WEWNETRZNA_91827"),"Notatka wykluczona z raportu HTML");
            for(int i=0;i<raport.tabele().size();i++) {
                Path csv=folder.resolve("raport_"+i+".csv");Raporty.zapiszCsv(raport.tabele().get(i),csv);
                check(!Files.readString(csv).contains("NOTATKA_WEWNETRZNA_91827"),"Notatka wykluczona z CSV");
            }
        } finally {
            try(var pliki=Files.walk(folder)){for(Path f:pliki.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(f);}
        }
        try(Connection c=TestDatabase.polacz();Statement q=c.createStatement()) {
            q.execute("CREATE TRIGGER test_notes_fail BEFORE UPDATE ON rezerwacje FOR EACH ROW BEGIN "
                    + "IF NEW.uwagi='FAIL_NOTATKA' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='test notatki'; END IF; END");
        }
        try {
            odmowa(() -> BazaDanych.zapiszNotatkeRezerwacji(r.getId(),poufna,"FAIL_NOTATKA"));
            check(tresc(r.getId()).equals(poufna),"Wycofanie błędnego zapisu");
        } finally {try(Connection c=TestDatabase.polacz();Statement q=c.createStatement()){q.execute("DROP TRIGGER test_notes_fail");}}
        BazaDanych.anulujRezerwacje(r.getId());
        check(!BazaDanych.pobierzNotatkeRezerwacji(r.getId()).edytowalna() && tresc(r.getId()).equals(poufna),"Anulowana rezerwacja zachowuje notatkę do odczytu");
        odmowa(() -> BazaDanych.zapiszNotatkeRezerwacji(r.getId(),poufna,"Po anulowaniu"));
        var recepcja=BazaDanych.zaloguj("recepcja","Recepcja123!");
        var live=BazaDanych.zapiszRezerwacje(g,IntegrationProbe.pokoj("N101"),LocalDate.now(),LocalDate.now().plusDays(2),1,recepcja.getId());
        BazaDanych.zameldujGoscia(live.getId());
        BazaDanych.zapiszNotatkeRezerwacji(live.getId(),"","Prośba w trakcie pobytu");
        check(tresc(live.getId()).equals("Prośba w trakcie pobytu"),"Edycja zameldowanego pobytu");
        BazaDanych.zapiszWplate(live.getId(),new BigDecimal("200"),"KARTA",recepcja.getId());
        BazaDanych.wymeldujGoscia(live.getId());
        check(!BazaDanych.pobierzNotatkeRezerwacji(live.getId()).edytowalna(),"Zakończona rezerwacja tylko do odczytu");
        odmowa(() -> BazaDanych.zapiszNotatkeRezerwacji(live.getId(),"Prośba w trakcie pobytu","Zmiana archiwum"));
        System.out.println("NOTES_OK: "+assertions+" asercji; walidacja Unicode, konkurencyjna edycja, role, statusy, prywatność eksportów i rollback.");
    }
    private static boolean wyscig(int id,String tekst,String login,String haslo,CountDownLatch start) throws Exception {
        BazaDanych.zaloguj(login,haslo);start.await();
        try { BazaDanych.zapiszNotatkeRezerwacji(id,"Wspólna",tekst);return true; }
        catch(SQLException e) { if(!e.getMessage().contains("innym stanowisku"))throw e;return false; }
    }
}
