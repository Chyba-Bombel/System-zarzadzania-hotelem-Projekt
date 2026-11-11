package pl.hotel.konsola;

import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import java.sql.SQLException;

public final class MenuKonsoli {
    private MenuKonsoli() {}

    public static void main(String[] args){
        boolean dziala = true;

        System.out.println("Witaj w obsłudze hotelu!");

        while (dziala){
            try {
            if (StanKonsoli.zalogowanyPracownik != null) {
                try { BazaDanych.sprawdzSesje(StanKonsoli.zalogowanyPracownik,false); }
                catch (SecurityException e) { StanKonsoli.zalogowanyPracownik=null; StanKonsoli.goscie.clear(); StanKonsoli.pokoje.clear(); StanKonsoli.rezerwacje.clear(); StanKonsoli.pracownicy.clear(); System.out.println(pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e)); }
                catch (SQLException e) { System.out.println("Nie udało się sprawdzić sesji."); break; }
            }
            if (StanKonsoli.zalogowanyPracownik == null){
                StanKonsoli.zalogowanyPracownik = KontaKonsola.zalogujPracownika();

                if (StanKonsoli.zalogowanyPracownik == null){
                    break;
                }

                try {
                    StanKonsoli.goscie = BazaDanych.pobierzGosci();
                    StanKonsoli.pokoje = BazaDanych.pobierzPokoje();
                    StanKonsoli.pracownicy = KontaKonsola.czyAdministrator() ? BazaDanych.pobierzPracownikow() : new java.util.ArrayList<>();

                    StanKonsoli.rezerwacje = BazaDanych.pobierzRezerwacje(StanKonsoli.goscie, StanKonsoli.pokoje);
                } catch (SQLException e){
                    System.out.println("Nie udało się wczytać danych: " + pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e));

                    StanKonsoli.zalogowanyPracownik = null;
                    break;
                }
                System.out.println("Zalogowano: " + StanKonsoli.zalogowanyPracownik.getImie() + StanKonsoli.zalogowanyPracownik.getNazwisko() + " | Rola: " + StanKonsoli.zalogowanyPracownik.getRola());
            }
            System.out.println("\n--- MENU HOTELU ---");
            System.out.println("1. Wyświetl gości");
            System.out.println("2. Dodaj gościa");
            System.out.println("3. Wyszukaj gościa po nazwisku");
            if (KontaKonsola.czyAdministrator()){
                System.out.println("4. Wyświetl pracowników");
            }
            System.out.println("5. Wyświetl pokoje");
            System.out.println("6.Sprawdź wolne pokoje");
            System.out.println("7.Dodaj Rezerwacje");
            System.out.println("8.Wyświetl rezerwacje");
            System.out.println("9 Anuluj Rezerwacje");
            System.out.println("10. Edytuj dane gościa");
            if (KontaKonsola.czyAdministrator()){
                System.out.println("11. Dodaj pokój");
                System.out.println("12. Edytuj pokój");
            }
            System.out.println("13. Zmień termin rezerwacji");
            System.out.println("14. Zamelduj gościa");
            System.out.println("15. Wymelduj gościa");
            System.out.println("16. Oznacz pokój jako posprzątany");
            System.out.println("17. Pokaż rozliczenie rezerwacji");
            System.out.println("18. Dodaj wpłatę lub zaliczkę");
            if (KontaKonsola.czyAdministrator()){
                System.out.println("19. Zarejestruj zwrot");
            }
            System.out.println("20. Wyloguj");
            if (KontaKonsola.czyAdministrator()){
                System.out.println("21. Dodaj konto pracownika ");
                System.out.println("22. Dezaktywuj konto pracownika");
            }
            System.out.println("23. Zmień własne hasło");
            System.out.println("24. Zmień pokój rezerwacji");
            System.out.println("25. Historia rezerwacji");
            System.out.println("26. Potwierdzenie rezerwacji HTML / PDF");
            System.out.println("0. Zakończ");
            System.out.print("Wybierz opcję: ");

            if (!StanKonsoli.scanner.hasNextLine()){
                break;
            }
            String wybor = StanKonsoli.scanner.nextLine().trim();
            switch (wybor){
                case "1":
                    GoscieKonsola.wyswietlGosci();
                    break;
                case "2":
                    dziala = GoscieKonsola.dodajGoscia();
                    break;
                case "3":
                    dziala = GoscieKonsola.wyszukajGoscia();
                    break;
                case "4":
                    if (KontaKonsola.wymagaAdministratora()){
                        KontaKonsola.wyswietlPracownikow();
                    }
                    break;
                case "5":
                    for (Pokoj pokoj : StanKonsoli.pokoje) pokoj.wyswietlDane();
                    break;
                case "6":
                    dziala = PokojeKonsola.pokazWolnePokoje();
                    break;
                case "7":
                    dziala = RezerwacjeKonsola.dodajRezerwacja();
                    break;
                case "8":
                    if (StanKonsoli.rezerwacje.isEmpty()){
                        System.out.println("Brak Rezerwacji.");
                    }
                    for (Rezerwacja rezerwacja : StanKonsoli.rezerwacje){
                        rezerwacja.wyswietlDane();
                    }
                    break;
                case "9":
                    dziala = RezerwacjeKonsola.anulujRezerwacje();
                    break;
                case "10":
                    dziala = GoscieKonsola.edytujGoscia();
                    break;
                case "11":
                    if (KontaKonsola.wymagaAdministratora()){
                        dziala = PokojeKonsola.dodajPokoj();
                    }
                    break;
                case "12":
                    if (KontaKonsola.wymagaAdministratora()){
                        dziala = PokojeKonsola.edytujPokoj();
                    }
                    break;
                case "13":
                    dziala = RezerwacjeKonsola.zmienTerminRezerwacji();
                    break;
                case "14":
                    dziala = PobytyKonsola.zameldujGoscia();
                    break;
                case "15":
                    dziala = PobytyKonsola.wymeldujGoscia();
                    break;
                case "16":
                    dziala = PokojeKonsola.oznaczPokojJakoPosprzatany();
                    break;
                case "17":
                    dziala = PlatnosciKonsola.pokazRozliczenie();
                    break;
                case "18":
                    dziala = PlatnosciKonsola.dodajWplate();
                    break;
                case "19":
                    if (KontaKonsola.wymagaAdministratora()){
                        dziala = PlatnosciKonsola.dodajZwrot();
                    }
                    break;
                case "20":
                    BazaDanych.wyloguj();
                    StanKonsoli.zalogowanyPracownik = null;

                    StanKonsoli.rezerwacje.clear();
                    StanKonsoli.goscie.clear();
                    StanKonsoli.pokoje.clear();
                    StanKonsoli.pracownicy.clear();

                    System.out.println("Wylogowano.");
                    break;
                case "21":
                    if (KontaKonsola.wymagaAdministratora()){
                        dziala = KontaKonsola.dodajPracownika();
                    }
                    break;
                case "22":
                    if (KontaKonsola.wymagaAdministratora()){
                        dziala = KontaKonsola.dezaktywujPracownika();
                    }
                    break;
                case "23":
                    dziala = KontaKonsola.zmienWlasneHaslo();
                    break;
                case "24":
                    dziala = ZmianaPokojuKonsola.wykonaj();
                    break;
                case "25":
                    dziala = HistoriaKonsola.pokaz();
                    break;
                case "26":
                    dziala = PotwierdzenieKonsola.zapisz();
                    break;
                case "0":
                    dziala = false;
                    break;
                default:
                    System.out.println("Nie ma takiej opcji. Wybierz numer podany w menu.");
            }
            } catch (SecurityException e) {
                System.out.println(pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e));
                if (e instanceof pl.hotel.bezpieczenstwo.SesjaWygasla) StanKonsoli.zalogowanyPracownik=null;
            }
        }
        BazaDanych.wyloguj();
        System.out.println("Program zakońcozny. Do zobaczenia!");
        StanKonsoli.scanner.close();

    }
}
