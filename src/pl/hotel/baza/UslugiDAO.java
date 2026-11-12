package pl.hotel.baza;
import pl.hotel.model.*;
import java.sql.*;
import java.util.*;
final class UslugiDAO {
    private UslugiDAO(){}
    static List<PozycjaUslugi> pobierz(Connection c,int id)throws SQLException {
        List<PozycjaUslugi> lista=new ArrayList<>();
        try(PreparedStatement q=c.prepareStatement("SELECT * FROM pozycje_uslug WHERE rezerwacja_id=? ORDER BY id")){
            q.setInt(1,id);try(ResultSet r=q.executeQuery()){while(r.next()){
                Timestamp anulowano=r.getTimestamp("anulowano");
                lista.add(new PozycjaUslugi(r.getLong("id"),id,r.getLong("usluga_id"),r.getString("nazwa"),r.getString("jednostka"),r.getBigDecimal("cena_jednostkowa"),r.getInt("ilosc"),r.getString("status"),r.getTimestamp("utworzono").toLocalDateTime(),anulowano==null?null:anulowano.toLocalDateTime()));
            }}
        }return List.copyOf(lista);
    }
    static UslugiRezerwacji dane(int id)throws SQLException {
        try(Connection c=PolaczenieBazy.polacz()){
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setReadOnly(true);c.setAutoCommit(false);
            try{var d=new UslugiRezerwacji(RozliczeniaDAO.pobierz(c,id),pobierz(c,id));c.commit();return d;}
            catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException b){e.addSuppressed(b);}throw e;}
        }
    }
}
