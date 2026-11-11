package pl.hotel.konsola;

import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.sql.SQLException;

/** Obsługa konsoli: PokojeKonsola. */
final class PokojeKonsola {
    private PokojeKonsola() {}

    static boolean czyWolny(Pokoj pokoj, LocalDate od, LocalDate doDnia){
        if (pokoj.getStan().equals("WYLACZONY")) return false;
        for (Rezerwacja rezerwacja : StanKonsoli.rezerwacje){
            if (rezerwacja.kolidacja(pokoj.getNumer(), od, doDnia)){
                return false;
            }

        }
        return true;
    }

    static boolean pokazWolnePokoje(){
        LocalDate od = WejscieKonsoli.wczytajDate("Przyjazd");
        if (od == null)return false;
        LocalDate doDnia = WejscieKonsoli.wczytajDate("Wyjazd");
        if (doDnia == null)return false;
        if (!RezerwacjeKonsola.poprawnyTermin(od, doDnia)) return true;
        Integer osoby = WejscieKonsoli.wczytajLiczbe("Liczba osob: ");
        if (osoby == null) return false;
        boolean znaleziono = false;
        for (Pokoj pokoj: StanKonsoli.pokoje){
            if (pokoj.getLiczbaMiejsc() >= osoby && czyWolny(pokoj, od, doDnia)){
                pokoj.wyswietlDane();
                znaleziono = true;
            }
        }
        if (!znaleziono) System.out.println("Brak wolnych pokoi dla podanej liczby osób");
        return true;
    }

    static boolean dodajPokoj(){
        String numer = WejscieKonsoli.wczytajNiepustyTekst("Numer Pokoju: ");
        if (numer == null) return false;

        String typ = WejscieKonsoli.wczytajNiepustyTekst("Typ pokoju, np. Standard lub Apartament: ");

        if (typ == null) return false;

        Integer liczbaMiejsc;

        while (true){
            liczbaMiejsc = WejscieKonsoli.wczytajLiczbe("Liczba miejsc(1-10)");

            if (liczbaMiejsc == null) return false;

            if (liczbaMiejsc <= 10){
                break;
            }
            System.out.println("Maksymalna liczba miejsc wynosi 10.");
        }

        BigDecimal cena = WejscieKonsoli.wczytajCene("Cena za noc: ");
        if (cena == null) return false;

        try {
            BazaDanych.dodajPokoj(numer, typ, liczbaMiejsc, cena);
            System.out.println("Dodano pokój do bazy.");
        } catch (SQLException e){
            if (e.getErrorCode() == 1062){
                System.out.println("Pokój o takim numerze już istnieje");
            } else {
                System.out.println("Błąd zapisu " + e.getMessage());
            }
            return true;
        }
        try {
            StanKonsoli.pokoje = BazaDanych.pobierzPokoje();
        } catch (SQLException e){
            System.out.println("Pokój zapisano ale nie udało sie odświeżyć listy: " + e.getMessage());
            System.out.println("Uruchom program ponownie aby wczytać aktualne dane");
        }
        return true;
    }

