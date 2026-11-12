package pl.hotel.baza;
import pl.hotel.model.*;
import java.sql.*;
import java.math.BigDecimal;
final class ZapisUslugDAO {
    private ZapisUslugDAO(){}
    private static void aktywna(Connection c,int id)throws SQLException {
        try(PreparedStatement q=c.prepareStatement("SELECT status FROM rezerwacje WHERE id=? FOR UPDATE")){
            q.setInt(1,id);try(ResultSet r=q.executeQuery()){
                if(!r.next())throw new SQLException("Nie znaleziono rezerwacji.");
                if(!java.util.Set.of("POTWIERDZONA","ZAMELDOWANA").contains(r.getString(1)))throw new SQLException("Archiwalny pobyt: tylko odczyt usług.");
            }
        }
    }
    static PozycjaUslugi dodaj(int id,UslugaHotelowa oczekiwana,int ile)throws SQLException {
        if(id<1||oczekiwana==null||oczekiwana.id()<1||ile<1||ile>1000)throw new IllegalArgumentException("Wybierz usługę i ilość 1–1000.");
        try(Connection c=PolaczenieBazy.polacz()){
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);c.setAutoCommit(false);
            try{
                aktywna(c,id);UslugaHotelowa u;
                try(PreparedStatement q=c.prepareStatement("SELECT * FROM uslugi WHERE id=? LOCK IN SHARE MODE")){
                    q.setLong(1,oczekiwana.id());try(ResultSet r=q.executeQuery()){
                        if(!r.next())throw new SQLException("Usługa nie istnieje.");u=KatalogUslugDAO.wiersz(r);
                        if(!u.aktywna()||!u.zgodna(oczekiwana))throw new SQLException("Oferta lub cena zostały zmienione. Odśwież katalog.");
                    }
                }
                try(PreparedStatement q=c.prepareStatement("SELECT COUNT(*) FROM pozycje_uslug WHERE rezerwacja_id=? AND status='AKTYWNA'")){
                    q.setInt(1,id);try(ResultSet r=q.executeQuery()){r.next();if(r.getInt(1)>=100)throw new SQLException("Limit: 100 aktywnych pozycji usług.");}
                }
                BigDecimal przed=KosztyDAO.naleznosc(c,id),wartosc=u.cena().multiply(BigDecimal.valueOf(ile));
                if(przed.add(wartosc).compareTo(new BigDecimal("99999999.99"))>0)throw new SQLException("Należność przekracza 99999999.99 zł.");
                long pozycja;
                try(PreparedStatement q=c.prepareStatement("INSERT INTO pozycje_uslug(rezerwacja_id,usluga_id,pracownik_id,nazwa,jednostka,cena_jednostkowa,ilosc) VALUES(?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                    q.setInt(1,id);q.setLong(2,u.id());q.setLong(3,KontrolaDostepu.identyfikator());q.setString(4,u.nazwa());q.setString(5,u.jednostka());q.setBigDecimal(6,u.cena());q.setInt(7,ile);q.executeUpdate();
                    try(ResultSet r=q.getGeneratedKeys()){if(!r.next())throw new SQLException("Brak ID pozycji.");pozycja=r.getLong(1);}
                }
                HistoriaDAO.opis(c,id,"DODANIE_USLUGI","Należność: "+przed,"#"+pozycja+" | "+u.nazwa()+" | "+ile+" × "+u.cena()+" zł / "+u.jednostka()+" | Należność: "+przed.add(wartosc),null);
                var wynik=UslugiDAO.pobierz(c,id).stream().filter(p->p.id()==pozycja).findFirst().orElseThrow();c.commit();return wynik;
            }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException b){e.addSuppressed(b);}throw e;}
        }
    }
    static void anuluj(int id,long pozycja)throws SQLException {
        if(id<1||pozycja<1)throw new IllegalArgumentException("Podaj poprawne ID.");
        try(Connection c=PolaczenieBazy.polacz()){
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);c.setAutoCommit(false);
            try{
                aktywna(c,id);
                var p=UslugiDAO.pobierz(c,id).stream().filter(x->x.id()==pozycja).findFirst().orElseThrow(()->new SQLException("Pozycja nie należy do tej rezerwacji."));
                if(!p.aktywna())throw new SQLException("Pozycja już anulowana.");
                BigDecimal nowa=KosztyDAO.naleznosc(c,id).subtract(p.wartosc()),netto=RozliczeniaDAO.pobierzNetto(c,id);
                if(netto.compareTo(nowa)>0)throw new SQLException("Najpierw zwróć nadpłatę: "+netto.subtract(nowa)+" zł.");
                anulujWiersz(c,p);c.commit();
            }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException b){e.addSuppressed(b);}throw e;}
        }
    }
    static void anulujWszystkie(Connection c,int id)throws SQLException {
        for(var p:UslugiDAO.pobierz(c,id))if(p.aktywna())anulujWiersz(c,p);
    }
    private static void anulujWiersz(Connection c,PozycjaUslugi p)throws SQLException {
        try(PreparedStatement q=c.prepareStatement("UPDATE pozycje_uslug SET status='ANULOWANA',anulowano=CURRENT_TIMESTAMP(6),anulowal_pracownik_id=? WHERE id=? AND rezerwacja_id=? AND status='AKTYWNA'")){
            q.setLong(1,KontrolaDostepu.identyfikator());q.setLong(2,p.id());q.setInt(3,p.rezerwacjaId());if(q.executeUpdate()!=1)throw new SQLException("Pozycja została zmieniona.");
        }
        HistoriaDAO.opis(c,p.rezerwacjaId(),"ANULOWANIE_USLUGI",p.opis(),"ANULOWANA | wartość: "+p.wartosc()+" zł",null);
    }
}
