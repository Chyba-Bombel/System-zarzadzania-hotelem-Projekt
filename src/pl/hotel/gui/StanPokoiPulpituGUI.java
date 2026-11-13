package pl.hotel.gui;

import pl.hotel.pulpit.PodsumowaniePulpitu;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import static pl.hotel.gui.NarzedziaGUI.*;

final class StanPokoiPulpituGUI {
    private StanPokoiPulpituGUI() {}

    static VBox utworz(HotelGUI app, PodsumowaniePulpitu p) {
        PieChart wykres = new PieChart(FXCollections.observableArrayList(
                new PieChart.Data("Zajęte",p.zajete()), new PieChart.Data("Gotowe",p.gotowe()),
                new PieChart.Data("Do sprzątania",p.sprzatanie()), new PieChart.Data("Wyłączone",p.wylaczone())));
        wykres.setLegendVisible(false); wykres.setLabelsVisible(false); wykres.setAnimated(false);
        wykres.setPrefSize(162,162); wykres.setMinSize(144,144); wykres.setMaxSize(162,162);
        wykres.setAccessibleText("Stan pokoi: zajęte " + p.zajete() + ", gotowe " + p.gotowe() + ", do sprzątania " + p.sprzatanie() + ", wyłączone " + p.wylaczone());
        Circle tlo = new Circle(64); tlo.getStyleClass().add("donut-empty");
        Circle srodek = new Circle(42); srodek.getStyleClass().add("donut-hole");
        VBox liczba = new VBox(1, etykieta(String.valueOf(p.wszystkie()),"donut-number"), etykieta("pokoi","muted")); liczba.setAlignment(Pos.CENTER);
        StackPane ring = new StackPane(tlo,wykres,srodek,liczba); ring.setPrefHeight(162);
        GridPane legenda = new GridPane(); legenda.setHgap(10); legenda.setVgap(11);
        String[] nazwy = {"Zajęte", "Gotowe", "Do sprzątania", "Wyłączone"}; int[] wartosci = {p.zajete(),p.gotowe(),p.sprzatanie(),p.wylaczone()};
        for (int i=0;i<nazwy.length;i++) {
            Label kropka = etykieta("●","legend-dot"); kropka.getStyleClass().add("legend-"+i);
            legenda.add(kropka,0,i); legenda.add(new Label(nazwy[i]),1,i); legenda.add(etykieta(String.valueOf(wartosci[i]),"strong"),2,i);
        }
        Button pokoje = przycisk(p.sprzatanie() > 0 ? p.sprzatanie() + " pokoi do przygotowania →" : "Przejdź do pokoi →", "callout", () -> {
            app.stan.stanPokoju.setValue(p.sprzatanie() > 0 ? "DO_SPRZATANIA" : "Wszystkie stany"); app.przejdz("Pokoje");
        }); pokoje.setId("dashboard-rooms"); pokoje.setMaxWidth(Double.MAX_VALUE); pokoje.setWrapText(true);
        HBox diagram = new HBox(8,ring,legenda); diagram.setAlignment(Pos.CENTER);
        VBox card = new VBox(14, etykieta("Stan pokoi","section-title"), diagram, pokoje); card.getStyleClass().add("card"); card.setMinWidth(0);
        return card;
    }
}
