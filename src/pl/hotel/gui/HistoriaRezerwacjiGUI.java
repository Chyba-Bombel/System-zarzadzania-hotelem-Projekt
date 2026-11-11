package pl.hotel.gui;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.format.DateTimeFormatter;
import pl.hotel.baza.BazaDanych;
import pl.hotel.model.Rezerwacja;
import pl.hotel.model.ZmianaRezerwacji;
import static pl.hotel.gui.NarzedziaGUI.*;

final class HistoriaRezerwacjiGUI {
    private final HotelGUI app;
    HistoriaRezerwacjiGUI(HotelGUI app) { this.app = app; }

    void pokaz(Rezerwacja r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Historia rezerwacji #" + r.getId());
        dialog.setHeaderText("Zmiany rezerwacji #" + r.getId() + " • " + osoba(r.getGosc()));
        dialog.initOwner(app.stan.stage); dialog.setResizable(true);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/hotel.css").toExternalForm());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        ((Button)dialog.getDialogPane().lookupButton(ButtonType.CLOSE)).setText("Zamknij");
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setId("history-close");
        TableView<ZmianaRezerwacji> tabela = new TableView<>(); tabela.setId("history-table");
        tabela.setPrefHeight(250);
        DateTimeFormatter czas = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        TableColumn<ZmianaRezerwacji,java.time.LocalDateTime> data = new TableColumn<>("Data i czas serwera");
        data.setPrefWidth(175);
        data.setCellValueFactory(v -> new javafx.beans.property.ReadOnlyObjectWrapper<>(v.getValue().utworzono()));
        tabela.getColumns().add(data);
        data.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(java.time.LocalDateTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : czas.format(value));
            }
        });
        kol(tabela, "Pracownik", 150, ZmianaRezerwacji::login);
        kol(tabela, "Zdarzenie", 205, ZmianaRezerwacji::opisAkcji);
        pusta(tabela, "Brak zapisanych zmian. Historia działa od aktualizacji programu.");
        TextArea szczegoly = new TextArea(); szczegoly.setId("history-details");
        szczegoly.setEditable(false); szczegoly.setWrapText(true); szczegoly.setPrefRowCount(7);
        tabela.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> szczegoly.setText(b == null ? "" : b.szczegoly()));
        Label opis = new Label(); opis.setId("history-page");
        SimpleBooleanProperty saStarsze = new SimpleBooleanProperty(false);
        long[] ostatnieId = {0};
        Button starsze = new Button("Starsze wpisy"); starsze.setId("history-older");
        Runnable najnowsze = () -> wczytaj(r.getId(), 0, tabela, opis, saStarsze, ostatnieId);
        Button odswiez = przycisk("Najnowsze / odśwież", "", najnowsze); odswiez.setId("history-refresh");
        starsze.setOnAction(e -> wczytaj(r.getId(), ostatnieId[0], tabela, opis, saStarsze, ostatnieId));
        starsze.disableProperty().bind(saStarsze.not());
        Label uwaga = new Label("Historia zapisuje utworzenie, termin, pokój i status. Nie odtwarza dawnych zmian ani podziału noclegów między pokoje.");
        uwaga.setWrapText(true); uwaga.setMaxWidth(570);
        VBox box = new VBox(10, uwaga, tabela, szczegoly, pasek(odswiez, starsze), opis);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        dialog.getDialogPane().setContent(box); dialog.getDialogPane().setPrefWidth(620);
        dialog.getDialogPane().disableProperty().bind(app.stan.busy);
        dialog.setOnCloseRequest(e -> { if (app.stan.busy.get()) e.consume(); });
        dialog.setOnHidden(e -> { if (app.stan.aktywnyDialog == dialog) app.stan.aktywnyDialog = null; });
        app.stan.aktywnyDialog = dialog; dialog.show(); najnowsze.run();
    }

    private void wczytaj(int id, long przedId, TableView<ZmianaRezerwacji> tabela, Label opis,
            SimpleBooleanProperty saStarsze, long[] ostatnieId) {
        app.zadaniaGUI.uwierzytelnione(false, "Odczyt historii", () -> BazaDanych.pobierzHistorieRezerwacji(id, przedId), strona -> {
            tabela.getSortOrder().clear(); tabela.getItems().setAll(strona.wpisy());
            tabela.getSelectionModel().selectFirst();
            saStarsze.set(strona.maStarsze());
            ostatnieId[0] = strona.wpisy().isEmpty() ? 0 : strona.wpisy().get(strona.wpisy().size()-1).id();
            opis.setText("Wpisów na stronie: " + strona.wpisy().size() + (strona.maStarsze() ? " • dostępne starsze wpisy" : " • koniec historii"));
        });
    }
}
