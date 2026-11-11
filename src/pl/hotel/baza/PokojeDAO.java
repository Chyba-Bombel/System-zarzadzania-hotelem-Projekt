package pl.hotel.baza;

import pl.hotel.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.math.BigDecimal;

final class PokojeDAO {
    private PokojeDAO() {}

    public static ArrayList<Pokoj> pobierzPokoje() throws SQLException{
        ArrayList<Pokoj> lista = new ArrayList<>();

        String sql = "SELECT id, numer, liczba_miejsc, cena_za_noc, stan "
                + "FROM pokoje ORDER BY numer";
        try (
                Connection polaczenie = PolaczenieBazy.polacz();
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

    public static void dodajPokoj(
            String numer,
            String typ,
            int liczbaMiejsc,
            BigDecimal cena
    )throws SQLException{

        pl.hotel.walidacja.Dane.tekst(numer,10,"Numer pokoju");
        pl.hotel.walidacja.Dane.tekst(typ,40,"Typ pokoju");
        if (numer == null || numer.isBlank() || numer.length() > 10){
            throw new SQLException("Numer pokoju musi mięc od 1 do 10 znaków.");
        }
        if (typ == null || typ.isBlank() || typ.length() > 40){
            throw new SQLException("Typ pokoju musi mięc od 1 do 40 znaków");
        }
        if (liczbaMiejsc < 1 || liczbaMiejsc > 10){
            throw new SQLException("Pokój musi mieć od 1 do 10 miejsc.");
        }

        if (cena == null || cena.signum() <= 0 || cena.compareTo(new BigDecimal("99999999.99")) > 0 || cena.scale() > 2){

            throw new SQLException("Podaj dodatnią cenę z maksymalnie dwoma miejscami po przecinku.");
        }
        String sql =
                "INSERT INTO pokoje "
                        + "(numer, typ, liczba_miejsc, cena_za_noc, stan) "
                        + "VALUES (?, ?, ?, ?, 'GOTOWY')";
        try (Connection polaczenie = PolaczenieBazy.polacz();
            PreparedStatement zapytanie = polaczenie.prepareStatement(sql);
        ){
            zapytanie.setString(1,numer);
            zapytanie.setString(2,typ);
            zapytanie.setInt(3, liczbaMiejsc);
            zapytanie.setBigDecimal(4, cena);

            zapytanie.executeUpdate();
        }

    }

    public static boolean edytujPokoj(long id, String numer, String typ, BigDecimal cena)throws SQLException{
        pl.hotel.walidacja.Dane.tekst(numer,10,"Numer pokoju");
        pl.hotel.walidacja.Dane.tekst(typ,40,"Typ pokoju");
        if (numer == null || numer.isBlank() || numer.length() > 10){
            throw new SQLException("Numer pokoju musi mięc od 1 do 10 znaków.");
        }

        if (typ == null || typ.isBlank() || typ.length() > 40){
            throw new SQLException("Typ pokoju musi mięc od 1 do 40 znaków");
        }

        if (cena == null || cena.signum() <= 0 || cena.compareTo(new BigDecimal("99999999.99")) > 0 || cena.scale() > 2){

            throw new SQLException("Podaj dodatnią cenę z maksymalnie dwoma miejscami po przecinku.");
        }
        String sql =
                "UPDATE pokoje "
                        + "SET numer = ?, typ = ?, cena_za_noc = ? "
                        + "WHERE id = ?";
        try (Connection polaczenie = PolaczenieBazy.polacz();
            PreparedStatement zapytanie = polaczenie.prepareStatement(sql)
        ){
            zapytanie.setString(1, numer);
            zapytanie.setString(2, typ);
            zapytanie.setBigDecimal(3, cena);
            zapytanie.setLong(4, id);

            return zapytanie.executeUpdate() == 1;
        }

    }

    public static boolean oznaczPokojJakoPosprzatany(long pokojId) throws SQLException{
        String sql =
                "UPDATE pokoje "
                        + "SET stan = 'GOTOWY' "
                        + "WHERE id = ? "
                        + "AND stan = 'DO_SPRZATANIA' "
                        + "AND NOT EXISTS ("
                        + "SELECT 1 FROM rezerwacje "
                        + "WHERE pokoj_id = ? "
                        + "AND status = 'ZAMELDOWANA'"
                        + ")";
        try (Connection polaczenie = PolaczenieBazy.polacz(); PreparedStatement zapytanie = polaczenie.prepareStatement(sql)){
            zapytanie.setLong(1, pokojId);
            zapytanie.setLong(2, pokojId);

            return zapytanie.executeUpdate() == 1;

        }
    }
    static java.util.Map<Long,String> pobierzTypy() throws SQLException {
        java.util.Map<Long,String> typy = new java.util.HashMap<>();
        try (Connection c=PolaczenieBazy.polacz(); PreparedStatement p=c.prepareStatement("SELECT id,typ FROM pokoje"); ResultSet r=p.executeQuery()) {
            while(r.next()) typy.put(r.getLong("id"),r.getString("typ"));
        }
        return typy;
    }
}
