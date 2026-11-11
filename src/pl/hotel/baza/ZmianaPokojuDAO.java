package pl.hotel.baza;

import java.sql.*;

final class ZmianaPokojuDAO {
    private ZmianaPokojuDAO() {}

    private record Pokoj(int miejsca, String stan) {}

    static void zmienPokoj(int id, long poprzedniId, long nowyId) throws SQLException {
        if (id < 1 || poprzedniId < 1 || nowyId < 1)
            throw new SQLException("Podaj poprawne ID rezerwacji i pokoi.");
        if (poprzedniId == nowyId) throw new SQLException("Wybierz inny pokój.");
        try (Connection c = PolaczenieBazy.polacz()) {
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            c.setAutoCommit(false);
            try {
                Pokoj pierwszy = zablokuj(c, Math.min(poprzedniId, nowyId));
                Pokoj drugi = zablokuj(c, Math.max(poprzedniId, nowyId));
                Pokoj nowy = nowyId < poprzedniId ? pierwszy : drugi;
                String status;
                Date od;
                Date doDnia;
                int osoby;
                try (PreparedStatement q = c.prepareStatement(
                        "SELECT pokoj_id, status, data_od, data_do, liczba_osob "
                                + "FROM rezerwacje WHERE id = ? FOR UPDATE")) {
                    q.setInt(1, id);
                    try (ResultSet r = q.executeQuery()) {
                        if (!r.next()) throw new SQLException("Nie znaleziono rezerwacji.");
                        if (r.getLong("pokoj_id") != poprzedniId)
                            throw new SQLException("Pokój rezerwacji został zmieniony. Odśwież dane i spróbuj ponownie.");
                        status = r.getString("status");
                        od = r.getDate("data_od");
                        doDnia = r.getDate("data_do");
                        osoby = r.getInt("liczba_osob");
                    }
                }
                boolean zameldowana = "ZAMELDOWANA".equals(status);
                if (!zameldowana && !"POTWIERDZONA".equals(status))
                    throw new SQLException("Pokój można zmienić tylko w potwierdzonej lub zameldowanej rezerwacji.");
                if (!doDnia.toLocalDate().isAfter(java.time.LocalDate.now()))
                    throw new SQLException("Nie można przenieść pobytu, którego termin wyjazdu już minął lub przypada dzisiaj.");
                if ("WYLACZONY".equals(nowy.stan()))
                    throw new SQLException("Nowy pokój jest wyłączony z użytkowania.");
                if (osoby > nowy.miejsca()) throw new SQLException("Nowy pokój ma za mało miejsc.");
                if (zameldowana && !"GOTOWY".equals(nowy.stan()))
                    throw new SQLException("Dla zameldowanych gości nowy pokój musi być gotowy.");
                try (PreparedStatement q = c.prepareStatement(
                        "SELECT id FROM rezerwacje WHERE pokoj_id = ? AND id <> ? "
                                + "AND status IN ('POTWIERDZONA', 'ZAMELDOWANA') "
                                + "AND data_od < ? AND data_do > ?")) {
                    q.setLong(1, nowyId); q.setInt(2, id); q.setDate(3, doDnia); q.setDate(4, od);
                    try (ResultSet r = q.executeQuery()) {
                        if (r.next()) throw new SQLException("Nowy pokój jest zajęty w terminie rezerwacji.");
                    }
                }
                try (PreparedStatement q = c.prepareStatement("UPDATE rezerwacje SET pokoj_id = ? WHERE id = ?")) {
                    q.setLong(1, nowyId); q.setInt(2, id);
                    if (q.executeUpdate() != 1) throw new SQLException("Nie potwierdzono zmiany pokoju.");
                }
                if (zameldowana) {
                    try (PreparedStatement q = c.prepareStatement(
                            "UPDATE pokoje SET stan = 'DO_SPRZATANIA' WHERE id = ? AND stan <> 'WYLACZONY'")) {
                        q.setLong(1, poprzedniId); q.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }

    private static Pokoj zablokuj(Connection c, long id) throws SQLException {
        try (PreparedStatement q = c.prepareStatement(
                "SELECT liczba_miejsc, stan FROM pokoje WHERE id = ? FOR UPDATE")) {
            q.setLong(1, id);
            try (ResultSet r = q.executeQuery()) {
                if (!r.next()) throw new SQLException("Nie znaleziono pokoju.");
                return new Pokoj(r.getInt("liczba_miejsc"), r.getString("stan"));
            }
        }
    }
}
