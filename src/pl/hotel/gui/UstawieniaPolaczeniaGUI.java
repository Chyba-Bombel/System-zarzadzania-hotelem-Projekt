package pl.hotel.gui;

import pl.hotel.baza.TestPolaczenia;
import pl.hotel.bezpieczenstwo.KomunikatyBledu;
import pl.hotel.diagnostyka.DziennikBledow;
import pl.hotel.konfiguracja.*;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

final class UstawieniaPolaczeniaGUI {
    private final HotelGUI app;
    private final TextField url = new TextField();
    private final TextField user = new TextField();
    private final PasswordField haslo = new PasswordField();
    private final Label wynik = new Label();
    private final Dialog<ButtonType> dialog = new Dialog<>();
    private Task<String> test;

    UstawieniaPolaczeniaGUI(HotelGUI app) { this.app = app; }

    void pokaz() {
        try {
            try { wypelnij(KonfiguracjaAplikacji.wczytajPlik()); }
            catch (IOException | IllegalArgumentException e) {
                DziennikBledow.zapisz(e);
                wynik.setText("Nie odczytano ustawień. Możesz przywrócić kopię albo uzupełnić formularz i zapisać poprawny plik.");
            }
            dialog.initOwner(app.stan.stage);
            dialog.setTitle("Ustawienia połączenia z bazą");
            url.setId("config-url"); user.setId("config-user"); haslo.setId("config-password");
            wynik.setId("config-result"); wynik.setWrapText(true); wynik.setMinHeight(72); wynik.setPrefHeight(72); wynik.setMaxWidth(Double.MAX_VALUE);
            Label sciezka = new Label("Plik: " + SciezkiAplikacji.konfiguracja()); sciezka.setWrapText(true); sciezka.setMinHeight(Region.USE_PREF_SIZE);
            Label opis = new Label("To konto MySQL, nie konto pracownika. Test nie zapisuje ustawień. Zmienne HOTEL_DB_* mają pierwszeństwo również podczas testu. Każdy zapis zachowuje kopię poprzednich ustawień; dostępnych jest do 5 kopii.");
            opis.setWrapText(true); opis.setMinHeight(Region.USE_PREF_SIZE);
            Button sprawdz = new Button("Testuj połączenie"); sprawdz.setId("config-test");
            Button przywroc = new Button("Przywróć kopię"); przywroc.setId("config-restore");
            HBox akcje = new HBox(10, sprawdz, przywroc);
            VBox pola = new VBox(10, sciezka, new Label("Adres JDBC"), url, new Label("Użytkownik bazy"), user,
                    new Label("Hasło bazy"), haslo, opis, akcje, wynik);
            pola.setPrefWidth(620);
            dialog.getDialogPane().setContent(pola);
            ButtonType zapisz = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(zapisz, ButtonType.CANCEL);
            ((Button)dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Anuluj");
            var zapis = dialog.getDialogPane().lookupButton(zapisz); zapis.setId("config-save");
            zapis.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                try { KonfiguracjaAplikacji.zapisz(dane()); }
                catch (IOException | IllegalArgumentException | SecurityException ex) {
                    DziennikBledow.zapisz(ex);
                    wynik.setText("Nie zapisano ustawień. Sprawdź dane i uprawnienia do folderu konfiguracji oraz kopii.");
                    e.consume();
                }
            });
            przywroc.setOnAction(e -> przywroc());
            sprawdz.setOnAction(e -> {
                try {
                    Properties p = dane();
                    wynik.setText("Sprawdzanie połączenia…");
                    test = new Task<>() { @Override protected String call() throws Exception { return TestPolaczenia.sprawdz(p); } };
                    var zadanie = test;
                    javafx.scene.Node[] blokowane = {url, user, haslo, sprawdz, przywroc, zapis};
                    for (var n : blokowane) n.setDisable(true);
                    Runnable odblokuj = () -> { for (var n : blokowane) n.setDisable(false); };
                    zadanie.setOnSucceeded(ev -> { odblokuj.run(); if (dialog.isShowing()) wynik.setText(zadanie.getValue()); });
                    zadanie.setOnFailed(ev -> {
                        odblokuj.run(); DziennikBledow.zapisz(zadanie.getException());
                        if (dialog.isShowing()) wynik.setText(KomunikatyBledu.dlaUzytkownika(zadanie.getException()));
                    });
                    zadanie.setOnCancelled(ev -> odblokuj.run());
                    app.stan.executor.submit(zadanie);
                } catch (IllegalArgumentException ex) { wynik.setText(ex.getMessage()); }
            });
            dialog.setOnHidden(e -> { if (test != null) test.cancel(); haslo.clear(); });
            dialog.showAndWait();
        } catch (IllegalArgumentException | SecurityException e) {
            DziennikBledow.zapisz(e);
            app.komunikatyGUI.blad("Nie można otworzyć konfiguracji. Sprawdź plik: " + SciezkiAplikacji.konfiguracja());
        }
    }

    private Properties dane() {
        if (!url.getText().trim().startsWith("jdbc:mysql://") || user.getText().isBlank())
            throw new IllegalArgumentException("Podaj adres jdbc:mysql://serwer:port/baza i użytkownika bazy.");
        Properties p = new Properties();
        p.setProperty("url", url.getText().trim()); p.setProperty("user", user.getText().trim()); p.setProperty("password", haslo.getText());
        return p;
    }

    private void wypelnij(Properties p) {
        url.setText(p.getProperty("url", "")); user.setText(p.getProperty("user", "")); haslo.setText(p.getProperty("password", ""));
    }

    private void przywroc() {
        try {
            var kopie = KopieKonfiguracji.lista();
            if (kopie.isEmpty()) { wynik.setText("Nie ma jeszcze kopii. Pierwsza powstanie przy zapisie ustawień."); return; }
            ChoiceDialog<Path> wybor = new ChoiceDialog<>(kopie.get(0), kopie);
            wybor.initOwner(dialog.getDialogPane().getScene().getWindow());
            wybor.setTitle("Przywracanie konfiguracji");
            wybor.setHeaderText("Wybierz kopię. Aktualne ustawienia również zostaną zachowane.");
            ((Button)wybor.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Anuluj");
            var wybrana = wybor.showAndWait();
            if (wybrana.isEmpty()) return;
            KonfiguracjaAplikacji.przywroc(wybrana.get());
            wypelnij(KonfiguracjaAplikacji.wczytajPlik());
            wynik.setText("Przywrócono konfigurację z kopii. Możesz teraz sprawdzić połączenie.");
        } catch (IOException | IllegalArgumentException | SecurityException e) {
            DziennikBledow.zapisz(e); wynik.setText("Nie przywrócono konfiguracji. Sprawdź dostęp do plików kopii.");
        }
    }
}
