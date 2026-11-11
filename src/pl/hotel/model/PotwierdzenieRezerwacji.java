package pl.hotel.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public record PotwierdzenieRezerwacji(int id, String glownyGosc, String pokoj, String typ,
        LocalDate od, LocalDate doDnia, int liczbaOsob, BigDecimal cenaZaNoc, String status,
        List<String> uczestnicy, BigDecimal wplaty, BigDecimal zwroty, LocalDateTime utworzono) {
    public PotwierdzenieRezerwacji {
        Objects.requireNonNull(glownyGosc); Objects.requireNonNull(pokoj); Objects.requireNonNull(typ);
        Objects.requireNonNull(od); Objects.requireNonNull(doDnia); Objects.requireNonNull(cenaZaNoc);
        Objects.requireNonNull(wplaty); Objects.requireNonNull(zwroty); Objects.requireNonNull(utworzono);
        uczestnicy = List.copyOf(uczestnicy);
        if (id < 1 || !doDnia.isAfter(od) || liczbaOsob < 1 || liczbaOsob > 10
                || uczestnicy.size() > liczbaOsob - 1 || cenaZaNoc.signum() <= 0
                || wplaty.signum() < 0 || zwroty.signum() < 0
                || !List.of("POTWIERDZONA", "ZAMELDOWANA", "ZAKONCZONA", "ANULOWANA").contains(status))
            throw new IllegalArgumentException("Niepoprawne dane potwierdzenia rezerwacji.");
    }
    public long noce() { return ChronoUnit.DAYS.between(od, doDnia); }
    public BigDecimal wartoscPobytu() { return cenaZaNoc.multiply(BigDecimal.valueOf(noce())); }
    public boolean anulowana() { return status.equals("ANULOWANA"); }
    public BigDecimal naleznosc() { return anulowana() ? new BigDecimal("0.00") : wartoscPobytu(); }
    public BigDecimal netto() { return wplaty.subtract(zwroty); }
    public BigDecimal saldo() { return naleznosc().subtract(netto()); }
    public int brakujaceOsoby() { return liczbaOsob - 1 - uczestnicy.size(); }
    public String opisStatusu() {
        return switch (status) {
            case "POTWIERDZONA" -> "Potwierdzona";
            case "ZAMELDOWANA" -> "Zameldowana";
            case "ZAKONCZONA" -> "Zakończona";
            case "ANULOWANA" -> "Anulowana";
            default -> status;
        };
    }
}
