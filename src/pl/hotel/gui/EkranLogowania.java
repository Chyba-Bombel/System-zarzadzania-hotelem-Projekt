package pl.hotel.gui;

import pl.hotel.baza.BazaDanych;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.Map;
import static pl.hotel.gui.NarzedziaGUI.*;

/** Kontroler: EkranLogowania. */
final class EkranLogowania {
    private final HotelGUI app;
    EkranLogowania(HotelGUI app) { this.app = app; }

    void pokazLogowanie() {
        app.stan.sesja = null;
        app.stan.goscie.clear(); app.stan.pokoje.clear(); app.stan.rezerwacje.clear(); app.stan.konta.clear(); app.stan.typy = Map.of();
        app.stan.tabelaPlatnosci.getItems().clear(); app.stan.szczegoly.setText("Wybierz rezerwację.");
        app.stan.filtrTerminu = false;
        app.stan.szukajGoscia.clear(); app.stan.szukajPokoju.clear(); app.stan.szukajRezerwacji.clear(); app.stan.szukajKonta.clear();
        app.stan.stanPokoju.getSelectionModel().selectFirst(); app.stan.statusRezerwacji.getSelectionModel().selectFirst();
        TextField login = pole("Login pracownika"); login.setId("login");
        PasswordField haslo = new PasswordField(); haslo.setPromptText("Hasło"); haslo.setId("password");
        Label blad = etykieta("", "error-text"); blad.setWrapText(true);
        Button wejdz = przycisk("Zaloguj się", "primary", null); wejdz.setId("login-button");
        wejdz.setMaxWidth(Double.MAX_VALUE);
        Runnable loguj = () -> {
            if (app.stan.busy.get()) return;
            if (app.stan.proby >= 3) { blad.setText("Wykorzystano 3 próby. Uruchom program ponownie."); return; }
            String l = login.getText().trim(), h = haslo.getText();
            if (l.isEmpty() || h.isEmpty()) { blad.setText("Wpisz login i hasło."); return; }
            blad.setText(""); haslo.clear();
            app.zadaniaGUI.zadanie("Logowanie", () -> BazaDanych.zaloguj(l, h), p -> {
                if (p == null) {
                    app.stan.proby++;
                    blad.setText("Nieprawidłowe dane lub nieaktywne konto. Pozostało prób: " + (3 - app.stan.proby));
                    if (app.stan.proby >= 3) { wejdz.setDisable(true); haslo.setDisable(true); }
                } else {
                    app.stan.sesja = p; app.stan.proby = 0; app.stan.strona = "Pulpit";
                    app.pokazPanel(); app.zadaniaGUI.odswiez("Zalogowano poprawnie.");
                }
            });
        };
        wejdz.setOnAction(e -> loguj.run()); haslo.setOnAction(e -> loguj.run()); login.setOnAction(e -> haslo.requestFocus());
        VBox card = new VBox(12, etykieta("HOTEL", "section-title"), etykieta("Witaj ponownie", "title"),
                etykieta("Zaloguj się na konto pracownika, aby rozpocząć pracę.", "subtitle"),
                etykieta("Login", "form-label"), login, etykieta("Hasło", "form-label"), haslo, blad, wejdz,
                etykieta("Połączenie z bazą skonfigurujesz w pliku db.properties.", "muted"));
        card.setMaxWidth(440); card.setMaxHeight(Region.USE_PREF_SIZE); card.getStyleClass().add("login-card");
        StackPane page = new StackPane(card); page.getStyleClass().add("login-page");
        app.ustawScene(page, true); login.requestFocus();
    }
}
