package pl.hotel.gui;

import pl.hotel.model.Rezerwacja;
import pl.hotel.pulpit.PodsumowaniePulpitu;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import static pl.hotel.gui.NarzedziaGUI.*;

final class TabelaPobytowPulpituGUI {
    private TabelaPobytowPulpituGUI() {}

    static VBox utworz(HotelGUI app, PodsumowaniePulpitu p) {
        TableView<Rezerwacja> tabela = new TableView<>(); tabela.setId("dashboard-stays");
        tabela.setMinHeight(180); tabela.setPrefHeight(220); tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        kol(tabela,"Gość",210,r -> osoba(r.getGosc())); kol(tabela,"Pokój",65,r -> r.getPokoj().getNumer());
        DateTimeFormatter data = DateTimeFormatter.ofPattern("d MMM",Locale.forLanguageTag("pl-PL"));
        kol(tabela,"Pobyt",130,r -> data.format(r.getOd()) + " – " + data.format(r.getDoDnia()));
        kol(tabela,"Status",140,Rezerwacja::getStatus);
        TableColumn<Rezerwacja,Rezerwacja> akcje = new TableColumn<>("Akcja"); akcje.setPrefWidth(115); akcje.setSortable(false);
        akcje.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue()));
        akcje.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Rezerwacja r, boolean pusty) {
                super.updateItem(r,pusty); setText(null); setGraphic(null);
                if (pusty || r == null) return;
                Button b = przycisk("Otwórz", "small-button", () -> app.otworzRezerwacje(r)); setGraphic(b);
            }
        }); tabela.getColumns().add(akcje);
        Button przyjazdy = przycisk("Przyjazdy  " + p.przyjazdy().size(),"segment",null); przyjazdy.setId("dashboard-arrivals");
        Button wyjazdy = przycisk("Wyjazdy  " + p.wyjazdy().size(),"segment",null); wyjazdy.setId("dashboard-departures");
        Runnable przyloty = () -> {
            tabela.setItems(FXCollections.observableArrayList(p.przyjazdy())); przyjazdy.getStyleClass().add("selected"); wyjazdy.getStyleClass().remove("selected");
            pusta(tabela,"Nie ma potwierdzonych przyjazdów na dzisiaj.");
        };
        przyjazdy.setOnAction(e -> { przyjazdy.getStyleClass().remove("selected"); przyloty.run(); });
        wyjazdy.setOnAction(e -> {
            tabela.setItems(FXCollections.observableArrayList(p.wyjazdy())); wyjazdy.getStyleClass().remove("selected"); wyjazdy.getStyleClass().add("selected"); przyjazdy.getStyleClass().remove("selected");
            pusta(tabela,"Nie ma dzisiejszych ani zaległych wyjazdów do obsłużenia.");
        }); przyloty.run();
        HBox zakladki = new HBox(przyjazdy,wyjazdy); zakladki.getStyleClass().add("segmented-control"); zakladki.setAlignment(Pos.CENTER_LEFT);
        Label pomoc = etykieta("Wyjazdy obejmują również zaległe, nadal zameldowane pobyty.","muted"); pomoc.setWrapText(true);
        VBox card = new VBox(14, etykieta("Dzisiejsze pobyty","section-title"), zakladki, tabela, pomoc); card.setMinWidth(0); card.getStyleClass().add("card"); VBox.setVgrow(tabela,Priority.ALWAYS);
        return card;
    }
}
