package pl.hotel.model;

import java.time.LocalDateTime;

public record ZmianaRezerwacji(long id, long pracownikId, String login, String akcja,
        String przed, String po, LocalDateTime utworzono) {
    public String opisAkcji() {
        return switch (akcja) {
            case "UTWORZENIE" -> "Utworzenie rezerwacji";
            case "ZMIANA_TERMINU" -> "Zmiana terminu";
            case "ZMIANA_POKOJU" -> "Zmiana pokoju";
            case "ZAMELDOWANIE" -> "Zameldowanie";
            case "WYMELDOWANIE" -> "Wymeldowanie";
            case "ANULOWANIE" -> "Anulowanie";
            default -> akcja;
        };
    }
    public String szczegoly() {
        return "Przed zmianą:\n" + (przed == null ? "Brak — nowa rezerwacja." : przed)
                + "\n\nPo zmianie:\n" + po;
    }
}
