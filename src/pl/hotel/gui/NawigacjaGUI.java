package pl.hotel.gui;

import pl.hotel.konfiguracja.WersjaAplikacji;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import static pl.hotel.gui.NarzedziaGUI.*;

final class NawigacjaGUI {
    private final HotelGUI app;
    private BorderPane obszar;
    private VBox menu;
    private FadeTransition przejscie;
    NawigacjaGUI(HotelGUI app) { this.app = app; }

    void pokazPanel() {
        app.stan.shell = new BorderPane(); app.stan.shell.getStyleClass().add("app-shell");
        VBox nav = new VBox(10); nav.getStyleClass().add("sidebar"); nav.setPrefWidth(232); nav.setMinWidth(232);
        Label monogram = etykieta("H", "brand-monogram");
        VBox marka = new VBox(2, etykieta("HOTEL", "brand"), etykieta("MANAGER", "brand"), etykieta("PANEL RECEPCJI", "brand-sub"));
        HBox logo = new HBox(12, monogram, marka); logo.setAlignment(Pos.CENTER_LEFT); logo.getStyleClass().add("brand-block");
        nav.getChildren().add(logo);
        menu = new VBox(5);
        for (String nazwa : new String[]{"Pulpit", "Goście", "Pokoje", "Rezerwacje", "Kalendarz", "Płatności", "Raporty", "Pracownicy", "Moje konto"}) {
            if (nazwa.equals("Pracownicy") && !app.admin()) continue;
            Button b = przycisk(nazwa, "nav", () -> app.przejdz(nazwa)); b.setGraphic(IkonyGUI.ikona(nazwa));
            b.setMaxWidth(Double.MAX_VALUE); b.setId("nav-" + klucz(nazwa)); menu.getChildren().add(b);
        }
        ScrollPane lista = new ScrollPane(menu); lista.setFitToWidth(true); lista.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        lista.getStyleClass().add("nav-scroll"); lista.setMinHeight(160); VBox.setVgrow(lista, Priority.ALWAYS);
        nav.getChildren().add(lista);
        Button motyw = app.wyglad.przelacznik(); motyw.getStyleClass().add("nav"); motyw.setMaxWidth(Double.MAX_VALUE);
        Button wyglad = przycisk("Wygląd", "nav", app.wyglad::pokaz); wyglad.setId("appearance-open"); wyglad.setGraphic(IkonyGUI.ikona("Wygląd")); wyglad.setMaxWidth(Double.MAX_VALUE);
        Button informacje = przycisk("O programie", "nav", app.ustawienia::pokazInformacje); informacje.setId("about"); informacje.setGraphic(IkonyGUI.ikona("O programie")); informacje.setMaxWidth(Double.MAX_VALUE);
        VBox ustawienia = new VBox(4, motyw, wyglad, informacje); ustawienia.getStyleClass().add("nav-settings");
        String imie = app.stan.sesja.getImie(), nazwisko = app.stan.sesja.getNazwisko();
        Label avatar = etykieta(imie.substring(0,1) + nazwisko.substring(0,1), "avatar");
        VBox osoba = new VBox(4, etykieta(imie + " " + nazwisko, "user-name"), etykieta(app.admin() ? "Administrator" : "Recepcjonista", "user-role"));
        HBox profil = new HBox(10, avatar, osoba); profil.setAlignment(Pos.CENTER_LEFT); profil.getStyleClass().add("user-card");
        Button logout = przycisk("Wyloguj się", "nav", () -> {
            if (app.komunikatyGUI.potwierdz("Wylogowanie", "Zakończyć bieżącą sesję?")) app.ekranLogowania.pokazLogowanie();
        }); logout.setId("logout"); logout.setGraphic(IkonyGUI.ikona("Wyloguj się")); logout.setMaxWidth(Double.MAX_VALUE);
        nav.getChildren().addAll(ustawienia, profil, logout);
        obszar = new BorderPane();
        Label sesja = etykieta("● Sesja aktywna", "session-status");
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        app.stan.status.setMaxWidth(560); app.stan.status.setTooltip(new Tooltip("Wynik ostatniej operacji; nie jest ciągłym testem połączenia."));
        Label wersja = etykieta("v" + WersjaAplikacji.NUMER, "muted");
        HBox stopka = new HBox(14, sesja, app.stan.status, gap, app.stan.ostatnieOdswiezenie, wersja);
        stopka.setAlignment(Pos.CENTER_LEFT); stopka.getStyleClass().add("statusbar"); obszar.setBottom(stopka);
        app.stan.shell.setLeft(nav); app.stan.shell.setCenter(obszar);
        app.pokazStrone(); app.ustawScene(app.stan.shell, false);
    }

    void ustawTresc(Node content) {
        if (przejscie != null) przejscie.stop();
        ScrollPane scroll = new ScrollPane(content); scroll.setFitToWidth(true); scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); scroll.getStyleClass().add("page-scroll");
        obszar.setCenter(scroll);
        if (menu != null) for (Node n : menu.getChildren()) if (n instanceof Button b) {
            b.getStyleClass().remove("active");
            if (b.getText().equals(app.stan.strona)) b.getStyleClass().add("active");
        }
        przejscie = new FadeTransition(Duration.millis(130), content); przejscie.setFromValue(.75); przejscie.setToValue(1); przejscie.play();
    }
}
