package pl.hotel.gui;

import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import pl.hotel.serwis.GuiRepository;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import static pl.hotel.gui.NarzedziaGUI.*;

final class EkranPokoi {
    private final HotelGUI app;
    EkranPokoi(HotelGUI app) { this.app = app; }

    Node stronaPokoi() {
        HBox bar = pasek(app.stan.szukajPokoju, app.stan.stanPokoju);
        if (app.admin()) bar.getChildren().addAll(przycisk("+ Dodaj pokój", "primary", () -> formularzPokoju(null)),
                app.zaznaczony("Edytuj pokój", app.stan.tabelaPokoi, this::formularzPokoju));
        Button sprzataj = app.zaznaczony("Oznacz jako posprzątany", app.stan.tabelaPokoi, p -> {
            if (!p.getStan().equals("DO_SPRZATANIA")) { app.komunikatyGUI.blad("Wybierz pokój o stanie DO_SPRZATANIA."); return; }
            if (!app.komunikatyGUI.potwierdz("Sprzątanie", "Pokój " + p.getNumer() + " został posprzątany?")) return;
            app.zadaniaGUI.uwierzytelnione(false, "Zmiana stanu pokoju", () -> {
                if (!BazaDanych.oznaczPokojJakoPosprzatany(p.getId())) throw new SQLException("Nie zmieniono stanu. Odśwież dane pokoju.");
                return true;
            }, v -> app.zadaniaGUI.odswiez("Pokój jest gotowy."));
        });
        HBox dostepnosc = pasek(etykieta("Przyjazd", "form-label"), app.stan.dostepnyOd,
                etykieta("Wyjazd", "form-label"), app.stan.dostepnyDo,
                etykieta("Osoby", "form-label"), app.stan.miejsca,
                przycisk("Sprawdź termin", "", () -> {
                    try {
                        LocalDate a = dataZ(app.stan.dostepnyOd), b = dataZ(app.stan.dostepnyDo);
                        termin(a,b); app.stan.filtrOd = a; app.stan.filtrDo = b; app.stan.filtrOsoby = app.stan.miejsca.getValue();
                    }
                    catch (IllegalArgumentException e) { app.komunikatyGUI.blad(e.getMessage()); return; }
                    app.stan.filtrTerminu = true;
                    app.zadaniaGUI.odswiez("Sprawdzono dostępność według aktualnych rezerwacji.");
                }));
        app.stan.miejsca.setPrefWidth(72);
        Button reset = przycisk("Wszystkie pokoje", "", () -> { app.stan.filtrTerminu = false; app.tabeleGUI.filtruj(); });
        VBox page = app.strona("Pokoje", "Cennik, stan przygotowania i wyszukiwanie wolnych terminów.", bar,
                dostepnosc, pasek(app.stan.filtrInfo, reset), app.stan.tabelaPokoi, pasek(sprzataj));
        VBox.setVgrow(app.stan.tabelaPokoi, Priority.ALWAYS); return page;
    }

    void formularzPokoju(Pokoj p) {
        Formularz f = new Formularz(app, p == null ? "Nowy pokój" : "Edycja pokoju", "Cena dotyczy całego pokoju za jedną noc.", "Zapisz pokój");
        TextField numer = f.tekst("Numer", p == null ? "" : p.getNumer());
        TextField typ = f.tekst("Typ", p == null ? "Standard" : app.stan.typy.getOrDefault(p.getId(), "Standard"));
        Spinner<Integer> ile = new Spinner<>(1, 10, p == null ? 2 : p.getLiczbaMiejsc());
        ile.setDisable(p != null); f.dodaj("Liczba miejsc", ile);
        TextField cena = f.tekst("Cena za noc (zł)", p == null ? "" : p.getCenaZaNoc().toPlainString());
        if (p != null) f.notatka("Liczba miejsc pozostaje bez zmian, aby zachować zgodność istniejących rezerwacji. Wcześniejsze pobyty zachowają zapisaną stawkę.");
        f.pokaz(() -> {
            String n = numer.getText().trim(), t = typ.getText().trim();
            GuiRepository.tekst(n, 10, "Numer"); GuiRepository.tekst(t, 40, "Typ");
            BigDecimal kwota = GuiRepository.kwota(cena.getText()); int l = ile.getValue();
            app.zadaniaGUI.uwierzytelnione(true, "Zapis pokoju", () -> {
                if (p == null) BazaDanych.dodajPokoj(n, t, l, kwota);
                else if (!BazaDanych.edytujPokoj(p.getId(), n, t, kwota)) throw new SQLException("Nie potwierdzono aktualizacji pokoju.");
                return true;
            }, v -> { f.zamknij(); app.zadaniaGUI.odswiez("Zapisano dane pokoju."); });
        });
    }
}