    static boolean edytujPokoj(){
        try {
            StanKonsoli.pokoje = BazaDanych.pobierzPokoje();
        } catch (SQLException e){
            System.out.println("Nie udało sie pobrać pokoi: " + e.getMessage());
        }

        for (Pokoj pokoj : StanKonsoli.pokoje){
            pokoj.wyswietlDane();
        }

        String numer = WejscieKonsoli.wczytajNiepustyTekst("Obecny numer pokoju do edycji: ");
        if (numer == null) return false;

        Pokoj wybranyPokoj = null;

        for (Pokoj pokoj : StanKonsoli.pokoje){
            if (pokoj.getNumer().equalsIgnoreCase(numer)){
                wybranyPokoj = pokoj;
                break;
            }
        }
        if (wybranyPokoj == null){
            System.out.println("Nie znaleziono takiego pokoju");
            return true;
        }
        System.out.println("Obecne dane: ");
        wybranyPokoj.wyswietlDane();
        System.out.println("Podaj nowe dane. Jeśli numer lub typ mają zostać wpisz je ponownie");

        String nowyNumer = WejscieKonsoli.wczytajNiepustyTekst("Nowy numer pokoju: ");
        if (nowyNumer == null) return false;
        String nowyTyp = WejscieKonsoli.wczytajNiepustyTekst("Typ pokoju, np. Standard: ");
        if (nowyTyp == null) return false;
        BigDecimal nowaCena = WejscieKonsoli.wczytajCene("Nowa cena za noc: ");
        if (nowaCena == null) return false;
        try {
            boolean zapisano = BazaDanych.edytujPokoj(wybranyPokoj.getId(), nowyNumer, nowyTyp, nowaCena);
            if(!zapisano){
                System.out.println("Nie potwierdzono aktualizacji. Sprawdź aktualne dane pokoju.");
                return true;
            }
            System.out.println("Zapisano dane pokoju. ");

        }catch (SQLException e){
            if (e.getErrorCode() == 1062){
                System.out.println("Inny pokój ma już podobny numer");
            } else {
                System.out.println("Błąd aktualizacji: " + e.getMessage());
            }
            return true;
        }
        try {
            StanKonsoli.pokoje = BazaDanych.pobierzPokoje();
            StanKonsoli.goscie = BazaDanych.pobierzGosci();

            StanKonsoli.rezerwacje = BazaDanych.pobierzRezerwacje(StanKonsoli.goscie, StanKonsoli.pokoje);


        } catch (SQLException e){
            System.out.println("Dane zapisano ale nie udalo się odświeżyć list: " + e.getMessage());
            System.out.println("Uruchom program ponownie.");

        }
        return true;
    }

    static boolean oznaczPokojJakoPosprzatany(){
        try {
            StanKonsoli.pokoje = BazaDanych.pobierzPokoje();
        } catch (SQLException e) {
            System.out.println("Nie udało pobrać pokoi: " + e.getMessage());
            return true;
        }
        boolean znaleziono = false;

        System.out.println("--- POKOJE DO SPRZĄTANIA ---");

        for (Pokoj pokoj : StanKonsoli.pokoje){
            if (pokoj.getStan().equals("DO_SPRZATANIA")){
                pokoj.wyswietlDane();
                znaleziono = true;
            }
        }

        if (!znaleziono){
            System.out.println("Nie ma pokoi wymagających sprzątania.");
            return true;
        }

        String numer = WejscieKonsoli.wczytajNiepustyTekst("Numer posprzątanego pokoju: ");

        if (numer == null) return false;

        Pokoj wybranyPokoj = null;

        for (Pokoj pokoj : StanKonsoli.pokoje){
            if (pokoj.getNumer().equalsIgnoreCase(numer)){
                wybranyPokoj = pokoj;
                break;
            }
        }

        if (wybranyPokoj == null){
            System.out.println("Nie znaleziono takiego pokoju");
            return true;
        }

        try {
            boolean zmieniono = BazaDanych.oznaczPokojJakoPosprzatany(wybranyPokoj.getId());

            if (!zmieniono){
                System.out.println("Nie zmieniono stanu. Pokój nie wymaga sprzątania jest wyłączony, zajęty lub jego dane zostały zmienione");
                return true;
            }

            System.out.println("Pokój " + wybranyPokoj.getNumer() + " oznaczono jako GOTOWY.");



        } catch (SQLException e){
            System.out.println("Nie potwierdzono zmiany: " + e.getMessage());
            System.out.println("Przed ponowieniem sprawdź stan pokoju w bazie");
            return true;
        }
        try {
            StanKonsoli.pokoje = BazaDanych.pobierzPokoje();
            StanKonsoli.rezerwacje = BazaDanych.pobierzRezerwacje(StanKonsoli.goscie, StanKonsoli.pokoje);

        } catch (SQLException e) {
            System.out.println("Stan zapisano ale nie udało się odświeżyć list: " + e.getMessage());

            System.out.println("Uruchom program ponownie");
        }

        return true;
    }
}
