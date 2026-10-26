import com.mysql.cj.protocol.a.SqlDateValueEncoder;
import com.sun.source.tree.TryTree;

import javax.print.DocFlavor;
import java.net.ConnectException;
import java.sql.*;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
    public static Rezerwacja zapiszRezerwacje(
            Gosc gosc,
            Pokoj pokoj,
            LocalDate od,
            LocalDate doDnia,
            int liczbaOsob,
            long pracownikId
    ) throws SQLException {
        if(od.isBefore(LocalDate.now()) || !doDnia.isAfter(od) || ChronoUnit.DAYS.between(od, doDnia) > 30 ){
            throw new SQLException("Pobyt nie może trwać więcej niż 30 nocy i nie może zaczynać sie w przeszłości");
        }
        if (liczbaOsob < 1){
            throw new SQLException("Liczba osób musi być zawsze większa niż 0");
        }

        try (Connection polaczenie = polacz()) {
            polaczenie.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            polaczenie.setAutoCommit(false);

            try {
                BigDecimal cenaZaNoc;

                String sqlPokoj = "SELECT liczba_miejsc, cena_za_noc, stan "
                        + "FROM pokoje WHERE id = ? FOR UPDATE";

                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlPokoj)) {
                    zapytanie.setLong(1,pokoj.getId());

                    try(ResultSet wynik = zapytanie.executeQuery()) {

                        if(!wynik.next()){
                            throw new SQLException("Pokój nie istnieje");
                        }
                        if (wynik.getString("stan").equals("WYLACZONY")){
                            throw new SQLException("Pokój jest wyłaczony z uzytkownia");
                        }
                        if (liczbaOsob > wynik.getInt("liczba_miejsc")){
                            throw new SQLException("Za dużo osób dla wybranego pokoju");
                        }

                        cenaZaNoc = wynik.getBigDecimal("cena_za_noc");
                    }

                }
                String sqlPracownik =
                        "SELECT id FROM pracownicy "
                                + "WHERE id = ? AND aktywny = TRUE";

                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlPracownik)){
                    zapytanie.setLong(1, pracownikId);

                    try(ResultSet wynik = zapytanie.executeQuery()){
                        if (!wynik.next()){
                            throw new SQLException("Nie znaleziono aktywnego pracownika");
                        }
                    }

                }
                String sqlDostepnosc =
                        "SELECT id FROM rezerwacje "
                                + "WHERE pokoj_id = ? "
                                + "AND status IN ('POTWIERDZONA', 'ZAMELDOWANA') "
                                + "AND data_od < ? "
                                + "AND data_do > ?";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlDostepnosc)){

                    zapytanie.setLong(1,pokoj.getId());
                    zapytanie.setDate(2, Date.valueOf(doDnia));
                    zapytanie.setDate(3, Date.valueOf(od));

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if (wynik.next()){
                            throw new SQLException("Pokój jest zajęty w wybranym terminie");
                        }
                    }

                }
                String sqlZapis =
                        "INSERT INTO rezerwacje "
                                + "(gosc_id, pokoj_id, pracownik_id, "
                                + "data_od, data_do, liczba_osob, "
                                + "cena_za_noc, status) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, 'POTWIERDZONA')";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(
                        sqlZapis,
                        Statement.RETURN_GENERATED_KEYS
                )){
                    zapytanie.setLong(1, gosc.getId());
                    zapytanie.setLong(2, pokoj.getId());
                    zapytanie.setLong(3, pracownikId);
                    zapytanie.setDate(4, Date.valueOf(od));
                    zapytanie.setDate(5, Date.valueOf(doDnia));
                    zapytanie.setInt(6, liczbaOsob);
                    zapytanie.setBigDecimal(7, cenaZaNoc);

                    zapytanie.executeUpdate();

                    try (ResultSet wynik = zapytanie.getGeneratedKeys()){
                        if (!wynik.next()){
                            throw new SQLException("Baza nie zwróciła id.");
                        }

                        long idZbazy = wynik.getLong(1);

                        if (idZbazy < 1 || idZbazy > Integer.MAX_VALUE){
                            throw new SQLException("ID rezerwacji nie mieści się w typie int.");
                        }

                        Rezerwacja nowa = new Rezerwacja(
                                (int) idZbazy,
                                gosc,
                                pokoj,
                                od,
                                doDnia,
                                liczbaOsob,
                                cenaZaNoc,
                                "POTWIERDZONA"
                        );

                        polaczenie.commit();
                        return nowa;

                    }
                }
            } catch (SQLException | RuntimeException e){
                try {
                    polaczenie.rollback();
                } catch (SQLException bladWycofania){
                    e.addSuppressed(bladWycofania);
                }
                throw e;
            }


        }
    }
    public static boolean anulujRezerwacje(int id) throws SQLException{
        String sql =
                "UPDATE rezerwacje "
                        + "SET status = 'ANULOWANA' "
                        + "WHERE id = ? AND status = 'POTWIERDZONA' "
                        + "AND NOT EXISTS ("
                        + "SELECT 1 FROM platnosci "
                        + "WHERE rezerwacja_id = ?"
                        + ")";
        try (
                Connection polaczenie = polacz();

                PreparedStatement zapytanie = polaczenie.prepareStatement(sql)
        ){
            zapytanie.setInt(1, id);
            zapytanie.setInt(2,id);

            int zmienioneWiersze = zapytanie.executeUpdate();

            return zmienioneWiersze == 1;
        }
    }
}
