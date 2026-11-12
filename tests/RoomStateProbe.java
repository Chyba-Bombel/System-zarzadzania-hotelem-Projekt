import pl.hotel.baza.BazaDanych;
import pl.hotel.model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.concurrent.*;

public final class RoomStateProbe {
    private static int assertions;
    private static void check(boolean ok,String opis) {
        assertions++;
        if(!ok)throw new AssertionError(opis);
    }
    @FunctionalInterface interface Akcja { void run() throws Exception; }
    private static void odmowa(Akcja akcja) throws Exception {
        try { akcja.run();throw new AssertionError("Przyjęto niedozwoloną zmianę stanu"); }
        catch(SQLException ok){assertions++;}
    }
    private static Pokoj dodaj(String nr) throws Exception {
        BazaDanych.dodajPokoj(nr,"Stan",2,new BigDecimal("100"));return IntegrationProbe.pokoj(nr);
    }
    public static void main(String[] args) throws Exception {
        TestDatabase.wymagajTestowejBazy();
        BazaDanych.wyloguj();
        try{BazaDanych.wylaczPokoj(1,"GOTOWY");throw new AssertionError("Wyłączenie bez sesji");}catch(SecurityException ok){assertions++;}
        try{BazaDanych.przywrocPokoj(1,"WYLACZONY");throw new AssertionError("Przywrócenie bez sesji");}catch(SecurityException ok){assertions++;}
        var admin=BazaDanych.zaloguj("admin","Admin123!");
        Pokoj p=dodaj("D100");
        odmowa(()->BazaDanych.wylaczPokoj(0,"GOTOWY"));
        odmowa(()->BazaDanych.wylaczPokoj(Long.MAX_VALUE,"GOTOWY"));
        odmowa(()->BazaDanych.wylaczPokoj(p.getId(),null));
        odmowa(()->BazaDanych.wylaczPokoj(p.getId(),"GOTOWY');DROP TABLE pokoje;--"));
        odmowa(()->BazaDanych.przywrocPokoj(p.getId(),"GOTOWY"));
        var g=BazaDanych.zapiszGoscia("Test","Wyłączenie","500123456","room-state@example.com");
        Pokoj off=BazaDanych.wylaczPokoj(p.getId(),"GOTOWY");
        check(off.getStan().equals("WYLACZONY") && IntegrationProbe.pokoj("D100").getStan().equals("WYLACZONY"),"Wyłączenie zapisane w bazie");
        check(off.getId()==p.getId() && off.getLiczbaMiejsc()==p.getLiczbaMiejsc()
                && off.getCenaZaNoc().equals(p.getCenaZaNoc()),"Cena, pojemność i tożsamość pokoju zachowane");
        odmowa(()->BazaDanych.wylaczPokoj(p.getId(),"WYLACZONY"));
        odmowa(()->BazaDanych.przywrocPokoj(p.getId(),"GOTOWY"));
        LocalDate dzis=LocalDate.now();
        odmowa(()->BazaDanych.zapiszRezerwacje(g,p,dzis,dzis.plusDays(1),1,admin.getId()));
        BazaDanych.zaloguj("recepcja","Recepcja123!");
        try{BazaDanych.wylaczPokoj(p.getId(),"WYLACZONY");throw new AssertionError("Recepcja wyłącza pokój");}catch(SecurityException ok){assertions++;}
        try{BazaDanych.przywrocPokoj(p.getId(),"WYLACZONY");throw new AssertionError("Recepcja przywraca pokój");}catch(SecurityException ok){assertions++;}
        check(!BazaDanych.oznaczPokojJakoPosprzatany(p.getId()),"Sprzątanie nie obchodzi wyłączenia");
        BazaDanych.zaloguj("admin","Admin123!");
        Pokoj przywrocony=BazaDanych.przywrocPokoj(p.getId(),"WYLACZONY");
        check(przywrocony.getStan().equals("DO_SPRZATANIA"),"Przywrócenie nie ustawia GOTOWY");
        odmowa(()->BazaDanych.wylaczPokoj(p.getId(),"WYLACZONY"));
        var oczekujaca=BazaDanych.zapiszRezerwacje(g,przywrocony,dzis,dzis.plusDays(2),1,admin.getId());
        odmowa(()->BazaDanych.zameldujGoscia(oczekujaca.getId()));
        odmowa(()->BazaDanych.wylaczPokoj(p.getId(),"DO_SPRZATANIA"));
        check(IntegrationProbe.rezerwacja(oczekujaca.getId()).getStatus().equals("POTWIERDZONA"),"Odmowa wyłączenia nie zmienia rezerwacji");
        BazaDanych.anulujRezerwacje(oczekujaca.getId());
        BazaDanych.wylaczPokoj(p.getId(),"DO_SPRZATANIA");
        BazaDanych.przywrocPokoj(p.getId(),"WYLACZONY");
        check(BazaDanych.oznaczPokojJakoPosprzatany(p.getId()),"Przygotowanie po przywróceniu");
        var przyszla=BazaDanych.zapiszRezerwacje(g,p,dzis.plusDays(100),dzis.plusDays(102),1,admin.getId());
        odmowa(()->BazaDanych.wylaczPokoj(p.getId(),"GOTOWY"));
        BazaDanych.anulujRezerwacje(przyszla.getId());
        var live=BazaDanych.zapiszRezerwacje(g,p,dzis,dzis.plusDays(2),1,admin.getId());
        BazaDanych.zameldujGoscia(live.getId());
        odmowa(()->BazaDanych.wylaczPokoj(p.getId(),"GOTOWY"));
        BazaDanych.zapiszWplate(live.getId(),new BigDecimal("200"),"KARTA",admin.getId());
        BazaDanych.wymeldujGoscia(live.getId());
        BazaDanych.wylaczPokoj(p.getId(),"DO_SPRZATANIA");
        check(IntegrationProbe.rezerwacja(live.getId()).getStatus().equals("ZAKONCZONA"),"Archiwalny pobyt nie blokuje wyłączenia i pozostaje w bazie");
        BazaDanych.przywrocPokoj(p.getId(),"WYLACZONY");
        try(Connection c=TestDatabase.polacz();Statement q=c.createStatement()) {
            q.execute("CREATE TRIGGER test_room_state BEFORE UPDATE ON pokoje FOR EACH ROW BEGIN "
                    + "IF NEW.numer='D100' AND NEW.stan='WYLACZONY' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='test stanu'; END IF; END");
        }
        try {
            odmowa(()->BazaDanych.wylaczPokoj(p.getId(),"DO_SPRZATANIA"));
            check(IntegrationProbe.pokoj("D100").getStan().equals("DO_SPRZATANIA"),"Rollback błędnej zmiany stanu");
        } finally {try(Connection c=TestDatabase.polacz();Statement q=c.createStatement()){q.execute("DROP TRIGGER test_room_state");}}
        Pokoj wybrany=dodaj("D110");
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start=new CountDownLatch(1);
            Future<Boolean> offFuture=pool.submit(()->wylacz(wybrany,start));
            Future<Boolean> bookFuture=pool.submit(()->{
                var prac=BazaDanych.zaloguj("recepcja","Recepcja123!");start.await();
                try{BazaDanych.zapiszRezerwacje(g,wybrany,dzis.plusDays(10),dzis.plusDays(11),1,prac.getId());return true;}
                catch(SQLException e){if(!e.getMessage().contains("Pokój jest wył"))throw e;return false;}
            });
            start.countDown();
            boolean zamkniety=offFuture.get(20,TimeUnit.SECONDS),zarezerwowany=bookFuture.get(20,TimeUnit.SECONDS);
            check(zamkniety!=zarezerwowany,"Nowa rezerwacja i wyłączenie mają jednego zwycięzcę");
            check(!IntegrationProbe.pokoj("D110").getStan().equals("WYLACZONY") || BazaDanych.pobierzRezerwacje(BazaDanych.pobierzGosci(),BazaDanych.pobierzPokoje())
                    .stream().noneMatch(r->r.getPokoj().getId()==wybrany.getId() && r.getStatus().equals("POTWIERDZONA")),"Brak aktywnego pobytu w wyłączonym pokoju po wyścigu");
            Pokoj stary=dodaj("D111"),cel=dodaj("D112");
            var przenoszona=BazaDanych.zapiszRezerwacje(g,stary,dzis.plusDays(20),dzis.plusDays(22),1,admin.getId());
            CountDownLatch przeniesienie=new CountDownLatch(1);
            Future<Boolean> off2=pool.submit(()->wylacz(cel,przeniesienie));
            Future<Boolean> move=pool.submit(()->{
                BazaDanych.zaloguj("recepcja","Recepcja123!");przeniesienie.await();
                try{BazaDanych.zmienPokojRezerwacji(przenoszona.getId(),stary.getId(),cel.getId());return true;}
                catch(SQLException e){if(!e.getMessage().contains("wyłączony"))throw e;return false;}
            });
            przeniesienie.countDown();
            boolean offWin=off2.get(20,TimeUnit.SECONDS),moveWin=move.get(20,TimeUnit.SECONDS);
            check(offWin!=moveWin,"Przeniesienie i wyłączenie mają jednego zwycięzcę");
            check(!offWin || IntegrationProbe.rezerwacja(przenoszona.getId()).getPokoj().getId()==stary.getId(),"Odrzucone przeniesienie zachowuje stary pokój");
        } finally {pool.shutdownNow();}
        dodaj("D200");
        System.out.println("ROOM_STATE_OK: "+assertions+" asercji; role, rezerwacje, sprzątanie, stan nieaktualny, rollback i równoczesne zapisy.");
    }
    private static boolean wylacz(Pokoj p,CountDownLatch start) throws Exception {
        BazaDanych.zaloguj("admin","Admin123!");start.await();
        try{BazaDanych.wylaczPokoj(p.getId(),p.getStan());return true;}
        catch(SQLException e){if(!e.getMessage().contains("aktywną rezerwację"))throw e;return false;}
    }
}
