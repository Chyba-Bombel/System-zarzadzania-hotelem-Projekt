package pl.hotel.diagnostyka;

import pl.hotel.konfiguracja.WersjaAplikacji;

public final class InformacjeDiagnostyczne {
    private InformacjeDiagnostyczne() {}

    public static String tekst() {
        return "HotelManager " + WersjaAplikacji.NUMER
                + "\nJava: " + System.getProperty("java.version", "nieznana")
                + "\nSystem: " + System.getProperty("os.name", "nieznany")
                + "\nArchitektura: " + System.getProperty("os.arch", "nieznana");
    }
}
