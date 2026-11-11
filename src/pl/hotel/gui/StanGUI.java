package pl.hotel.gui;

import pl.hotel.model.*;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static pl.hotel.gui.NarzedziaGUI.*;

/** Stan jednej instancji aplikacji. Kontrolery współdzielą go przez HotelGUI. */
final class StanGUI {
    Stage stage;
    StackPane root;
    BorderPane shell;
    final SimpleBooleanProperty busy = new SimpleBooleanProperty(false);
    final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "hotel-jdbc"); t.setDaemon(true); return t;
    });
    Pracownik sesja;
    Dialog<?> aktywnyDialog;
    int proby;
    String strona = "Pulpit";
    final Label status = new Label("Gotowy");
    final ObservableList<Gosc> goscie = FXCollections.observableArrayList();
    final ObservableList<Pokoj> pokoje = FXCollections.observableArrayList();
    final ObservableList<Rezerwacja> rezerwacje = FXCollections.observableArrayList();
    final ObservableList<KontoPracownika> konta = FXCollections.observableArrayList();
    Map<Long, String> typy = Map.of();
    final TableView<Gosc> tabelaGosci = new TableView<>();
    final TableView<Pokoj> tabelaPokoi = new TableView<>();
    final TableView<Rezerwacja> tabelaRezerwacji = new TableView<>();
    final TableView<Rezerwacja> tabelaRozliczen = new TableView<>();
    final TableView<KontoPracownika> tabelaKont = new TableView<>();
    final FilteredList<Gosc> widoczniGoscie = new FilteredList<>(goscie);
    final FilteredList<Pokoj> widocznePokoje = new FilteredList<>(pokoje);
    final FilteredList<Rezerwacja> widoczneRezerwacje = new FilteredList<>(rezerwacje);
    final FilteredList<KontoPracownika> widoczneKonta = new FilteredList<>(konta);
    final TextField szukajGoscia = pole("Szukaj nazwiska, telefonu lub e-maila");
    final TextField szukajPokoju = pole("Szukaj numeru lub typu pokoju");
    final TextField szukajRezerwacji = pole("Szukaj ID, gościa lub numeru pokoju");
    final TextField szukajKonta = pole("Szukaj nazwiska lub loginu");
    final ComboBox<String> stanPokoju = wybor("Wszystkie stany", "GOTOWY", "DO_SPRZATANIA", "WYLACZONY");
    final ComboBox<String> statusRezerwacji = wybor("Wszystkie statusy", "POTWIERDZONA", "ZAMELDOWANA", "ZAKONCZONA", "ANULOWANA");
    final DatePicker dostepnyOd = data(LocalDate.now());
    final DatePicker dostepnyDo = data(LocalDate.now().plusDays(1));
    final Spinner<Integer> miejsca = new Spinner<>(1, 10, 1);
    boolean filtrTerminu;
    LocalDate filtrOd, filtrDo;
    int filtrOsoby;
    final Label filtrInfo = etykieta("Wybierz termin, aby sprawdzić dostępność.", "muted");
    final Label szczegoly = new Label("Wybierz rezerwację z listy i kliknij „Pokaż rozliczenie”.");
    final TableView<Platnosc> tabelaPlatnosci = new TableView<>();
}
