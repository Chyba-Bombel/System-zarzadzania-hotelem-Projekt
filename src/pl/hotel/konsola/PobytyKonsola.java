package pl.hotel.konsola;

import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import java.sql.SQLException;

final class PobytyKonsola {
    private PobytyKonsola() {}

    static boolean zameldujGoscia(){
        try {
            StanKonsoli.goscie = BazaDanych.pobierzGosci();
            StanKonsoli.pokoje = BazaDanych.pobierzPokoje();

            StanKonsoli.rezerwacje = BazaDanych.pobierzRezerwacje(StanKonsoli.goscie,StanKonsoli.pokoje);

        }catch (SQLException e){
            System.out.println("Błąd pobierania danych: " + pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e));
            return true;
        }

        for (Rezerwacja rezerwacja : StanKonsoli.rezerwacje){
            if (rezerwacja.getStatus().equals("POTWIERDZONA")){
                rezerwacja.wyswietlDane();
            }
        }

        Integer id = WejscieKonsoli.wczytajLiczbe("ID rezerwacji do zameldowania: ");
        if (id == null) return false;

        try {
            BazaDanych.zameldujGoscia(id);

            System.out.println("Zameldowano gościa.");
        } catch (SQLException e) {
            System.out.println("Nie powierdzono zameldowania: " + pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e));
            System.out.println("Przed ponowieniem sprawdź aktualny status w bazie.");
            return true;
        }
        try {
            StanKonsoli.rezerwacje = BazaDanych.pobierzRezerwacje(StanKonsoli.goscie, StanKonsoli.pokoje);
        } catch (SQLException e) {
            System.out.println("Zamledowanie zapisano ale nie udało sie odświeżyć listy: " + pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e));
            System.out.println("Uruchom program ponownie.");
        }
        return true;

    }

    static boolean wymeldujGoscia(){
        try {
            StanKonsoli.goscie = BazaDanych.pobierzGosci();
            StanKonsoli.pokoje = BazaDanych.pobierzPokoje();

            StanKonsoli.rezerwacje = BazaDanych.pobierzRezerwacje(StanKonsoli.goscie,StanKonsoli.pokoje);
        } catch (SQLException e) {
            System.out.println("Nie udało się pobrać danych: " + pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e));
            return true;
        }

        boolean znaleziono = false;

        for (Rezerwacja rezerwacja : StanKonsoli.rezerwacje){
            if (rezerwacja.getStatus().equals("ZAMELDOWANA")){
                rezerwacja.wyswietlDane();
                znaleziono = true;

            }
        }

        if (!znaleziono) {
            System.out.println("Nie ma zameldowanych rezerwacji.");
            return true;
        }

        Integer id  = WejscieKonsoli.wczytajLiczbe("Id rezereacji do wymeldowania: ");
        if (id == null) return false;
        try {
            BazaDanych.wymeldujGoscia(id);

            System.out.println("Wymeldowano gościa.");
        } catch (SQLException e){
            System.out.println("Nie potwierdzono wymeldowania: " + pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e));
            System.out.println("Przed ponowieniem sprawdź aktualny status w bazie.");
            return true;
        }
        try {
            StanKonsoli.pokoje = BazaDanych.pobierzPokoje();

            StanKonsoli.rezerwacje = BazaDanych.pobierzRezerwacje(StanKonsoli.goscie, StanKonsoli.pokoje);
        } catch (SQLException e) {
            System.out.println("Wymeldowanie zapisano ale nie udało się odświeżyć danych: " + pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e));
            System.out.println("Uruchom program ponownie");

        }
        return true;
    }
}
