package pl.hotel.baza;

import pl.hotel.model.*;
import pl.hotel.bezpieczenstwo.Hasla;
import java.security.GeneralSecurityException;
import java.sql.*;
import java.util.ArrayList;

final class PracownicyDAO {
    private PracownicyDAO() {}

    public static ArrayList<Pracownik> pobierzPracownikow() throws SQLException{
        ArrayList<Pracownik> lista = new ArrayList<>();

        String sql =
                "SELECT imie, nazwisko, rola "
                        + "FROM pracownicy "
                        + "WHERE aktywny = TRUE "
                        + "ORDER BY id";

        try (Connection polaczenie = PolaczenieBazy.polacz();
        PreparedStatement zapytanie = polaczenie.prepareStatement(sql);
        ResultSet wynik = zapytanie.executeQuery()
        ){
            while (wynik.next()){
                Pracownik pracownik = new Pracownik(
                        wynik.getString("imie"),
                        wynik.getString("nazwisko"),
                        wynik.getString("rola")
                );

                lista.add(pracownik);
            }
        }
        return lista;

    }

    public static void dodajPracownika(String imie, String nazwisko, String login, String haslo, String rola) throws SQLException{
        pl.hotel.walidacja.Dane.tekst(imie,60,"Imię");
        pl.hotel.walidacja.Dane.tekst(nazwisko,80,"Nazwisko");
        if (imie == null || imie.isBlank() || imie.length() > 60 || nazwisko == null || nazwisko.isBlank() || nazwisko.length() > 80){
            throw new SQLException("Niepoprawne imię lub nazwisko");
        }
        if (login == null || !login.matches("[A-Za-z0-9._-]{3,60}")){
            throw new SQLException("Login: 3-60 znakow. Dozwolone są litery bez polskich znaków, cyfry, kropka. _ i -.");
        }
        if (haslo == null || haslo.length() < 12 || haslo.length() > 128){
            throw new SQLException("Hasło musi mieć od 12 do 128 znaków.");
        }
        if (!"ADMIN".equals(rola) && !"RECEPCJA".equals(rola)){
            throw new SQLException("Niepoprawna rola.");
        }
        String hash;

        try {
            hash = Hasla.utworzHash(haslo);
        } catch (GeneralSecurityException e) {
            throw new SQLException("Nie udało się przygotować hasła.", e);

        }
        String sql = "INSERT INTO pracownicy "
                        + "(imie, nazwisko, login, hash_hasla, rola, aktywny) "
                        + "VALUES (?, ?, ?, ?, ?, TRUE)";
        try (Connection polaczenie = PolaczenieBazy.polacz(); PreparedStatement zapytanie = polaczenie.prepareStatement(sql)){
            zapytanie.setString(1, imie);
            zapytanie.setString(2, nazwisko);
            zapytanie.setString(3, login);
            zapytanie.setString(4, hash);
            zapytanie.setString(5, rola);

            zapytanie.executeUpdate();
        }
    }

    public static ArrayList<KontoPracownika> pobierzKontaPracownikow() throws SQLException{
        ArrayList<KontoPracownika> lista = new ArrayList<>();

        String sql = "SELECT id, imie, nazwisko, login, rola, aktywny " + "FROM pracownicy ORDER BY id";

        try (Connection polaczenie = PolaczenieBazy.polacz(); PreparedStatement zapytanie = polaczenie.prepareStatement(sql); ResultSet wynik = zapytanie.executeQuery()){
            while (wynik.next()){
                lista.add(new KontoPracownika(
                   wynik.getLong("id"),
                   wynik.getString("imie"),
                   wynik.getString("nazwisko"),
                   wynik.getString("login"),
                   wynik.getString("rola"),
                   wynik.getBoolean("aktywny")
                ));
            }

        }
        return lista;
    }

    public static void dezaktywujPracownika(
            long kontoId,
            long administratorId
    ) throws SQLException {

        if (kontoId == administratorId) {
            throw new SQLException("Nie możesz dezaktywować własnego konta.");
        }

        try (Connection polaczenie = PolaczenieBazy.polacz()) {

            polaczenie.setTransactionIsolation(
                    Connection.TRANSACTION_READ_COMMITTED
            );

            polaczenie.setAutoCommit(false);

            try {
                boolean administratorUprawniony = false;
                boolean znalezionoKonto = false;
                boolean kontoAktywne = false;
                String rolaKonta = null;
                int aktywniAdministratorzy = 0;

                String sqlKonta = "SELECT id, rola, aktywny FROM pracownicy " + "ORDER BY id FOR UPDATE";

                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlKonta); ResultSet wynik = zapytanie.executeQuery()) {
                    while (wynik.next()) {
                        long id = wynik.getLong("id");
                        String rola = wynik.getString("rola");
                        boolean aktywny = wynik.getBoolean("aktywny");

                        if (aktywny && rola.equals("ADMIN")) {
                            aktywniAdministratorzy++;
                        }

                        if (id == administratorId && aktywny && rola.equals("ADMIN")) {
                            administratorUprawniony = true;
                        }

                        if (id == kontoId) {
                            znalezionoKonto = true;
                            kontoAktywne = aktywny;
                            rolaKonta = rola;
                        }
                    }
                }

                if (!administratorUprawniony) {
                    throw new SQLException(
                            "Operacja wymaga aktywnego konta administratora."
                    );
                }

                if (!znalezionoKonto) {
                    throw new SQLException(
                            "Nie znaleziono konta o podanym ID."
                    );
                }

                if (!kontoAktywne) {
                    throw new SQLException(
                            "To konto jest już nieaktywne."
                    );
                }

                if ("ADMIN".equals(rolaKonta) && aktywniAdministratorzy <= 1) {
                    throw new SQLException("Nie można dezaktywować ostatniego " + "aktywnego administratora.");
                }

                String sqlZmiana = "UPDATE pracownicy " + "SET aktywny = FALSE WHERE id = ?";

                try (PreparedStatement zapytanie =
                             polaczenie.prepareStatement(sqlZmiana)) {

                    zapytanie.setLong(1, kontoId);
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
