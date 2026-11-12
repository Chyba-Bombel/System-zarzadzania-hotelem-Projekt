package pl.hotel.baza;
import pl.hotel.model.UslugaHotelowa;
import pl.hotel.walidacja.Dane;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;
final class KatalogUslugDAO {
    private KatalogUslugDAO(){}
    static UslugaHotelowa wiersz(ResultSet r)throws SQLException{return new UslugaHotelowa(r.getLong("id"),r.getString("nazwa"),r.getString("jednostka"),r.getBigDecimal("cena"),r.getBoolean("aktywna"));}
    static List<UslugaHotelowa> pobierz(boolean wszystkie)throws SQLException {
        List<UslugaHotelowa> lista=new ArrayList<>();
        try(Connection c=PolaczenieBazy.polacz();PreparedStatement q=c.prepareStatement("SELECT * FROM uslugi"+(wszystkie?"":" WHERE aktywna=TRUE")+" ORDER BY nazwa,id");ResultSet r=q.executeQuery()){
            while(r.next())lista.add(wiersz(r));
        }return List.copyOf(lista);
    }
    static UslugaHotelowa zapisz(UslugaHotelowa stara,String nazwa,String jednostka,BigDecimal cena,boolean aktywna)throws SQLException {
        Dane.tekst(nazwa,80,"Nazwa");Dane.tekst(jednostka,30,"Jednostka");
        if(cena==null||cena.signum()<=0||cena.scale()>2||cena.compareTo(new BigDecimal("99999999.99"))>0)throw new IllegalArgumentException("Niepoprawna cena usługi.");
        try(Connection c=PolaczenieBazy.polacz()){
            c.setAutoCommit(false);
            try {
                long id;
                if(stara==null){
                    try(PreparedStatement q=c.prepareStatement("INSERT INTO uslugi(kod,nazwa,jednostka,cena,aktywna) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                        q.setString(1,UUID.randomUUID().toString());q.setString(2,nazwa);q.setString(3,jednostka);q.setBigDecimal(4,cena);q.setBoolean(5,aktywna);q.executeUpdate();
                        try(ResultSet r=q.getGeneratedKeys()){if(!r.next())throw new SQLException("Brak ID usługi.");id=r.getLong(1);}
                    }
                }else{
                    id=stara.id();
                    try(PreparedStatement q=c.prepareStatement("SELECT * FROM uslugi WHERE id=? FOR UPDATE")){
                        q.setLong(1,id);try(ResultSet r=q.executeQuery()){if(!r.next()||!stara.zgodna(wiersz(r)))throw new SQLException("Katalog zmieniono. Odśwież dane.");}
                    }
                    try(PreparedStatement q=c.prepareStatement("UPDATE uslugi SET nazwa=?,jednostka=?,cena=?,aktywna=? WHERE id=?")){
                        q.setString(1,nazwa);q.setString(2,jednostka);q.setBigDecimal(3,cena);q.setBoolean(4,aktywna);q.setLong(5,id);q.executeUpdate();
                    }
                }
                c.commit();return new UslugaHotelowa(id,nazwa,jednostka,cena.setScale(2),aktywna);
            }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException b){e.addSuppressed(b);}throw e;}
        }
    }
}
