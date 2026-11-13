package pl.hotel.gui;

import pl.hotel.baza.BazaDanych;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.Map;
import static pl.hotel.gui.NarzedziaGUI.*;

final class EkranLogowania {
    private final HotelGUI app;
    EkranLogowania(HotelGUI app) { this.app = app; }

    void pokazLogowanie() {
        app.stan.executor.submit(BazaDanych::wyloguj);
        app.stan.sesja = null;
        app.stan.goscie.clear(); app.stan.pokoje.clear(); app.stan.rezerwacje.clear(); app.stan.konta.clear(); app.stan.typy = Map.of();
        app.stan.tabelaPlatnosci.getItems().clear(); app.stan.szczegoly.setText("Wybierz rezerwację.");
        app.stan.filtrTerminu = false;
        app.stan.stanGoscia.getSelectionModel().selectFirst();
        app.stan.szukajGoscia.clear(); app.stan.szukajPokoju.clear(); app.stan.szukajRezerwacji.clear(); app.stan.szukajKonta.clear();
        app.stan.stanPokoju.getSelectionModel().selectFirst(); app.stan.statusRezerwacji.getSelectionModel().selectFirst();
        TextField login = pole("Login pracownika"); login.setId("login");
        PasswordField haslo = new PasswordField(); ogranicz(haslo,128); haslo.setPromptText("Hasło"); haslo.setId("password");
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
                    blad.setText("Niepoprawne dane, nieaktywne lub czasowo zablokowane konto. Pozostało prób: " + (3 - app.stan.proby));
                    if (app.stan.proby >= 3) { wejdz.setDisable(true); haslo.setDisable(true); }
                } else {
                    app.stan.sesja = p; app.stan.proby = 0; app.stan.strona = "Pulpit";
                    app.pokazPanel(); app.zadaniaGUI.odswiez("Zalogowano poprawnie.");
                }
            });
        };
        wejdz.setOnAction(e -> loguj.run()); haslo.setOnAction(e -> loguj.run()); login.setOnAction(e -> haslo.requestFocus());
        Button konfiguracja = przycisk("Ustawienia połączenia z bazą", "", app.ustawienia::pokazPolaczenie);
        konfiguracja.setId("config-open");
        Button informacje = przycisk("O programie • " + pl.hotel.konfiguracja.WersjaAplikacji.NUMER, "", app.ustawienia::pokazInformacje);
        informacje.setId("about-open");
        VBox card = new VBox(12, etykieta("HOTEL MANAGER", "login-brand"), etykieta("Witaj ponownie", "title"),
                etykieta("Zaloguj się na konto pracownika, aby rozpocząć pracę.", "subtitle"),
                etykieta("Login", "form-label"), login, etykieta("Hasło", "form-label"), haslo, blad, wejdz,
                konfiguracja, informacje);
        card.setMaxWidth(460); card.setMaxHeight(Region.USE_PREF_SIZE); card.getStyleClass().add("login-card");
        VBox powitanie = new VBox(16, etykieta("H", "hero-monogram"), etykieta("Dobra organizacja.\nSpokojna recepcja.", "hero-title"),
                etykieta("Goście, rezerwacje i rozliczenia\nw jednym miejscu.", "hero-description"));
        powitanie.getStyleClass().add("login-hero"); powitanie.setMinWidth(340); powitanie.setPrefWidth(430);
        StackPane formularz = new StackPane(card); HBox.setHgrow(formularz, javafx.scene.layout.Priority.ALWAYS);
        HBox srodek = new HBox(36, powitanie, formularz); srodek.setAlignment(javafx.geometry.Pos.CENTER); srodek.setMaxWidth(1040);
        Button motyw = app.wyglad.przelacznik();
        Button wyglad = przycisk("Wygląd", "", app.wyglad::pokaz); wyglad.setId("appearance-open"); wyglad.setGraphic(IkonyGUI.ikona("Wygląd"));
        HBox gorny = new HBox(10, motyw, wyglad); gorny.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        BorderPane uklad = new BorderPane(); uklad.setTop(gorny); uklad.setCenter(new StackPane(srodek));
        StackPane page = new StackPane(uklad); page.getStyleClass().add("login-page");
        app.ustawScene(page, true); login.requestFocus();
    }
}
