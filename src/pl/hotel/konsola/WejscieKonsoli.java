package pl.hotel.konsola;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Obsługa konsoli: WejscieKonsoli. */
final class WejscieKonsoli {
    private WejscieKonsoli() {}

    static String wczytajNiepustyTekst(String komunikat) {
        while (true) {
            System.out.print(komunikat);
            if (!StanKonsoli.scanner.hasNextLine()) {
                return null;
            }
            String tekst = StanKonsoli.scanner.nextLine().trim();

            if (!tekst.isEmpty()) {
                return tekst;
            }

            System.out.println("To pole nie może być puste.");
        }
    }

    static Integer wczytajLiczbe(String komunikat){
        while (true){
            String tekst = wczytajNiepustyTekst(komunikat);
            if (tekst == null) return null;
            try {
                int liczba = Integer.parseInt(tekst);
                if (liczba > 0) return liczba;

            } catch (NumberFormatException e){

            }
            System.out.println("Wpisz dodatnią liczbe całkowitą.");
        }
    }

    static LocalDate wczytajDate(String komunikat){
        while (true){
            String tekst = wczytajNiepustyTekst(komunikat + " (RRRR-MM-DD): ");
            if (tekst == null) return null;
            try {
                return LocalDate.parse(tekst);
            } catch (DateTimeParseException e){
                System.out.println("Niepoprawna data. Przykład: 2026-12-23");
            }
        }
    }

    static boolean czyPoprawnyEmail(String email) {
        return email != null
                && email.length() <= 120
                && email.matches(
                "^[A-Za-z0-9_%+-]+(?:\\.[A-Za-z0-9_%+-]+)*"
                        + "@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+"
                        + "[A-Za-z]{2,63}$"
        );
    }

    static BigDecimal wczytajCene(String komunikat){
        while (true){
            String tekst = wczytajNiepustyTekst(komunikat);

            if (tekst == null) return null;

            tekst = tekst.replace(',','.');

            if (tekst.matches("[0-9]{1,8}(\\.[0-9]{1,2})?")) {
                BigDecimal cena = new BigDecimal(tekst);

                if (cena.signum() > 0) {
                    return cena;
                }
            }

            System.out.println("Podaj cenę od 0.01 do 99999999.99, np. 250,50.");
        }
    }
}
