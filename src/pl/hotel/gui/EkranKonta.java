package pl.hotel.gui;

import pl.hotel.baza.BazaDanych;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import static pl.hotel.gui.NarzedziaGUI.*;

final class EkranKonta {
    private final HotelGUI app;
    EkranKonta(HotelGUI app) { this.app = app; }

    Node stronaKonta() {
        Label opis = new Label("Hasło musi mieć 12–128 znaków. Zmiana wymaga podania obecnego hasła."); opis.setWrapText(true);
        VBox karta = new VBox(15, etykieta(osoba(app.stan.sesja), "section-title"),
                new Label("Login: " + app.stan.sesja.getLogin()), new Label("Rola: " + app.stan.sesja.getRola()), opis,
                przycisk("Zmień własne hasło", "primary", this::formularzHasla));
        karta.getStyleClass().add("card"); karta.setMaxWidth(650);
        return app.strona("Moje konto", "Dane aktualnie zalogowanej osoby.", karta);
    }

    void formularzHasla() {
        Formularz f = new Formularz(app, "Zmiana własnego hasła", "Konto: " + app.stan.sesja.getLogin(), "Zmień hasło");
        PasswordField stare = f.haslo("Obecne hasło"), nowe = f.haslo("Nowe hasło"), powtorz = f.haslo("Powtórz nowe hasło");
        f.notatka("Nowe hasło: 12–128 znaków. Spacje są zachowywane. Po zmianie hasła sesje wymagają ponownego logowania przy następnej operacji.");
        f.pokaz(() -> {
            String s = stare.getText(), n = nowe.getText(); sprawdzHasla(n,powtorz.getText());
            if (s.isEmpty()) throw new IllegalArgumentException("Podaj obecne hasło.");
            long id = app.stan.sesja.getId();
            app.zadaniaGUI.uwierzytelnione(false, "Zmiana hasła", () -> { BazaDanych.zmienHaslo(id,s,n); return true; },
                    v -> { stare.clear(); nowe.clear(); powtorz.clear(); f.zamknij(); app.ekranLogowania.pokazLogowanie(); app.stan.status.setText("Hasło zmieniono. Zaloguj się ponownie."); });
        });
    }
}
