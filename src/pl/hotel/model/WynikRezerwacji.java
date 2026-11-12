package pl.hotel.model;
import java.math.BigDecimal;
import java.time.LocalDate;
public record WynikRezerwacji(int id,String gosc,String pokoj,LocalDate od,LocalDate doDnia,String status,BigDecimal naleznosc,BigDecimal netto,BigDecimal uslugi) {
    public BigDecimal saldo(){return naleznosc.subtract(netto);}
}
