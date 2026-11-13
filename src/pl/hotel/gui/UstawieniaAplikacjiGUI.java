package pl.hotel.gui;

import pl.hotel.konfiguracja.*;
import pl.hotel.diagnostyka.DziennikBledow;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.Properties;

final class UstawieniaAplikacjiGUI {
    private final HotelGUI app;
    UstawieniaAplikacjiGUI(HotelGUI app) { this.app = app; }

    void pokazInformacje() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.initOwner(app.stan.stage);
        dialog.setTitle("O programie");
        dialog.setHeaderText("HotelManager " + WersjaAplikacji.NUMER);
        dialog.setContentText("System zarządzania hotelem\n\nKonfiguracja:\n" + SciezkiAplikacji.konfiguracja()
                + "\n\nDziennik błędów:\n" + SciezkiAplikacji.logi()
                + "\n\nAktualizacje pobierasz ręcznie z Releases projektu.");
        dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        dialog.showAndWait();
    }

    void pokazPolaczenie() {
        try {
            Properties p = KonfiguracjaAplikacji.wczytajPlik();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.initOwner(app.stan.stage);
            dialog.setTitle("Ustawienia połączenia z bazą");
            TextField url = new TextField(p.getProperty("url", ""));
            TextField user = new TextField(p.getProperty("user", ""));
            PasswordField haslo = new PasswordField();
            haslo.setText(p.getProperty("password", ""));
            Label sciezka = new Label("Plik: " + SciezkiAplikacji.konfiguracja());
            sciezka.setWrapText(true);
            Label informacja = new Label("To konto serwera MySQL, a nie konto pracownika. Zmienne HOTEL_DB_* mają pierwszeństwo przed plikiem. Zapis nie sprawdza połączenia; sprawdzisz je przy logowaniu.");
            informacja.setWrapText(true);
            Label blad = new Label();
            blad.setWrapText(true);
            VBox pola = new VBox(10, sciezka, new Label("Adres JDBC"), url, new Label("Użytkownik bazy"), user, new Label("Hasło bazy"), haslo, informacja, blad);
            pola.setPrefWidth(620);
            dialog.getDialogPane().setContent(pola);
            ButtonType zapisz = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(zapisz, ButtonType.CANCEL);
            dialog.getDialogPane().lookupButton(zapisz).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                try {
                    if (!url.getText().trim().startsWith("jdbc:mysql://") || user.getText().isBlank())
                        throw new IllegalArgumentException("Podaj adres jdbc:mysql://serwer:port/baza i użytkownika bazy.");
                    p.setProperty("url", url.getText().trim());
                    p.setProperty("user", user.getText().trim());
                    p.setProperty("password", haslo.getText());
                    KonfiguracjaAplikacji.zapisz(p);
                } catch (java.io.IOException | IllegalArgumentException ex) {
                    DziennikBledow.zapisz(ex);
                    blad.setText("Nie zapisano ustawień. Sprawdź adres JDBC, użytkownika i uprawnienia do folderu konfiguracji.");
                    e.consume();
                }
            });
            dialog.showAndWait();
            haslo.clear();
        } catch (java.io.IOException | IllegalArgumentException | SecurityException e) {
            DziennikBledow.zapisz(e);
            app.komunikatyGUI.blad("Nie można otworzyć konfiguracji. Sprawdź plik: " + SciezkiAplikacji.konfiguracja());
        }
    }
}
