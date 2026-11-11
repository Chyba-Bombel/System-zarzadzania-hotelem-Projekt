package pl.hotel.gui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import java.sql.SQLException;
import static pl.hotel.gui.NarzedziaGUI.*;

final class KomunikatyGUI {
    private final HotelGUI app;
    KomunikatyGUI(HotelGUI app) { this.app = app; }

    String komunikatBledu(Throwable e) {
        return pl.hotel.bezpieczenstwo.KomunikatyBledu.dlaUzytkownika(e);
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
