package pl.hotel.gui;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import static pl.hotel.gui.NarzedziaGUI.*;

final class KafelPulpituGUI {
    private KafelPulpituGUI() {}

    static VBox utworz(String tytul, String wartosc, String opis, String ikona, Double postep) {
        HBox naglowek = new HBox(10, IkonyGUI.ikona(ikona), etykieta(tytul,"muted")); naglowek.setAlignment(Pos.CENTER_LEFT);
        Label liczba = etykieta(wartosc,"metric"); Label pomoc = etykieta(opis,"muted"); pomoc.setWrapText(true); pomoc.setMinHeight(Region.USE_PREF_SIZE);
        VBox karta = new VBox(8, naglowek, liczba, pomoc); karta.getStyleClass().addAll("card","metric-card");
        karta.setMinHeight(150); karta.setMinWidth(0); karta.setMaxWidth(Double.MAX_VALUE);
        if (postep != null) { ProgressBar bar = new ProgressBar(postep); bar.setMaxWidth(Double.MAX_VALUE); karta.getChildren().add(bar); }
        return karta;
    }
}
