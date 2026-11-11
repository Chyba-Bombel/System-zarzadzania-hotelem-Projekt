package pl.hotel.konsola;

import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import java.math.BigDecimal;
import java.sql.SQLException;

final class PlatnosciKonsola {
    private PlatnosciKonsola() {}

    static boolean pokazRozliczenie(){
        Integer id = WejscieKonsoli.wczytajLiczbe("ID rezerwacji do rozliczenia: ");
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
            System.out.println("Błąd pobierania rozliczenia: " + pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e));
        }
        return true;
    }

    static String wybierzMetodePlatnosci(){
        while (true){
            System.out.println("1. Gotówka");
            System.out.println("2. Karta");
            System.out.println("3. Przelew");

            String wybor = WejscieKonsoli.wczytajNiepustyTekst("Metoda płatności: ");

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

    static boolean dodajWplate(){

        Integer id = WejscieKonsoli.wczytajLiczbe("ID rezerwacji: ");
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
            System.out.println("Błąd odczytu rozliczenia: " + pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e));
            return true;
        }

        BigDecimal kwota = WejscieKonsoli.wczytajCene("Kwota wpłaty: ");
        if (kwota == null) return false;

        String metoda = wybierzMetodePlatnosci();
        if (metoda == null) return false;

        try {
            BazaDanych.zapiszWplate(id,kwota,metoda,StanKonsoli.zalogowanyPracownik.getId());
            System.out.println("Zapisano wpłatę: " + kwota + " zł.");
        } catch (SQLException e) {
            System.out.println("Nie potwierdzono wpłaty: " + pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e));
            System.out.println("Przed ponowieniem sprawdź historie płatności");
        }
        return true;
    }

    static boolean dodajZwrot(){
        Integer id = WejscieKonsoli.wczytajLiczbe("ID rezerwacji: ");
        if (id == null) return false;

        try {
            Rozliczenie rozliczenie = BazaDanych.pobierzRozliczenie(id);
            BigDecimal dostepneDoZwrotu = rozliczenie.zaplaconoNetto();

            if (dostepneDoZwrotu.signum() <= 0){
                System.out.println("Brak środków, które można zwrócić");
                return true;
            }
            System.out.println("Maksymalna kwota zwrotu: " + dostepneDoZwrotu + " zł");

            if (!rozliczenie.status().equals("ANULOWANA")){
                System.out.println("Uwaga: zwrot nie zmniejsza ceny pobytu. Zwiększy pozostałą należność.");

            }
        } catch (SQLException e) {
            System.out.println("Błąd odczytu rozliczenia: " + pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e));
            return true;

        }
        BigDecimal kwota = WejscieKonsoli.wczytajCene("Kwota zwrotu: ");
        if (kwota == null) return false;

        String metoda = wybierzMetodePlatnosci();
        if (metoda == null) return false;

        String potwierdzenie = WejscieKonsoli.wczytajNiepustyTekst("Potwierdzasz zwrot " + kwota + " zł (t/n) ? ");
        if (potwierdzenie == null) return false;

        if (!potwierdzenie.equalsIgnoreCase("t")){
            System.out.println("Zrezygnowano ze zwrotu.");
            return true;
        }
        try {
            BazaDanych.zapiszZwrot(id, kwota, metoda, StanKonsoli.zalogowanyPracownik.getId());

            System.out.println("Zarejestrowano zwrot " + kwota + " zł.");
        } catch (SQLException e){
            System.out.println("Nie potwierdzono zwrotu: " + pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e));

            System.out.println("Przed ponowieniem sprawdź historię płatności w opcji 17.");
        }
        return true;
    }
}
