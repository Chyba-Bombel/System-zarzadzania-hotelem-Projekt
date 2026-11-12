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
            case "WPLATA" -> "Przyjęcie wpłaty";
            case "ZWROT" -> "Zwrot środków";
            case "DODANIE_USLUGI" -> "Dodanie usługi";
            case "ANULOWANIE_USLUGI" -> "Anulowanie usługi";
            case "ZMIANA_NOTATKI" -> "Zmiana notatki";
            case "ZMIANA_UCZESTNIKOW" -> "Zmiana gości pobytu";
            default -> akcja;
        };
    }
    public String szczegoly() {
        return "Przed zmianą:\n" + (przed == null ? (akcja.equals("UTWORZENIE")?"Brak — nowa rezerwacja.":"Brak poprzedniego stanu — wpis odtworzony z ewidencji.") : przed)
                + "\n\nPo zmianie:\n" + po;
    }
}
