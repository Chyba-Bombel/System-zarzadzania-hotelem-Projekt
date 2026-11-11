package pl.hotel.konsola;

import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import java.util.ArrayList;
import java.sql.SQLException;

/** Obsługa konsoli: KontaKonsola. */
final class KontaKonsola {
    private KontaKonsola() {}

    static void wyswietlPracownikow(){
        System.out.println("\n---PRACOWNICY ---");

        for (Pracownik pracownik : StanKonsoli.pracownicy){
            pracownik.wyswietlDane();
        }
    }

    static Pracownik zalogujPracownika(){
        System.out.println("--- LOGOWANIE ---");

        for (int proba = 1; proba <=3; proba++){
            String login = WejscieKonsoli.wczytajNiepustyTekst("Login: ");
            if (login == null) return null;

            System.out.println("Hasło: ");

            if (!StanKonsoli.scanner.hasNextLine()){
                return null;
            }
            String haslo = StanKonsoli.scanner.nextLine();

            try {
                Pracownik pracownik = BazaDanych.zaloguj(login,haslo);

                if (pracownik != null){
                    return pracownik;
                }
                System.out.println("Nieprawidłowy login lub hasło albo nieaktywne konto.");

                System.out.println("Pozostało prób: " + (3 - proba));

            }catch (SQLException e){
                System.out.println("Błąd logowania: " + e.getMessage());

                return null;
            }
        }
        return null;
    }

    static boolean czyAdministrator(){
        return StanKonsoli.zalogowanyPracownik != null && "ADMIN".equals(StanKonsoli.zalogowanyPracownik.getRola());
    }

    static boolean wymagaAdministratora(){

        if (!czyAdministrator()){
            System.out.println("Ta operacja wymaga konta administratora");

            return false;
        }

        return true;
    }

    static boolean dodajPracownika(){
        if (!wymagaAdministratora()){
            return true;
        }
        String imie = WejscieKonsoli.wczytajNiepustyTekst("Imię pracownika: ");
        if (imie == null) return false;

        String nazwisko = WejscieKonsoli.wczytajNiepustyTekst("Nazwisko: ");
        if (nazwisko == null) return false;

        String login = WejscieKonsoli.wczytajNiepustyTekst("Login: ");
        if (login == null) return false;

        System.out.println("Hasło(12-128 znaków): ");
        if (!StanKonsoli.scanner.hasNextLine()) return false;
        String haslo = StanKonsoli.scanner.nextLine();

        System.out.println("Powtórz hasło: ");
        if(!StanKonsoli.scanner.hasNextLine()) return false;

        String powtorzoneHaslo = StanKonsoli.scanner.nextLine();

        if(!haslo.equals(powtorzoneHaslo)){
            System.out.println("Hasła różnią się.");
            return true;
        }
        String rola;
        while (true){
            System.out.println("1. Recepcjonista");
            System.out.println("2. Admnistrator");
            String wybor = WejscieKonsoli.wczytajNiepustyTekst("Wybierz rolę: ");
            if (wybor == null) return false;

            if (wybor.equals("1")){
                rola = "RECEPCJA";
                break;
            }
            if (wybor.equals("2")){
                rola = "ADMIN";
                break;
            }
            System.out.println("Wybierz 1 lub 2");
        }
        try {
            BazaDanych.dodajPracownika(imie,nazwisko,login,haslo,rola);
            System.out.println("Utworzono konto pracownika.");
        }catch (SQLException e){
            if (e.getErrorCode() == 1062){
                System.out.println("Podany login jest już zajęty.");
            } else {
                System.out.println("Nie potwierdzono utworzenia konta: " + e.getMessage());
            }
            return true;
        }
        try {
            StanKonsoli.pracownicy = BazaDanych.pobierzPracownikow();
        } catch (SQLException e) {
            System.out.println("Konto utworzono lecz nie oświeżono listy: " + e.getMessage());
        }
        return true;




    }

    public static boolean dezaktywujPracownika(){
        if (!wymagaAdministratora()){
            return true;
        }
        try {
            ArrayList<KontoPracownika> konta = BazaDanych.pobierzKontaPracownikow();

            for (KontoPracownika konto : konta){
                konto.wyswietlDane();
            }
        }catch (SQLException e){
            System.out.println("Nie udało sie pobrać konto: " + e.getMessage());
            return true;
        }
        Integer id = WejscieKonsoli.wczytajLiczbe("ID Pracownika do dezaktywacji: ");
        if (id == null) return false;

        String potwierdzenie = WejscieKonsoli.wczytajNiepustyTekst("Dezaktywować Konto o ID " + id + "? (t/n: ");

        if (potwierdzenie == null) return false;

        if (!potwierdzenie.equalsIgnoreCase("t")){
            System.out.println("Zrezygnowano z operacji");
            return true;
        }
        try {
            BazaDanych.dezaktywujPracownika(id,StanKonsoli.zalogowanyPracownik.getId());

            System.out.println("Dezaktywacja konta przebiegła pomyślnie.");
        } catch (SQLException e) {
            System.out.println("Nie potwierdzono dezaktywacji: " +e.getMessage());
            System.out.println("Przed ponowną próbą sprawdź stan konta");
            return true;
        }
        try {
            StanKonsoli.pracownicy = BazaDanych.pobierzPracownikow();
        } catch (SQLException e) {
            System.out.println("Konto dezaktywowano, ale nie odświeżono listy: " + e.getMessage());
        }
        return true;

    }

    static boolean zmienWlasneHaslo(){
        if (StanKonsoli.zalogowanyPracownik == null){
            System.out.println("Najpierw zaloguj sie");
            return true;
        }
        String obecneHaslo = WejscieKonsoli.wczytajNiepustyTekst("Obecne hasło: ");
        if (obecneHaslo == null) return false;
        String noweHaslo = WejscieKonsoli.wczytajNiepustyTekst("Nowe hasło (12-128 znaków)");

        if (noweHaslo == null) return false;

        String powtorzoneHaslo = WejscieKonsoli.wczytajNiepustyTekst("Powtórz nowe haslo: ");

        if (powtorzoneHaslo == null) return false;

        if (!noweHaslo.equals(powtorzoneHaslo)) {
            System.out.println("Nowe hasła różnią się.");
            return true;
        }

        try {
            BazaDanych.zmienHaslo(StanKonsoli.zalogowanyPracownik.getId(), obecneHaslo, noweHaslo);
            System.out.println("Zmieniono hasło. Przy następnym logowaniu użyj nowego hasła.");
        } catch (SQLException e) {
            System.out.println("Nie potwierdzono zmiany hasła: " + e.getMessage());

            System.out.println("Jeżeli utracono połączenie podczas zapisu, hasło mogło zostać zmienione.");
        }
        return true;

    }
}
