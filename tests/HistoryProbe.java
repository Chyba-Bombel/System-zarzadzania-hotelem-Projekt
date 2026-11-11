import pl.hotel.baza.BazaDanych;
import pl.hotel.model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class HistoryProbe {
    private static int assertions;
    private static void check(boolean ok, String opis) {
        assertions++;
        if (!ok) throw new AssertionError(opis);
    }
    @FunctionalInterface interface Akcja { void wykonaj() throws Exception; }
    private static void odmowa(Akcja akcja) throws Exception {
        try { akcja.wykonaj(); throw new AssertionError("Przyjęto niedozwoloną operację"); }
        catch (SQLException ok) { assertions++; }
    }
    private static List<ZmianaRezerwacji> historia(int id) throws Exception {
        return BazaDanych.pobierzHistorieRezerwacji(id,0).wpisy();
    }
    private static int rezerwacje() throws Exception {
        return BazaDanych.pobierzRezerwacje(BazaDanych.pobierzGosci(),BazaDanych.pobierzPokoje()).size();
    }
    public static void main(String[] args) throws Exception {
        TestDatabase.wymagajTestowejBazy();
        BazaDanych.wyloguj();
        try { historia(1); throw new AssertionError("Historia dostępna bez sesji"); }
        catch (SecurityException ok) { assertions++; }
        var admin=BazaDanych.zaloguj("admin","Admin123!");
        var g=BazaDanych.zapiszGoscia("Test","Historia","500100200","history@example.com");
        for(String nr:List.of("H100","H101"))BazaDanych.dodajPokoj(nr,"Historia",2,new BigDecimal("100"));
        Pokoj a=IntegrationProbe.pokoj("H100"),b=IntegrationProbe.pokoj("H101");
        LocalDate dzis=LocalDate.now();
        Rezerwacja r=BazaDanych.zapiszRezerwacje(g,a,dzis,dzis.plusDays(2),1,admin.getId());
        var pierwszy=historia(r.getId()).get(0);
        check(pierwszy.akcja().equals("UTWORZENIE") && pierwszy.przed()==null,"Utworzenie ze stanem początkowym");
        check(pierwszy.pracownikId()==admin.getId() && pierwszy.login().equals("admin"),"Tożsamość autora");
        check(pierwszy.po().contains("H100") && pierwszy.po().contains("100.00") && pierwszy.utworzono()!=null,"Dane początkowe i czas serwera");
        Rezerwacja przyszla=BazaDanych.zapiszRezerwacje(g,a,dzis.plusDays(10),dzis.plusDays(12),1,admin.getId());
        BazaDanych.zmienTerminRezerwacji(przyszla.getId(),dzis.plusDays(11),dzis.plusDays(13));
        var termin=historia(przyszla.getId()).get(0);
        check(termin.akcja().equals("ZMIANA_TERMINU") && termin.przed().contains(dzis.plusDays(10).toString())
                && termin.po().contains(dzis.plusDays(11).toString()),"Poprzedni i nowy termin");
        BazaDanych.zmienTerminRezerwacji(przyszla.getId(),dzis.plusDays(11),dzis.plusDays(13));
        check(historia(przyszla.getId()).size()==2,"Brak pozornej zmiany przy identycznym terminie");
        var recepcja=BazaDanych.zaloguj("recepcja","Recepcja123!");
        BazaDanych.zmienPokojRezerwacji(przyszla.getId(),a.getId(),b.getId());
        var pokoj=historia(przyszla.getId()).get(0);
        check(pokoj.akcja().equals("ZMIANA_POKOJU") && pokoj.przed().contains("H100") && pokoj.po().contains("H101"),"Poprzedni i nowy pokój");
        check(pokoj.pracownikId()==recepcja.getId() && pokoj.login().equals("recepcja"),"Autor zmiany różny od autora rezerwacji");
        odmowa(()->BazaDanych.zmienPokojRezerwacji(przyszla.getId(),a.getId(),b.getId()));
        check(historia(przyszla.getId()).size()==3,"Odmowa nie dodaje zdarzenia");
        BazaDanych.anulujRezerwacje(przyszla.getId());
        check(historia(przyszla.getId()).get(0).akcja().equals("ANULOWANIE"),"Historia anulowania");
        BazaDanych.zameldujGoscia(r.getId());
        var meldunek=historia(r.getId()).get(0);
        check(meldunek.akcja().equals("ZAMELDOWANIE") && meldunek.przed().contains("POTWIERDZONA")
                && meldunek.po().contains("ZAMELDOWANA"),"Historia zameldowania");
        odmowa(()->BazaDanych.wymeldujGoscia(r.getId()));
        check(historia(r.getId()).size()==2,"Nierozliczony pobyt nie daje historii wymeldowania");
        BazaDanych.zapiszWplate(r.getId(),new BigDecimal("200"),"KARTA",recepcja.getId());
        BazaDanych.wymeldujGoscia(r.getId());
        check(historia(r.getId()).get(0).akcja().equals("WYMELDOWANIE"),"Historia wymeldowania");
        check(historia(r.getId()).get(0).po().contains("ZAKONCZONA"),"Zapis końcowego statusu");
        odmowa(()->BazaDanych.pobierzHistorieRezerwacji(Integer.MAX_VALUE,0));
        odmowa(()->BazaDanych.pobierzHistorieRezerwacji(r.getId(),-1));
        BazaDanych.zaloguj("admin","Admin123!");
        Rezerwacja zmieniana=BazaDanych.zapiszRezerwacje(g,a,dzis.plusDays(20),dzis.plusDays(22),1,admin.getId());
        Rezerwacja live=BazaDanych.zapiszRezerwacje(g,b,dzis,dzis.plusDays(2),1,admin.getId());
        BazaDanych.zameldujGoscia(live.getId());
        BazaDanych.zapiszWplate(live.getId(),new BigDecimal("200"),"KARTA",admin.getId());
        try(Connection c=TestDatabase.polacz();Statement q=c.createStatement()) {
            q.execute("CREATE TRIGGER test_history BEFORE INSERT ON historia_rezerwacji FOR EACH ROW "
                    + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='wymuszony blad historii'");
        }
        try {
            int przed=rezerwacje();
            odmowa(()->BazaDanych.zapiszRezerwacje(g,a,dzis.plusDays(25),dzis.plusDays(26),1,admin.getId()));
            check(rezerwacje()==przed,"Błąd historii wycofuje utworzenie rezerwacji");
            odmowa(()->BazaDanych.zmienTerminRezerwacji(zmieniana.getId(),dzis.plusDays(21),dzis.plusDays(23)));
            check(IntegrationProbe.rezerwacja(zmieniana.getId()).getOd().equals(dzis.plusDays(20)),"Rollback terminu");
            odmowa(()->BazaDanych.zmienPokojRezerwacji(zmieniana.getId(),a.getId(),b.getId()));
            check(IntegrationProbe.rezerwacja(zmieniana.getId()).getPokoj().getId()==a.getId(),"Rollback pokoju");
            odmowa(()->BazaDanych.anulujRezerwacje(zmieniana.getId()));
            check(IntegrationProbe.rezerwacja(zmieniana.getId()).getStatus().equals("POTWIERDZONA"),"Rollback anulowania");
            odmowa(()->BazaDanych.wymeldujGoscia(live.getId()));
            check(IntegrationProbe.rezerwacja(live.getId()).getStatus().equals("ZAMELDOWANA")
                    && IntegrationProbe.pokoj("H101").getStan().equals("GOTOWY"),"Błąd historii wycofuje status i sprzątanie");
            check(historia(zmieniana.getId()).size()==1 && historia(live.getId()).size()==2,"Błędy nie zostawiają częściowej historii");
        } finally {
            try(Connection c=TestDatabase.polacz();Statement q=c.createStatement()){q.execute("DROP TRIGGER test_history");}
        }
        BazaDanych.wymeldujGoscia(live.getId());
        for(int i=0;i<105;i++) {
            int offset=i%2==0?21:20;
            BazaDanych.zmienTerminRezerwacji(zmieniana.getId(),dzis.plusDays(offset),dzis.plusDays(offset+2));
        }
        var strona=BazaDanych.pobierzHistorieRezerwacji(zmieniana.getId(),0);
        check(strona.wpisy().size()==100 && strona.maStarsze(),"Limit pierwszej strony");
        long cursor=strona.wpisy().get(99).id();
        BazaDanych.zmienTerminRezerwacji(zmieniana.getId(),dzis.plusDays(20),dzis.plusDays(22));
        var starsze=BazaDanych.pobierzHistorieRezerwacji(zmieniana.getId(),cursor);
        check(starsze.wpisy().size()==6 && !starsze.maStarsze(),"Starsza strona stabilna mimo nowego wpisu");
        Set<Long> ids=new HashSet<>();strona.wpisy().forEach(x->ids.add(x.id()));starsze.wpisy().forEach(x->ids.add(x.id()));
        check(ids.size()==106,"Brak duplikatów i pominięć na stronach");
        check(starsze.wpisy().get(5).akcja().equals("UTWORZENIE"),"Najstarszy wpis na końcu historii");
        check(BazaDanych.pobierzHistorieRezerwacji(zmieniana.getId(),starsze.wpisy().get(5).id()).wpisy().isEmpty(),"Pusta strona za końcem");
        System.out.println("HISTORY_OK: "+assertions+" asercji; autorzy, stany przed/po, transakcje, odmowy i stronicowanie.");
    }
}
