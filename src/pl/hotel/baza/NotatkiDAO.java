package pl.hotel.baza;

import java.sql.*;
import pl.hotel.model.NotatkaRezerwacji;

final class NotatkiDAO {
    private NotatkiDAO() {}

    static NotatkaRezerwacji pobierz(int id) throws SQLException {
        if (id < 1) throw new SQLException("Podaj poprawne ID rezerwacji.");
        try (Connection c = PolaczenieBazy.polacz()) { return odczytaj(c, id, false); }
    }

    static NotatkaRezerwacji zapisz(int id, String oczekiwanaTresc, String nowaTresc) throws SQLException {
        if (id < 1) throw new SQLException("Podaj poprawne ID rezerwacji.");
        if (oczekiwanaTresc == null || oczekiwanaTresc.codePointCount(0, oczekiwanaTresc.length()) > 500)
            throw new IllegalArgumentException("Najpierw pobierz aktualną notatkę rezerwacji.");
        String tekst = przygotuj(nowaTresc);
        try (Connection c = PolaczenieBazy.polacz()) {
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED); c.setAutoCommit(false);
            try {
                NotatkaRezerwacji aktualna = odczytaj(c, id, true);
                if (!aktualna.edytowalna()) throw new SQLException("Notatka anulowanej lub zakończonej rezerwacji jest tylko do odczytu.");
                if (!aktualna.tresc().equals(oczekiwanaTresc))
                    throw new SQLException("Notatka została zmieniona na innym stanowisku. Odśwież zapisaną treść i porównaj swój tekst przed ponownym zapisem.");
                if (!aktualna.tresc().equals(tekst)) {
                    try (PreparedStatement q = c.prepareStatement("UPDATE rezerwacje SET uwagi=? WHERE id=?")) {
                        q.setString(1, tekst); q.setInt(2, id);
                        if (q.executeUpdate() != 1) throw new SQLException("Nie potwierdzono zapisu notatki.");
                    }
                }
                c.commit(); return new NotatkaRezerwacji(id, aktualna.status(), tekst);
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }

    private static NotatkaRezerwacji odczytaj(Connection c, int id, boolean blokuj) throws SQLException {
        try (PreparedStatement q = c.prepareStatement("SELECT status, uwagi FROM rezerwacje WHERE id=?" + (blokuj ? " FOR UPDATE" : ""))) {
            q.setInt(1, id);
            try (ResultSet r = q.executeQuery()) {
                if (!r.next()) throw new SQLException("Nie znaleziono rezerwacji.");
                return new NotatkaRezerwacji(id, r.getString("status"), r.getString("uwagi"));
            }
        }
    }

    private static String przygotuj(String tekst) {
        if (tekst == null) throw new IllegalArgumentException("Podaj tekst notatki. Pusty tekst usuwa jej treść.");
        String wynik = tekst.replace("\r\n", "\n").replace('\r', '\n');
        if (wynik.codePointCount(0, wynik.length()) > 500)
            throw new IllegalArgumentException("Notatka może mieć najwyżej 500 znaków.");
        if (wynik.codePoints().anyMatch(cp -> cp >= 0xD800 && cp <= 0xDFFF
                || Character.isISOControl(cp) && cp != '\n' && cp != '\t'))
            throw new IllegalArgumentException("Notatka zawiera niedozwolony znak sterujący lub niepoprawny znak Unicode.");
        return wynik.isBlank() ? "" : wynik;
    }
}
