package pl.hotel.konsola;

import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.Locale;
import pl.hotel.baza.BazaDanych;
import pl.hotel.bezpieczenstwo.KomunikatyBledu;
import pl.hotel.potwierdzenia.Potwierdzenia;

final class PotwierdzenieKonsola {
    private PotwierdzenieKonsola() {}

    static boolean zapisz() {
        Integer id = WejscieKonsoli.wczytajLiczbe("ID rezerwacji: ");
        if (id == null) return false;
        try {
            var p = BazaDanych.pobierzPotwierdzenieRezerwacji(id);
            System.out.println(Potwierdzenia.uwaga(p));
            System.out.println("Stan na: " + Potwierdzenia.czas(p) + " (czas serwera MySQL)");
            for (var w : Potwierdzenia.pobyt(p)) System.out.println(w.get(0) + ": " + w.get(1));
            for (var g : Potwierdzenia.goscie(p)) System.out.println("Gość: " + g);
            for (var w : Potwierdzenia.rozliczenie(p)) System.out.println(w.get(0) + ": " + w.get(1));
            String sciezka = WejscieKonsoli.wczytajNiepustyTekst("Ścieżka zapisu HTML (np. " + Potwierdzenia.nazwaPliku(p) + "): ");
            if (sciezka == null) return false;
            if (!sciezka.toLowerCase(Locale.ROOT).endsWith(".html")) sciezka += ".html";
            Path cel = Path.of(sciezka);
            if (Files.exists(cel)) {
                String zgoda = WejscieKonsoli.wczytajNiepustyTekst("Plik istnieje. Zastąpić? TAK/NIE: ");
                if (zgoda == null) return false;
                if (!zgoda.equalsIgnoreCase("TAK")) return true;
            }
            BazaDanych.sprawdzSesje(StanKonsoli.zalogowanyPracownik,false);
            Potwierdzenia.zapiszHtml(p,cel);
            System.out.println("Zapisano potwierdzenie: " + cel.toAbsolutePath());
            System.out.println("Otwórz HTML w przeglądarce. Ctrl+P umożliwia wydruk lub zapis do PDF.");
        } catch (SQLException e) {
            System.out.println("Nie udało się pobrać danych: " + KomunikatyBledu.dlaUzytkownika(e));
        } catch (InvalidPathException e) {
            System.out.println("Niepoprawna ścieżka pliku.");
        } catch (IOException e) {
            System.out.println("Nie udało się zapisać pliku. Sprawdź folder i uprawnienia do zapisu.");
        }
        return true;
    }
}
