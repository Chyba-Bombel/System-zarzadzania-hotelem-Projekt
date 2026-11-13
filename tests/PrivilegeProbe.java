import pl.hotel.baza.BazaDanych;
import pl.hotel.raporty.Raporty;
import java.sql.*;
import java.nio.file.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public final class PrivilegeProbe {
    static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
    public static void main(String[] args) throws Exception {
        TestDatabase.wymagajTestowejBazy();
        Path plik=Path.of(System.getProperty("hotel.config"));String stara=Files.readString(plik);Properties p=new Properties();p.load(new java.io.StringReader(stara));
        String baza=java.net.URI.create(p.getProperty("url").substring(5)).getPath().substring(1);
        String konto="hotel_probe_"+Long.toString(System.currentTimeMillis(),36),haslo=UUID.randomUUID().toString();
        try(Connection root=TestDatabase.polacz();Statement q=root.createStatement()) {
            try {
                for(String host:List.of("localhost","127.0.0.1")) {
                    String user="'"+konto+"'@'"+host+"'";
                    q.execute("CREATE USER "+user+" IDENTIFIED BY '"+haslo+"'");
                    q.execute("GRANT SELECT ON "+baza+".* TO "+user);
                    for(String t:List.of("goscie","pokoje","pracownicy","rezerwacje"))q.execute("GRANT INSERT,UPDATE ON "+baza+"."+t+" TO "+user);
                    q.execute("GRANT INSERT ON "+baza+".platnosci TO "+user);
                    q.execute("GRANT INSERT,DELETE ON "+baza+".uczestnicy_rezerwacji TO "+user);
                }
                for(String host:List.of("localhost","127.0.0.1")) {
                    String granty=Files.readString(Path.of("db/administracja/uprawnienia.sql"))
                        .replace("hotel_manager",baza).replace("'hotel_app'","'"+konto+"'")
                        .replace("SET @host = 'localhost';","SET @host = '"+host+"';");
                    for(String polecenie:granty.split(";"))if(!polecenie.isBlank())q.execute(polecenie);
                }
                try(Connection c=DriverManager.getConnection(p.getProperty("url"),konto,haslo);Statement z=c.createStatement()) {
                    for(String sql:List.of("CREATE TABLE niedozwolona_tabela (id INT)","DELETE FROM historia_rezerwacji WHERE 1=0","UPDATE historia_rezerwacji SET akcja=akcja WHERE 1=0","DELETE FROM platnosci WHERE 1=0","UPDATE platnosci SET kwota=kwota WHERE 1=0","DELETE FROM uslugi WHERE 1=0","DELETE FROM pozycje_uslug WHERE 1=0")) {
                        try{z.execute(sql);throw new AssertionError("Nadmierne uprawnienia techniczne");}
                        catch(SQLException ok){check(ok.getErrorCode()==1142||ok.getErrorCode()==1044,"Odmowa musi wynikać z uprawnień");}
                    }
                }
                p.setProperty("user",konto);p.setProperty("password",haslo);
                try(var w=Files.newBufferedWriter(plik)){p.store(w,null);}
                check(BazaDanych.zaloguj("admin","Admin123!")!=null,"Login na ograniczonym koncie MySQL");
                var g=BazaDanych.zapiszGoscia("Jan","Minimalny","500123456","min@example.com");
                BazaDanych.dodajPokoj("MIN","Test",1,new BigDecimal("100"));
                var pokoj=BazaDanych.pobierzPokoje().stream().filter(x->x.getNumer().equals("MIN")).findFirst().orElseThrow();
                var r=BazaDanych.zapiszRezerwacje(g,pokoj,LocalDate.now().plusDays(9),LocalDate.now().plusDays(10),1,1,List.of());
                BazaDanych.zapiszWplate(r.getId(),new BigDecimal("50"),"KARTA",1);BazaDanych.zapiszZwrot(r.getId(),new BigDecimal("20"),"KARTA",1);
                BazaDanych.zmienUczestnikow(r.getId(),List.of());
                BazaDanych.dodajPokoj("MIN2","Test",1,new BigDecimal("200"));
                var drugi=BazaDanych.pobierzPokoje().stream().filter(x->x.getNumer().equals("MIN2")).findFirst().orElseThrow();
                BazaDanych.zmienPokojRezerwacji(r.getId(),pokoj.getId(),drugi.getId());
                check(IntegrationProbe.rezerwacja(r.getId()).getPokoj().getId()==drugi.getId(),"Zmiana pokoju na ograniczonym koncie");
                check(BazaDanych.pobierzHistorieRezerwacji(r.getId(),0).wpisy().size()==4,"Historia na ograniczonym koncie");
                check(BazaDanych.pobierzPotwierdzenieRezerwacji(r.getId()).pokoj().equals("MIN2"),"Potwierdzenie na ograniczonym koncie");
                var u=BazaDanych.zapiszUsluge(null,"Minimalny parking","doba",new BigDecimal("10"),true);
                var pozycja=BazaDanych.dodajUslugeDoRezerwacji(r.getId(),u,2,1);
                check(BazaDanych.pobierzUslugiRezerwacji(r.getId()).rozliczenie().uslugi().compareTo(new BigDecimal("20"))==0,"Usługi na ograniczonych grantach");
                BazaDanych.anulujUslugeRezerwacji(r.getId(),pozycja.id());
                check(!BazaDanych.wyszukajRezerwacje(pl.hotel.model.FiltrRezerwacji.wszystkie(),0).wyniki().isEmpty(),"Wyszukiwanie na ograniczonych grantach");
                BazaDanych.zapiszNotatkeRezerwacji(r.getId(),"","Notatka na ograniczonym koncie");
                check(BazaDanych.pobierzNotatkeRezerwacji(r.getId()).tresc().equals("Notatka na ograniczonym koncie"),"Notatki na ograniczonym koncie");
                BazaDanych.dodajPokoj("MIN3","Test",1,new BigDecimal("100"));
                var pusty=BazaDanych.pobierzPokoje().stream().filter(x->x.getNumer().equals("MIN3")).findFirst().orElseThrow();
                BazaDanych.wylaczPokoj(pusty.getId(),"GOTOWY");
                check(BazaDanych.przywrocPokoj(pusty.getId(),"WYLACZONY").getStan().equals("DO_SPRZATANIA"),"Dostępność pokoju na ograniczonym koncie");
                check(BazaDanych.edytujGoscia(g.getId(),"Jan","Minimalny","500123457","min@example.com"),"Edycja gościa");
                check(Raporty.generuj(LocalDate.now(),LocalDate.now()).tabele().size()==5,"Raport na ograniczonym koncie");
                var arch=BazaDanych.zapiszGoscia("Jan","MinimalneArchiwum","500123456","min-archive@example.com");
                BazaDanych.archiwizujGoscia(arch.getId());
                check(BazaDanych.pobierzGosci().stream().anyMatch(x->x.getId()==arch.getId()&&x.isArchiwalny()),"Archiwizacja na ograniczonym koncie");
                BazaDanych.przywrocGoscia(arch.getId());
                System.out.println("PRIVILEGES_OK: brak DDL i zmiany/usuwania płatności; obsługa pobytu działa na minimalnych grantach.");
            } finally {
                Files.writeString(plik,stara);BazaDanych.wyloguj();
                for(String host:List.of("localhost","127.0.0.1"))q.execute("DROP USER IF EXISTS '"+konto+"'@'"+host+"'");
            }
        }
    }
}
