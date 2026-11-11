package pl.hotel.gui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import java.sql.SQLException;
import static pl.hotel.gui.NarzedziaGUI.*;

/** Kontroler: KomunikatyGUI. */
final class KomunikatyGUI {
    private final HotelGUI app;
    KomunikatyGUI(HotelGUI app) { this.app = app; }

    String komunikatBledu(Throwable e) {
        if (e instanceof SQLException s) {
            if (s.getErrorCode() == 1062) return "Taki numer pokoju lub login już istnieje. Wpisz inną wartość.";
            if (s.getErrorCode() == 1045) return "Odmowa połączenia z bazą. Sprawdź login i hasło MySQL w db.properties.";
            if (s.getErrorCode() == 1049) return "Baza nie istnieje. Zaimportuj db/HotelManager_baza.sql jednorazowo na nowym serwerze.";
            if (s.getSQLState() != null && s.getSQLState().startsWith("08"))
                return "Utracono połączenie z MySQL. Sprawdź serwer i db.properties. Po operacji zapisu odśwież dane przed ponowieniem — zapis mógł zostać wykonany.";
            if (s.getErrorCode() == 1213 || s.getErrorCode() == 1205)
                return "Inny użytkownik zmieniał te dane. Odśwież listę i spróbuj ponownie.";
        }
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    void blad(String tekst) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle("Nie można wykonać operacji"); a.setHeaderText("Sprawdź dane i spróbuj ponownie");
        a.setContentText(tekst); a.initOwner(wlasciciel()); a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE); a.showAndWait();
    }

    boolean potwierdz(String tytul, String tekst) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION); a.setTitle(tytul); a.setHeaderText(tytul); a.setContentText(tekst);
        a.initOwner(wlasciciel()); a.getButtonTypes().setAll(ButtonType.YES,ButtonType.NO);
        ((Button) a.getDialogPane().lookupButton(ButtonType.YES)).setText("Tak");
        ((Button) a.getDialogPane().lookupButton(ButtonType.NO)).setText("Nie");
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    Window wlasciciel() { return app.stan.aktywnyDialog != null && app.stan.aktywnyDialog.isShowing() ? app.stan.aktywnyDialog.getDialogPane().getScene().getWindow() : app.stan.stage; }
}
