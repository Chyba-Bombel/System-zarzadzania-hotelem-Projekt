import pl.hotel.baza.BazaDanych;
import pl.hotel.model.*;
import pl.hotel.potwierdzenia.Potwierdzenia;
import pl.hotel.raporty.Raporty;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static pl.hotel.model.FiltrRezerwacji.TrybDat.*;
import static pl.hotel.model.FiltrRezerwacji.StanRozliczenia.*;
public final class ServicesSearchProbe {
    private static int assertions;
    private static long admin;
    private static Gosc g;
    @FunctionalInterface interface Akcja {void wykonaj()throws Exception;}
    private static void check(boolean ok,String opis){assertions++;if(!ok)throw new AssertionError(opis);}
    private static BigDecimal kw(String s){return new BigDecimal(s);}
    private static void odmowa(Akcja a)throws Exception {try{a.wykonaj();throw new AssertionError("Przyjęto niedozwoloną operację");}catch(SQLException|IllegalArgumentException|SecurityException ok){assertions++;}}
    private static Rezerwacja pobyt(String nr,LocalDate d,int noce)throws Exception {
        BazaDanych.dodajPokoj(nr,"Usługi",2,kw("100"));return BazaDanych.zapiszRezerwacje(g,IntegrationProbe.pokoj(nr),d,d.plusDays(noce),2,admin,List.of(new Uczestnik("Anna","Pruska%_=")));
    }
    private static FiltrRezerwacji filtr(String n,String p,LocalDate a,LocalDate b,FiltrRezerwacji.TrybDat t,FiltrRezerwacji.StanRozliczenia s){return new FiltrRezerwacji(n,p,"",a,b,t,s);}
    private static List<WynikRezerwacji> szukaj(FiltrRezerwacji f)throws Exception{return BazaDanych.wyszukajRezerwacje(f,0).wyniki();}
    private static List<ZmianaRezerwacji> historia(int id)throws Exception{return BazaDanych.pobierzHistorieRezerwacji(id,0).wpisy();}
    private static void wyscig(Akcja a,Akcja b)throws Exception {
        var pula=Executors.newFixedThreadPool(2);var gotowe=new CountDownLatch(2);var start=new CountDownLatch(1);List<Future<Boolean>> wyniki=new ArrayList<>();
        try{
            for(var x:List.of(a,b))wyniki.add(pula.submit(()->{BazaDanych.zaloguj("admin","Admin123!");gotowe.countDown();start.await();try{x.wykonaj();return true;}catch(SQLException ok){return false;}finally{BazaDanych.wyloguj();}}));
            check(gotowe.await(20,TimeUnit.SECONDS),"Sesje równoczesne");start.countDown();boolean x=wyniki.get(0).get(30,TimeUnit.SECONDS),y=wyniki.get(1).get(30,TimeUnit.SECONDS);check(x!=y,"Dokładnie jedna kolidująca operacja zatwierdzona");
        }finally{start.countDown();pula.shutdownNow();}
    }
    public static void main(String[] args)throws Exception {
        TestDatabase.wymagajTestowejBazy();BazaDanych.wyloguj();odmowa(()->BazaDanych.pobierzKatalogUslug(false));odmowa(()->szukaj(FiltrRezerwacji.wszystkie()));
        admin=BazaDanych.zaloguj("admin","Admin123!").getId();var dzis=LocalDate.now();var d=dzis.plusDays(100);
        g=BazaDanych.zapiszGoscia("Maria","Naz%_=' OR 1=1","500123456","services@example.com");
        check(BazaDanych.pobierzKatalogUslug(false).size()>=3,"Przykładowy katalog");
        odmowa(()->BazaDanych.zapiszUsluge(null,"","doba",kw("10"),true));odmowa(()->BazaDanych.zapiszUsluge(null,"Zła cena","doba",kw("0"),true));odmowa(()->BazaDanych.zapiszUsluge(null,"Zła cena","doba",kw("1.001"),true));
        var stara=BazaDanych.zapiszUsluge(null,"Test <script>alert('x')</script>","porcja",kw("25"),true);var r=pobyt("US100",d,2);
        odmowa(()->BazaDanych.dodajUslugeDoRezerwacji(r.getId(),stara,0,admin));odmowa(()->BazaDanych.dodajUslugeDoRezerwacji(r.getId(),stara,1001,admin));
        var line=BazaDanych.dodajUslugeDoRezerwacji(r.getId(),stara,2,admin);check(BazaDanych.pobierzRozliczenie(r.getId()).naleznosc().compareTo(kw("250"))==0,"Suma noclegów i usług");
        var nowa=BazaDanych.zapiszUsluge(stara,"Śniadanie po zmianie","porcja",kw("35"),true);
        odmowa(()->BazaDanych.zapiszUsluge(stara,"Nadpisanie","szt.",kw("1"),true));odmowa(()->BazaDanych.dodajUslugeDoRezerwacji(r.getId(),stara,1,admin));
        var stare=BazaDanych.pobierzUslugiRezerwacji(r.getId()).pozycje().get(0);check(stare.nazwa().equals(stara.nazwa())&&stare.cena().compareTo(kw("25"))==0,"Nazwa i cena historyczna");
        var druga=BazaDanych.dodajUslugeDoRezerwacji(r.getId(),nowa,1,admin);var off=BazaDanych.zapiszUsluge(nowa,nowa.nazwa(),nowa.jednostka(),nowa.cena(),false);
        odmowa(()->BazaDanych.dodajUslugeDoRezerwacji(r.getId(),off,1,admin));check(BazaDanych.pobierzKatalogUslug(false).stream().noneMatch(u->u.id()==off.id()),"Nieaktywna ukryta");
        var karta=BazaDanych.pobierzPotwierdzenieRezerwacji(r.getId());check(karta.naleznosc().compareTo(kw("285"))==0,"Usługi na karcie");check(Potwierdzenia.html(karta).contains("&lt;script&gt;")&&!Potwierdzenia.html(karta).contains("<script>"),"HTML escape usługi");
        check(Raporty.generuj(dzis,dzis).tabele().get(4).wiersze().stream().anyMatch(w->w.get(0).equals(""+line.id())&&w.get(6).equals("50,00")),"Raport usług po dacie dodania");
        BazaDanych.anulujUslugeRezerwacji(r.getId(),druga.id());odmowa(()->BazaDanych.anulujUslugeRezerwacji(r.getId(),druga.id()));
        BazaDanych.zapiszWplate(r.getId(),kw("250"),"KARTA",admin);odmowa(()->BazaDanych.anulujUslugeRezerwacji(r.getId(),line.id()));
        check(historia(r.getId()).get(0).akcja().equals("WPLATA"),"Odmowa nie dopisuje historii");
        BazaDanych.zapiszZwrot(r.getId(),kw("50"),"KARTA",admin);BazaDanych.anulujUslugeRezerwacji(r.getId(),line.id());
        var parking=BazaDanych.zapiszUsluge(null,"Test parking","doba",kw("10"),true);var postoj=BazaDanych.dodajUslugeDoRezerwacji(r.getId(),parking,10,admin);
        BazaDanych.zmienTerminRezerwacji(r.getId(),d,d.plusDays(1));check(BazaDanych.pobierzRozliczenie(r.getId()).naleznosc().compareTo(kw("200"))==0,"Zmiana dat uwzględnia usługi");
        check(BazaDanych.pobierzUslugiRezerwacji(r.getId()).pozycje().stream().filter(u->u.id()==postoj.id()).findFirst().orElseThrow().ilosc()==10,"Stała ilość po zmianie dat");
        check(IntegrationProbe.rezerwacja(r.getId()).getSaldo().signum()==0,"Saldo na głównej liście");
        var f=filtr("Pruska%_=","US100",d,d,POBYT,ROZLICZONA);check(szukaj(f).size()==1,"Nazwisko dodatkowe, daty, pokój i saldo");
        check(szukaj(filtr("Naz%_=' OR 1=1","US100",null,null,POBYT,WSZYSTKIE)).size()==1,"Dosłowny SQL i LIKE w nazwisku");
        check(szukaj(filtr("' OR 1=1 --","",null,null,POBYT,WSZYSTKIE)).isEmpty(),"Próba SQL nie rozszerza wyników");
        check(szukaj(filtr("","US100",d.plusDays(1),d.plusDays(1),POBYT,WSZYSTKIE)).isEmpty(),"Wyjazd nie jest noclegiem");
        check(szukaj(filtr("","US100",d.plusDays(1),d.plusDays(1),WYJAZD,WSZYSTKIE)).size()==1,"Wyjazd włącznie z granicą");
        check(szukaj(filtr("","US100",d,d,PRZYJAZD,WSZYSTKIE)).size()==1,"Przyjazd włącznie z granicą");
        odmowa(()->BazaDanych.wyszukajRezerwacje(null,0));odmowa(()->filtr("","",d,d.minusDays(1),POBYT,WSZYSTKIE));
        BazaDanych.zapiszNotatkeRezerwacji(r.getId(),"","Późny przyjazd");BazaDanych.zmienUczestnikow(r.getId(),List.of(new Uczestnik("Jan","Nowy")));
        check(historia(r.getId()).get(0).akcja().equals("ZMIANA_UCZESTNIKOW"),"Historia osób");
        int przed=historia(r.getId()).size();
        try(Connection c=TestDatabase.polacz();Statement q=c.createStatement()){q.execute("CREATE TRIGGER test_service_history BEFORE INSERT ON historia_rezerwacji FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='test rollback'");}
        try{
            odmowa(()->BazaDanych.zapiszZwrot(r.getId(),kw("1"),"KARTA",admin));
            odmowa(()->BazaDanych.dodajUslugeDoRezerwacji(r.getId(),parking,1,admin));
            odmowa(()->BazaDanych.zapiszNotatkeRezerwacji(r.getId(),"Późny przyjazd","Niezapisana"));
            odmowa(()->BazaDanych.zmienUczestnikow(r.getId(),List.of(new Uczestnik("Ewa","Niezapisana"))));
            check(BazaDanych.pobierzRozliczenie(r.getId()).zaplaconoNetto().compareTo(kw("200"))==0,"Rollback zwrotu");
            check(BazaDanych.pobierzNotatkeRezerwacji(r.getId()).tresc().equals("Późny przyjazd")&&IntegrationProbe.rezerwacja(r.getId()).getUczestnicy().get(0).nazwisko().equals("Nowy"),"Rollback notatki i osób");
            check(historia(r.getId()).size()==przed,"Brak częściowej historii");
        }finally{try(Connection c=TestDatabase.polacz();Statement q=c.createStatement()){q.execute("DROP TRIGGER test_service_history");}}
        odmowa(()->BazaDanych.anulujRezerwacje(r.getId()));BazaDanych.zapiszZwrot(r.getId(),kw("200"),"KARTA",admin);BazaDanych.anulujRezerwacje(r.getId());
        check(BazaDanych.pobierzUslugiRezerwacji(r.getId()).pozycje().stream().noneMatch(PozycjaUslugi::aktywna),"Anulowanie pobytu anuluje usługi");
        odmowa(()->BazaDanych.dodajUslugeDoRezerwacji(r.getId(),parking,1,admin));
        var live=pobyt("US101",dzis,1);BazaDanych.zameldujGoscia(live.getId());var dodana=BazaDanych.dodajUslugeDoRezerwacji(live.getId(),parking,3,admin);BazaDanych.zapiszWplate(live.getId(),kw("100"),"KARTA",admin);
        odmowa(()->BazaDanych.wymeldujGoscia(live.getId()));BazaDanych.zapiszWplate(live.getId(),kw("30"),"KARTA",admin);BazaDanych.wymeldujGoscia(live.getId());odmowa(()->BazaDanych.anulujUslugeRezerwacji(live.getId(),dodana.id()));
        var recepcja=BazaDanych.zaloguj("recepcja","Recepcja123!");odmowa(()->BazaDanych.zapiszUsluge(null,"Zakazana","szt.",kw("1"),true));
        odmowa(()->BazaDanych.dodajUslugeDoRezerwacji(r.getId(),parking,1,admin));BazaDanych.zaloguj("admin","Admin123!");
        var race=pobyt("US102",d.plusDays(20),1);var rl=BazaDanych.dodajUslugeDoRezerwacji(race.getId(),parking,4,admin);
        wyscig(()->BazaDanych.zapiszWplate(race.getId(),kw("140"),"KARTA",admin),()->BazaDanych.anulujUslugeRezerwacji(race.getId(),rl.id()));
        check(BazaDanych.pobierzRozliczenie(race.getId()).saldo().signum()>=0,"Wyścig nie tworzy nadpłaty");
        var live2=pobyt("US103",dzis,1);BazaDanych.zameldujGoscia(live2.getId());BazaDanych.zapiszWplate(live2.getId(),kw("100"),"KARTA",admin);
        wyscig(()->BazaDanych.dodajUslugeDoRezerwacji(live2.getId(),parking,1,admin),()->BazaDanych.wymeldujGoscia(live2.getId()));
        var saldo=BazaDanych.pobierzRozliczenie(live2.getId());check(!saldo.status().equals("ZAKONCZONA")||saldo.saldo().signum()==0,"Brak nieopłaconej usługi w zamkniętym pobycie");
        BazaDanych.dodajPokoj("SZPAGE","Wyszukiwanie",1,kw("100"));var pokoj=IntegrationProbe.pokoj("SZPAGE");
        try(Connection c=TestDatabase.polacz();PreparedStatement q=c.prepareStatement("INSERT INTO rezerwacje(gosc_id,pokoj_id,pracownik_id,data_od,data_do,liczba_osob,cena_za_noc,status) VALUES(?,?,?,?,?,1,100,'ANULOWANA')")){
            for(int i=0;i<105;i++){q.setInt(1,g.getId());q.setLong(2,pokoj.getId());q.setLong(3,admin);q.setDate(4,java.sql.Date.valueOf(d));q.setDate(5,java.sql.Date.valueOf(d.plusDays(1)));q.addBatch();}q.executeBatch();
        }
        var pf=filtr("","SZPAGE",null,null,POBYT,WSZYSTKIE);var s=BazaDanych.wyszukajRezerwacje(pf,0);check(s.wyniki().size()==100&&s.maStarsze(),"Pierwsza strona 100 wyników");
        BazaDanych.zapiszRezerwacje(g,pokoj,d.plusDays(5),d.plusDays(6),1,admin);
        var dalej=BazaDanych.wyszukajRezerwacje(pf,s.wyniki().get(99).id());check(dalej.wyniki().size()==5&&!dalej.maStarsze(),"Starsza strona bez przesunięcia po nowym zapisie");
        var ids=new HashSet<Integer>();for(var w:s.wyniki())ids.add(w.id());for(var w:dalej.wyniki())check(ids.add(w.id()),"Bez duplikatów");
        try(Connection c=TestDatabase.polacz();PreparedStatement q=c.prepareStatement("INSERT INTO historia_rezerwacji(rezerwacja_id,pracownik_id,login_pracownika,akcja,przed,po,utworzono) VALUES(?,?,'admin','ZMIANA_NOTATKI','a','b','2000-01-01 00:00:00')")){
            for(int i=0;i<105;i++){q.setInt(1,r.getId());q.setLong(2,admin);q.addBatch();}q.executeBatch();
        }
        var hs=BazaDanych.pobierzHistorieRezerwacji(r.getId(),0);check(hs.wpisy().get(0).akcja().equals("ANULOWANIE")&&hs.maStarsze(),"Starsza data ważniejsza od nowego ID historii");
        var hd=BazaDanych.pobierzHistorieRezerwacji(r.getId(),hs.wpisy().get(99).id());check(!hd.wpisy().isEmpty()&&!hd.maStarsze(),"Historia po dacie i ID");
        BazaDanych.zaloguj("recepcja","Recepcja123!");check(!szukaj(pf).isEmpty(),"Wyszukiwanie recepcji");
        System.out.println("SERVICES_SEARCH_OK: "+assertions+" asercji; ceny historyczne, historia, role, rollback, saldo, karta, raporty, daty, SQL/LIKE, stronicowanie i wyścigi.");
    }
}
