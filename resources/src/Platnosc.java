import java.math.BigDecimal;
import java.time.LocalDateTime;
public record Platnosc (
        long id,
        BigDecimal kwota,
        String rodzaj,
        String metoda,
        LocalDateTime data
){
}
