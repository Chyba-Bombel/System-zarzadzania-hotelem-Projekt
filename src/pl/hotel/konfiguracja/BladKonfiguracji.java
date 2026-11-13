package pl.hotel.konfiguracja;

import java.sql.SQLException;

public final class BladKonfiguracji extends SQLException {
    public BladKonfiguracji(String komunikat, Throwable przyczyna) { super(komunikat, przyczyna); }
}
