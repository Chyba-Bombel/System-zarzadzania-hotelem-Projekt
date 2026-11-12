package pl.hotel.konsola;

import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import java.util.Locale;
import java.sql.SQLException;

final class GoscieKonsola {
    private GoscieKonsola() {}

    static boolean dodajGoscia(){
        System.out.println("\n---NOWY GOŚĆ---");
        String imie = WejscieKonsoli.wczytajNiepustyTekst("Imię: ");

        if (imie == null) return false;

        String nazwisko = WejscieKonsoli.wczytajNiepustyTekst("Nazwisko: ");
        if (nazwisko == null) return false;

        String telefon = WejscieKonsoli.wczytajNiepustyTekst("Telefon: ");
        if (telefon == null) return false;

       String email;
       while (true){
           email = WejscieKonsoli.wczytajNiepustyTekst("E-mail: ");

           if (email == null) return false;

           if (WejscieKonsoli.czyPoprawnyEmail(email)){
               break;
           }
           System.out.println("Niepoprawny email. Przykład jankowalski123@gmail.com");
       }

        try {
            Gosc nowyGosc = BazaDanych.zapiszGoscia(imie, nazwisko, telefon, email);
            StanKonsoli.goscie.add(nowyGosc);
            System.out.println("Zapisano gościa w bazie");
            nowyGosc.wyswietlDane();
        } catch (SQLException e){
            System.out.println("Bład podczas zapisywania: " + pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e));
            System.out.println("Przed ponowną próba sprawdź czy gośc pojawił sie w bazie danych");
        }

        return true;

    }

    static void wyswietlGosci(){
        System.out.println("\n--- Lista GOŚCI ---");

        if (StanKonsoli.goscie.stream().noneMatch(g->!g.isArchiwalny())){
            System.out.println("Nie znaleziono gości w bazie");
            return;
        }

        for (Gosc gosc : StanKonsoli.goscie){
            if(!gosc.isArchiwalny())gosc.wyswietlDane();
        }
    }

    static boolean wyszukajGoscia() {
        String szukaneNazwisko = WejscieKonsoli.wczytajNiepustyTekst("Podaj nazwisko lub jego fragment: ");
        if (szukaneNazwisko == null) return false;

        boolean znaleziono = false;

        for (Gosc gosc : StanKonsoli.goscie) {
            if (!gosc.isArchiwalny() && gosc.getNazwisko().toLowerCase(Locale.ROOT).contains((szukaneNazwisko.toLowerCase(Locale.ROOT)))) {
                gosc.wyswietlDane();
                znaleziono = true;
            }
        }
        if (znaleziono == false) {
            System.out.println("Nie znaleziono gościa o takim nazwisku");
        }
        return true;

    }

    static boolean edytujGoscia(){
        try {
            StanKonsoli.goscie = BazaDanych.pobierzGosci();
        } catch (SQLException e){
            System.out.println("Nie udało sie pobrać gości: " + pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e));
            return true;
        }
        wyswietlGosci();

        Integer id = WejscieKonsoli.wczytajLiczbe("ID gościa do edycji");
        if (id == null) return false;
        Gosc wybranyGosc = null;

        for (Gosc gosc : StanKonsoli.goscie){
            if (gosc.getId() == id){
                wybranyGosc = gosc;
                break;
            }
        }

        if (wybranyGosc == null){
            System.out.println("Nie znaleziono gościa o takim ID.");
            return true;
        }
        if(wybranyGosc.isArchiwalny()){System.out.println("Najpierw przywróć kartotekę gościa.");return true;}
        System.out.println("Obecne dane:");
        wybranyGosc.wyswietlDane();

        System.out.println("Podaj nowe dane: ");

        String imie = WejscieKonsoli.wczytajNiepustyTekst("Nowe imię: ");
        if (imie == null) return false;

        String nazwisko = WejscieKonsoli.wczytajNiepustyTekst("Nowe nazwisko: ");
        if (nazwisko == null) return false;

        String telefon = WejscieKonsoli.wczytajNiepustyTekst("Telefon ");
        if (telefon == null) return false;

        String email;

        while (true){
            email = WejscieKonsoli.wczytajNiepustyTekst("Nowy email: ");
            if (email == null) return false;

            if (WejscieKonsoli.czyPoprawnyEmail(email)){
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
            System.out.println("Błąd aktualizacji: " + pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e));
            return true;
        }

        try {
            StanKonsoli.goscie = BazaDanych.pobierzGosci();
            StanKonsoli.rezerwacje = BazaDanych.pobierzRezerwacje(
                    StanKonsoli.goscie,
                    StanKonsoli.pokoje
            );
        } catch (SQLException e){
            System.out.println("Dane zapisano, anie udało się odświeżyć list" + pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e));
            System.out.println("Uruchom program ponownie, aby wczytać aktualne dane.");
        }

        return true;

    }
}
