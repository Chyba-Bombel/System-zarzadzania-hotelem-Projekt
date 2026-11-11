package pl.hotel.baza;

import pl.hotel.model.*;
import java.sql.*;

final class UczestnicyDAO {
    private UczestnicyDAO() {}

    static void zapiszUczestnikow(Connection c, int id, java.util.List<Uczestnik> osoby) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("DELETE FROM uczestnicy_rezerwacji WHERE rezerwacja_id=?")) {
            p.setInt(1, id); p.executeUpdate();
        }
        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO uczestnicy_rezerwacji (rezerwacja_id, pozycja, imie, nazwisko) VALUES (?, ?, ?, ?)")) {
            int pozycja = 1;
            for (Uczestnik u : osoby) {
                p.setInt(1,id); p.setInt(2,pozycja++); p.setString(3,u.imie()); p.setString(4,u.nazwisko()); p.addBatch();
            }
            p.executeBatch();
        }
    }

    public static void zmienUczestnikow(int id, java.util.List<Uczestnik> uczestnicy) throws SQLException {
        java.util.List<Uczestnik> osoby = java.util.List.copyOf(uczestnicy);
        try (Connection c = PolaczenieBazy.polacz()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement("SELECT liczba_osob, status FROM rezerwacje WHERE id=? FOR UPDATE")) {
                    p.setInt(1,id);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new SQLException("Nie znaleziono rezerwacji.");
                        if (!java.util.Set.of("POTWIERDZONA", "ZAMELDOWANA").contains(r.getString("status")))
                            throw new SQLException("Możesz zmieniać osoby tylko w potwierdzonym lub zameldowanym pobycie.");
                        if (osoby.size() != r.getInt("liczba_osob") - 1)
                            throw new SQLException("Liczba osób rezerwacji uległa zmianie. Odśwież dane.");
                    }
                }
                zapiszUczestnikow(c,id,osoby);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }
}
