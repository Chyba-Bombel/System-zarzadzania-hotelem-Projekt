import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
public class Main {
    //Listy przechowujące obiekty podczas gdy program działa
    private static ArrayList<Gosc> goscie = new ArrayList<>();
    private static ArrayList<Pracownik> pracownicy = new ArrayList<>();
    //Scanner i sixseven
    private static Scanner scanner = new Scanner(System.in);
    private static int nastepneID = 1;

    public static void main(String[] args){
        dodajDanePrzykladowe();

        boolean dziala = true;

        System.out.println("Witaj w obsłudze hotelu!");

        while (dziala){
            System.out.println("\n--- MENU HOTELU ---");
            System.out.println("1. Wyświetl gości");
            System.out.println("2. Dodaj gościa");
            System.out.println("3. Wyszukaj gościa po nazwisku");
            System.out.println("4. Wyświetl pracowników");
            System.out.println("0. Zakończ");
            System.out.print("Wybierz opcję: ");

            //warunek sprawdza czy coś wprowadziliśmy do scannera
            if (!scanner.hasNextLine()){
                break;
            }
            String wybor = scanner.nextLine().trim();
            switch (wybor){
                case "1":
                    //funkcja wyswietlania gosci
                    break;
                case "2":
                    dziala = dodajGoscia();
                    break;
                case "3":
                    //funkcja wyszukiwania goscia po nazwisku(lub części:) )
                    break;
                case "4":
                    //funkcja wyswietlenia pracownikow
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
        goscie.add(new Gosc(nastepneID++, "Jan", "Kowalski", "500100100"));
        goscie.add(new Gosc(nastepneID++, "Anna", "Pruska", "500123134"));

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
    private static boolean dodajGoscia(){
        System.out.println("\n---NOWY GOŚĆ---");
        String imie = wczytajNiepustyTekst("Imię: ");

        if (imie == null) return false;

        String nazwisko = wczytajNiepustyTekst("Nazwisko: ");
        if (nazwisko == null) return false;

        String telefon = wczytajNiepustyTekst("Telefon: ");
        if (telefon == null) return false;

        Gosc nowyGosc = new Gosc(nastepneID++, imie, nazwisko, telefon);
        goscie.add(nowyGosc);

        System.out.println("Dodano Gościa: ");
        nowyGosc.wyswietlDane();
        return true;



    }

}