package pl.hotel.baza;

import java.sql.*;
import java.math.BigDecimal;

final class AnulowanieRezerwacjiDAO {
    private AnulowanieRezerwacjiDAO() {}

    public static boolean anulujRezerwacje(int id)
            throws SQLException {

        try (Connection polaczenie = PolaczenieBazy.polacz()) {

            polaczenie.setTransactionIsolation(
                    Connection.TRANSACTION_READ_COMMITTED
            );

            polaczenie.setAutoCommit(false);

            try {
                String sqlRezerwacja = "SELECT status FROM rezerwacje "
                                + "WHERE id = ? FOR UPDATE";

                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlRezerwacja)) {

                    zapytanie.setInt(1, id);

                    try (ResultSet wynik = zapytanie.executeQuery()) {

                        if (!wynik.next()) {
                            throw new SQLException(
                                    "Nie znaleziono rezerwacji."
                            );
                        }

                        if (!wynik.getString("status").equals("POTWIERDZONA")) {

                            throw new SQLException("Anulować można tylko potwierdzoną rezerwację.");
                        }
                    }
                }

                BigDecimal netto = RozliczeniaDAO.pobierzNetto(polaczenie, id);

                if (netto.signum() > 0) {
                    throw new SQLException("Przed anulowaniem zwróć pozostałe środki: " + netto + " zł. Użyj opcji 19."
                    );
                }

                if (netto.signum() < 0) {
                    throw new SQLException("Nieprawidłowe rozliczenie: zwroty przekraczają wpłaty."
                    );
                }

                String sqlZmiana = "UPDATE rezerwacje "
                                + "SET status = 'ANULOWANA' "
                                + "WHERE id = ?";

                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlZmiana)) {
                    zapytanie.setInt(1, id);
                    zapytanie.executeUpdate();
                }

                polaczenie.commit();

                return true;

            } catch (SQLException | RuntimeException e) {
                try {
                    polaczenie.rollback();
                } catch (SQLException bladWycofania) {
                    e.addSuppressed(bladWycofania);
                }

                throw e;
            }
        }
    }
}
