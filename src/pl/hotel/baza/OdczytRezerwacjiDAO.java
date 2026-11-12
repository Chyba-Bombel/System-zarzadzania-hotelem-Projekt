package pl.hotel.baza;
import pl.hotel.model.*;
import java.sql.*;
import java.util.*;
final class OdczytRezerwacjiDAO {
    private OdczytRezerwacjiDAO(){}
    static ArrayList<Rezerwacja> pobierzRezerwacje(ArrayList<Gosc> goscie,ArrayList<Pokoj> pokoje)throws SQLException {
        Map<Long,Gosc> mg=new HashMap<>();Map<Long,Pokoj> mp=new HashMap<>();
        for(var g:goscie)mg.put((long)g.getId(),g);for(var p:pokoje)mp.put(p.getId(),p);
        try(Connection c=PolaczenieBazy.polacz()){
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setReadOnly(true);c.setAutoCommit(false);
            try{
                var lista=wiersze(c,mg,mp);Map<Integer,Rezerwacja> mapa=new HashMap<>();Map<Integer,List<Uczestnik>> osoby=new HashMap<>();
                for(var r:lista)mapa.put(r.getId(),r);
                try(PreparedStatement q=c.prepareStatement("SELECT rezerwacja_id,imie,nazwisko FROM uczestnicy_rezerwacji ORDER BY rezerwacja_id,pozycja");ResultSet r=q.executeQuery()){
                    while(r.next())osoby.computeIfAbsent(r.getInt(1),k->new ArrayList<>()).add(new Uczestnik(r.getString(2),r.getString(3)));
                }
                osoby.forEach((id,o)->{if(mapa.containsKey(id))mapa.get(id).ustawUczestnikow(o);});c.commit();return lista;
            }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException b){e.addSuppressed(b);}throw e;}
        }
    }
    private static ArrayList<Rezerwacja> wiersze(Connection c,Map<Long,Gosc> goscie,Map<Long,Pokoj> pokoje)throws SQLException {
        String sql="SELECT r.*,COALESCE(s.suma,0) AS uslugi,COALESCE(t.netto,0) AS netto FROM rezerwacje r "
            +"LEFT JOIN (SELECT rezerwacja_id,SUM(cena_jednostkowa*ilosc) suma FROM pozycje_uslug WHERE status='AKTYWNA' GROUP BY rezerwacja_id) s ON s.rezerwacja_id=r.id "
            +"LEFT JOIN (SELECT rezerwacja_id,SUM(CASE WHEN rodzaj='WPLATA' THEN kwota ELSE -kwota END) netto FROM platnosci GROUP BY rezerwacja_id) t ON t.rezerwacja_id=r.id ORDER BY r.id";
        ArrayList<Rezerwacja> lista=new ArrayList<>();
        try(PreparedStatement q=c.prepareStatement(sql);ResultSet w=q.executeQuery()){
            while(w.next()){
                long id=w.getLong("id");if(id<1||id>Integer.MAX_VALUE)throw new SQLException("ID poza zakresem int.");
                var g=goscie.get(w.getLong("gosc_id"));var p=pokoje.get(w.getLong("pokoj_id"));
                if(g==null||p==null)throw new SQLException("Nie znaleziono gościa lub pokoju. Odśwież dane.");
                var r=new Rezerwacja((int)id,g,p,w.getDate("data_od").toLocalDate(),w.getDate("data_do").toLocalDate(),w.getInt("liczba_osob"),w.getBigDecimal("cena_za_noc"),w.getString("status"));
                r.ustawRozliczenie(w.getBigDecimal("uslugi"),w.getBigDecimal("netto"));lista.add(r);
            }
        }return lista;
    }
}
