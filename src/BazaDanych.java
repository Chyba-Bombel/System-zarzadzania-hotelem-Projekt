import java.net.ConnectException;
import java.sql.*;
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
                "SELECT id, imie, nazwisko, telefon, email "
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
                String email = wynik.getString("email");

                Gosc gosc = new Gosc(
                        id,
                        imie,
                        nazwisko,
                        telefon,
                        email
                );

                lista.add(gosc);
            }
        }

        return lista;
    }
    public static Gosc zapiszGoscia(String imie, String nazwisko, String telefon, String email) throws SQLException{
        String sql = "INSERT INTO goscie (imie, nazwisko, telefon, email) "
                + "VALUES (?, ?, ?, ?)";

        try (
            Connection polaczenie = polacz();

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
    public static ArrayList<Pokoj> pobierzPokoje() throws SQLException{
        ArrayList<Pokoj> lista = new ArrayList<>();

        String sql = "SELECT id, numer, liczba_miejsc, cena_za_noc, stan "
                + "FROM pokoje ORDER BY numer";
        try (
                Connection polaczenie = polacz();
                PreparedStatement zapytanie = polaczenie.prepareStatement(sql);
                ResultSet wynik = zapytanie.executeQuery()
        ){
            while (wynik.next()){
                Pokoj pokoj = new Pokoj(
                        wynik.getLong("id"),
                        wynik.getString("numer"),
                        wynik.getInt("liczba_miejsc"),
                        wynik.getBigDecimal("cena_za_noc"),
                        wynik.getString("stan")

                );

                lista.add(pokoj);
            }
            return lista;
        }
    }
    public static ArrayList<Rezerwacja> pobierzRezerwacje(ArrayList<Gosc> goscie, ArrayList<Pokoj> pokoje) throws SQLException{
        ArrayList<Rezerwacja> lista = new ArrayList<>();

        String sql = "SELECT id, gosc_id, pokoj_id, data_od, data_do, "
                + "liczba_osob, cena_za_noc, status "
                + "FROM rezerwacje ORDER BY id";
        try (
                Connection polaczenie = polacz();

                PreparedStatement zapytanie = polaczenie.prepareStatement(sql);
                ResultSet wynik = zapytanie.executeQuery();
                ){
            while (wynik.next()){
                long idZBazy = wynik.getLong("id");
                long idGoscia = wynik.getLong("gosc_id");
                long idPokoju = wynik.getLong("pokoj_id");

                if (idZBazy < 1 || idZBazy > Integer.MAX_VALUE){
                    throw new SQLException(
                            "ID rezerwacji nie mieści sie w typie INT. "
                    );
                }
                Gosc znalezionyGosc = null;

                for (Gosc gosc: goscie){
                    if (gosc.getId() == idGoscia){
                        znalezionyGosc = gosc;
                        break;
                    }
                }
                Pokoj znalezionyPokoj = null;
                for (Pokoj pokoj : pokoje){
                    if (pokoj.getId() == idPokoju){
                        znalezionyPokoj = pokoj;
                        break;
                    }
                }
                if (znalezionyGosc == null || znalezionyPokoj == null){
                    throw new SQLException(
                            "Nie znaleziono goscia lub pokoju "+ "dla rezerwacji" + idZBazy
                    );
                }
                Rezerwacja rezerwacja = new Rezerwacja(
                        (int) idZBazy,
                        znalezionyGosc,
                        znalezionyPokoj,
                        wynik.getDate("data_od").toLocalDate(),
                        wynik.getDate("data_do").toLocalDate(),
                        wynik.getInt("liczba_osob"),
                        wynik.getBigDecimal("cena_za_noc"),
                        wynik.getString("status")
                );

                lista.add(rezerwacja);
            }

        }
        return lista;
    }
}
