package pl.hotel.serwis;

import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class GuiRepository {
    private GuiRepository() {}

    public record Dane(ArrayList<Gosc> goscie, ArrayList<Pokoj> pokoje,
                       ArrayList<Rezerwacja> rezerwacje, ArrayList<KontoPracownika> konta,
                       Map<Long, String> typy) {}

    public static void sprawdzSesje(Pracownik pracownik, boolean admin) throws SQLException {
        BazaDanych.sprawdzSesje(pracownik,admin);
    }

    public static Dane pobierz(Pracownik pracownik) throws SQLException {
        sprawdzSesje(pracownik, false);
        ArrayList<Gosc> goscie = BazaDanych.pobierzGosci();
        ArrayList<Pokoj> pokoje = BazaDanych.pobierzPokoje();
        ArrayList<Rezerwacja> rezerwacje = BazaDanych.pobierzRezerwacje(goscie, pokoje);
        ArrayList<KontoPracownika> konta = pracownik.getRola().equals("ADMIN")
                ? BazaDanych.pobierzKontaPracownikow() : new ArrayList<>();
        Map<Long,String> typy = BazaDanych.pobierzTypyPokoi();
        return new Dane(goscie, pokoje, rezerwacje, konta, typy);
    }

    public static void sprawdzGoscia(String imie, String nazwisko, String telefon, String email) {
        tekst(imie, 60, "Imię"); tekst(nazwisko, 80, "Nazwisko"); tekst(telefon, 30, "Telefon");
        if (email == null || email.length() > 120 || !email.matches(
                "^[A-Za-z0-9_%+-]+(?:\\.[A-Za-z0-9_%+-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$"))
            throw new IllegalArgumentException("Podaj poprawny e-mail, np. jan@example.com (do 120 znaków).");
    }

    public static void tekst(String tekst, int maks, String nazwa) {
        if (tekst == null || tekst.isBlank() || tekst.length() > maks)
            throw new IllegalArgumentException(nazwa + ": wpisz od 1 do " + maks + " znaków.");
    }

    public static BigDecimal kwota(String tekst) {
        String liczba = tekst == null ? "" : tekst.trim().replace(',', '.');
        if (!liczba.matches("[0-9]{1,8}(\\.[0-9]{1,2})?"))
            throw new IllegalArgumentException("Wpisz kwotę, np. 250,50 (maksymalnie 2 miejsca po przecinku).");
        BigDecimal wynik = new BigDecimal(liczba);
        if (wynik.signum() <= 0) throw new IllegalArgumentException("Kwota musi być większa od zera.");
        return wynik;
    }
}
