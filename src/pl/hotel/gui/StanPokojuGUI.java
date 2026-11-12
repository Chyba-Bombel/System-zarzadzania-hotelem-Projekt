package pl.hotel.gui;

import pl.hotel.baza.BazaDanych;
import pl.hotel.model.Pokoj;

final class StanPokojuGUI {
    private final HotelGUI app;
    StanPokojuGUI(HotelGUI app) { this.app = app; }

    void pokaz(Pokoj wybrany, boolean wylacz) {
        app.zadaniaGUI.odswiezPo("Przygotowanie zmiany stanu pokoju", () -> {
            Pokoj p = app.stan.pokoje.stream().filter(x -> x.getId()==wybrany.getId()).findFirst().orElse(null);
            if (p == null) { app.komunikatyGUI.blad("Nie znaleziono pokoju."); return; }
            if (wylacz && p.getStan().equals("WYLACZONY")) { app.komunikatyGUI.blad("Pokój jest już wyłączony."); return; }
            if (!wylacz && !p.getStan().equals("WYLACZONY")) { app.komunikatyGUI.blad("Wybierz wyłączony pokój."); return; }
            var blokujace = app.stan.rezerwacje.stream().filter(r -> r.getPokoj().getId()==p.getId()
                    && (r.getStatus().equals("POTWIERDZONA") || r.getStatus().equals("ZAMELDOWANA"))).toList();
            if (!blokujace.isEmpty()) {
                String ids = blokujace.stream().limit(5).map(r -> "#"+r.getId()).collect(java.util.stream.Collectors.joining(", "));
                app.komunikatyGUI.blad("Pokój ma aktywne rezerwacje: " + ids + (blokujace.size()>5 ? " i kolejne." : ".")
                        + " Najpierw obsłuż je w zakładce Rezerwacje."); return;
            }
            String nazwa = wylacz ? "Wyłącz pokój" : "Przywróć pokój";
            Formularz f = new Formularz(app,nazwa,"Pokój " + p.getNumer() + " • obecny stan: " + p.getStan(),nazwa);
            f.notatka(wylacz ? "Pokój otrzyma stan WYLACZONY. Nie będzie można utworzyć w nim nowej rezerwacji ani zameldować gości."
                    : "Pokój otrzyma stan DO_SPRZATANIA. Przed zameldowaniem trzeba go przygotować i oznaczyć jako posprzątany.");
            f.notatka("Zmiana dotyczy bieżącego stanu pokoju, bez planowania wyłączeń na przyszłe terminy. Cena i pojemność pozostają bez zmian.");
            f.notatka("Przy zapisie program ponownie sprawdzi stan i aktywne rezerwacje. Zakończone i anulowane pobyty pozostaną w bazie.");
            f.pokaz(() -> app.zadaniaGUI.uwierzytelnione(true,nazwa,() -> wylacz
                    ? BazaDanych.wylaczPokoj(p.getId(),p.getStan()) : BazaDanych.przywrocPokoj(p.getId(),p.getStan()),
                    wynik -> { f.zamknij(); app.zadaniaGUI.odswiez("Pokój " + wynik.getNumer() + " • " + wynik.getStan()); }));
        });
    }
}
