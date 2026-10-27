import com.mysql.cj.protocol.a.SqlDateValueEncoder;
import com.mysql.cj.protocol.a.TextResultsetReader;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.TryTree;

import javax.print.DocFlavor;
import javax.sound.midi.MidiDevice;
import java.net.ConnectException;
import java.security.PublicKey;
import java.sql.*;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.IllegalFormatCodePointException;

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
    public static ArrayList<Pracownik> pobierzPracownikow() throws SQLException{
        ArrayList<Pracownik> lista = new ArrayList<>();


        String sql =
                "SELECT imie, nazwisko, rola "
                        + "FROM pracownicy "
                        + "WHERE aktywny = TRUE "
                        + "ORDER BY id";

        try (Connection polaczenie = polacz();
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
    public static boolean edytujGoscia(
            int id,
            String imie,
            String nazwisko,
            String telefon,
            String email
    )throws SQLException{
        if (imie.length() > 60 || nazwisko.length() > 80 || telefon.length() > 30 || email.length() > 120){
            throw new SQLException("Przekroczono maksymalną długość danych.");
        }
        String sql =
                "UPDATE goscie "
                        + "SET imie = ?, nazwisko = ?, telefon = ?, email = ? "
                        + "WHERE id = ?";
        try (Connection polaczenie = polacz();

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
    public static void dodajPokoj(
            String numer,
            String typ,
            int liczbaMiejsc,
            BigDecimal cena
    )throws SQLException{

        if (numer.isBlank() || numer.length() > 10){
            throw new SQLException("Numer pokoju musi mięc od 1 do 10 znaków.");
        }
        if (typ.isBlank() || typ.length() > 40){
            throw new SQLException("Typ pokoju musi mięc od 1 do 40 znaków");
        }
        if (liczbaMiejsc < 1 || liczbaMiejsc > 10){
            throw new SQLException("Pokój musi mieć od 1 do 10 miejsc.");
        }

        if (cena.signum() <= 0 || cena.compareTo(new BigDecimal(99999999.99)) > 0 || cena.scale() > 2){

            throw new SQLException("Podaj dodatnią cenę z maksymalnie dwoma miejscami po przecinku.");
        }
        String sql =
                "INSERT INTO pokoje "
                        + "(numer, typ, liczba_miejsc, cena_za_noc, stan) "
                        + "VALUES (?, ?, ?, ?, 'GOTOWY')";
        try (Connection polaczenie = polacz();
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
        if (numer.isBlank() || numer.length() > 10){
            throw new SQLException("Numer pokoju musi mięc od 1 do 10 znaków.");
        }

        if (typ.isBlank() || typ.length() > 40){
            throw new SQLException("Typ pokoju musi mięc od 1 do 40 znaków");
        }

        if (cena.signum() <= 0 || cena.compareTo(new BigDecimal(99999999.99)) > 0 || cena.scale() > 2){

            throw new SQLException("Podaj dodatnią cenę z maksymalnie dwoma miejscami po przecinku.");
        }
        String sql =
                "UPDATE pokoje "
                        + "SET numer = ?, typ = ?, cena_za_noc = ? "
                        + "WHERE id = ?";
        try (Connection polaczenie = polacz();
            PreparedStatement zapytanie = polaczenie.prepareStatement(sql)
        ){
            zapytanie.setString(1, numer);
            zapytanie.setString(2, typ);
            zapytanie.setBigDecimal(3, cena);
            zapytanie.setLong(4, id);

            return zapytanie.executeUpdate() == 1;
        }

    }
    public static void zmienTerminRezerwacji(int id, LocalDate noweOd, LocalDate noweDo) throws SQLException {
        if (noweOd.isBefore(LocalDate.now()) || !noweDo.isAfter(noweOd) || ChronoUnit.DAYS.between(noweOd, noweDo) > 30){
            throw new SQLException("Pobyt musi trwać do 30 nocy i nie może zaczynać sie w przeszłości");
        }
        try (Connection polaczenie = polacz()){
            polaczenie.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            polaczenie.setAutoCommit(false);
            try {
                long pokojID;

                String sqlPokojID =  "SELECT pokoj_id FROM rezerwacje WHERE id = ?";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlPokojID)){
                    zapytanie.setInt(1, id);

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if(!wynik.next()){
                            throw new SQLException("Nie znaleizono rezerwacji.");

                        }
                        pokojID = wynik.getLong("pokoj_id");


                    }

                }
                String sqlPokoj =  "SELECT stan FROM pokoje WHERE id = ? FOR UPDATE";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlPokoj)){
                    zapytanie.setLong(1,pokojID);
                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if (!wynik.next()){
                            throw new SQLException("Pokój nie istnieje");
                        }

                        if (wynik.getString("stan").equals("WYLACZONY")){
                            throw new SQLException("Pokój jest wyłączony z użytkownia.");

                        }

                    }

                }
                String sqlRezerwacja = "SELECT status, pokoj_id FROM rezerwacje "
                        + "WHERE id = ? FOR UPDATE";
                try(PreparedStatement zapytanie = polaczenie.prepareStatement(sqlRezerwacja)) {
                    zapytanie.setInt(1,id);

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if (!wynik.next()){
                            throw new SQLException("Nie znaleziono rezerwacji.");

                        }

                        if (wynik.getLong("pokoj_id") != pokojID){
                            throw new SQLException("Zmieniono pokój rezerwacji. Spróbuj ponownie.");
                        }
                        if (!wynik.getString("status").equals("POTWIERDZONA")){
                            throw new SQLException("Termin można zmienić tylko w potwierdzonej rezerwacji");

                        }

                    }

                }
                String sqlPlatnosci = "SELECT id FROM platnosci WHERE rezerwacja_id = ?";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlPlatnosci)){
                    zapytanie.setInt(1,id);
                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if (wynik.next()){
                            throw new SQLException("Rezerwacja ma płatności.");
                        }
                    }

                }
                String sqlDostepnosc = "SELECT id FROM rezerwacje "
                        + "WHERE pokoj_id = ? "
                        + "AND id <> ? "
                        + "AND status IN ('POTWIERDZONA', 'ZAMELDOWANA') "
                        + "AND data_od < ? "
                        + "AND data_do > ?";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlDostepnosc)){
                    zapytanie.setLong(1,pokojID);
                    zapytanie.setInt(2,id);
                    zapytanie.setDate(3, Date.valueOf(noweDo));
                    zapytanie.setDate(4, Date.valueOf(noweOd));

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if (wynik.next()){
                            throw new SQLException("Pokój jest zajęty w nowym terminie");
                        }

                    }

                }
                String sqlZmiana = "UPDATE rezerwacje "
                        + "SET data_od = ?, data_do = ? "
                        + "WHERE id = ?";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlZmiana)){
                    zapytanie.setDate(1,Date.valueOf(noweOd));
                    zapytanie.setDate(2, Date.valueOf(noweDo));
                    zapytanie.setInt(3, id);

                    zapytanie.executeUpdate();

                }

                polaczenie.commit();
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
    public static void zameldujGoscia(int id) throws SQLException{
        try (Connection polaczenie = polacz()){

            polaczenie.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            polaczenie.setAutoCommit(false);

            try {
                long pokojId;

                String sqlPokojId = "SELECT pokoj_id FROM rezerwacje WHERE id = ?";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlPokojId)){
                    zapytanie.setInt(1,id);
                    try(ResultSet wynik = zapytanie.executeQuery()) {
                        if (!wynik.next()){
                            throw new SQLException("Nie znaleziono rezerwacji.");
                        }

                        pokojId = wynik.getLong("pokoj_id");

                    }
                }

                String sqlPokoj = "SELECT stan FROM pokoje WHERE id = ? FOR UPDATE";

                try (PreparedStatement zapytanie  = polaczenie.prepareStatement(sqlPokoj)){
                    zapytanie.setLong(1, pokojId);

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if (!wynik.next()){
                            throw new SQLException("Pokój nie istnieje");
                        }

                        if (!wynik.getString("stan").equals("GOTOWY")){
                            throw new SQLException("Pokój nie jest gotowy do zameldowania");
                        }
                    }

                }
                String sqlRezerwacja = "SELECT pokoj_id, status, data_od, data_do " + "FROM rezerwacje WHERE id = ? FOR UPDATE";

                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlRezerwacja)){
                    zapytanie.setInt(1,id);

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if (!wynik.next()){
                            throw new SQLException("Nie znaleziono rezerwacji.");
                        }
                        if (wynik.getLong("pokoj_id") != pokojId){
                            throw new SQLException("Zmieniono pokój rezerwacji. Spróbuj ponownie.");
                        }
                        if (!wynik.getString("status").equals("POTWIERDZONA")){
                            throw new SQLException("Zameldować można tylko potwierdzoną rezerwację.");
                        }

                        LocalDate dzis = LocalDate.now();
                        LocalDate od = wynik.getDate("data_od").toLocalDate();
                        LocalDate doDnia = wynik.getDate("data_do").toLocalDate();

                        if (dzis.isBefore(od) || !dzis.isBefore(doDnia)){
                            throw new SQLException("Dzisiejsza data nie przypada w terminie pobytu");
                        }

                    }

                }
                String sqlZajety =
                        "SELECT id FROM rezerwacje " + "WHERE pokoj_id = ? "
                                + "AND status = 'ZAMELDOWANA' "
                                + "AND id <> ?";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlZajety)){
                    zapytanie.setLong(1,pokojId);
                    zapytanie.setInt(2,id);

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if (wynik.next()){
                            throw new SQLException("W tym pokoju jest już zameldowany gość");
                        }

                    }

                }
                String sqlZmiana =
                        "UPDATE rezerwacje "
                                + "SET status = 'ZAMELDOWANA' "
                                + "WHERE id = ?";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlZmiana)){
                    zapytanie.setInt(1,id);
                    zapytanie.executeUpdate();


                }
                polaczenie.commit();
            } catch (SQLException | RuntimeException e){
                try {
                    polaczenie.rollback();
                }catch (SQLException bladWycofania){
                    e.addSuppressed(bladWycofania);
                }
                throw e;
            }

        }
    }
    public static void wymeldujGoscia(int id)
            throws SQLException {

        try (Connection polaczenie = polacz()) {

            polaczenie.setTransactionIsolation(
                    Connection.TRANSACTION_READ_COMMITTED
            );

            polaczenie.setAutoCommit(false);

            try {
                long pokojId;

                String sqlPokojId =
                        "SELECT pokoj_id FROM rezerwacje WHERE id = ?";

                try (PreparedStatement zapytanie =
                             polaczenie.prepareStatement(sqlPokojId)) {

                    zapytanie.setInt(1, id);

                    try (ResultSet wynik = zapytanie.executeQuery()) {
                        if (!wynik.next()) {
                            throw new SQLException(
                                    "Nie znaleziono rezerwacji."
                            );
                        }

                        pokojId = wynik.getLong("pokoj_id");
                    }
                }

                // Najpierw blokujemy pokój.
                String sqlPokoj =
                        "SELECT stan FROM pokoje WHERE id = ? FOR UPDATE";

                String stanPokoju;

                try (PreparedStatement zapytanie =
                             polaczenie.prepareStatement(sqlPokoj)) {

                    zapytanie.setLong(1, pokojId);

                    try (ResultSet wynik = zapytanie.executeQuery()) {
                        if (!wynik.next()) {
                            throw new SQLException("Pokój nie istnieje.");
                        }

                        stanPokoju = wynik.getString("stan");
                    }
                }

                // Następnie blokujemy rezerwację.
                String sqlRezerwacja =
                        "SELECT pokoj_id, status FROM rezerwacje "
                                + "WHERE id = ? FOR UPDATE";

                try (PreparedStatement zapytanie =
                             polaczenie.prepareStatement(sqlRezerwacja)) {

                    zapytanie.setInt(1, id);

                    try (ResultSet wynik = zapytanie.executeQuery()) {
                        if (!wynik.next()) {
                            throw new SQLException(
                                    "Nie znaleziono rezerwacji."
                            );
                        }

                        if (wynik.getLong("pokoj_id") != pokojId) {
                            throw new SQLException(
                                    "Zmieniono pokój rezerwacji. Spróbuj ponownie."
                            );
                        }

                        if (!wynik.getString("status")
                                .equals("ZAMELDOWANA")) {

                            throw new SQLException(
                                    "Wymeldować można tylko "
                                            + "zameldowaną rezerwację."
                            );
                        }
                    }
                }

                String sqlZakonczenie =
                        "UPDATE rezerwacje "
                                + "SET status = 'ZAKONCZONA' "
                                + "WHERE id = ?";

                try (PreparedStatement zapytanie =
                             polaczenie.prepareStatement(sqlZakonczenie)) {

                    zapytanie.setInt(1, id);
                    zapytanie.executeUpdate();
                }


                if (!stanPokoju.equals("WYLACZONY")) {

                    String sqlSprzatanie =
                            "UPDATE pokoje "
                                    + "SET stan = 'DO_SPRZATANIA' "
                                    + "WHERE id = ?";

                    try (PreparedStatement zapytanie =
                                 polaczenie.prepareStatement(sqlSprzatanie)) {

                        zapytanie.setLong(1, pokojId);
                        zapytanie.executeUpdate();
                    }
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
