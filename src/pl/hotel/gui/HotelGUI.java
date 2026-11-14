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

public final class HotelGUI extends Application {
    final StanGUI stan = new StanGUI();
    final MotywyGUI motywy = new MotywyGUI();
    final UstawieniaWygladuGUI wyglad = new UstawieniaWygladuGUI(this);
    final NawigacjaGUI nawigacja = new NawigacjaGUI(this);
    final UstawieniaAplikacjiGUI ustawienia = new UstawieniaAplikacjiGUI(this);
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
        IkonaAplikacjiGUI.ustaw(stage);
        motywy.uruchom();
        stage.setTitle("HotelManager " + pl.hotel.konfiguracja.WersjaAplikacji.NUMER + " • System zarządzania");
        stage.setMinWidth(1040); stage.setMinHeight(680);
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
        Scene scene = new Scene(stan.root, login ? 1140 : 1440, login ? 760 : 960);
        motywy.zastosuj(scene);
        stan.stage.setScene(scene);
    }

    void pokazPanel() { nawigacja.pokazPanel(); }

    void przejdz(String strona) { stan.strona = strona; pokazStrone(); }

    void otworzRezerwacje(pl.hotel.model.Rezerwacja r) {
        stan.szukajRezerwacji.clear(); stan.statusRezerwacji.getSelectionModel().selectFirst();
        przejdz("Rezerwacje"); stan.tabelaRezerwacji.getSelectionModel().select(r); stan.tabelaRezerwacji.scrollTo(r);
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
        nawigacja.ustawTresc(content);
    }

    VBox strona(String tytul, String opis, Node... elementy) {
        Button refresh = przycisk("Odśwież dane", "", () -> zadaniaGUI.odswiez("Dane zaktualizowane.")); refresh.setId("refresh"); refresh.setGraphic(IkonyGUI.ikona("odswiez"));
        VBox tekst = new VBox(5, etykieta(tytul, "title"), etykieta(opis, "subtitle"));
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        Label breadcrumb = etykieta("Hotel  /  " + tytul, "breadcrumb");
        VBox naglowek = new VBox(8, breadcrumb, tekst); tekst.setMinWidth(0); naglowek.setMinWidth(0); HBox.setHgrow(naglowek, Priority.ALWAYS);
        HBox header = new HBox(12, naglowek, gap, refresh); header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("page-header");
        VBox page = new VBox(18, header); page.setPadding(new Insets(24)); page.setFillWidth(true); page.setMinWidth(0); page.getChildren().addAll(elementy);
        for (Node element : elementy) if (element instanceof Label l) l.setWrapText(true);
        return page;
    }

    boolean admin() { return stan.sesja != null && stan.sesja.getRola().equals("ADMIN"); }

    <T> Button zaznaczony(String tekst, TableView<T> tabela, Consumer<T> akcja) {
        Button b = przycisk(tekst,"",() -> { T item = tabela.getSelectionModel().getSelectedItem(); if (item != null) akcja.accept(item); });
        b.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull()); return b;
    }

    @Override public void stop() { motywy.zamknij(); stan.executor.shutdownNow(); }
    void pokazLogowanie() { ekranLogowania.pokazLogowanie(); }
}
