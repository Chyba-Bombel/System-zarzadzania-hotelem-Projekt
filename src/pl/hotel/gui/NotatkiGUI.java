package pl.hotel.gui;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import pl.hotel.baza.BazaDanych;
import pl.hotel.model.NotatkaRezerwacji;
import pl.hotel.model.Rezerwacja;
import static pl.hotel.gui.NarzedziaGUI.*;

final class NotatkiGUI {
    private final HotelGUI app;
    private NotatkaRezerwacji pobrana;
    NotatkiGUI(HotelGUI app) { this.app = app; }

    void pokaz(Rezerwacja r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Notatka recepcji #" + r.getId());
        dialog.setHeaderText("Notatka recepcji • rezerwacja #" + r.getId());
        dialog.initOwner(app.stan.stage); dialog.setResizable(true);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/hotel.css").toExternalForm());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button zamknij = (Button)dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        zamknij.setText("Zamknij"); zamknij.setId("notes-close");
        Label opis = etykieta("Pobieranie notatki…", "muted"); opis.setId("notes-status");
        Label info = etykieta("", "muted"); info.setId("notes-result"); info.setWrapText(true); info.setMaxWidth(590);
        Label licznik = etykieta("0 / 500", "muted"); licznik.setId("notes-count");
        TextArea aktualna = pole("notes-current",3); aktualna.setEditable(false);
        TextArea edycja = pole("notes-editor",5); edycja.setEditable(false);
        edycja.setTextFormatter(new TextFormatter<String>(zmiana -> {
            String tekst = zmiana.getControlNewText();
            return tekst.codePointCount(0,tekst.length()) <= 500 ? zmiana : null;
        }));
        edycja.textProperty().addListener((o,a,b) -> licznik.setText(b.codePointCount(0,b.length()) + " / 500"));
        SimpleBooleanProperty moznaEdytowac = new SimpleBooleanProperty(false);
        Button zapisz = przycisk("Zapisz notatkę", "primary", null); zapisz.setId("notes-save");
        Button kopiuj = przycisk("Użyj zapisanej treści", "", () -> { if (pobrana != null) edycja.setText(pobrana.tresc()); }); kopiuj.setId("notes-use-current");
        Button wyczysc = przycisk("Wyczyść pole", "", () -> edycja.clear()); wyczysc.setId("notes-clear");
        for (Button b : new Button[]{zapisz,kopiuj,wyczysc}) b.disableProperty().bind(moznaEdytowac.not());
        java.util.function.Consumer<NotatkaRezerwacji> pokazAktualna = n -> {
            pobrana = n; aktualna.setText(n.tresc()); moznaEdytowac.set(n.edytowalna()); edycja.setEditable(n.edytowalna());
            opis.setText("Rezerwacja #" + n.rezerwacjaId() + " • " + n.status() + (n.edytowalna() ? " • edycja dostępna" : " • tylko do odczytu"));
        };
        Runnable odswiez = () -> app.zadaniaGUI.uwierzytelnione(false, "Odczyt notatki", () -> BazaDanych.pobierzNotatkeRezerwacji(r.getId()), n -> {
            boolean pierwszyOdczyt = pobrana == null;
            pokazAktualna.accept(n);
            if (pierwszyOdczyt) edycja.setText(n.tresc());
            info.setText(n.edytowalna() ? "Pobrano zapisaną notatkę. Odświeżanie pozostawia Twój tekst bez zmian; porównaj obie treści przed zapisem."
                    : "Notatka zakończonej lub anulowanej rezerwacji jest tylko do odczytu. Odświeżanie nie usuwa tekstu z dolnego pola.");
        });
        Button odswiezPrzycisk = przycisk("Odśwież zapisaną", "", odswiez); odswiezPrzycisk.setId("notes-refresh");
        zapisz.setOnAction(e -> {
            if (pobrana == null || !pobrana.edytowalna()) return;
            String oczekiwana = pobrana.tresc(), nowa = edycja.getText(); info.setText("");
            app.zadaniaGUI.uwierzytelnione(false, "Zapis notatki", () -> BazaDanych.zapiszNotatkeRezerwacji(r.getId(),oczekiwana,nowa), n -> {
                pokazAktualna.accept(n); edycja.setText(n.tresc()); info.setText("Zapisano notatkę.");
                app.stan.status.setText("Zapisano notatkę rezerwacji #" + r.getId() + ".");
            });
        });
        Label zasady = etykieta("Notatka jest wewnętrzna — nie trafia do raportów ani potwierdzenia dla gościa. "
                + "Limit: 500 znaków. Pusty tekst usuwa notatkę po zapisaniu. Zamknięcie okna nie zapisuje zmian.", "muted");
        zasady.setWrapText(true); zasady.setMaxWidth(590);
        VBox box = new VBox(9,opis,zasady,new Label("Zapisana notatka"),aktualna,new Label("Twój tekst"),edycja,licznik,
                pasek(zapisz,odswiezPrzycisk),pasek(kopiuj,wyczysc),info);
        ScrollPane przewijanie = new ScrollPane(box); przewijanie.setFitToWidth(true); przewijanie.setPrefViewportHeight(540);
        dialog.getDialogPane().setContent(przewijanie); dialog.getDialogPane().setPrefWidth(640);
        dialog.getDialogPane().disableProperty().bind(app.stan.busy);
        dialog.setOnCloseRequest(e -> { if (app.stan.busy.get()) e.consume(); });
        dialog.setOnHidden(e -> { if (app.stan.aktywnyDialog == dialog) app.stan.aktywnyDialog = null; });
        app.stan.aktywnyDialog = dialog; dialog.show(); odswiez.run();
    }

    private TextArea pole(String id, int wiersze) {
        TextArea pole = new TextArea(); pole.setId(id); pole.setWrapText(true); pole.setPrefRowCount(wiersze); return pole;
    }
}
