import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
public class BazaDanych {
    private static final String URL =
            "jdbc:mysql://localhost:3306/hotel_manager"
                    + "?sslMode=DISABLED"
                    + "&allowPublicKeyRetrieval=true"
                    + "&connectTimeout=5000"
                    + "&socketTimeout=10000";

    private static final String UZYTKOWNIK = "hotel_app";
    private static final String HASLO = "HotelDemo2026!";

    public static Connection polacz() throws SQLException{
        return DriverManager.getConnection(
                URL,
                UZYTKOWNIK,
                HASLO
        );
    }
    public static ArrayList<Gosc> pobierzGosci()
            throws SQLException {

        ArrayList<Gosc> lista = new ArrayList<>();

        String sql =
                "SELECT id, imie, nazwisko, telefon "
                        + "FROM goscie ORDER BY id";

        try (
                Connection polaczenie = polacz();

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

                Gosc gosc = new Gosc(
                        id,
                        imie,
                        nazwisko,
                        telefon
                );

                lista.add(gosc);
            }
        }

        return lista;
    }
}
