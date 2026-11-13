package pl.hotel.konfiguracja;

import java.util.Properties;

public final class WersjaAplikacji {
    public static final String NUMER = odczytaj();
    private WersjaAplikacji() {}

    private static String odczytaj() {
        try (var in = WersjaAplikacji.class.getResourceAsStream("/wersja.properties")) {
            if (in == null) return "nieznana";
            Properties p = new Properties();
            p.load(in);
            return p.getProperty("version", "nieznana");
        } catch (java.io.IOException e) { return "nieznana"; }
    }
}
