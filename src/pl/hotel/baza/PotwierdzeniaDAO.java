package pl.hotel.baza;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import pl.hotel.model.PotwierdzenieRezerwacji;

final class PotwierdzeniaDAO {
    private PotwierdzeniaDAO() {}

    static PotwierdzenieRezerwacji pobierz(int id) throws SQLException {
        if (id < 1) throw new SQLException("Podaj poprawne ID rezerwacji.");
        try (Connection c = PolaczenieBazy.polacz()) {
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            c.setReadOnly(true); c.setAutoCommit(false);
            try {
                PotwierdzenieRezerwacji wynik;
                String sql = "SELECT g.imie, g.nazwisko, p.numer, p.typ, r.data_od, r.data_do, "
                        + "r.liczba_osob, r.cena_za_noc, r.status, CURRENT_TIMESTAMP(6) AS czas "
                        + "FROM rezerwacje r JOIN goscie g ON g.id=r.gosc_id "
                        + "JOIN pokoje p ON p.id=r.pokoj_id WHERE r.id=?";
                try (PreparedStatement q = c.prepareStatement(sql)) {
                    q.setInt(1, id);
                    try (ResultSet r = q.executeQuery()) {
                        if (!r.next()) throw new SQLException("Nie znaleziono rezerwacji.");
                        var osoby = uczestnicy(c, id);
                        BigDecimal[] kwoty = kwoty(c, id);
                        wynik = new PotwierdzenieRezerwacji(id, r.getString("imie") + " " + r.getString("nazwisko"),
                                r.getString("numer"), r.getString("typ"), r.getDate("data_od").toLocalDate(),
                                r.getDate("data_do").toLocalDate(), r.getInt("liczba_osob"), r.getBigDecimal("cena_za_noc"),
                                r.getString("status"), osoby, kwoty[0], kwoty[1], r.getTimestamp("czas").toLocalDateTime());
                    }
                }
                c.commit(); return wynik;
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }

    private static ArrayList<String> uczestnicy(Connection c, int id) throws SQLException {
        ArrayList<String> osoby = new ArrayList<>();
        try (PreparedStatement q = c.prepareStatement(
                "SELECT imie, nazwisko FROM uczestnicy_rezerwacji WHERE rezerwacja_id=? ORDER BY pozycja")) {
            q.setInt(1, id);
            try (ResultSet r = q.executeQuery()) {
                while (r.next()) osoby.add(r.getString("imie") + " " + r.getString("nazwisko"));
            }
        }
        return osoby;
    }

    private static BigDecimal[] kwoty(Connection c, int id) throws SQLException {
        try (PreparedStatement q = c.prepareStatement(
                "SELECT COALESCE(SUM(CASE WHEN rodzaj='WPLATA' THEN kwota ELSE 0 END),0) AS wplaty, "
                        + "COALESCE(SUM(CASE WHEN rodzaj='ZWROT' THEN kwota ELSE 0 END),0) AS zwroty "
                        + "FROM platnosci WHERE rezerwacja_id=?")) {
            q.setInt(1, id);
            try (ResultSet r = q.executeQuery()) {
                r.next(); return new BigDecimal[]{r.getBigDecimal("wplaty"),r.getBigDecimal("zwroty")};
            }
        }
    }
}
