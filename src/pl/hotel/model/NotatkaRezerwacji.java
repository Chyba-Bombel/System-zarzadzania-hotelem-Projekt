package pl.hotel.model;

import java.util.Objects;

public record NotatkaRezerwacji(int rezerwacjaId, String status, String tresc) {
    public NotatkaRezerwacji {
        if (rezerwacjaId < 1) throw new IllegalArgumentException("Niepoprawne ID rezerwacji.");
        Objects.requireNonNull(status); Objects.requireNonNull(tresc);
    }
    public boolean edytowalna() {
        return status.equals("POTWIERDZONA") || status.equals("ZAMELDOWANA");
    }
}
