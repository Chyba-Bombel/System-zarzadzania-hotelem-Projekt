import java.net.IDN;
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
    private static int nastepneIDRezerwacji = 1;

    public static void main(String[] args){

        dodajDanePrzykladowe();
        try {
            goscie = BazaDanych.pobierzGosci();
            pokoje = BazaDanych.pobierzPokoje();
            rezerwacje = BazaDanych.pobierzRezerwacje(
                    goscie,
                    pokoje
            );


            System.out.println(
                    "Wczytano gości z bazy: " + goscie.size()
            );
            System.out.println("Wczytano pokoje: " + pokoje.size());
            System.out.println("Wczytano rezerwacji: " + rezerwacje.size());
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
                    System.out.println("Ze względu na aktualizacje zapis rezerwacji nie jest możliwy. Spróbuj ponownie później");
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
                    System.out.println("Anulowanie rezerwacji aktualnie nie jest możliwe. Sprobój ponownie później ");
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
    private static void dodajDanePrzykladowe(){
        pracownicy.add(new Pracownik("Piotr", "Nowak", "Recopcjonista"));
        pracownicy.add(new Pracownik("Piotr", "Zieliński", "Kierownik"));





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
            long pracownikId
        }
        return true;
    }
    private static boolean anulujRezerwacje(){
        Integer id = wczytajLiczbe("ID Rezerwacji do anulowania: ");
        if (id == null) return false;
        for (Rezerwacja rezerwacja : rezerwacje){
            if (rezerwacja.GetId() == id){
                if (rezerwacja.isAnulowana()){
                    System.out.println("Ta rezerwacja została anulowana");
                } else {
                    rezerwacja.anuluj();
                    System.out.println("Anulowano rezerwację. Termin jest ponownie dostępny");

                }
                return true;
            }
        }
        System.out.println("Nie znaleziono rezerwacji.");
        return true;
    }


}