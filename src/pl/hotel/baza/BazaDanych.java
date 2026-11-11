package pl.hotel.baza;

import pl.hotel.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Publiczny punkt dostępu do bazy. Implementacje SQL znajdują się w klasach DAO.
 * Zachowuje dotychczasowe nazwy metod używane przez konsolę, GUI i testy. */
public final class BazaDanych {
    private BazaDanych() {}

    public static Connection polacz() throws SQLException {
        return PolaczenieBazy.polacz();
    }

    public static ArrayList<Gosc> pobierzGosci()
            throws SQLException {
        return GoscieDAO.pobierzGosci();
    }

    public static Gosc zapiszGoscia(String imie, String nazwisko, String telefon, String email) throws SQLException {
        return GoscieDAO.zapiszGoscia(imie, nazwisko, telefon, email);
    }

    public static boolean edytujGoscia(
            int id,
            String imie,
            String nazwisko,
            String telefon,
            String email
    )throws SQLException {
        return GoscieDAO.edytujGoscia(id, imie, nazwisko, telefon, email);
    }

    public static ArrayList<Pokoj> pobierzPokoje() throws SQLException {
        return PokojeDAO.pobierzPokoje();
    }

    public static void dodajPokoj(
            String numer,
            String typ,
            int liczbaMiejsc,
            BigDecimal cena
    )throws SQLException {
        PokojeDAO.dodajPokoj(numer, typ, liczbaMiejsc, cena);
    }

    public static boolean edytujPokoj(long id, String numer, String typ, BigDecimal cena)throws SQLException {
        return PokojeDAO.edytujPokoj(id, numer, typ, cena);
    }

    public static boolean oznaczPokojJakoPosprzatany(long pokojId) throws SQLException {
        return PokojeDAO.oznaczPokojJakoPosprzatany(pokojId);
    }

    public static ArrayList<Rezerwacja> pobierzRezerwacje(ArrayList<Gosc> goscie, ArrayList<Pokoj> pokoje) throws SQLException {
        return OdczytRezerwacjiDAO.pobierzRezerwacje(goscie, pokoje);
    }

    public static Rezerwacja zapiszRezerwacje(
            Gosc gosc,
            Pokoj pokoj,
            LocalDate od,
            LocalDate doDnia,
            int liczbaOsob,
            long pracownikId
    ) throws SQLException {
        return TworzenieRezerwacjiDAO.zapiszRezerwacje(gosc, pokoj, od, doDnia, liczbaOsob, pracownikId);
    }

    public static Rezerwacja zapiszRezerwacje(Gosc gosc, Pokoj pokoj, LocalDate od,
            LocalDate doDnia, int liczbaOsob, long pracownikId,
            java.util.List<Uczestnik> uczestnicy) throws SQLException {
        return TworzenieRezerwacjiDAO.zapiszRezerwacje(gosc, pokoj, od, doDnia, liczbaOsob, pracownikId, uczestnicy);
    }

    public static boolean anulujRezerwacje(int id)
            throws SQLException {
        return AnulowanieRezerwacjiDAO.anulujRezerwacje(id);
    }

    public static void zmienTerminRezerwacji(int id, LocalDate noweOd, LocalDate noweDo) throws SQLException {
        TerminyRezerwacjiDAO.zmienTerminRezerwacji(id, noweOd, noweDo);
    }

    public static void zameldujGoscia(int id) throws SQLException {
        PobytyDAO.zameldujGoscia(id);
    }

    public static void wymeldujGoscia(int id)
            throws SQLException {
        PobytyDAO.wymeldujGoscia(id);
    }

    public static Rozliczenie pobierzRozliczenie(int id) throws SQLException {
        return RozliczeniaDAO.pobierzRozliczenie(id);
    }

    public static void zapiszWplate(
            int rezerwacjaId,
            BigDecimal kwota,
            String metoda,
            long pracownikId
    ) throws SQLException {
        WplatyDAO.zapiszWplate(rezerwacjaId, kwota, metoda, pracownikId);
    }

    public static void zapiszZwrot(int rezerwacjaId, BigDecimal kwota, String metoda, long pracownikId) throws SQLException {
        ZwrotyDAO.zapiszZwrot(rezerwacjaId, kwota, metoda, pracownikId);
    }

    public static ArrayList<Pracownik> pobierzPracownikow() throws SQLException {
        return PracownicyDAO.pobierzPracownikow();
    }

    public static void dodajPracownika(String imie, String nazwisko, String login, String haslo, String rola) throws SQLException {
        PracownicyDAO.dodajPracownika(imie, nazwisko, login, haslo, rola);
    }

    public static ArrayList<KontoPracownika> pobierzKontaPracownikow() throws SQLException {
        return PracownicyDAO.pobierzKontaPracownikow();
    }

    public static void dezaktywujPracownika(
            long kontoId,
            long administratorId
    ) throws SQLException {
        PracownicyDAO.dezaktywujPracownika(kontoId, administratorId);
    }

    public static Pracownik zaloguj(String login, String haslo) throws SQLException {
        return LogowanieDAO.zaloguj(login, haslo);
    }

    public static void zmienHaslo(
            long pracownikId,
            String obecneHaslo,
            String noweHaslo
    ) throws SQLException {
        HaslaDAO.zmienHaslo(pracownikId, obecneHaslo, noweHaslo);
    }

    public static void zmienUczestnikow(int id, java.util.List<Uczestnik> uczestnicy) throws SQLException {
        UczestnicyDAO.zmienUczestnikow(id, uczestnicy);
    }
}
