package pl.hotel.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import pl.hotel.baza.BazaDanych;
import pl.hotel.model.PotwierdzenieRezerwacji;
import pl.hotel.model.Rezerwacja;
import pl.hotel.potwierdzenia.Potwierdzenia;
import static pl.hotel.gui.NarzedziaGUI.*;

final class PotwierdzenieGUI {
    private final HotelGUI app;
    private PotwierdzenieRezerwacji wynik;
    PotwierdzenieGUI(HotelGUI app) { this.app = app; }

    void pokaz(Rezerwacja r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Potwierdzenie rezerwacji #" + r.getId());
        dialog.setHeaderText("Potwierdzenie danych rezerwacji #" + r.getId());
        dialog.initOwner(app.stan.stage); dialog.setResizable(true);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/hotel.css").toExternalForm());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button zamknij = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        zamknij.setText("Zamknij"); zamknij.setId("confirmation-close");
        Label status = etykieta("", "muted"), czas = etykieta("", "muted"), zapis = etykieta("", "muted");
        status.setId("confirmation-status"); czas.setId("confirmation-time"); zapis.setId("confirmation-output");
        for (Label l : List.of(status,czas,zapis)) { l.setWrapText(true); l.setMaxWidth(600); }
        TableView<List<String>> pobyt = tabela("confirmation-stay"), saldo = tabela("confirmation-balance");
        ListView<String> goscie = new ListView<>(); goscie.setId("confirmation-guests"); goscie.setPrefHeight(300);
        ListView<String> uslugi=new ListView<>();uslugi.setId("confirmation-services");
        TabPane tabs = new TabPane(); tabs.setId("confirmation-tabs");
        for (Tab tab : List.of(new Tab("Pobyt",pobyt),new Tab("Goście",goscie),new Tab("Rozliczenie",saldo),new Tab("Usługi",uslugi))) {
            tab.setClosable(false); tabs.getTabs().add(tab);
        }
        Button zapisz = przycisk("Zapisz HTML", "primary", null); zapisz.setId("confirmation-save"); zapisz.setDisable(true);
        Runnable odswiez = () -> {
            wynik = null; zapisz.setDisable(true); pobyt.getItems().clear(); saldo.getItems().clear(); goscie.getItems().clear();uslugi.getItems().clear();
            status.setText("Pobieranie danych…"); czas.setText(""); zapis.setText("");
            app.zadaniaGUI.uwierzytelnione(false, "Przygotowanie potwierdzenia",
                    () -> BazaDanych.pobierzPotwierdzenieRezerwacji(r.getId()), p -> {
                uslugi.getItems().setAll(p.uslugi().stream().map(u->u.opis()).toList());
                wynik = p; pobyt.getItems().setAll(Potwierdzenia.pobyt(p)); saldo.getItems().setAll(Potwierdzenia.rozliczenie(p));
                goscie.getItems().setAll(Potwierdzenia.goscie(p)); status.setText(Potwierdzenia.uwaga(p));
                status.getStyleClass().remove("error-text"); if(p.anulowana()) status.getStyleClass().add("error-text");
                czas.setText("Stan na: " + Potwierdzenia.czas(p) + " (czas serwera MySQL)"); zapisz.setDisable(false);
            });
        };
        Button odswiezPrzycisk = przycisk("Odśwież dane", "", odswiez); odswiezPrzycisk.setId("confirmation-refresh");
        zapisz.setOnAction(e -> { if (wynik != null) eksport(wynik, dialog, zapis); });
        Label uwaga = etykieta("Eksport zapisuje dane widoczne w podglądzie. Po zmianie rezerwacji lub wpłaty odśwież dane. "
                + "Plik HTML otwórz w przeglądarce; Ctrl+P umożliwia wydruk lub zapis do PDF. Dokument nie jest fakturą.", "muted");
        uwaga.setWrapText(true); uwaga.setMaxWidth(600);
        dialog.getDialogPane().setContent(new VBox(12, status, czas, tabs, pasek(odswiezPrzycisk,zapisz), uwaga, zapis));
        dialog.getDialogPane().setPrefWidth(650);
        dialog.getDialogPane().disableProperty().bind(app.stan.busy);
        dialog.setOnCloseRequest(e -> { if (app.stan.busy.get()) e.consume(); });
        dialog.setOnHidden(e -> { if (app.stan.aktywnyDialog == dialog) app.stan.aktywnyDialog = null; });
        app.stan.aktywnyDialog = dialog; dialog.show(); odswiez.run();
    }

    private TableView<List<String>> tabela(String id) {
        TableView<List<String>> t = new TableView<>(); t.setId(id); t.setPrefHeight(300);
        kol(t,"Pole",210,w -> w.get(0)); kol(t,"Wartość",380,w -> w.get(1)); return t;
    }

    private void eksport(PotwierdzenieRezerwacji p, Dialog<ButtonType> dialog, Label wynikZapisu) {
        FileChooser wybor = new FileChooser(); wybor.setTitle("Zapisz potwierdzenie rezerwacji");
        wybor.getExtensionFilters().add(new FileChooser.ExtensionFilter("Dokument HTML (*.html)","*.html"));
        wybor.setInitialFileName(Potwierdzenia.nazwaPliku(p));
        var plik = wybor.showSaveDialog(dialog.getDialogPane().getScene().getWindow());
        if (plik == null) return;
        Path cel = plik.toPath();
        if (!cel.toString().toLowerCase(Locale.ROOT).endsWith(".html")) cel = Path.of(cel + ".html");
        if (Files.exists(cel) && !app.komunikatyGUI.potwierdz("Zastąpienie pliku", "Zastąpić istniejący plik?\n" + cel)) return;
        Path docelowy = cel;
        app.zadaniaGUI.uwierzytelnione(false, "Zapis potwierdzenia", () -> {
            Potwierdzenia.zapiszHtml(p, docelowy); return docelowy;
        }, zapisany -> {
            wynikZapisu.setText("Zapisano: " + zapisany + ". Otwórz w przeglądarce i naciśnij Ctrl+P, aby wydrukować lub zapisać PDF.");
            app.stan.status.setText("Zapisano potwierdzenie rezerwacji #" + p.id() + ".");
        });
    }
}
