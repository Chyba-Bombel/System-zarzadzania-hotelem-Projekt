package pl.hotel.model;
import java.math.BigDecimal;
public record UslugaHotelowa(long id,String nazwa,String jednostka,BigDecimal cena,boolean aktywna) {
    public boolean zgodna(UslugaHotelowa u) {
        return u!=null&&id==u.id&&nazwa.equals(u.nazwa)&&jednostka.equals(u.jednostka)&&cena.compareTo(u.cena)==0&&aktywna==u.aktywna;
    }
}
