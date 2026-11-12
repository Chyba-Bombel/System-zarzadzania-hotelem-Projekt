package pl.hotel.konsola;

import java.sql.SQLException;
import pl.hotel.baza.BazaDanych;
import pl.hotel.bezpieczenstwo.KomunikatyBledu;
import pl.hotel.model.Pokoj;

final class StanPokojuKonsola {
    private StanPokojuKonsola() {}

    static boolean zmien() {
        if (!KontaKonsola.wymagaAdministratora()) return true;
        try {
            var pokoje = BazaDanych.pobierzPokoje();
            for (Pokoj p : pokoje) p.wyswietlDane();
            String numer = WejscieKonsoli.wczytajNiepustyTekst("Numer pokoju: ");
            if (numer == null) return false;
            Pokoj p = pokoje.stream().filter(x -> x.getNumer().equalsIgnoreCase(numer)).findFirst().orElse(null);
            if (p == null) { System.out.println("Nie znaleziono pokoju."); return true; }
            System.out.println("1. Wyłącz pokój | 2. Przywróć do sprzątania | 0. Wróć");
            String wybor = WejscieKonsoli.wczytajNiepustyTekst("Wybierz: ");
            if (wybor == null) return false;
            if (!wybor.equals("1") && !wybor.equals("2")) return true;
            System.out.println("Pokój z aktywnymi rezerwacjami nie może zmienić dostępności. Przywrócenie wymaga sprzątania przed zameldowaniem.");
            String zgoda = WejscieKonsoli.wczytajNiepustyTekst("Potwierdź wpisując TAK: ");
            if (zgoda == null) return false;
            if (!zgoda.equalsIgnoreCase("TAK")) return true;
            Pokoj wynik = wybor.equals("1") ? BazaDanych.wylaczPokoj(p.getId(),p.getStan()) : BazaDanych.przywrocPokoj(p.getId(),p.getStan());
            System.out.println("Zapisano stan pokoju: " + wynik.getNumer() + " | " + wynik.getStan());
        } catch (SQLException e) {
            System.out.println("Nie potwierdzono zmiany stanu: " + KomunikatyBledu.dlaUzytkownika(e)); return true;
        }
        try {
            StanKonsoli.goscie = BazaDanych.pobierzGosci(); StanKonsoli.pokoje = BazaDanych.pobierzPokoje();
            StanKonsoli.rezerwacje = BazaDanych.pobierzRezerwacje(StanKonsoli.goscie,StanKonsoli.pokoje);
        } catch (SQLException e) {
            System.out.println("Zapisano zmianę, ale nie udało się odświeżyć list. Uruchom program ponownie.");
        }
        return true;
    }
}
