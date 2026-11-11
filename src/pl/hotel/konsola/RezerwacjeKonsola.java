package pl.hotel.konsola;

import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import java.time.LocalDate;
import java.sql.SQLException;

/** Obsługa konsoli: RezerwacjeKonsola. */
final class RezerwacjeKonsola {
    private RezerwacjeKonsola() {}

    static boolean poprawnyTermin(LocalDate od, LocalDate doDnia){
        if (od.isBefore(LocalDate.now()) || !doDnia.isAfter(od)){
            System.out.println("Przyjazd nie może być w przeszłości a wyjazd musi być po przyjeździe.");
            return false;
        }
        return true;
    }

    static boolean dodajRezerwacja(){
        GoscieKonsola.wyswietlGosci();
        Integer id  = WejscieKonsoli.wczytajLiczbe("ID Goscia: ");
        if (id == null) return false;
        Gosc wybranyGosc = null;
        for (Gosc g : StanKonsoli.goscie){
            if (g.getId() == id) wybranyGosc = g;

        }
        if (wybranyGosc == null){
            System.out.println("Nie takiego gościa o podanym ID");
            return true;
        }
        for (Pokoj pokoj : StanKonsoli.pokoje){
            pokoj.wyswietlDane();
        }
        String numer = WejscieKonsoli.wczytajNiepustyTekst("Numer Pokoju: ");
        if (numer == null) return false;
        Pokoj wybranyPokoj = null;
        for (Pokoj pokoj : StanKonsoli.pokoje){
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
        Integer osoby = WejscieKonsoli.wczytajLiczbe("Liczba osób: ");
        if (osoby == null) return false;
        if (osoby > wybranyPokoj.getLiczbaMiejsc()){
            System.out.println("Za dużo osób dla tego pokoju");
            return true;
        }
        LocalDate od = WejscieKonsoli.wczytajDate("Przyjazd");
        if (od == null) return false;
        LocalDate doDnia = WejscieKonsoli.wczytajDate("Wyjazd");
        if (doDnia == null) return false;
        if (!poprawnyTermin(od,doDnia)) return true;
        try {
            long pracownikId = StanKonsoli.zalogowanyPracownik.getId();

            Rezerwacja nowa = BazaDanych.zapiszRezerwacje(
                    wybranyGosc,
                    wybranyPokoj,
                    od,
                    doDnia,
                    osoby,
                    pracownikId
            );
            StanKonsoli.rezerwacje.add(nowa);

            System.out.println("Pomyślnie zapisano rezerwacje");
            nowa.wyswietlDane();
        } catch (SQLException e){
            System.out.println("Błąd zapisu rezerwacji: " + e.getMessage());
            System.out.println("Przed ponowną próbą sprawdź liste rezerwacji");
        }
        return true;
    }

    static boolean anulujRezerwacje(){
        Integer id = WejscieKonsoli.wczytajLiczbe("ID Rezerwacji do anulowania: ");
        if (id == null) return false;

        try {
            boolean anulowano = BazaDanych.anulujRezerwacje(id);
            if (anulowano){
                for (Rezerwacja rezerwacja : StanKonsoli.rezerwacje){
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

    static boolean zmienTerminRezerwacji(){
        try {
            StanKonsoli.goscie = BazaDanych.pobierzGosci();
            StanKonsoli.pokoje = BazaDanych.pobierzPokoje();

            StanKonsoli.rezerwacje = BazaDanych.pobierzRezerwacje(StanKonsoli.goscie, StanKonsoli.pokoje);

        } catch (SQLException e){
            System.out.println("Nie udało się pobrać danych: " + e.getMessage());
            return true;
        }
        if (StanKonsoli.rezerwacje.isEmpty()){
            System.out.println("Brak rezerwacji.");
            return true;
        }

        for (Rezerwacja rezerwacja: StanKonsoli.rezerwacje){
            rezerwacja.wyswietlDane();
        }
        Integer id = WejscieKonsoli.wczytajLiczbe("ID rezerwacji do zmiany terminu");

        if (id == null) return false;

        LocalDate noweOD = WejscieKonsoli.wczytajDate("Nowy przyjazd: ");
        if (noweOD == null) return false;

        LocalDate noweDo = WejscieKonsoli.wczytajDate("Nowy wyjazd: ");
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
            StanKonsoli.rezerwacje = BazaDanych.pobierzRezerwacje(StanKonsoli.goscie,StanKonsoli.pokoje);

            for (Rezerwacja rezerwacja : StanKonsoli.rezerwacje){
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
}
