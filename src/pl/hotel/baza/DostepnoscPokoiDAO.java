package pl.hotel.baza;

import java.sql.*;
import java.util.List;
import pl.hotel.model.Pokoj;

final class DostepnoscPokoiDAO {
    private DostepnoscPokoiDAO() {}

    static Pokoj zmien(long id, String oczekiwanyStan, boolean wylacz) throws SQLException {
        if (id < 1) throw new SQLException("Podaj poprawne ID pokoju.");
        if (oczekiwanyStan == null || !List.of("GOTOWY","DO_SPRZATANIA","WYLACZONY").contains(oczekiwanyStan))
            throw new SQLException("Najpierw pobierz aktualny stan pokoju.");
        try (Connection c = PolaczenieBazy.polacz()) {
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED); c.setAutoCommit(false);
            try {
                Pokoj pokoj;
                try (PreparedStatement q = c.prepareStatement(
                        "SELECT numer, liczba_miejsc, cena_za_noc, stan FROM pokoje WHERE id=? FOR UPDATE")) {
                    q.setLong(1,id);
                    try (ResultSet r = q.executeQuery()) {
                        if (!r.next()) throw new SQLException("Nie znaleziono pokoju.");
                        pokoj = new Pokoj(id,r.getString("numer"),r.getInt("liczba_miejsc"),r.getBigDecimal("cena_za_noc"),r.getString("stan"));
                    }
                }
                if (!pokoj.getStan().equals(oczekiwanyStan))
                    throw new SQLException("Stan pokoju został zmieniony. Odśwież dane i spróbuj ponownie.");
                if (wylacz && pokoj.getStan().equals("WYLACZONY")) throw new SQLException("Pokój jest już wyłączony.");
                if (!wylacz && !pokoj.getStan().equals("WYLACZONY")) throw new SQLException("Przywrócić można tylko wyłączony pokój.");
                try (PreparedStatement q = c.prepareStatement(
                        "SELECT id FROM rezerwacje WHERE pokoj_id=? AND status IN ('POTWIERDZONA','ZAMELDOWANA') ORDER BY id LIMIT 1")) {
                    q.setLong(1,id);
                    try (ResultSet r = q.executeQuery()) {
                        if (r.next()) throw new SQLException("Pokój ma aktywną rezerwację #" + r.getLong("id")
                                + ". Najpierw przenieś, anuluj lub zakończ aktywne pobyty tego pokoju.");
                    }
                }
                String nowyStan = wylacz ? "WYLACZONY" : "DO_SPRZATANIA";
                try (PreparedStatement q = c.prepareStatement("UPDATE pokoje SET stan=? WHERE id=?")) {
                    q.setString(1,nowyStan); q.setLong(2,id);
                    if (q.executeUpdate() != 1) throw new SQLException("Nie potwierdzono zmiany stanu pokoju.");
                }
                c.commit();
                return new Pokoj(id,pokoj.getNumer(),pokoj.getLiczbaMiejsc(),pokoj.getCenaZaNoc(),nowyStan);
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }
}
