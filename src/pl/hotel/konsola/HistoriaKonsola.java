package pl.hotel.konsola;

import java.sql.SQLException;
import pl.hotel.baza.BazaDanych;
import pl.hotel.bezpieczenstwo.KomunikatyBledu;

final class HistoriaKonsola {
    private HistoriaKonsola() {}

    static boolean pokaz() {
        Integer id = WejscieKonsoli.wczytajLiczbe("ID rezerwacji: ");
        if (id == null) return false;
        long przedId = 0;
        while (true) {
            try {
                var strona = BazaDanych.pobierzHistorieRezerwacji(id, przedId);
                if (strona.wpisy().isEmpty()) System.out.println("Brak zapisanych zmian. Historia działa od aktualizacji programu.");
                for (var wpis : strona.wpisy()) {
                    System.out.println("#" + wpis.id() + " | " + wpis.utworzono() + " | " + wpis.login() + " | " + wpis.opisAkcji());
                    System.out.println(wpis.szczegoly());
                }
                if (!strona.maStarsze()) return true;
                String dalej = WejscieKonsoli.wczytajNiepustyTekst("Wyświetlić starsze wpisy? TAK/NIE: ");
                if (dalej == null) return false;
                if (!dalej.equalsIgnoreCase("TAK")) return true;
                przedId = strona.wpisy().get(strona.wpisy().size()-1).id();
            } catch (SQLException e) {
                System.out.println("Nie udało się odczytać historii: " + KomunikatyBledu.dlaUzytkownika(e)); return true;
            }
        }
    }
}
