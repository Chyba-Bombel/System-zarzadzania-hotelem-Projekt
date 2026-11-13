package pl.hotel.gui;

import pl.hotel.konfiguracja.*;
import pl.hotel.diagnostyka.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import java.nio.file.Files;

final class UstawieniaAplikacjiGUI {
    private final HotelGUI app;
    UstawieniaAplikacjiGUI(HotelGUI app) { this.app = app; }

    void pokazInformacje() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.initOwner(app.stan.stage);
        dialog.setTitle("O programie");
        dialog.setHeaderText("HotelManager " + WersjaAplikacji.NUMER);
        Label opis = new Label(InformacjeDiagnostyczne.tekst() + "\n\nKonfiguracja:\n" + SciezkiAplikacji.konfiguracja()
                + "\n\nDziennik błędów:\n" + SciezkiAplikacji.logi()
                + "\n\nAktualizacje pobierasz ręcznie z Releases projektu.");
        opis.setWrapText(true); opis.setMinHeight(Region.USE_PREF_SIZE);
        Button kopiuj = new Button("Kopiuj informacje o wersji"); kopiuj.setId("about-copy");
        Button otworz = new Button("Otwórz folder logów"); otworz.setId("about-logs");
        Label stan = new Label(); stan.setWrapText(true); stan.setMinHeight(48); stan.setId("about-result");
        kopiuj.setOnAction(e -> {
            try {
                ClipboardContent zawartosc = new ClipboardContent(); zawartosc.putString(InformacjeDiagnostyczne.tekst());
                if (Clipboard.getSystemClipboard().setContent(zawartosc)) stan.setText("Skopiowano wersję, Javę, system i architekturę. Bez ustawień połączenia.");
                else stan.setText("Schowek jest niedostępny.");
            } catch (RuntimeException ex) { DziennikBledow.zapisz(ex); stan.setText("Nie można skopiować informacji do schowka."); }
        });
        otworz.setOnAction(e -> {
            try {
                Files.createDirectories(SciezkiAplikacji.logi());
                if (!java.awt.Desktop.isDesktopSupported() || !java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN))
                    stan.setText("Otwórz ręcznie folder: " + SciezkiAplikacji.logi());
                else { java.awt.Desktop.getDesktop().open(SciezkiAplikacji.logi().toFile()); stan.setText("Przekazano folder do menedżera plików."); }
            } catch (java.io.IOException | RuntimeException ex) {
                DziennikBledow.zapisz(ex); stan.setText("Otwórz ręcznie folder: " + SciezkiAplikacji.logi());
            }
        });
        VBox tresc = new VBox(12, opis, kopiuj, otworz, stan); tresc.setPrefWidth(600);
        dialog.getDialogPane().setContent(tresc);
        dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        ((Button)dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Zamknij");
        dialog.showAndWait();
    }

    void pokazPolaczenie() { new UstawieniaPolaczeniaGUI(app).pokaz(); }
}
