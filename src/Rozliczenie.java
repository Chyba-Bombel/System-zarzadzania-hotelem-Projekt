import java.math.BigDecimal;
import java.util.List;

public record Rozliczenie (
        long rezerwacjaId,
        String status,
        BigDecimal cenaPobytu,
        BigDecimal naleznosc,
        BigDecimal wplaty,
        BigDecimal zwroty,
        List<Platnosc> platnosci
){

    public BigDecimal zaplaconoNetto(){
        return wplaty.subtract(zwroty);
    }
}
