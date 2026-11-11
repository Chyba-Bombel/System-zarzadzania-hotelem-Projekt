package pl.hotel.baza;

import pl.hotel.model.*;
import pl.hotel.bezpieczenstwo.Hasla;
import java.security.GeneralSecurityException;
import java.sql.*;

/** Operacje obszaru: LogowanieDAO. */
final class LogowanieDAO {
    private LogowanieDAO() {}

    public static Pracownik zaloguj(String login, String haslo) throws SQLException{
        String sql = "SELECT id, imie, nazwisko, login, hash_hasla, rola "
                        + "FROM pracownicy "
                        + "WHERE login = ? AND aktywny = TRUE";
        try (Connection polaczenie = PolaczenieBazy.polacz(); PreparedStatement zapytanie = polaczenie.prepareStatement(sql)){
            zapytanie.setString(1, login);
            try (ResultSet wynik = zapytanie.executeQuery()){
                if (!wynik.next()){
                    return null;
                }

                boolean poprawneHaslo;

                try {
                    poprawneHaslo = Hasla.sprawdz(haslo, wynik.getString("hash_hasla"));
                } catch (GeneralSecurityException e){
                    throw new SQLException("Nie udało sie sprawdzić hasła.",e);
                }

                if (!poprawneHaslo){
                    return null;
                }
                String rola = wynik.getString("rola");
                if (!"ADMIN".equals(rola) && !"RECEPCJA".equals(rola)){
                    return null;
                }
                return new Pracownik(wynik.getLong("id"), wynik.getString("imie"), wynik.getString("nazwisko"), wynik.getString("login"), rola);


            }

        }
    }
}
