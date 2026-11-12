package pl.hotel.baza;
import java.sql.*;
import java.math.BigDecimal;
final class KosztyDAO {
    private KosztyDAO(){}
    static BigDecimal sumaUslug(Connection c,int id)throws SQLException {
        try(PreparedStatement q=c.prepareStatement("SELECT COALESCE(SUM(cena_jednostkowa*ilosc),0) FROM pozycje_uslug WHERE rezerwacja_id=? AND status='AKTYWNA'")){
            q.setInt(1,id);try(ResultSet r=q.executeQuery()){r.next();return r.getBigDecimal(1);}
        }
    }
    static BigDecimal naleznosc(Connection c,int id)throws SQLException {
        try(PreparedStatement q=c.prepareStatement("SELECT status,cena_za_noc*DATEDIFF(data_do,data_od) AS noclegi FROM rezerwacje WHERE id=?")){
            q.setInt(1,id);try(ResultSet r=q.executeQuery()){
                if(!r.next())throw new SQLException("Nie znaleziono rezerwacji.");
                return r.getString(1).equals("ANULOWANA")?BigDecimal.ZERO:r.getBigDecimal(2).add(sumaUslug(c,id));
            }
        }
    }
}
