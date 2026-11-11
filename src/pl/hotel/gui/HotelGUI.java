package pl.hotel.gui;

import pl.hotel.raporty.Raporty;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.function.Consumer;
import static pl.hotel.gui.NarzedziaGUI.*;

/** Cykl życia okna i nawigacja. Ekrany, tabele i zadania mają osobne klasy. */
public final class HotelGUI extends Application {
    final StanGUI stan = new StanGUI();
    final EkranLogowania ekranLogowania = new EkranLogowania(this);
    final EkranRaportow ekranRaportow = new EkranRaportow(this);
    final EkranPulpitu ekranPulpitu = new EkranPulpitu(this);
    final EkranGosci ekranGosci = new EkranGosci(this);
    final EkranPokoi ekranPokoi = new EkranPokoi(this);
    final EkranKalendarza ekranKalendarza = new EkranKalendarza(this);
    final EkranRezerwacji ekranRezerwacji = new EkranRezerwacji(this);
    final EkranPlatnosci ekranPlatnosci = new EkranPlatnosci(this);
    final EkranPracownikow ekranPracownikow = new EkranPracownikow(this);
    final EkranKonta ekranKonta = new EkranKonta(this);
    final TabeleGUI tabeleGUI = new TabeleGUI(this);
    final ZadaniaGUI zadaniaGUI = new ZadaniaGUI(this);
    final KomunikatyGUI komunikatyGUI = new KomunikatyGUI(this);

    @Override public void start(Stage stage) {
        stan.stage = stage;
        stage.setTitle("Hotel • System zarządzania");
        stage.setMinWidth(1080); stage.setMinHeight(720);
        tabeleGUI.konfigurujTabele();
        ekranLogowania.pokazLogowanie();
        stage.show();
    }

    void ustawScene(Node content, boolean login) {
        content.disableProperty().bind(stan.busy);
        stan.root = new StackPane(content);
        VBox overlay = new VBox(14, new ProgressIndicator(), etykieta("Trwa operacja…", "section-title"));
        overlay.setAlignment(Pos.CENTER); overlay.getStyleClass().add("busy-overlay");
        overlay.visibleProperty().bind(stan.busy); overlay.managedProperty().bind(stan.busy);
        stan.root.getChildren().add(overlay);
        Scene scene = new Scene(stan.root, login ? 1140 : 1320, login ? 760 : 850);
        scene.getStylesheets().add(getClass().getResource("/hotel.css").toExternalForm());
        stan.stage.setScene(scene);
    }

    void pokazPanel() {
        stan.shell = new BorderPane();
        VBox nav = new VBox(7); nav.setPrefWidth(226); nav.getStyleClass().add("sidebar");
        nav.getChildren().addAll(etykieta("HOTEL", "brand"), etykieta("PANEL RECEPCJI", "brand-sub"), new Separator());
        for (String nazwa : new String[]{"Pulpit", "Goście", "Pokoje", "Rezerwacje", "Kalendarz", "Płatności", "Raporty", "Pracownicy", "Moje konto"}) {
            if (nazwa.equals("Pracownicy") && !admin()) continue;
            Button b = new Button(nazwa); b.setMaxWidth(Double.MAX_VALUE); b.getStyleClass().add("nav");
            b.setId("nav-" + klucz(nazwa));
            b.setOnAction(e -> {
                stan.strona = nazwa;
                for (Node n : nav.getChildren()) if (n instanceof Button) n.getStyleClass().remove("active");
                b.getStyleClass().add("active"); pokazStrone();
            });
            if (nazwa.equals(stan.strona)) b.getStyleClass().add("active");
            nav.getChildren().add(b);
        }
        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS); nav.getChildren().add(spacer);
        VBox osoba = new VBox(5, etykieta(stan.sesja.getImie() + " " + stan.sesja.getNazwisko(), "user-name"),
                etykieta(admin() ? "ADMINISTRATOR" : "RECEPCJONISTA", "user-role"));
        osoba.getStyleClass().add("user-card"); nav.getChildren().add(osoba);
        Button logout = przycisk("Wyloguj się", "", () -> {
            if (komunikatyGUI.potwierdz("Wylogowanie", "Zakończyć bieżącą sesję?")) ekranLogowania.pokazLogowanie();
        }); logout.setId("logout"); logout.setMaxWidth(Double.MAX_VALUE); nav.getChildren().add(logout);
        stan.shell.setLeft(nav);
        HBox footer = new HBox(stan.status); footer.getStyleClass().add("statusbar"); stan.shell.setBottom(footer);
        pokazStrone(); ustawScene(stan.shell, false);
    }

    void pokazStrone() {
        Node content = switch (stan.strona) {
            case "Goście" -> ekranGosci.stronaGosci();
            case "Pokoje" -> ekranPokoi.stronaPokoi();
            case "Rezerwacje" -> ekranRezerwacji.stronaRezerwacji();
            case "Kalendarz" -> ekranKalendarza.stronaKalendarza();
            case "Płatności" -> ekranPlatnosci.stronaPlatnosci();
            case "Raporty" -> ekranRaportow.stronaRaportow();
            case "Pracownicy" -> admin() ? ekranPracownikow.stronaPracownikow() : ekranPulpitu.pulpit();
            case "Moje konto" -> ekranKonta.stronaKonta();
            default -> ekranPulpitu.pulpit();
        };
        stan.shell.setCenter(content);
    }

    VBox strona(String tytul, String opis, Node... elementy) {
        Button refresh = przycisk("Odśwież dane", "", () -> zadaniaGUI.odswiez("Dane zaktualizowane.")); refresh.setId("refresh");
        VBox tekst = new VBox(5, etykieta(tytul, "title"), etykieta(opis, "subtitle"));
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox header = new HBox(12, tekst, gap, refresh); header.setAlignment(Pos.CENTER_LEFT);
        VBox page = new VBox(18, header); page.setPadding(new Insets(28)); page.getChildren().addAll(elementy);
        for (Node element : elementy) if (element instanceof Label l) l.setWrapText(true);
        return page;
    }

    boolean admin() { return stan.sesja != null && stan.sesja.getRola().equals("ADMIN"); }

    <T> Button zaznaczony(String tekst, TableView<T> tabela, Consumer<T> akcja) {
        Button b = przycisk(tekst,"",() -> { T item = tabela.getSelectionModel().getSelectedItem(); if (item != null) akcja.accept(item); });
        b.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull()); return b;
    }

    @Override public void stop() { stan.executor.shutdownNow(); }
    void pokazLogowanie() { ekranLogowania.pokazLogowanie(); }
}
