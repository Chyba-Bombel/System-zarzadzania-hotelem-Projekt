package pl.hotel.baza;

import pl.hotel.bezpieczenstwo.Hasla;
import java.security.GeneralSecurityException;
import java.sql.*;

final class HaslaDAO {
    private HaslaDAO() {}

    public static void zmienHaslo(
            long pracownikId,
            String obecneHaslo,
            String noweHaslo
    ) throws SQLException {

        if (obecneHaslo == null || obecneHaslo.length() > 128 || noweHaslo == null) {
            throw new SQLException("Nie podano hasła.");
        }

        if (noweHaslo.length() < 12 || noweHaslo.length() > 128) {
            throw new SQLException(
                    "Nowe hasło musi mieć od 12 do 128 znaków."
            );
        }

        if (noweHaslo.equals(obecneHaslo)) {
            throw new SQLException(
                    "Nowe hasło musi różnić się od obecnego."
            );
        }

        try (Connection polaczenie = PolaczenieBazy.polacz()) {

            polaczenie.setAutoCommit(false);

            try {
                String zapisanyHash;

                String sqlKonto = "SELECT hash_hasla FROM pracownicy " + "WHERE id = ? AND aktywny = TRUE " + "FOR UPDATE";

                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlKonto)) {
                    zapytanie.setLong(1, pracownikId);

                    try (ResultSet wynik = zapytanie.executeQuery()) {
                        if (!wynik.next()) {
                            throw new SQLException("Konto nie istnieje lub jest nieaktywne.");
                        }

                        zapisanyHash = wynik.getString("hash_hasla");
                    }
                }

                String nowyHash;

                try {
                    if (!Hasla.sprawdz(obecneHaslo, zapisanyHash)) {
                        throw new SQLException("Obecne hasło jest niepoprawne.");
                    }

                    nowyHash = Hasla.utworzHash(noweHaslo);

                } catch (GeneralSecurityException e) {
                    throw new SQLException("Nie udało się przetworzyć hasła.", e);
                }

                String sqlZmiana = "UPDATE pracownicy " + "SET hash_hasla = ? WHERE id = ?";

                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlZmiana)) {
                    zapytanie.setString(1, nowyHash);
                    zapytanie.setLong(2, pracownikId);

                    zapytanie.executeUpdate();
                }

                polaczenie.commit();

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
