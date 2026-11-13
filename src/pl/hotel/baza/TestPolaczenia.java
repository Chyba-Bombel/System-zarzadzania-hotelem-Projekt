package pl.hotel.baza;

import pl.hotel.konfiguracja.KonfiguracjaAplikacji;
import java.sql.SQLException;
import java.util.Properties;

public final class TestPolaczenia {
    private TestPolaczenia() {}

    public static String sprawdz(Properties formularz) throws SQLException {
        Properties dane = KonfiguracjaAplikacji.efektywne(formularz);
        try (var polaczenie = PolaczenieBazy.polacz(dane);
             var zapytanie = polaczenie.prepareStatement("SELECT 1")) {
            zapytanie.setQueryTimeout(5);
            try (var wynik = zapytanie.executeQuery()) {
                if (!wynik.next() || wynik.getInt(1) != 1) throw new SQLException("Nie potwierdzono odpowiedzi serwera.");
            }
        }
        return "Połączenie działa. Serwer wykonał SELECT 1. Test nie zapisuje ustawień i nie sprawdza całej struktury bazy.";
    }
}
