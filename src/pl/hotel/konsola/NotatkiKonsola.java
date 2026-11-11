package pl.hotel.konsola;

import java.sql.SQLException;
import pl.hotel.baza.BazaDanych;
import pl.hotel.bezpieczenstwo.KomunikatyBledu;

final class NotatkiKonsola {
    private NotatkiKonsola() {}

    static boolean pokaz() {
        Integer id = WejscieKonsoli.wczytajLiczbe("ID rezerwacji: ");
        if (id == null) return false;
        try {
            var n = BazaDanych.pobierzNotatkeRezerwacji(id);
            System.out.println("Zapisana notatka rezerwacji #" + id + ":");
            System.out.println(n.tresc().isEmpty() ? "Brak notatki." : n.tresc());
            if (!n.edytowalna()) { System.out.println("Notatka jest tylko do odczytu."); return true; }
            System.out.println("1. Zmień notatkę | 2. Usuń treść notatki | 0. Wróć");
            String wybor = WejscieKonsoli.wczytajNiepustyTekst("Wybierz: ");
            if (wybor == null) return false;
            String tekst;
            if (wybor.equals("2")) tekst = "";
            else if (wybor.equals("1")) { tekst = wczytaj(); if (tekst == null) return true; }
            else return true;
            try {
                BazaDanych.zapiszNotatkeRezerwacji(id,n.tresc(),tekst);
                System.out.println("Zapisano notatkę.");
            } catch (SQLException | IllegalArgumentException e) {
                System.out.println("Nie zapisano notatki: " + KomunikatyBledu.dlaUzytkownika(e));
                System.out.println("Twój niezapisany tekst:\n" + tekst);
            }
        } catch (SQLException e) {
            System.out.println("Nie udało się pobrać notatki: " + KomunikatyBledu.dlaUzytkownika(e));
        }
        return true;
    }

    private static String wczytaj() {
        System.out.println("Wpisz nową treść, maksymalnie 500 znaków. :zapisz kończy, :anuluj wraca bez zapisu.");
        System.out.println("Aby wpisać linię zaczynającą się dwukropkiem, rozpocznij ją od ::.");
        StringBuilder tekst = new StringBuilder(); boolean pierwsza = true;
        while (StanKonsoli.scanner.hasNextLine()) {
            String linia = StanKonsoli.scanner.nextLine();
            if (linia.equals(":anuluj")) return null;
            if (linia.equals(":zapisz")) return tekst.toString();
            if (linia.startsWith("::")) linia = linia.substring(1);
            String nowy = tekst + (pierwsza ? "" : "\n") + linia;
            if (nowy.codePointCount(0,nowy.length()) > 500) {
                System.out.println("Przekroczono limit. Ostatnia linia została pominięta; wpisz krótszą albo :anuluj."); continue;
            }
            tekst.setLength(0); tekst.append(nowy); pierwsza = false;
        }
        return null;
    }
}
