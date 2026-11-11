package pl.hotel.baza;

import java.sql.*;

/** Operacje obszaru: PolaczenieBazy. */
final class PolaczenieBazy {
    private PolaczenieBazy() {}

    public static Connection polacz() throws SQLException {
        java.util.Properties p = new java.util.Properties();
        java.nio.file.Path plik = java.nio.file.Path.of(
                System.getProperty("hotel.config", "db.properties"));
        try (java.io.Reader r = java.nio.file.Files.newBufferedReader(
                plik, java.nio.charset.StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (java.io.IOException e) {
            throw new SQLException("Nie można odczytać " + plik.toAbsolutePath()
                    + ". Sprawdź katalog roboczy programu.", e);
        }
        for (String klucz : new String[]{"url", "user", "password"}) {
            if (!p.containsKey(klucz)) throw new SQLException("Brak pola " + klucz + " w db.properties.");
        }
        return DriverManager.getConnection(p.getProperty("url"), p.getProperty("user"), p.getProperty("password"));
    }
}
