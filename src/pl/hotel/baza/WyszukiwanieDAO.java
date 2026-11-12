package pl.hotel.baza;
import pl.hotel.model.*;
import java.sql.*;
import java.util.*;
final class WyszukiwanieDAO {
    private WyszukiwanieDAO(){}
    private static String wzorzec(String s){return "%"+s.replace("=","==").replace("%","=%").replace("_","=_")+"%";}
    static StronaRezerwacji szukaj(FiltrRezerwacji f,long przedId)throws SQLException {
        if(f==null||przedId<0)throw new IllegalArgumentException("Podaj filtr i poprawny kursor.");
        StringBuilder sql=new StringBuilder("SELECT * FROM (SELECT r.id,g.imie,g.nazwisko,p.numer,r.data_od,r.data_do,r.status, "
            +"CASE WHEN r.status='ANULOWANA' THEN 0 ELSE r.cena_za_noc*DATEDIFF(r.data_do,r.data_od)+COALESCE(s.suma,0) END naleznosc, "
            +"COALESCE(t.netto,0) netto,COALESCE(s.suma,0) uslugi FROM rezerwacje r JOIN goscie g ON g.id=r.gosc_id JOIN pokoje p ON p.id=r.pokoj_id "
            +"LEFT JOIN (SELECT rezerwacja_id,SUM(cena_jednostkowa*ilosc) suma FROM pozycje_uslug WHERE status='AKTYWNA' GROUP BY rezerwacja_id) s ON s.rezerwacja_id=r.id "
            +"LEFT JOIN (SELECT rezerwacja_id,SUM(CASE WHEN rodzaj='WPLATA' THEN kwota ELSE -kwota END) netto FROM platnosci GROUP BY rezerwacja_id) t ON t.rezerwacja_id=r.id) x WHERE 1=1");
        List<Object> args=new ArrayList<>();
        if(!f.nazwisko().isEmpty()){
            sql.append(" AND (x.nazwisko LIKE ? ESCAPE '=' OR EXISTS(SELECT 1 FROM uczestnicy_rezerwacji u WHERE u.rezerwacja_id=x.id AND u.nazwisko LIKE ? ESCAPE '='))");
            args.add(wzorzec(f.nazwisko()));args.add(wzorzec(f.nazwisko()));
        }
        if(!f.pokoj().isEmpty()){sql.append(" AND x.numer=?");args.add(f.pokoj());}
        if(!f.status().isEmpty()){sql.append(" AND x.status=?");args.add(f.status());}
        String pole=f.tryb()==FiltrRezerwacji.TrybDat.WYJAZD?"x.data_do":"x.data_od";
        if(f.tryb()==FiltrRezerwacji.TrybDat.POBYT){
            if(f.od()!=null){sql.append(" AND x.data_do>?");args.add(java.sql.Date.valueOf(f.od()));}
            if(f.doDnia()!=null){sql.append(" AND x.data_od<?");args.add(java.sql.Date.valueOf(f.doDnia().plusDays(1)));}
        }else{
            if(f.od()!=null){sql.append(" AND "+pole+">=?");args.add(java.sql.Date.valueOf(f.od()));}
            if(f.doDnia()!=null){sql.append(" AND "+pole+"<=?");args.add(java.sql.Date.valueOf(f.doDnia()));}
        }
        sql.append(switch(f.rozliczenie()){
            case WSZYSTKIE->"";
            case DO_ZAPLATY->" AND x.naleznosc>x.netto";
            case NIEOPLACONA->" AND x.naleznosc>0 AND x.netto=0";
            case CZESCIOWO_OPLACONA->" AND x.netto>0 AND x.netto<x.naleznosc";
            case ROZLICZONA->" AND x.netto=x.naleznosc";
            case NADPLATA->" AND x.netto>x.naleznosc";
        });
        if(przedId!=0){sql.append(" AND x.id<?");args.add(przedId);}
        sql.append(" ORDER BY x.id DESC LIMIT 101");List<WynikRezerwacji> lista=new ArrayList<>();
        try(Connection c=PolaczenieBazy.polacz();PreparedStatement q=c.prepareStatement(sql.toString())){
            for(int i=0;i<args.size();i++)q.setObject(i+1,args.get(i));
            try(ResultSet r=q.executeQuery()){while(r.next()){
                long id=r.getLong("id");if(id<1||id>Integer.MAX_VALUE)throw new SQLException("ID poza zakresem int.");
                lista.add(new WynikRezerwacji((int)id,r.getString("imie")+" "+r.getString("nazwisko"),r.getString("numer"),r.getDate("data_od").toLocalDate(),r.getDate("data_do").toLocalDate(),r.getString("status"),r.getBigDecimal("naleznosc"),r.getBigDecimal("netto"),r.getBigDecimal("uslugi")));
            }}
        }
        boolean starsze=lista.size()>100;if(starsze)lista.remove(100);return new StronaRezerwacji(lista,starsze);
    }
}
