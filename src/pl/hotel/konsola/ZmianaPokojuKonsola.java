package pl.hotel.konsola;

import java.sql.SQLException;
import pl.hotel.baza.BazaDanych;
import pl.hotel.model.Rezerwacja;
import pl.hotel.model.Pokoj;
import pl.hotel.bezpieczenstwo.KomunikatyBledu;

final class ZmianaPokojuKonsola {
    private ZmianaPokojuKonsola() {}

    static boolean wykonaj() {
        try {
            odswiez();
            for (Rezerwacja r : StanKonsoli.rezerwacje) r.wyswietlDane();
        } catch (SQLException e) {
            System.out.println(KomunikatyBledu.dlaUzytkownika(e)); return true;
        }
        Integer id = WejscieKonsoli.wczytajLiczbe("ID rezerwacji: ");
        if (id == null) return false;
        Rezerwacja r = StanKonsoli.rezerwacje.stream().filter(x -> x.getId() == id).findFirst().orElse(null);
        if (r == null) { System.out.println("Nie znaleziono rezerwacji."); return true; }
        for (Pokoj pokoj : StanKonsoli.pokoje) pokoj.wyswietlDane();
        String numer = WejscieKonsoli.wczytajNiepustyTekst("Numer nowego pokoju: ");
        if (numer == null) return false;
        Pokoj cel = StanKonsoli.pokoje.stream().filter(x -> x.getNumer().equalsIgnoreCase(numer)).findFirst().orElse(null);
        if (cel == null) { System.out.println("Nie znaleziono pokoju."); return true; }
        System.out.println("Cena i płatności pozostaną bez zmian. Pokój zmieni się dla całego pobytu w kalendarzu.");
        System.out.println("Przy zameldowanym pobycie poprzedni pokój będzie wymagał sprzątania.");
        String zgoda = WejscieKonsoli.wczytajNiepustyTekst("Potwierdź wpisując TAK: ");
        if (zgoda == null) return false;
        if (!zgoda.equalsIgnoreCase("TAK")) return true;
        try {
            BazaDanych.zmienPokojRezerwacji(r.getId(), r.getPokoj().getId(), cel.getId());
        } catch (SQLException e) {
            System.out.println("Nie potwierdzono zmiany: " + KomunikatyBledu.dlaUzytkownika(e)); return true;
        }
        System.out.println("Zapisano zmianę pokoju.");
        try { odswiez(); }
        catch (SQLException e) { System.out.println("Zapisano zmianę, ale nie udało się odświeżyć list. Uruchom program ponownie."); }
        return true;
    }

    private static void odswiez() throws SQLException {
        StanKonsoli.goscie = BazaDanych.pobierzGosci();
        StanKonsoli.pokoje = BazaDanych.pobierzPokoje();
        StanKonsoli.rezerwacje = BazaDanych.pobierzRezerwacje(StanKonsoli.goscie, StanKonsoli.pokoje);
    }
}
