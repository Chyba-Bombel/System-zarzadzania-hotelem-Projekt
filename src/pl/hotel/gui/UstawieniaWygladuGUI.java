package pl.hotel.gui;

import pl.hotel.konfiguracja.PreferencjeWygladu;
import pl.hotel.konfiguracja.PreferencjeWygladu.Motyw;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

final class UstawieniaWygladuGUI {
    private final HotelGUI app;
    UstawieniaWygladuGUI(HotelGUI app) { this.app = app; }

    Button przelacznik() {
        Button b = new Button(); b.setId("theme-toggle"); b.getStyleClass().add("theme-toggle");
        b.textProperty().bind(Bindings.createStringBinding(() -> app.motywy.wartosc().motyw() == Motyw.JASNY ? "Tryb ciemny" : "Tryb jasny", app.motywy.property()));
        b.graphicProperty().bind(Bindings.createObjectBinding(() -> IkonyGUI.ikona(app.motywy.wartosc().motyw() == Motyw.JASNY ? "ksiezyc" : "slonce"), app.motywy.property()));
        b.setOnAction(e -> {
            PreferencjeWygladu p = app.motywy.wartosc();
            boolean ok = app.motywy.zmien(new PreferencjeWygladu(p.motyw() == Motyw.JASNY ? Motyw.CIEMNY : Motyw.JASNY, p.skala()));
            if (!ok) app.komunikatyGUI.blad("Motyw zmieniono na tę sesję. Nie udało się zapamiętać wyboru w folderze użytkownika.");
        });
        return b;
    }

    void pokaz() {
        Dialog<ButtonType> d = new Dialog<>(); d.initOwner(app.stan.stage); d.setTitle("Wygląd aplikacji");
        d.setHeaderText("Dopasuj HotelManager do siebie");
        ComboBox<Motyw> motyw = new ComboBox<>(FXCollections.observableArrayList(Motyw.values()));
        motyw.setId("theme-choice"); motyw.setValue(app.motywy.wartosc().motyw()); motyw.setMaxWidth(Double.MAX_VALUE);
        ComboBox<Integer> skala = new ComboBox<>(FXCollections.observableArrayList(100,110,120));
        skala.setId("font-scale"); skala.setValue(app.motywy.wartosc().skala()); skala.setMaxWidth(Double.MAX_VALUE);
        skala.setConverter(new StringConverter<>() { public String toString(Integer v) { return v == null ? "" : v + "%"; } public Integer fromString(String v) { return 100; } });
        Label stan = new Label("Zmiany są widoczne od razu i zapamiętywane na tym koncie systemowym."); stan.setWrapText(true); stan.setMinHeight(Region.USE_PREF_SIZE);
        Runnable zmien = () -> {
            if (motyw.getValue() == null || skala.getValue() == null) return;
            boolean ok = app.motywy.zmien(new PreferencjeWygladu(motyw.getValue(), skala.getValue()));
            stan.setText(ok ? "Zapisano preferencje wyglądu." : "Wygląd zmieniono na tę sesję. Sprawdź uprawnienia do folderu użytkownika.");
        };
        motyw.valueProperty().addListener((o,a,b) -> zmien.run()); skala.valueProperty().addListener((o,a,b) -> zmien.run());
        VBox tresc = new VBox(12, new Label("Motyw kolorystyczny"), motyw, new Label("Rozmiar tekstu"), skala, stan);
        tresc.setPrefWidth(420); tresc.getStyleClass().add("appearance-form");
        d.getDialogPane().setContent(tresc); d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        ((Button)d.getDialogPane().lookupButton(ButtonType.CLOSE)).setText("Gotowe"); d.showAndWait();
    }
}
