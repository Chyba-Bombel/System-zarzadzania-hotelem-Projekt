import pl.hotel.baza.BazaDanych;
import pl.hotel.model.*;
import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

public final class GuestArchiveProbe {
    private static int assertions;
    @FunctionalInterface interface Akcja{void wykonaj()throws Exception;}
    private static void check(boolean ok,String opis){assertions++;if(!ok)throw new AssertionError(opis);}
    private static void odmowa(Akcja a)throws Exception{
        try{a.wykonaj();throw new AssertionError("Przyjęto niedozwoloną operację archiwum");}
        catch(SQLException|SecurityException ok){assertions++;}
    }
    private static Gosc gosc(int id)throws Exception{return BazaDanych.pobierzGosci().stream().filter(g->g.getId()==id).findFirst().orElseThrow();}
    private static void wyscig(Akcja a,Akcja b)throws Exception{
        var gotowe=new CountDownLatch(2);var start=new CountDownLatch(1);var pula=Executors.newFixedThreadPool(2);List<Future<Boolean>> wyniki=new ArrayList<>();
        try{
            for(var x:List.of(a,b))wyniki.add(pula.submit(()->{BazaDanych.zaloguj("admin","Admin123!");gotowe.countDown();start.await();try{x.wykonaj();return true;}catch(SQLException ok){return false;}finally{BazaDanych.wyloguj();}}));
            check(gotowe.await(20,TimeUnit.SECONDS),"Sesje wątków");start.countDown();
            boolean x=wyniki.get(0).get(30,TimeUnit.SECONDS),y=wyniki.get(1).get(30,TimeUnit.SECONDS);check(x!=y,"Archiwizacja i nowa rezerwacja nie mogą zatwierdzić się razem");
        }finally{start.countDown();pula.shutdownNow();}
    }
    public static void main(String[] args)throws Exception{
        TestDatabase.wymagajTestowejBazy();BazaDanych.wyloguj();odmowa(()->BazaDanych.archiwizujGoscia(1));odmowa(()->BazaDanych.przywrocGoscia(1));
        long admin=BazaDanych.zaloguj("admin","Admin123!").getId();
        var g=BazaDanych.zapiszGoscia("Jan","Archiwalny","500123456","archive@example.com");
        check(!gosc(g.getId()).isArchiwalny()&&gosc(g.getId()).getArchiwizowano()==null,"Nowy gość aktywny");
        for(var nr:List.of("AR100","AR101"))BazaDanych.dodajPokoj(nr,"Archiwum",2,new BigDecimal("100"));
        var pokoj=IntegrationProbe.pokoj("AR100");var d=LocalDate.now().plusDays(210);
        var r=BazaDanych.zapiszRezerwacje(g,pokoj,d,d.plusDays(1),1,admin);
        odmowa(()->BazaDanych.archiwizujGoscia(g.getId()));check(!gosc(g.getId()).isArchiwalny(),"Potwierdzony pobyt blokuje archiwum");
        BazaDanych.anulujRezerwacje(r.getId());var historia=BazaDanych.pobierzHistorieRezerwacji(r.getId(),0).wpisy();
        var recepcja=BazaDanych.zaloguj("recepcja","Recepcja123!");BazaDanych.archiwizujGoscia(g.getId());
        var arch=gosc(g.getId());check(arch.isArchiwalny()&&arch.getArchiwizowano()!=null&&arch.getArchiwizowalLogin().equals("recepcja"),"Archiwizacja recepcji z czasem i autorem");
        check(arch.getImie().equals(g.getImie())&&arch.getEmail().equals(g.getEmail()),"Kontakty zachowane");
        check(IntegrationProbe.rezerwacja(r.getId()).getGosc().isArchiwalny(),"Archiwalny gość nadal rozwiązuje relację rezerwacji");
        check(BazaDanych.pobierzHistorieRezerwacji(r.getId(),0).wpisy().equals(historia),"Historia rezerwacji nie jest modyfikowana");
        check(BazaDanych.pobierzPotwierdzenieRezerwacji(r.getId()).glownyGosc().contains("Archiwalny"),"Karta zachowuje gościa");
        check(BazaDanych.wyszukajRezerwacje(new FiltrRezerwacji("Archiwalny","AR100","ANULOWANA",null,null,FiltrRezerwacji.TrybDat.POBYT,FiltrRezerwacji.StanRozliczenia.WSZYSTKIE),0).wyniki().stream().anyMatch(w->w.id()==r.getId()),"Wyszukiwanie zachowuje stare rezerwacje");
        check(!BazaDanych.edytujGoscia(g.getId(),"Zmienione","Archiwalny","500123456","archive@example.com"),"Edycja archiwum odrzucona");
        odmowa(()->BazaDanych.zapiszRezerwacje(g,pokoj,d.plusDays(3),d.plusDays(4),1,recepcja.getId()));
        odmowa(()->BazaDanych.archiwizujGoscia(g.getId()));
        BazaDanych.przywrocGoscia(g.getId());var aktywny=gosc(g.getId());
        check(!aktywny.isArchiwalny()&&aktywny.getArchiwizowano()==null&&aktywny.getArchiwizowalLogin()==null,"Przywrócenie czyści bieżące metadane archiwum");
        odmowa(()->BazaDanych.przywrocGoscia(g.getId()));
        var nowa=BazaDanych.zapiszRezerwacje(aktywny,pokoj,d.plusDays(3),d.plusDays(4),1,recepcja.getId());check(nowa.getId()>0,"Nowa rezerwacja po przywróceniu");BazaDanych.anulujRezerwacje(nowa.getId());
        BazaDanych.zaloguj("admin","Admin123!");
        try(Connection c=TestDatabase.polacz();Statement q=c.createStatement()){q.execute("CREATE TRIGGER test_guest_archive BEFORE UPDATE ON goscie FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='test rollback archiwum'");}
        try{odmowa(()->BazaDanych.archiwizujGoscia(g.getId()));check(!gosc(g.getId()).isArchiwalny()&&gosc(g.getId()).getArchiwizowano()==null,"Błąd bazy wycofuje wszystkie metadane");}
        finally{try(Connection c=TestDatabase.polacz();Statement q=c.createStatement()){q.execute("DROP TRIGGER test_guest_archive");}}
        odmowa(()->BazaDanych.archiwizujGoscia(-1));odmowa(()->BazaDanych.przywrocGoscia(Integer.MAX_VALUE));
        var race=BazaDanych.zapiszGoscia("Jan","WyścigArchiwum","500123456","archive-race@example.com");
        wyscig(()->BazaDanych.archiwizujGoscia(race.getId()),()->BazaDanych.zapiszRezerwacje(race,IntegrationProbe.pokoj("AR101"),d,d.plusDays(1),1,admin));
        boolean maPobyt=BazaDanych.pobierzRezerwacje(BazaDanych.pobierzGosci(),BazaDanych.pobierzPokoje()).stream().anyMatch(x->x.getGosc().getId()==race.getId()&&x.getStatus().equals("POTWIERDZONA"));
        check(gosc(race.getId()).isArchiwalny()!=maPobyt,"Brak aktywnego pobytu z gościem zarchiwizowanym przez wyścig");
        var live=BazaDanych.zapiszRezerwacje(g,pokoj,LocalDate.now(),LocalDate.now().plusDays(1),1,admin);BazaDanych.zameldujGoscia(live.getId());
        odmowa(()->BazaDanych.archiwizujGoscia(g.getId()));BazaDanych.zapiszWplate(live.getId(),new BigDecimal("100"),"KARTA",admin);BazaDanych.wymeldujGoscia(live.getId());
        var finanse=BazaDanych.pobierzRozliczenie(live.getId());BazaDanych.archiwizujGoscia(g.getId());
        check(BazaDanych.pobierzRozliczenie(live.getId()).equals(finanse),"Płatności i rozliczenie zakończonego pobytu zachowane");
        check(IntegrationProbe.rezerwacja(live.getId()).getStatus().equals("ZAKONCZONA"),"Status zakończonego pobytu zachowany");
        BazaDanych.przywrocGoscia(g.getId());
        System.out.println("GUEST_ARCHIVE_OK: "+assertions+" asercji; role, aktywne pobyty, przywrócenie, historia, finanse, stare formularze, rollback i równoczesna rezerwacja.");
    }
}
