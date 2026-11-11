import pl.hotel.baza.BazaDanych;
import pl.hotel.model.*;
import pl.hotel.raporty.Raporty;
import pl.hotel.bezpieczenstwo.*;
import java.sql.*;
import java.time.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public final class SecurityProbe {
    static int asercje;
    interface Akcja { void run() throws Exception; }
    static void check(boolean ok,String nazwa) { if(!ok)throw new AssertionError(nazwa); asercje++; }
    static void odmowa(Akcja a) throws Exception {
        try { a.run(); throw new AssertionError("Operacja powinna być odrzucona"); }
        catch(SecurityException | SQLException | IllegalArgumentException ok) { asercje++; }
    }
    static void admin() throws Exception { check(BazaDanych.zaloguj("admin","Admin123!")!=null,"Logowanie administratora"); }
    static void sql(String text) throws Exception { try(var c=TestDatabase.polacz();var p=c.createStatement()){p.execute(text);} }
    static String skalar(String text) throws Exception {
        try(var c=TestDatabase.polacz();var p=c.createStatement();var r=p.executeQuery(text)){r.next();return r.getString(1);}
    }
    static int rownolegle(Akcja akcja) throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch gotowi=new CountDownLatch(2),start=new CountDownLatch(1);
        Callable<Boolean> praca=()->{if(BazaDanych.zaloguj("admin","Admin123!")==null)throw new AssertionError("Login wątku");gotowi.countDown();start.await();
            try{akcja.run();return true;}catch(SQLException e){if(!e.getMessage().contains("przekracza")&&!e.getMessage().startsWith("Za duża wpłata")&&!e.getMessage().startsWith("Za duży zwrot"))throw e;return false;}};
        try{var x=pool.submit(praca);var y=pool.submit(praca);if(!gotowi.await(15,TimeUnit.SECONDS))throw new AssertionError("Brak równoległych sesji");start.countDown();return (x.get(20,TimeUnit.SECONDS)?1:0)+(y.get(20,TimeUnit.SECONDS)?1:0);}
        finally{start.countDown();pool.shutdownNow();}
    }
    public static void main(String[] args) throws Exception {
        TestDatabase.wymagajTestowejBazy();
        if(args.length>0) { check(BazaDanych.zaloguj("rate_probe","RateHaslo123!")==null,"Blokada przetrwała zmianę procesu");System.out.println("LOCKOUT_PROCESS_OK");return; }
        BazaDanych.wyloguj(); odmowa(()->BazaDanych.pobierzGosci()); odmowa(()->BazaDanych.pobierzKontaPracownikow());
        odmowa(()->Raporty.generuj(LocalDate.now(),LocalDate.now()));
        check(BazaDanych.zaloguj("admin' OR '1'='1","dowolne")==null,"SQL injection logowania");
        check(BazaDanych.zaloguj("admin","x".repeat(10000))==null,"Długie hasło odrzucone");
        check(!Hasla.sprawdz("x".repeat(10000),"zły hash"),"Ograniczenie hasła przed KDF");
        check(!Hasla.sprawdz("abc","pbkdf2_sha256$1$"+"x".repeat(5000)),"Ograniczenie wielkości hasha");
        check(!Hasla.sprawdz("abc","pbkdf2_sha256$2147483647$AAAAAAAAAAAAAAAAAAAAAA==$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="),"Limit kosztu KDF");
        check(!Hasla.sprawdz("abc","pbkdf2_sha256$1$AAAAAAAAAAAAAAAAAAAAAA==$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="),"Zbyt słaby koszt KDF");
        String h1=Hasla.utworzHash("PoprawneHaslo123!"),h2=Hasla.utworzHash("PoprawneHaslo123!");
        check(!h1.equals(h2)&&Hasla.sprawdz("PoprawneHaslo123!",h1),"Losowa sól i poprawna weryfikacja");
        admin();
        String atak="x'); DROP TABLE goscie; --";
        int przed=BazaDanych.pobierzGosci().size();
        var g=BazaDanych.zapiszGoscia("Jan",atak,"123456789","security@example.com");
        check(BazaDanych.pobierzGosci().size()==przed+1&&BazaDanych.pobierzGosci().stream().anyMatch(x->x.getNazwisko().equals(atak)),"SQL injection jest tekstem, nie instrukcją");
        odmowa(()->BazaDanych.zapiszGoscia(null,"X","123","x@x.pl"));
        odmowa(()->BazaDanych.zapiszGoscia(" ","X","123","x@x.pl"));
        odmowa(()->BazaDanych.zapiszGoscia("A".repeat(61),"X","123","x@x.pl"));
        odmowa(()->BazaDanych.zapiszGoscia("Jan","X","123","zły-email"));
        odmowa(()->BazaDanych.edytujGoscia(g.getId(),null,"X","123","x@x.pl"));
        odmowa(()->BazaDanych.dodajPokoj(null,"Typ",1,BigDecimal.ONE));
        odmowa(()->BazaDanych.dodajPokoj("SEC","Typ",1,null));
        BazaDanych.dodajPokoj("SEC","Test",2,new BigDecimal("100"));
        var pokoj=BazaDanych.pobierzPokoje().stream().filter(p->p.getNumer().equals("SEC")).findFirst().orElseThrow();
        odmowa(()->BazaDanych.zapiszRezerwacje(null,pokoj,LocalDate.now(),LocalDate.now().plusDays(1),1,1));
        odmowa(()->BazaDanych.zapiszRezerwacje(g,pokoj,null,LocalDate.now().plusDays(1),1,1));
        var rezerwacja=BazaDanych.zapiszRezerwacje(g,pokoj,LocalDate.now().plusDays(2),LocalDate.now().plusDays(3),1,1);
        BazaDanych.zapiszWplate(rezerwacja.getId(),new BigDecimal("50"),"KARTA",1);
        BazaDanych.zaloguj("recepcja","Recepcja123!");
        odmowa(()->BazaDanych.dodajPracownika("Nowy","Admin","hacker","HasloAtak123!","ADMIN"));
        odmowa(()->BazaDanych.dodajPokoj("SEC2","Test",1,BigDecimal.ONE));
        odmowa(()->BazaDanych.edytujPokoj(pokoj.getId(),"SEC","Test",BigDecimal.ONE));
        odmowa(()->BazaDanych.pobierzKontaPracownikow());
        odmowa(()->BazaDanych.zapiszZwrot(rezerwacja.getId(),BigDecimal.ONE,"KARTA",2));
        odmowa(()->BazaDanych.zapiszZwrot(rezerwacja.getId(),BigDecimal.ONE,"KARTA",1));
        odmowa(()->BazaDanych.zapiszWplate(rezerwacja.getId(),BigDecimal.ONE,"KARTA",1));
        odmowa(()->BazaDanych.dezaktywujPracownika(1,1));
        odmowa(()->BazaDanych.sprawdzSesje(new Pracownik(1,"Fake","Admin","admin","ADMIN"),true));
        check(BazaDanych.pobierzRozliczenie(rezerwacja.getId()).wplaty().compareTo(new BigDecimal("50"))==0,"Brak nieuprawnionych zmian finansowych");
        admin();
        odmowa(()->BazaDanych.zapiszWplate(rezerwacja.getId(),new BigDecimal("-1"),"KARTA",1));
        odmowa(()->BazaDanych.zapiszWplate(rezerwacja.getId(),new BigDecimal("0.001"),"KARTA",1));
        odmowa(()->BazaDanych.zapiszWplate(rezerwacja.getId(),new BigDecimal("51"),"KARTA",1));
        odmowa(()->BazaDanych.zapiszZwrot(rezerwacja.getId(),new BigDecimal("51"),"KARTA",1));
        odmowa(()->BazaDanych.zapiszWplate(rezerwacja.getId(),BigDecimal.ONE,"KARTA');DELETE FROM platnosci;--",1));
        var finansowa=BazaDanych.zapiszRezerwacje(g,pokoj,LocalDate.now().plusDays(5),LocalDate.now().plusDays(6),1,1);
        check(rownolegle(()->BazaDanych.zapiszWplate(finansowa.getId(),new BigDecimal("60"),"KARTA",1))==1,"Dwie wpłaty nie przekraczają należności");
        check(rownolegle(()->BazaDanych.zapiszZwrot(finansowa.getId(),new BigDecimal("40"),"KARTA",1))==1,"Dwa zwroty nie przekraczają wpłat");
        BazaDanych.dodajPracownika("Rate","Limit","rate_probe","RateHaslo123!","RECEPCJA");
        for(int i=0;i<5;i++)check(BazaDanych.zaloguj("rate_probe","zle")==null,"Błędne logowanie "+i);
        check(BazaDanych.zaloguj("rate_probe","RateHaslo123!")==null,"Poprawne hasło nie omija czasowej blokady");
        String childClasspath=java.util.stream.Stream.of(SecurityProbe.class,BazaDanych.class,com.mysql.cj.jdbc.Driver.class)
                .map(c->{try{return Path.of(c.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();}catch(Exception e){throw new IllegalStateException(e);}})
                .distinct().collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));
        Process proc=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-Dhotel.tests=true","-Dhotel.config="+System.getProperty("hotel.config"),"-cp",childClasspath,"SecurityProbe","blocked-check").redirectErrorStream(true).start();
        String child=new String(proc.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        check(proc.waitFor()==0&&child.contains("LOCKOUT_PROCESS_OK"),"Restart procesu nie resetuje blokady");
        sql("UPDATE pracownicy SET blokada_do=DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 1 SECOND) WHERE login='rate_probe'");
        var rate=BazaDanych.zaloguj("rate_probe","RateHaslo123!");check(rate!=null,"Blokada wygasa");
        check(skalar("SELECT nieudane_logowania FROM pracownicy WHERE login='rate_probe'").equals("0"),"Udane logowanie zeruje licznik");
        sql("UPDATE pracownicy SET aktywny=FALSE WHERE login='rate_probe'");
        odmowa(()->BazaDanych.pobierzGosci());
        admin();sql("UPDATE pracownicy SET rola='RECEPCJA' WHERE id=1");
        odmowa(()->BazaDanych.pobierzKontaPracownikow());sql("UPDATE pracownicy SET rola='ADMIN' WHERE id=1");
        String staryHash=skalar("SELECT hash_hasla FROM pracownicy WHERE id=1");
        ExecutorService pool=Executors.newSingleThreadExecutor(); CountDownLatch gotowy=new CountDownLatch(1),zmieniono=new CountDownLatch(1);
        Future<Boolean> drugaSesja=pool.submit(()->{BazaDanych.zaloguj("admin","Admin123!");gotowy.countDown();zmieniono.await();try{BazaDanych.pobierzGosci();return false;}catch(SesjaWygasla ok){return true;}});
        try {
            check(gotowy.await(15,TimeUnit.SECONDS),"Druga sesja zalogowana");admin();
            BazaDanych.zmienHaslo(1,"Admin123!","NoweAdminHaslo123!");
            odmowa(()->BazaDanych.pobierzGosci());zmieniono.countDown();check(drugaSesja.get(15,TimeUnit.SECONDS),"Zmiana hasła unieważnia inną sesję");
        } finally {
            zmieniono.countDown();pool.shutdownNow();
            try(var c=TestDatabase.polacz();var p=c.prepareStatement("UPDATE pracownicy SET hash_hasla=? WHERE id=1")){p.setString(1,staryHash);p.executeUpdate();}
        }
        admin();
        String maska=KomunikatyBledu.dlaUzytkownika(new SQLException("haslo=TAJNE SQL INSERT sekret","42000",1064));
        check(!maska.contains("TAJNE")&&!maska.contains("INSERT"),"Brak szczegółów SQL w komunikacie");
        Path config=Path.of(System.getProperty("hotel.config")); String stara=Files.readString(config); Properties props=new Properties();props.load(new java.io.StringReader(stara));
        try {
            for(String opcja:List.of("allowMultiQueries=true","autoDeserialize=true","allowLoadLocalInfile=true","allowUrlInLocalInfile=true","allowLoadLocalInfileInPath=/tmp","socketTimeout=0")) {
                String url=props.getProperty("url");Files.writeString(config,stara.replace(url,url+"&"+opcja));
                try {BazaDanych.zaloguj("admin","Admin123!");throw new AssertionError("Niebezpieczna opcja JDBC przyjęta: "+opcja);}
                catch(SQLException ok){check(ok.getSQLState()==null,"Opcja odrzucona przed połączeniem");}
            }
            Files.writeString(config,stara.replace(props.getProperty("url"),"jdbc:mysql://example.invalid:3306/hotel_test?sslMode=DISABLED"));
            try {BazaDanych.zaloguj("admin","Admin123!");throw new AssertionError("Zdalna baza bez TLS przyjęta");}
            catch(SQLException ok){check(ok.getMessage().contains("VERIFY_IDENTITY"),"Zdalny TLS wymagany przed połączeniem");}
        } finally { Files.writeString(config,stara); }
        admin();
        System.out.println("SECURITY_OK: "+asercje+" asercji; uprawnienia, podszywanie, SQL injection, blokada procesu, sesje, hasła, walidacja, finanse i JDBC.");
    }
}
