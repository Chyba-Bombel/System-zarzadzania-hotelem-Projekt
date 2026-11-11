package pl.hotel.baza;

import pl.hotel.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class BazaDanych {
    private BazaDanych() {}

    public static ArrayList<Gosc> pobierzGosci()
            throws SQLException {
        KontrolaDostepu.wymagaj(false);
        return GoscieDAO.pobierzGosci();
    }

    public static Gosc zapiszGoscia(String imie, String nazwisko, String telefon, String email) throws SQLException {
        KontrolaDostepu.wymagaj(false);
        return GoscieDAO.zapiszGoscia(imie, nazwisko, telefon, email);
    }

    public static boolean edytujGoscia(
            int id,
            String imie,
            String nazwisko,
            String telefon,
            String email
    )throws SQLException {
        KontrolaDostepu.wymagaj(false);
        return GoscieDAO.edytujGoscia(id, imie, nazwisko, telefon, email);
    }

    public static ArrayList<Pokoj> pobierzPokoje() throws SQLException {
        KontrolaDostepu.wymagaj(false);
        return PokojeDAO.pobierzPokoje();
    }

    public static void dodajPokoj(
            String numer,
            String typ,
            int liczbaMiejsc,
            BigDecimal cena
    )throws SQLException {
        KontrolaDostepu.wymagaj(true);
        PokojeDAO.dodajPokoj(numer, typ, liczbaMiejsc, cena);
    }

    public static boolean edytujPokoj(long id, String numer, String typ, BigDecimal cena)throws SQLException {
        KontrolaDostepu.wymagaj(true);
        return PokojeDAO.edytujPokoj(id, numer, typ, cena);
    }

    public static boolean oznaczPokojJakoPosprzatany(long pokojId) throws SQLException {
        KontrolaDostepu.wymagaj(false);
        return PokojeDAO.oznaczPokojJakoPosprzatany(pokojId);
    }

    public static ArrayList<Rezerwacja> pobierzRezerwacje(ArrayList<Gosc> goscie, ArrayList<Pokoj> pokoje) throws SQLException {
        KontrolaDostepu.wymagaj(false);
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
        KontrolaDostepu.tozsamosc(pracownikId, false);
        return TworzenieRezerwacjiDAO.zapiszRezerwacje(gosc, pokoj, od, doDnia, liczbaOsob, pracownikId);
    }

    public static Rezerwacja zapiszRezerwacje(Gosc gosc, Pokoj pokoj, LocalDate od,
            LocalDate doDnia, int liczbaOsob, long pracownikId,
            java.util.List<Uczestnik> uczestnicy) throws SQLException {
        KontrolaDostepu.tozsamosc(pracownikId, false);
        return TworzenieRezerwacjiDAO.zapiszRezerwacje(gosc, pokoj, od, doDnia, liczbaOsob, pracownikId, uczestnicy);
    }

    public static boolean anulujRezerwacje(int id)
            throws SQLException {
        KontrolaDostepu.wymagaj(false);
        return AnulowanieRezerwacjiDAO.anulujRezerwacje(id);
    }

    public static void zmienTerminRezerwacji(int id, LocalDate noweOd, LocalDate noweDo) throws SQLException {
        KontrolaDostepu.wymagaj(false);
        TerminyRezerwacjiDAO.zmienTerminRezerwacji(id, noweOd, noweDo);
    }

    public static void zmienPokojRezerwacji(int id, long poprzedniPokojId, long nowyPokojId) throws SQLException {
        KontrolaDostepu.wymagaj(false);
        ZmianaPokojuDAO.zmienPokoj(id, poprzedniPokojId, nowyPokojId);
    }

    public static void zameldujGoscia(int id) throws SQLException {
        KontrolaDostepu.wymagaj(false);
        PobytyDAO.zameldujGoscia(id);
    }

    public static void wymeldujGoscia(int id)
            throws SQLException {
        KontrolaDostepu.wymagaj(false);
        PobytyDAO.wymeldujGoscia(id);
    }

    public static Rozliczenie pobierzRozliczenie(int id) throws SQLException {
        KontrolaDostepu.wymagaj(false);
        return RozliczeniaDAO.pobierzRozliczenie(id);
    }

    public static void zapiszWplate(
            int rezerwacjaId,
            BigDecimal kwota,
            String metoda,
            long pracownikId
    ) throws SQLException {
        KontrolaDostepu.tozsamosc(pracownikId, false);
        WplatyDAO.zapiszWplate(rezerwacjaId, kwota, metoda, pracownikId);
    }

    public static void zapiszZwrot(int rezerwacjaId, BigDecimal kwota, String metoda, long pracownikId) throws SQLException {
        KontrolaDostepu.tozsamosc(pracownikId, true);
        ZwrotyDAO.zapiszZwrot(rezerwacjaId, kwota, metoda, pracownikId);
    }

    public static ArrayList<Pracownik> pobierzPracownikow() throws SQLException {
        KontrolaDostepu.wymagaj(true);
        return PracownicyDAO.pobierzPracownikow();
    }

    public static void dodajPracownika(String imie, String nazwisko, String login, String haslo, String rola) throws SQLException {
        KontrolaDostepu.wymagaj(true);
        PracownicyDAO.dodajPracownika(imie, nazwisko, login, haslo, rola);
    }

    public static ArrayList<KontoPracownika> pobierzKontaPracownikow() throws SQLException {
        KontrolaDostepu.wymagaj(true);
        return PracownicyDAO.pobierzKontaPracownikow();
    }

    public static void dezaktywujPracownika(
            long kontoId,
            long administratorId
    ) throws SQLException {
        KontrolaDostepu.tozsamosc(administratorId, true);
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
        KontrolaDostepu.tozsamosc(pracownikId, false);
        HaslaDAO.zmienHaslo(pracownikId, obecneHaslo, noweHaslo);
    }

    public static void zmienUczestnikow(int id, java.util.List<Uczestnik> uczestnicy) throws SQLException {
        KontrolaDostepu.wymagaj(false);
        UczestnicyDAO.zmienUczestnikow(id, uczestnicy);
    }
    public static void wyloguj() { KontrolaDostepu.wyloguj(); }
    public static void sprawdzSesje(Pracownik pracownik, boolean admin) throws SQLException { KontrolaDostepu.sprawdz(pracownik, admin); }
    public static java.util.Map<Long,String> pobierzTypyPokoi() throws SQLException {
        KontrolaDostepu.wymagaj(false);
        return PokojeDAO.pobierzTypy();
    }
    public static pl.hotel.raporty.Raporty.Raport generujRaport(LocalDate od,LocalDate koniec) throws SQLException {
        KontrolaDostepu.wymagaj(false);
        return RaportDAO.generuj(od,koniec);
    }
}
