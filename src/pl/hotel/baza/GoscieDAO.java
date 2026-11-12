package pl.hotel.baza;

import pl.hotel.model.*;
import java.sql.*;
import java.util.ArrayList;

final class GoscieDAO {
    private GoscieDAO() {}

    public static ArrayList<Gosc> pobierzGosci()
            throws SQLException {

        ArrayList<Gosc> lista = new ArrayList<>();

        String sql =
                "SELECT g.*,k.login AS archiwizowal_login "
                        + "FROM goscie g LEFT JOIN pracownicy k ON k.id=g.archiwizowal_pracownik_id ORDER BY g.id";

        try (
                Connection polaczenie = PolaczenieBazy.polacz();

                PreparedStatement zapytanie =
                        polaczenie.prepareStatement(sql);

                ResultSet wynik = zapytanie.executeQuery()
        ) {
            while (wynik.next()) {

                long idZBazy = wynik.getLong("id");

                if (idZBazy > Integer.MAX_VALUE || idZBazy < 1) {
                    throw new SQLException(
                            "ID gościa nie mieści się w typie int."
                    );
                }

                int id = (int) idZBazy;
                String imie = wynik.getString("imie");
                String nazwisko = wynik.getString("nazwisko");
                String telefon = wynik.getString("telefon");
                String email = wynik.getString("email");

                Gosc gosc = new Gosc(
                        id,
                        imie,
                        nazwisko,
                        telefon,
                        email,
                        wynik.getBoolean("archiwizowany"),
                        wynik.getTimestamp("archiwizowano")==null?null:wynik.getTimestamp("archiwizowano").toLocalDateTime(),
                        wynik.getString("archiwizowal_login")
                );

                lista.add(gosc);
            }
        }

        return lista;
    }

    public static Gosc zapiszGoscia(String imie, String nazwisko, String telefon, String email) throws SQLException{
        pl.hotel.walidacja.Dane.gosc(imie,nazwisko,telefon,email);
        String sql = "INSERT INTO goscie (imie, nazwisko, telefon, email) "
                + "VALUES (?, ?, ?, ?)";

        try (
            Connection polaczenie = PolaczenieBazy.polacz();

            PreparedStatement zapytanie = polaczenie.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)

        ){
            zapytanie.setString(1, imie);
            zapytanie.setString(2, nazwisko);
            zapytanie.setString(3, telefon);
            zapytanie.setString(4, email);

            zapytanie.executeUpdate();

            try(ResultSet wynik = zapytanie.getGeneratedKeys()) {
                if (wynik.next()){
                    long idZBazy = wynik.getLong(1);
                    if (idZBazy < 1 || idZBazy > Integer.MAX_VALUE){
                        throw new SQLException(
                                "ID zapisanego gościa nie mieści sie w typie int"
                        );
                    }
                    return new Gosc((int) idZBazy, imie, nazwisko, telefon, email);
                }

            }

        }

        throw new SQLException(
                "Nie udało sie odczytać ID zapisanego gościa"
        );
    }

    public static boolean edytujGoscia(
            int id,
            String imie,
            String nazwisko,
            String telefon,
            String email
    )throws SQLException{
        pl.hotel.walidacja.Dane.gosc(imie,nazwisko,telefon,email);
        String sql =
                "UPDATE goscie "
                        + "SET imie = ?, nazwisko = ?, telefon = ?, email = ? "
                        + "WHERE id = ? AND archiwizowany=FALSE";
        try (Connection polaczenie = PolaczenieBazy.polacz();

            PreparedStatement zapytanie = polaczenie.prepareStatement(sql)
        )   {
            zapytanie.setString(1, imie);
            zapytanie.setString(2, nazwisko);
            zapytanie.setString(3, telefon);
            zapytanie.setString(4, email);
            zapytanie.setInt(5, id);
            return zapytanie.executeUpdate() == 1;
        }

    }
}
