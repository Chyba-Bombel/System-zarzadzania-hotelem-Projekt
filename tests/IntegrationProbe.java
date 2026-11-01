import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.concurrent.*;

/** Test scenariuszy na izolowanej bazie; uruchamiany ręcznie przez weryfikację paczki. */
public class IntegrationProbe {
    @FunctionalInterface interface Operacja { void run() throws Exception; }
    static void check(boolean warunek,String opis) { if(!warunek)throw new AssertionError(opis); }
    static void odmowa(Operacja p) throws Exception {
        try {p.run();throw new AssertionError("Operacja miała zostać odrzucona");}catch(SQLException oczekiwany){}
    }
    static Pokoj pokoj(String numer) throws Exception {return BazaDanych.pobierzPokoje().stream().filter(p->p.getNumer().equals(numer)).findFirst().orElseThrow();}
    static Rezerwacja rezerwacja(int id) throws Exception {
        return BazaDanych.pobierzRezerwacje(BazaDanych.pobierzGosci(),BazaDanych.pobierzPokoje()).stream().filter(r->r.getId()==id).findFirst().orElseThrow();
    }
    public static void main(String[] args) throws Exception {
        Pracownik admin=BazaDanych.zaloguj("admin","Admin123!");check(admin!=null,"Admin login");
        check(BazaDanych.zaloguj("admin","zle")==null,"Złe hasło");
        GuiRepository.sprawdzSesje(admin,true);
        var recepcja=BazaDanych.zaloguj("recepcja","Recepcja123!");
        try{GuiRepository.sprawdzSesje(recepcja,true);throw new AssertionError("Recepcja uzyskała rolę admin");}catch(SecurityException ok){}
        Gosc g=BazaDanych.zapiszGoscia("Jan","O'Connor","500100200","probe@example.com");
        check(BazaDanych.edytujGoscia(g.getId(),"Jan","Testowy","123456789","changed@example.com"),"Guest edit");
        BazaDanych.dodajPokoj("T900","Test",2,new BigDecimal("100.00"));Pokoj p=pokoj("T900");
        odmowa(()->BazaDanych.dodajPokoj("T900","Test",2,new BigDecimal("100.00")));
        LocalDate dzis=LocalDate.now();
        Rezerwacja r=BazaDanych.zapiszRezerwacje(g,p,dzis,dzis.plusDays(2),2,admin.getId());
        odmowa(()->BazaDanych.zapiszRezerwacje(g,p,dzis,dzis.plusDays(1),2,admin.getId()));
        BazaDanych.zapiszWplate(r.getId(),new BigDecimal("50.00"),"GOTOWKA",admin.getId());
        odmowa(()->BazaDanych.zapiszWplate(r.getId(),new BigDecimal("151.00"),"KARTA",admin.getId()));
        check(BazaDanych.pobierzRozliczenie(r.getId()).saldo().compareTo(new BigDecimal("150"))==0,"Saldo");
        BazaDanych.zameldujGoscia(r.getId());odmowa(()->BazaDanych.wymeldujGoscia(r.getId()));
        check(rezerwacja(r.getId()).getStatus().equals("ZAMELDOWANA"),"Rollback wymeldowania");
        check(pokoj("T900").getStan().equals("GOTOWY"),"Rollback stanu pokoju");
        BazaDanych.zapiszWplate(r.getId(),new BigDecimal("150.00"),"KARTA",admin.getId());
        BazaDanych.wymeldujGoscia(r.getId());check(pokoj("T900").getStan().equals("DO_SPRZATANIA"),"Sprzątanie");
        check(BazaDanych.oznaczPokojJakoPosprzatany(p.getId()),"Gotowy");
        Rezerwacja b=BazaDanych.zapiszRezerwacje(g,p,dzis.plusDays(3),dzis.plusDays(6),2,admin.getId());
        BazaDanych.zapiszWplate(b.getId(),new BigDecimal("250"),"PRZELEW",admin.getId());
        odmowa(()->BazaDanych.zmienTerminRezerwacji(b.getId(),dzis.plusDays(3),dzis.plusDays(4)));
        check(rezerwacja(b.getId()).liczbaNocy()==3,"Rollback dat");
        BazaDanych.zapiszZwrot(b.getId(),new BigDecimal("150"),"PRZELEW",admin.getId());
        BazaDanych.zmienTerminRezerwacji(b.getId(),dzis.plusDays(3),dzis.plusDays(4));
        odmowa(()->BazaDanych.anulujRezerwacje(b.getId()));
        BazaDanych.zapiszZwrot(b.getId(),new BigDecimal("100"),"PRZELEW",admin.getId());
        check(BazaDanych.anulujRezerwacje(b.getId()),"Anulowanie");
        odmowa(()->BazaDanych.zapiszZwrot(b.getId(),new BigDecimal("1"),"KARTA",admin.getId()));
        BazaDanych.dodajPracownika("Test","Konto","probe_user","ProbeHaslo123!","RECEPCJA");
        var konto=BazaDanych.zaloguj("probe_user","ProbeHaslo123!");check(konto!=null,"Nowe konto");
        BazaDanych.zmienHaslo(konto.getId(),"ProbeHaslo123!","NoweHaslo123!");
        check(BazaDanych.zaloguj("probe_user","ProbeHaslo123!")==null,"Stare hasło");
        check(BazaDanych.zaloguj("probe_user","NoweHaslo123!")!=null,"Nowe hasło");
        odmowa(()->BazaDanych.dezaktywujPracownika(admin.getId(),admin.getId()));
        BazaDanych.dezaktywujPracownika(konto.getId(),admin.getId());
        check(BazaDanych.zaloguj("probe_user","NoweHaslo123!")==null,"Dezaktywacja");
        // Równoczesne próby rezerwacji tego samego terminu.
        BazaDanych.dodajPokoj("T901","Test",2,new BigDecimal("100"));Pokoj p2=pokoj("T901");
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        Callable<Boolean> zapisz=()->{start.await();try{BazaDanych.zapiszRezerwacje(g,p2,dzis.plusDays(10),dzis.plusDays(12),2,1);return true;}catch(SQLException e){if(!e.getMessage().contains("zajęty"))throw e;return false;}};
        var a=pool.submit(zapisz);var c=pool.submit(zapisz);start.countDown();
        check((a.get()?1:0)+(c.get()?1:0)==1,"Dwie równoczesne rezerwacje");pool.shutdownNow();
        // Brak zmiany ceny historycznej przy edycji pokoju.
        BazaDanych.edytujPokoj(p.getId(),"T900","Test",new BigDecimal("999"));
        check(rezerwacja(r.getId()).obliczCene().compareTo(new BigDecimal("200"))==0,"Cena historyczna");
        // Pełny zapis i edycja dodatkowych osób oraz wycofanie obu tabel po błędzie.
        BazaDanych.dodajPokoj("T902","Rodzinny",4,new BigDecimal("300")); Pokoj rodzinny=pokoj("T902");
        var osoby=java.util.List.of(new Uczestnik("Łucja", "O'Connor"), new Uczestnik("Jan", "Nowak"));
        Rezerwacja grupa=BazaDanych.zapiszRezerwacje(g,rodzinny,dzis.plusDays(20),dzis.plusDays(22),3,1,osoby);
        check(rezerwacja(grupa.getId()).getUczestnicy().equals(osoby),"Trwały odczyt nazwisk Unicode/apostrof");
        var nowe=java.util.List.of(new Uczestnik("Anna", "Pruska"),new Uczestnik("Piotr", "Wencel"));
        BazaDanych.zmienUczestnikow(grupa.getId(),nowe);
        check(rezerwacja(grupa.getId()).getUczestnicy().equals(nowe),"Edycja gości");
        odmowa(()->BazaDanych.zmienUczestnikow(grupa.getId(),java.util.List.of()));
        check(rezerwacja(grupa.getId()).getUczestnicy().equals(nowe),"Odrzucona liczba osób zachowuje dane");
        try { BazaDanych.zapiszRezerwacje(g,rodzinny,dzis.plusDays(25),dzis.plusDays(26),3,1,java.util.List.of());
            throw new AssertionError("Brakujące nazwiska przyjęte"); } catch (IllegalArgumentException ok) {}
        try { new Uczestnik(" ","Nowak"); throw new AssertionError("Puste imię przyjęte"); } catch (IllegalArgumentException ok) {}
        // Wymuszony błąd drugiego INSERT pozwala sprawdzić atomowość, nie tylko walidację przed zapisem.
        try(var con=BazaDanych.polacz();var st=con.createStatement()) {
            st.execute("CREATE TRIGGER test_uczestnicy BEFORE INSERT ON uczestnicy_rezerwacji FOR EACH ROW "
                + "BEGIN IF NEW.imie='FAIL' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='test rollback'; END IF; END");
        }
        var bledne=java.util.List.of(new Uczestnik("Anna","Test"),new Uczestnik("FAIL","Test"));
        odmowa(()->BazaDanych.zmienUczestnikow(grupa.getId(),bledne));
        check(rezerwacja(grupa.getId()).getUczestnicy().equals(nowe),"Rollback usunięcia i częściowego zapisu osób");
        int przed=BazaDanych.pobierzRezerwacje(BazaDanych.pobierzGosci(),BazaDanych.pobierzPokoje()).size();
        odmowa(()->BazaDanych.zapiszRezerwacje(g,rodzinny,dzis.plusDays(25),dzis.plusDays(26),3,1,bledne));
        check(BazaDanych.pobierzRezerwacje(BazaDanych.pobierzGosci(),BazaDanych.pobierzPokoje()).size()==przed,"Rollback nowej rezerwacji z osobami");
        try(var con=BazaDanych.polacz();var st=con.createStatement()){st.execute("DROP TRIGGER test_uczestnicy");}
        BazaDanych.anulujRezerwacje(grupa.getId());
        odmowa(()->BazaDanych.zmienUczestnikow(grupa.getId(),osoby));
        System.out.println("PARTICIPANTS_OK: zapis, odczyt, edycja, walidacja, historia i rollback obu tabel.");
        System.out.println("INTEGRATION_OK: odczyt/zapis, terminy, rollback, płatności, pobyt, role, konta i blokada konkurencyjnych rezerwacji.");
    }
}
