package pl.hotel.gui;

import pl.hotel.kalendarz.PlanKalendarza;
import pl.hotel.kalendarz.PlanKalendarza.PoleDnia;
import pl.hotel.model.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.*;
import java.time.format.*;
import java.util.Locale;
import static pl.hotel.gui.NarzedziaGUI.*;

final class EkranKalendarza {
    private final HotelGUI app;
    private YearMonth miesiac = YearMonth.now();
    private String filtr = "";
    private static final Locale PL = Locale.forLanguageTag("pl-PL");
    EkranKalendarza(HotelGUI app) { this.app = app; }

    Node stronaKalendarza() {
        PlanKalendarza plan = new PlanKalendarza(miesiac, app.stan.rezerwacje);
        Label tytul = etykieta(miesiac.format(DateTimeFormatter.ofPattern("LLLL yyyy", PL)), "section-title");
        DatePicker wybor = data(miesiac.atDay(1)); wybor.setId("calendar-month");
        Button przejdz = przycisk("Przejdź", "", () -> {
            try { ustawMiesiac(YearMonth.from(dataZ(wybor))); }
            catch (IllegalArgumentException e) { app.komunikatyGUI.blad(e.getMessage()); }
        }); przejdz.setId("calendar-go");
        Button wstecz = przycisk("‹", "", () -> ustawMiesiac(miesiac.minusMonths(1)));
        Button dalej = przycisk("›", "", () -> ustawMiesiac(miesiac.plusMonths(1)));
        wstecz.setId("calendar-prev"); dalej.setId("calendar-next");
        Button dzis = przycisk("Dzisiaj", "", () -> ustawMiesiac(YearMonth.now())); dzis.setId("calendar-today");
        TextField szukaj = pole("Numer lub typ pokoju"); szukaj.setId("calendar-search"); szukaj.setText(filtr);
        FilteredList<Pokoj> pokoje = new FilteredList<>(app.stan.pokoje);
        Runnable filtruj = () -> pokoje.setPredicate(p -> pasuje(szukaj.getText(), p.getNumer() + " " + app.stan.typy.getOrDefault(p.getId(), "")));
        szukaj.textProperty().addListener((o,a,b) -> { filtr = b; filtruj.run(); }); filtruj.run();
        Label szczegoly = new Label("Kliknij rezerwację, aby zobaczyć szczegóły. Znak + rozpoczyna rezerwację od wybranej nocy.");
        szczegoly.setId("calendar-details"); szczegoly.setWrapText(true); szczegoly.setMinHeight(Region.USE_PREF_SIZE);
        VBox panel = new VBox(8, szczegoly); panel.getStyleClass().add("calendar-details");
        TableView<Pokoj> tabela = new TableView<>(pokoje); tabela.setId("calendar-table");
        tabela.getStyleClass().add("calendar-table"); tabela.setFixedCellSize(46); tabela.setMinHeight(130);
        TableColumn<Pokoj,String> numer = new TableColumn<>("Pokój / miejsca"); numer.setPrefWidth(150); numer.setSortable(false); numer.setReorderable(false);
        numer.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getNumer() + " • " + v.getValue().getLiczbaMiejsc() + " os."
                + (v.getValue().getStan().equals("WYLACZONY") ? " • wyłączony" : "")));
        tabela.getColumns().add(numer);
        for (int i = 1; i <= miesiac.lengthOfMonth(); i++) {
            LocalDate d = miesiac.atDay(i);
            TableColumn<Pokoj,PoleDnia> col = new TableColumn<>(i + "\n" + d.getDayOfWeek().getDisplayName(TextStyle.SHORT, PL));
            col.setPrefWidth(68); col.setMinWidth(62); col.setSortable(false); col.setReorderable(false);
            if (d.equals(LocalDate.now())) col.setStyle("-fx-background-color: #dbe9ff;");
            col.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(plan.dzien(v.getValue(), d)));
            col.setCellFactory(c -> new KomorkaKalendarza(pole -> klik(pole, panel, szczegoly)));
            tabela.getColumns().add(col);
        }
        pusta(tabela, "Brak pokoi pasujących do filtra.");
        FlowPane legenda = new FlowPane(8,8);
        for (String[] e : new String[][]{{"free","+ Wolna noc"},{"confirmed","Potwierdzona"},{"checked-in","Zameldowana"},
                {"finished","Zakończona (historia)"},{"unavailable","Niedostępne"},{"conflict","! Konflikt"}}) {
            Label l = new Label(e[1]); l.getStyleClass().addAll("calendar-legend", "calendar-" + e[0]); legenda.getChildren().add(l);
        }
        Label wskazowka = etykieta("Każda kolumna oznacza noc zaczynającą się danego dnia. Dzień wyjazdu jest wolny dla kolejnego pobytu. "
                + "Anulowane są pomijane, zakończone nie blokują nowych rezerwacji. Przewijaj tabelę poziomo, aby zobaczyć cały miesiąc. "
                + "Stan pokoju jest aktualny, bez historii wyłączeń. Odśwież dane przed planowaniem; zapis ponownie sprawdza dostępność.", "muted");
        wskazowka.setMinHeight(Region.USE_PREF_SIZE); wskazowka.setStyle("-fx-font-size: 11px;");
        VBox page = app.strona("Kalendarz rezerwacji", "Plan miesięczny • pokoje w wierszach, noce w kolumnach",
                pasek(wstecz, tytul, dalej, dzis), pasek(new Label("Miesiąc według daty:"), wybor, przejdz, szukaj),
                legenda, tabela, panel, wskazowka);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        if (miesiac.equals(YearMonth.now())) Platform.runLater(() -> tabela.scrollToColumn(tabela.getColumns().get(LocalDate.now().getDayOfMonth())));
        return page;
    }

    private void ustawMiesiac(YearMonth wybrany) {
        if (wybrany.getYear() < 1900 || wybrany.getYear() > 9998) { app.komunikatyGUI.blad("Wybierz rok od 1900 do 9998."); return; }
        miesiac = wybrany; app.pokazStrone();
    }

    private void klik(PoleDnia pole, VBox panel, Label szczegoly) {
        panel.getChildren().setAll(szczegoly);
        if (pole.rezerwacje().isEmpty()) {
            if (pole.moznaRezerwowac(LocalDate.now())) app.ekranRezerwacji.formularzRezerwacji(pole.pokoj().getId(), pole.data());
            return;
        }
        ComboBox<Rezerwacja> lista = new ComboBox<>(FXCollections.observableArrayList(pole.rezerwacje()));
        lista.setId("calendar-bookings"); lista.setConverter(konwerter(r -> "#" + r.getId() + " • " + osoba(r.getGosc()) + " • " + r.getStatus()));
        Runnable opis = () -> {
            Rezerwacja r = lista.getValue();
            szczegoly.setText("Rezerwacja #" + r.getId() + " • pokój " + pole.pokoj().getNumer() + " • " + r.getStatus()
                    + "\nGoście: " + r.getGosciePobytu() + "\nPobyt: " + r.getOd() + " → " + r.getDoDnia()
                    + " • " + r.getLiczbaOsob() + " os. • Cena pobytu: " + zl(r.obliczCene())
                    + (pole.konflikt() ? "\nUWAGA: nakładające się aktywne rezerwacje. Sprawdź dane na liście rezerwacji." : ""));
        };
        lista.valueProperty().addListener((o,a,b) -> { if (b != null) opis.run(); }); lista.setValue(pole.widoczna());
        Button otworz = przycisk("Otwórz na liście rezerwacji", "", () -> {
            Rezerwacja r = lista.getValue(); app.stan.szukajRezerwacji.clear(); app.stan.statusRezerwacji.getSelectionModel().selectFirst();
            app.stan.strona = "Rezerwacje"; app.pokazPanel(); app.stan.tabelaRezerwacji.getSelectionModel().select(r); app.stan.tabelaRezerwacji.scrollTo(r);
        }); otworz.setId("calendar-open");
        if (pole.rezerwacje().size() > 1) panel.getChildren().add(lista);
        panel.getChildren().add(otworz);
        if (pole.moznaRezerwowac(LocalDate.now())) panel.getChildren().add(przycisk("Nowa rezerwacja od " + pole.data(), "primary",
                () -> app.ekranRezerwacji.formularzRezerwacji(pole.pokoj().getId(), pole.data())));
    }
}
