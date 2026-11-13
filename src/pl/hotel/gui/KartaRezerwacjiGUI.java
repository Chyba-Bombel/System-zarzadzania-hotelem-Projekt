package pl.hotel.gui;

import pl.hotel.model.Rezerwacja;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import static pl.hotel.gui.NarzedziaGUI.*;

final class KartaRezerwacjiGUI {
    private final VBox karta = new VBox(10);
    KartaRezerwacjiGUI(HotelGUI app) {
        karta.getStyleClass().addAll("card","reservation-summary");
        app.stan.tabelaRezerwacji.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> pokaz(b));
        pokaz(app.stan.tabelaRezerwacji.getSelectionModel().getSelectedItem());
    }

    VBox widok() { return karta; }

    private void pokaz(Rezerwacja r) {
        karta.getChildren().clear();
        if (r == null) { karta.getChildren().add(etykieta("Wybierz rezerwację, aby zobaczyć kartę pobytu i dostępne działania.","muted")); return; }
        HBox tytul = new HBox(12,etykieta("Rezerwacja #"+r.getId(),"section-title"),StatusyGUI.etykieta(r.getStatus()));
        Label goscie = etykieta(r.getGosciePobytu(),"strong"); goscie.setWrapText(true);
        FlowPane dane = new FlowPane(18,10,new Label("Pokój "+r.getPokoj().getNumer()),new Label(r.getOd()+" → "+r.getDoDnia()),new Label(r.getLiczbaOsob()+" os. • "+r.liczbaNocy()+" nocy"),
                new Label("Należność: "+zl(r.getNaleznosc())),new Label("Saldo: "+zl(r.getSaldo())));
        Label opis=etykieta("Należność obejmuje aktywne usługi. Dane według ostatniego odczytu; przed operacją możesz je odświeżyć.","muted"); opis.setWrapText(true);
        karta.getChildren().addAll(tytul,goscie,dane,opis);
    }
}
