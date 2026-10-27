import com.mysql.cj.protocol.a.SqlDateValueEncoder;

import javax.swing.plaf.InsetsUIResource;
import java.math.BigDecimal;
import java.net.IDN;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.SQLException;
public class Main {
    //Listy przechowujące obiekty podczas gdy program działa
    private static ArrayList<Gosc> goscie = new ArrayList<>();
    private static ArrayList<Pracownik> pracownicy = new ArrayList<>();
    private static ArrayList<Pokoj> pokoje = new ArrayList<>();
    private static ArrayList<Rezerwacja> rezerwacje = new ArrayList<>();
    //Scanner i sixseven
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args){
        try {
            goscie = BazaDanych.pobierzGosci();
            pokoje = BazaDanych.pobierzPokoje();
            rezerwacje = BazaDanych.pobierzRezerwacje(
                    goscie,
                    pokoje
            );
            pracownicy = BazaDanych.pobierzPracownikow();


            System.out.println(
                    "Wczytano gości z bazy: " + goscie.size()
            );
            System.out.println("Wczytano pokoje: " + pokoje.size());
            System.out.println("Wczytano rezerwacji: " + rezerwacje.size());
            System.out.println("Wczytano pracowników: " + pracownicy.size());
        } catch (SQLException e) {
            System.out.println(
                    "Nie udało się wczytać gości: "
                            + e.getMessage()
            );
            return;
        }

        boolean dziala = true;

        System.out.println("Witaj w obsłudze hotelu!");

        while (dziala){
            System.out.println("\n--- MENU HOTELU ---");
            System.out.println("1. Wyświetl gości");
            System.out.println("2. Dodaj gościa");
            System.out.println("3. Wyszukaj gościa po nazwisku");
            System.out.println("4. Wyświetl pracowników");
            System.out.println("5. Wyświetl pokoje");
            System.out.println("6.Sprawdź wolne pokoje");
            System.out.println("7.Dodaj Rezerwacje");
            System.out.println("8.Wyświetl rezerwacje");
            System.out.println("9 Anuluj Rezerwacje");
            System.out.println("10. Edytuj dane gościa");
            System.out.println("11. Dodaj pokój");
            System.out.println("12. Edytuj pokój");
            System.out.println("13. Zmień termin rezerwacji");
            System.out.println("14. Zamelduj gościa");
            System.out.println("15. Wymelduj gościa");
            System.out.println("16. Oznacz pokój jako posprzątany");
            System.out.println("17. Pokaż rozliczenie rezerwacji");
            System.out.println("18. Dodaj wpłatę lub zaliczkę");
            System.out.println("0. Zakończ");
            System.out.print("Wybierz opcję: ");

            //warunek sprawdza czy coś wprowadziliśmy do scannera
            if (!scanner.hasNextLine()){
                break;
            }
            String wybor = scanner.nextLine().trim();
            switch (wybor){
                case "1":
                    wyswietlGosci();
                    break;
                case "2":
                    dziala = dodajGoscia();
                    break;
                case "3":
                    dziala = wyszukajGoscia();
                    break;
                case "4":
                    wyswietlPracownikow();
                    break;
                case "5":
                    for (Pokoj pokoj : pokoje) pokoj.wyswietlDane();
                    break;
                case "6":
                    dziala = pokazWolnePokoje();
                    break;
                case "7":
                    dziala = dodajRezerwacja();
                    break;
                case "8":
                    if (rezerwacje.isEmpty()){
                        System.out.println("Brak Rezerwacji.");
                    }
                    for (Rezerwacja rezerwacja : rezerwacje){
                        rezerwacja.wyswietlDane();
                    }
                    break;
                case "9":
                    dziala = anulujRezerwacje();
                    break;
                case "10":
                    dziala = edytujGoscia();
                    break;
                case "11":
                    dziala = dodajPokoj();
                    break;
                case "12":
                    dziala = edytujPokoj();
                    break;
                case "13":
                    dziala = zmienTerminRezerwacji();
                    break;
                case "14":
                    dziala = zameldujGoscia();
                    break;
                case "15":
                    dziala = wymeldujGoscia();
                    break;
                case "16":
                    dziala = oznaczPokojJakoPosprzatany();
                    break;
                case "17":
                    dziala = pokazRozliczenie();
                    break;
                case "18":
                    dziala = dodajWplate();
                    break;
                case "0":
                    dziala = false;
                    break;
                default:
                    System.out.println("Nie ma takiej opcji. Wybierz numer podany w menu.");
            }
        }
        System.out.println("Program zakońcozny. Do zobaczenia!");
        scanner.close();



    }
    private static String wczytajNiepustyTekst(String komunikat) {
        while (true) {
            System.out.print(komunikat);
            if (!scanner.hasNextLine()) {
                return null;
            }
            String tekst = scanner.nextLine().trim();

            if (!tekst.isEmpty()) {
                return tekst;
            }

            System.out.println("To pole nie może być puste.");
        }
    }
    private static Integer wczytajLiczbe(String komunikat){
        while (true){
            String tekst = wczytajNiepustyTekst(komunikat);
            if (tekst == null) return null;
            try {
                int liczba = Integer.parseInt(tekst);
                if (liczba > 0) return liczba;

            } catch (NumberFormatException e){

            }
            System.out.println("Wpisz dodatnią liczbe całkowitą.");
        }
    }
    private static LocalDate wczytajDate(String komunikat){
        while (true){
            String tekst = wczytajNiepustyTekst(komunikat + " (RRRR-MM-DD): ");
            if (tekst == null) return null;
            try {
                return LocalDate.parse(tekst);
            } catch (DateTimeParseException e){
                System.out.println("Niepoprawna data. Przykład: 2026-12-23");
            }
        }
    }

    private static boolean dodajGoscia(){
        System.out.println("\n---NOWY GOŚĆ---");
        String imie = wczytajNiepustyTekst("Imię: ");

        if (imie == null) return false;

        String nazwisko = wczytajNiepustyTekst("Nazwisko: ");
        if (nazwisko == null) return false;

        String telefon = wczytajNiepustyTekst("Telefon: ");
        if (telefon == null) return false;

       String email;
       while (true){
           email = wczytajNiepustyTekst("E-mail: ");

           if (email == null) return false;

           if (czyPoprawnyEmail(email)){
               break;
           }
           System.out.println("Niepoprawny email. Przykład jankowalski123@gmail.com");
       }

        try {
            Gosc nowyGosc = BazaDanych.zapiszGoscia(imie, nazwisko, telefon, email);
            goscie.add(nowyGosc);
            System.out.println("Zapisano gościa w bazie");
            nowyGosc.wyswietlDane();
        } catch (SQLException e){
            System.out.println("Bład podczas zapisywania: " + e.getMessage());
            System.out.println("Przed ponowną próba sprawdź czy gośc pojawił sie w bazie danych");
        }

        return true;



    }
    private static boolean czyPoprawnyEmail(String email) {
        return email != null
                && email.length() <= 120
                && email.matches(
                "^[A-Za-z0-9_%+-]+(?:\\.[A-Za-z0-9_%+-]+)*"
                        + "@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+"
                        + "[A-Za-z]{2,63}$"
        );
    }
    private static void wyswietlGosci(){
        System.out.println("\n--- Lista GOŚCI ---");

        if (goscie.isEmpty()){
            System.out.println("Nie znaleziono gości w bazie");
            return;
        }

        for (Gosc gosc : goscie){
            gosc.wyswietlDane();
        }
    }
    private static boolean wyszukajGoscia() {
        String szukaneNazwisko = wczytajNiepustyTekst("Podaj nazwisko lub jego fragment: ");
        if (szukaneNazwisko == null) return false;

        boolean znaleziono = false;

        for (Gosc gosc : goscie) {
            if (gosc.getNazwisko().toLowerCase(Locale.ROOT).contains((szukaneNazwisko.toLowerCase(Locale.ROOT)))) {
                gosc.wyswietlDane();
                znaleziono = true;
            }
        }
        if (znaleziono == false) {
            System.out.println("Nie znaleziono gościa o takim nazwisku");
        }
        return true;

    }
    private static void wyswietlPracownikow(){
        System.out.println("\n---PRACOWNICY ---");

        for (Pracownik pracownik : pracownicy){
            pracownik.wyswietlDane();
        }
    }
    private static boolean poprawnyTermin(LocalDate od, LocalDate doDnia){
        if (od.isBefore(LocalDate.now()) || !doDnia.isAfter(od)){
            System.out.println("Przyjazd nie może być w przeszłości a wyjazd musi być po przyjeździe.");
            return false;
        }
        return true;
    }
    private static boolean czyWolny(Pokoj pokoj, LocalDate od, LocalDate doDnia){
        if (pokoj.getStan().equals("WYLACZONY")) return false;
        for (Rezerwacja rezerwacja : rezerwacje){
            if (rezerwacja.kolidacja(pokoj.getNumer(), od, doDnia)){
                return false;
            }

        }
        return true;
    }
    private static boolean pokazWolnePokoje(){
        LocalDate od = wczytajDate("Przyjazd");
        if (od == null)return false;
        LocalDate doDnia = wczytajDate("Wyjazd");
        if (doDnia == null)return false;
        if (!poprawnyTermin(od, doDnia)) return true;
        Integer osoby = wczytajLiczbe("Liczba osob: ");
        if (osoby == null) return false;
        boolean znaleziono = false;
        for (Pokoj pokoj: pokoje){
            if (pokoj.getLiczbaMiejsc() >= osoby && czyWolny(pokoj, od, doDnia)){
                pokoj.wyswietlDane();
                znaleziono = true;
            }
        }
        if (!znaleziono) System.out.println("Brak wolnych pokoi dla podanej liczby osób");
        return true;
    }
    private static boolean dodajRezerwacja(){
        wyswietlGosci();
        Integer id  = wczytajLiczbe("ID Goscia: ");
        if (id == null) return false;
        Gosc wybranyGosc = null;
        for (Gosc g : goscie){
            if (g.getId() == id) wybranyGosc = g;

        }
        if (wybranyGosc == null){
            System.out.println("Nie takiego gościa o podanym ID");
            return true;
        }
        for (Pokoj pokoj : pokoje){
            pokoj.wyswietlDane();
        }
        String numer = wczytajNiepustyTekst("Numer Pokoju: ");
        if (numer == null) return false;
        Pokoj wybranyPokoj = null;
        for (Pokoj pokoj : pokoje){
            if (pokoj.getNumer().equalsIgnoreCase(numer)){
                wybranyPokoj = pokoj;
                break;
            }
        }
        if (wybranyPokoj == null){
            System.out.println("Nie ma takiego pokoju");
            return true;
        }
        if (wybranyPokoj.getStan().equals("WYLACZONY")) {
            System.out.println("Ten pokój jest wyłączony z użytkowania.");
            return true;
        }
        Integer osoby = wczytajLiczbe("Liczba osób: ");
        if (osoby == null) return false;
        if (osoby > wybranyPokoj.getLiczbaMiejsc()){
            System.out.println("Za dużo osób dla tego pokoju");
            return true;
        }
        LocalDate od = wczytajDate("Przyjazd");
        if (od == null) return false;
        LocalDate doDnia = wczytajDate("Wyjazd");
        if (doDnia == null) return false;
        if (!poprawnyTermin(od,doDnia)) return true;
        try {
            long pracownikId = 2;

            Rezerwacja nowa = BazaDanych.zapiszRezerwacje(
                    wybranyGosc,
                    wybranyPokoj,
                    od,
                    doDnia,
                    osoby,
                    pracownikId
            );
            rezerwacje.add(nowa);

            System.out.println("Pomyślnie zapisano rezerwacje");
            nowa.wyswietlDane();
        } catch (SQLException e){
            System.out.println("Błąd zapisu rezerwacji: " + e.getMessage());
            System.out.println("Przed ponowną próbą sprawdź liste rezerwacji");
        }
        return true;
    }
    private static boolean anulujRezerwacje(){
        Integer id = wczytajLiczbe("ID Rezerwacji do anulowania: ");
        if (id == null) return false;

        try {
            boolean anulowano = BazaDanych.anulujRezerwacje(id);
            if (anulowano){
                for (Rezerwacja rezerwacja : rezerwacje){
                    if (rezerwacja.getId() == id){
                        rezerwacja.anuluj();
                        break;
                    }
                }

                System.out.println("Anulowano rezerwację. Termin jest dostępny ponownie.");
            } else {
                System.out.println("Nie anulowano rezerwacji; nie istniję bądź nie ma statusu Potwierdzona lub ma przypisane płatności");
            }
        } catch (SQLException e){
            System.out.println("Błąd anulowania: " + e.getMessage());
            System.out.println("Przed ponowieniem sprawdź status rezerwacji");
        }
        return true;
    }
    private static boolean edytujGoscia(){
        try {
            goscie = BazaDanych.pobierzGosci();
        } catch (SQLException e){
            System.out.println("Nie udało sie pobrać gości: " + e.getMessage());
            return true;
        }
        wyswietlGosci();

        Integer id = wczytajLiczbe("ID gościa do edycji");
        if (id == null) return false;
        Gosc wybranyGosc = null;

        for (Gosc gosc : goscie){
            if (gosc.getId() == id){
                wybranyGosc = gosc;
                break;
            }
        }

        if (wybranyGosc == null){
            System.out.println("Nie znaleziono gościa o takim ID.");
            return true;
        }
        System.out.println("Obecne dane:");
        wybranyGosc.wyswietlDane();

        System.out.println("Podaj nowe dane: ");

        String imie = wczytajNiepustyTekst("Nowe imię: ");
        if (imie == null) return false;

        String nazwisko = wczytajNiepustyTekst("Nowe nazwisko: ");
        if (nazwisko == null) return false;

        String telefon = wczytajNiepustyTekst("Telefon ");
        if (telefon == null) return false;

        String email;

        while (true){
            email = wczytajNiepustyTekst("Nowy email: ");
            if (email == null) return false;

            if (czyPoprawnyEmail(email)){
                break;
            }
            System.out.println("Niepoprawny format e-maila.");
        }

        try {
            boolean zapisano = BazaDanych.edytujGoscia(
                    id,
                    imie,
                    nazwisko,
                    telefon,
                    email
            );
            if(!zapisano){
                System.out.println("Nie potwierdzono zmiany. Sprawdź aktualne dane gościa");
                return true;
            }
        }catch (SQLException e){
            System.out.println("Błąd aktualizacji: " + e.getMessage());
            return true;
        }

        try {
            goscie = BazaDanych.pobierzGosci();
            rezerwacje = BazaDanych.pobierzRezerwacje(
                    goscie,
                    pokoje
            );
        } catch (SQLException e){
            System.out.println("Dane zapisano, anie udało się odświeżyć list" + e.getMessage());
            System.out.println("Uruchom program ponownie, aby wczytać aktualne dane.");
        }

        return true;



    }
    private static BigDecimal wczytajCene(String komunikat){
        while (true){
            String tekst = wczytajNiepustyTekst(komunikat);

            if (tekst == null) return null;

            tekst = tekst.replace(',','.');

            if (tekst.matches("[0-9]{1,8}(\\.[0-9]{1,2})?")) {
                BigDecimal cena = new BigDecimal(tekst);

                if (cena.signum() > 0) {
                    return cena;
                }
            }

            System.out.println("Podaj cenę od 0.01 do 99999999.99, np. 250,50.");
        }
    }

    private static boolean dodajPokoj(){
        String numer = wczytajNiepustyTekst("Numer Pokoju: ");
        if (numer == null) return false;

        String typ = wczytajNiepustyTekst("Typ pokoju, np. Standard lub Apartament: ");

        if (typ == null) return false;

        Integer liczbaMiejsc;

        while (true){
            liczbaMiejsc = wczytajLiczbe("Liczba miejsc(1-10)");

            if (liczbaMiejsc == null) return false;

            if (liczbaMiejsc <= 10){
                break;
            }
            System.out.println("Maksymalna liczba miejsc wynosi 10.");
        }

        BigDecimal cena = wczytajCene("Cena za noc: ");
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
            pokoje = BazaDanych.pobierzPokoje();
        } catch (SQLException e){
            System.out.println("Pokój zapisano ale nie udało sie odświeżyć listy: " + e.getMessage());
            System.out.println("Uruchom program ponownie aby wczytać aktualne dane");
        }
        return true;
    }

    private static boolean edytujPokoj(){
        try {
            pokoje = BazaDanych.pobierzPokoje();
        } catch (SQLException e){
            System.out.println("Nie udało sie pobrać pokoi: " + e.getMessage());
        }

        for (Pokoj pokoj : pokoje){
            pokoj.wyswietlDane();
        }

        String numer = wczytajNiepustyTekst("Obecny numer pokoju do edycji: ");
        if (numer == null) return false;

        Pokoj wybranyPokoj = null;

        for (Pokoj pokoj : pokoje){
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

        String nowyNumer = wczytajNiepustyTekst("Nowy numer pokoju: ");
        if (nowyNumer == null) return false;
        String nowyTyp = wczytajNiepustyTekst("Typ pokoju, np. Standard: ");
        if (nowyTyp == null) return false;
        BigDecimal nowaCena = wczytajCene("Nowa cena za noc: ");
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
            pokoje = BazaDanych.pobierzPokoje();
            goscie = BazaDanych.pobierzGosci();

            rezerwacje = BazaDanych.pobierzRezerwacje(goscie, pokoje);


        } catch (SQLException e){
            System.out.println("Dane zapisano ale nie udalo się odświeżyć list: " + e.getMessage());
            System.out.println("Uruchom program ponownie.");

        }
        return true;
    }
    private static boolean zmienTerminRezerwacji(){
        try {
            goscie = BazaDanych.pobierzGosci();
            pokoje = BazaDanych.pobierzPokoje();

            rezerwacje = BazaDanych.pobierzRezerwacje(goscie, pokoje);

        } catch (SQLException e){
            System.out.println("Nie udało się pobrać danych: " + e.getMessage());
            return true;
        }
        if (rezerwacje.isEmpty()){
            System.out.println("Brak rezerwacji.");
            return true;
        }

        for (Rezerwacja rezerwacja: rezerwacje){
            rezerwacja.wyswietlDane();
        }
        Integer id = wczytajLiczbe("ID rezerwacji do zmiany terminu");

        if (id == null) return false;

        LocalDate noweOD = wczytajDate("Nowy przyjazd: ");
        if (noweOD == null) return false;

        LocalDate noweDo = wczytajDate("Nowy wyjazd: ");
        if (noweDo == null) return false;

        try {
            BazaDanych.zmienTerminRezerwacji(id, noweOD, noweDo);
            System.out.println("Zapisano nowy termin rezeracji.");

        }catch (SQLException e){
            System.out.println("Nie potwierdzono zmiany terminu: " + e.getMessage());
            System.out.println("Przed ponowienie sprawdź aktualne dane rezerwacji.");
            return true;
        }

        try {
            rezerwacje = BazaDanych.pobierzRezerwacje(goscie,pokoje);

            for (Rezerwacja rezerwacja : rezerwacje){
                if (rezerwacja.getId() == id){
                    rezerwacja.wyswietlDane();
                    break;
                }
            }
        } catch (SQLException e){
            System.out.println("Termin zapisano, ale nie udało się odświeżyć listy: " + e.getMessage());
            System.out.println("Uruchom program ponownie.");
        }
        return true;
    }
    private static boolean zameldujGoscia(){
        try {
            goscie = BazaDanych.pobierzGosci();
            pokoje = BazaDanych.pobierzPokoje();

            rezerwacje = BazaDanych.pobierzRezerwacje(goscie,pokoje);


        }catch (SQLException e){
            System.out.println("Błąd pobierania danych: " + e.getMessage());
            return true;
        }

        for (Rezerwacja rezerwacja : rezerwacje){
            if (rezerwacja.getStatus().equals("POTWIERDZONA")){
                rezerwacja.wyswietlDane();
            }
        }

        Integer id = wczytajLiczbe("ID rezerwacji do zameldowania: ");
        if (id == null) return false;

        try {
            BazaDanych.zameldujGoscia(id);

            System.out.println("Zameldowano gościa.");
        } catch (SQLException e) {
            System.out.println("Nie powierdzono zameldowania: " + e.getMessage());
            System.out.println("Przed ponowieniem sprawdź aktualny status w bazie.");
            return true;
        }
        try {
            rezerwacje = BazaDanych.pobierzRezerwacje(goscie, pokoje);
        } catch (SQLException e) {
            System.out.println("Zamledowanie zapisano ale nie udało sie odświeżyć listy: " + e.getMessage());
            System.out.println("Uruchom program ponownie.");
        }
        return true;

    }

    private static boolean wymeldujGoscia(){
        try {
            goscie = BazaDanych.pobierzGosci();
            pokoje = BazaDanych.pobierzPokoje();

            rezerwacje = BazaDanych.pobierzRezerwacje(goscie,pokoje);
        } catch (SQLException e) {
            System.out.println("Nie udało się pobrać danych: " + e.getMessage());
            return true;
        }

        boolean znaleziono = false;

        for (Rezerwacja rezerwacja : rezerwacje){
            if (rezerwacja.getStatus().equals("ZAMELDOWANA")){
                rezerwacja.wyswietlDane();
                znaleziono = true;

            }
        }

        if (!znaleziono) {
            System.out.println("Nie ma zameldowanych rezerwacji.");
            return true;
        }

        Integer id  = wczytajLiczbe("Id rezereacji do wymeldowania: ");
        if (id == null) return false;
        try {
            BazaDanych.wymeldujGoscia(id);

            System.out.println("Wymeldowano gościa.");
        } catch (SQLException e){
            System.out.println("Nie potwierdzono wymeldowania: " + e.getMessage());
            System.out.println("Przed ponowieniem sprawdź aktualny status w bazie.");
            return true;
        }
        try {
            pokoje = BazaDanych.pobierzPokoje();

            rezerwacje = BazaDanych.pobierzRezerwacje(goscie, pokoje);
        } catch (SQLException e) {
            System.out.println("Wymeldowanie zapisano ale nie udało się odświeżyć danych: " + e.getMessage());
            System.out.println("Uruchom program ponownie");

        }
        return true;
    }

    private static boolean oznaczPokojJakoPosprzatany(){
        try {
            pokoje = BazaDanych.pobierzPokoje();
        } catch (SQLException e) {
            System.out.println("Nie udało pobrać pokoi: " + e.getMessage());
            return true;
        }
        boolean znaleziono = false;

        System.out.println("--- POKOJE DO SPRZĄTANIA ---");

        for (Pokoj pokoj : pokoje){
            if (pokoj.getStan().equals("DO_SPRZATANIA")){
                pokoj.wyswietlDane();
                znaleziono = true;
            }
        }

        if (!znaleziono){
            System.out.println("Nie ma pokoi wymagających sprzątania.");
            return true;
        }

        String numer = wczytajNiepustyTekst("Numer posprzątanego pokoju: ");

        if (numer == null) return false;

        Pokoj wybranyPokoj = null;

        for (Pokoj pokoj : pokoje){
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
            pokoje = BazaDanych.pobierzPokoje();
            rezerwacje = BazaDanych.pobierzRezerwacje(goscie, pokoje);

        } catch (SQLException e) {
            System.out.println("Stan zapisano ale nie udało się odświeżyć list: " + e.getMessage());

            System.out.println("Uruchom program ponownie");
        }

        return true;
    }
    private static boolean pokazRozliczenie(){
        Integer id = wczytajLiczbe("ID rezerwacji do rozliczenia: ");
        if (id == null) return false;

        try {
            Rozliczenie rozliczenie = BazaDanych.pobierzRozliczenie(id);
            System.out.println("\n--- ROZLICZENIE ---");

            System.out.println("Rezerwacja: " + rozliczenie.rezerwacjaId());

            System.out.println("Status: " + rozliczenie.status());

            System.out.println("Cena pobytu: " + rozliczenie.cenaPobytu() + " zł");

            System.out.println("Wpłaty: " + rozliczenie.wplaty() + " zł");

            System.out.println("Zapłacono netto: " + rozliczenie.zaplaconoNetto() + " zł");

            BigDecimal saldo = rozliczenie.saldo();

            if (saldo.signum() > 0){
                System.out.println("Do dopłaty: " + saldo + " zł");

            } else if (saldo.signum() < 0) {
                System.out.println("Do zwrotu: " + saldo.abs() + " zł");

            }else {
                System.out.println("Rezerwacja rozliczona.");
            }

            System.out.println("\n--- HISTORIA PŁATNOŚCI ---");
            if (rozliczenie.platnosci().isEmpty()){
                System.out.println("Brak płatności.");
            }


            for (Platnosc platnosc : rozliczenie.platnosci()){
                System.out.println(
                        "ID: " + platnosc.id()
                                + " | " + platnosc.rodzaj()
                                + " | " + platnosc.kwota() + " zł"
                                + " | " + platnosc.metoda()
                                + " | " + platnosc.data()
                );
            }

        } catch (Exception e) {
            System.out.println("Błąd pobierania rozliczenia: " + e.getMessage());
        }
        return true;
    }

    private static String wybierzMetodePlatnosci(){
        while (true){
            System.out.println("1. Gotówka");
            System.out.println("2. Karta");
            System.out.println("3. Przelew");

            String wybor = wczytajNiepustyTekst("Metoda płatności: ");

            if (wybor == null) return null;

            switch (wybor){
                case "1":
                    return "GOTOWKA";
                case "2":
                    return "KARTA";
                case "3":
                    return "PRZELEW";
                default:
                    System.out.println("Wybierz 1,2 lub 3.");
            }
        }
    }

    private static boolean dodajWplate(){

        Integer id = wczytajLiczbe("ID rezerwacji: ");
        if (id == null) return false;

        try {
            Rozliczenie rozliczenie = BazaDanych.pobierzRozliczenie(id);

            if (rozliczenie.status().equals("ANULOWANA")){
                System.out.println("Nie można wplacać na anulowaną rezerwacje.");
                return true;
            }
            if (rozliczenie.saldo().signum() <= 0){
                System.out.println("Brak kwoty do dopłaty.");
                return true;
            }

            System.out.println("Pozostało do zapłaty: " + rozliczenie.saldo() + " zł");
        } catch (SQLException e){
            System.out.println("Błąd odczytu rozliczenia: " + e.getMessage());
            return true;
        }

        BigDecimal kwota = wczytajCene("Kwota wpłaty: ");
        if (kwota == null) return false;

        String metoda = wybierzMetodePlatnosci();
        if (metoda == null) return false;

        try {
            BazaDanych.zapiszWplate(id,kwota,metoda,2L);
            System.out.println("Zapisano wpłatę: " + kwota + " zł.");
        } catch (SQLException e) {
            System.out.println("Nie potwierdzono wpłaty: " + e.getMessage());
            System.out.println("Przed ponowieniem sprawdź historie płatności");
        }
        return true;
    }


}