package pl.hotel.gui;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import pl.hotel.baza.BazaDanych;
import pl.hotel.model.Pokoj;
import pl.hotel.model.Rezerwacja;
import static pl.hotel.gui.NarzedziaGUI.*;

final class ZmianaPokojuGUI {
    private final HotelGUI app;
    ZmianaPokojuGUI(HotelGUI app) { this.app = app; }

    void pokaz(Rezerwacja wybrana) {
        app.zadaniaGUI.odswiezPo("Przygotowanie zmiany pokoju", () -> {
            Rezerwacja r = app.stan.rezerwacje.stream()
                    .filter(x -> x.getId() == wybrana.getId()).findFirst().orElse(null);
            if (r == null || !(r.getStatus().equals("POTWIERDZONA") || r.getStatus().equals("ZAMELDOWANA"))) {
                app.komunikatyGUI.blad("Wybierz potwierdzoną lub zameldowaną rezerwację."); return;
            }
            boolean pobyt = r.getStatus().equals("ZAMELDOWANA");
            var pokoje = app.stan.pokoje.stream()
                    .filter(p -> p.getId() != r.getPokoj().getId())
                    .filter(p -> p.getLiczbaMiejsc() >= r.getLiczbaOsob())
                    .filter(p -> !p.getStan().equals("WYLACZONY"))
                    .filter(p -> !pobyt || p.getStan().equals("GOTOWY"))
                    .filter(p -> app.stan.rezerwacje.stream().noneMatch(x -> x.getId() != r.getId()
                            && x.kolidacja(p.getNumer(), r.getOd(), r.getDoDnia())))
                    .toList();
            if (pokoje.isEmpty()) {
                app.komunikatyGUI.blad("Brak innego pokoju spełniającego wymagania w całym terminie pobytu."); return;
            }
            Formularz f = new Formularz(app, "Zmiana pokoju", "Rezerwacja #" + r.getId()
                    + " • " + osoba(r.getGosc()), "Zmień pokój");
            f.notatka("Obecny pokój: " + r.getPokoj().getNumer() + " • " + r.getOd() + " – " + r.getDoDnia()
                    + " • liczba osób: " + r.getLiczbaOsob());
            ComboBox<Pokoj> pokoj = new ComboBox<>(FXCollections.observableArrayList(pokoje));
            pokoj.setId("change-room-target");
            pokoj.setConverter(konwerter(p -> p.getNumer() + " • " + p.getLiczbaMiejsc() + " os. • " + p.getStan()));
            pokoj.getSelectionModel().selectFirst(); f.dodaj("Nowy pokój", pokoj);
            f.notatka("Cena całego pobytu pozostaje bez zmian: " + zl(r.obliczCene())
                    + ". Wpłaty i lista gości zostają zachowane. Dostępność zostanie ponownie sprawdzona podczas zapisu.");
            f.notatka("Zmiana dotyczy całego pobytu w kalendarzu. Program nie przechowuje historii noclegów w różnych pokojach.");
            if (pobyt) f.notatka("Poprzedni pokój zostanie oznaczony jako wymagający sprzątania.");
            f.pokaz(() -> {
                Pokoj cel = pokoj.getValue();
                if (cel == null) throw new IllegalArgumentException("Wybierz nowy pokój.");
                app.zadaniaGUI.uwierzytelnione(false, "Zmiana pokoju", () -> {
                    BazaDanych.zmienPokojRezerwacji(r.getId(), r.getPokoj().getId(), cel.getId()); return true;
                }, v -> { f.zamknij(); app.zadaniaGUI.odswiez("Zmieniono pokój rezerwacji #" + r.getId() + "."); });
            });
        });
    }
}
