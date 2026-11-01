import javafx.application.Application;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.scene.image.Image;

/** Pełny interfejs okienkowy. Zapytania JDBC nigdy nie wykonują się na wątku JavaFX. */
public class HotelGUI extends Application {
    private Stage stage;
    private StackPane root;
    private BorderPane shell;
    private final SimpleBooleanProperty busy = new SimpleBooleanProperty(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "hotel-jdbc"); t.setDaemon(true); return t;
    });
    private Pracownik sesja;
    private Dialog<?> aktywnyDialog;
    private int proby;
    private String strona = "Pulpit";
    private final Label status = new Label("Gotowy");
    private final ObservableList<Gosc> goscie = FXCollections.observableArrayList();
    private final ObservableList<Pokoj> pokoje = FXCollections.observableArrayList();
    private final ObservableList<Rezerwacja> rezerwacje = FXCollections.observableArrayList();
    private final ObservableList<KontoPracownika> konta = FXCollections.observableArrayList();
    private Map<Long, String> typy = Map.of();
    private final TableView<Gosc> tabelaGosci = new TableView<>();
    private final TableView<Pokoj> tabelaPokoi = new TableView<>();
    private final TableView<Rezerwacja> tabelaRezerwacji = new TableView<>();
    private final TableView<Rezerwacja> tabelaRozliczen = new TableView<>();
    private final TableView<KontoPracownika> tabelaKont = new TableView<>();
    private final FilteredList<Gosc> widoczniGoscie = new FilteredList<>(goscie);
    private final FilteredList<Pokoj> widocznePokoje = new FilteredList<>(pokoje);
    private final FilteredList<Rezerwacja> widoczneRezerwacje = new FilteredList<>(rezerwacje);
    private final FilteredList<KontoPracownika> widoczneKonta = new FilteredList<>(konta);
    private final TextField szukajGoscia = pole("Szukaj nazwiska, telefonu lub e-maila");
    private final TextField szukajPokoju = pole("Szukaj numeru lub typu pokoju");
    private final TextField szukajRezerwacji = pole("Szukaj ID, gościa lub numeru pokoju");
    private final TextField szukajKonta = pole("Szukaj nazwiska lub loginu");
    private final ComboBox<String> stanPokoju = wybor("Wszystkie stany", "GOTOWY", "DO_SPRZATANIA", "WYLACZONY");
    private final ComboBox<String> statusRezerwacji = wybor("Wszystkie statusy", "POTWIERDZONA", "ZAMELDOWANA", "ZAKONCZONA", "ANULOWANA");
    private final DatePicker dostepnyOd = data(LocalDate.now());
    private final DatePicker dostepnyDo = data(LocalDate.now().plusDays(1));
    private final Spinner<Integer> miejsca = new Spinner<>(1, 10, 1);
    private boolean filtrTerminu;
    private LocalDate filtrOd, filtrDo;
    private int filtrOsoby;
    private final Label filtrInfo = etykieta("Wybierz termin, aby sprawdzić dostępność.", "muted");
    private final Label szczegoly = new Label("Wybierz rezerwację z listy i kliknij „Pokaż rozliczenie”.");
    private final TableView<Platnosc> tabelaPlatnosci = new TableView<>();

    @Override public void start(Stage stage) {
        this.stage = stage;
        stage.getIcons().add(
                new Image(
                        HotelGUI.class.getResourceAsStream("/ikona.png")
                )
        );
        stage.setTitle("Hotel • System zarządzania");
        stage.setMinWidth(1080); stage.setMinHeight(720);
        konfigurujTabele();
        pokazLogowanie();
        stage.show();
    }

    private void ustawScene(Node content, boolean login) {
        content.disableProperty().bind(busy);
        root = new StackPane(content);
        VBox overlay = new VBox(14, new ProgressIndicator(), etykieta("Trwa operacja…", "section-title"));
        overlay.setAlignment(Pos.CENTER); overlay.getStyleClass().add("busy-overlay");
        overlay.visibleProperty().bind(busy); overlay.managedProperty().bind(busy);
        root.getChildren().add(overlay);
        Scene scene = new Scene(root, login ? 1140 : 1320, login ? 760 : 850);
        scene.getStylesheets().add(getClass().getResource("/hotel.css").toExternalForm());
        stage.setScene(scene);
    }

    private void pokazLogowanie() {
        sesja = null;
        goscie.clear(); pokoje.clear(); rezerwacje.clear(); konta.clear(); typy = Map.of();
        tabelaPlatnosci.getItems().clear(); szczegoly.setText("Wybierz rezerwację.");
        filtrTerminu = false;
        szukajGoscia.clear(); szukajPokoju.clear(); szukajRezerwacji.clear(); szukajKonta.clear();
        stanPokoju.getSelectionModel().selectFirst(); statusRezerwacji.getSelectionModel().selectFirst();
        TextField login = pole("Login pracownika"); login.setId("login");
        PasswordField haslo = new PasswordField(); haslo.setPromptText("Hasło"); haslo.setId("password");
        Label blad = etykieta("", "error-text"); blad.setWrapText(true);
        Button wejdz = przycisk("Zaloguj się", "primary", null); wejdz.setId("login-button");
        wejdz.setMaxWidth(Double.MAX_VALUE);
        Runnable loguj = () -> {
            if (busy.get()) return;
            if (proby >= 3) { blad.setText("Wykorzystano 3 próby. Uruchom program ponownie."); return; }
            String l = login.getText().trim(), h = haslo.getText();
            if (l.isEmpty() || h.isEmpty()) { blad.setText("Wpisz login i hasło."); return; }
            blad.setText(""); haslo.clear();
            zadanie("Logowanie", () -> BazaDanych.zaloguj(l, h), p -> {
                if (p == null) {
                    proby++;
                    blad.setText("Nieprawidłowe dane lub nieaktywne konto. Pozostało prób: " + (3 - proby));
                    if (proby >= 3) { wejdz.setDisable(true); haslo.setDisable(true); }
                } else {
                    sesja = p; proby = 0; strona = "Pulpit";
                    pokazPanel(); odswiez("Zalogowano poprawnie.");
                }
            });
        };
        wejdz.setOnAction(e -> loguj.run()); haslo.setOnAction(e -> loguj.run()); login.setOnAction(e -> haslo.requestFocus());
        VBox card = new VBox(12, etykieta("HOTEL", "section-title"), etykieta("Witaj ponownie", "title"),
                etykieta("Zaloguj się na konto pracownika, aby rozpocząć pracę.", "subtitle"),
                etykieta("Login", "form-label"), login, etykieta("Hasło", "form-label"), haslo, blad, wejdz,
                etykieta("Połączenie z bazą skonfigurujesz w pliku db.properties.", "muted"));
        card.setMaxWidth(440); card.setMaxHeight(Region.USE_PREF_SIZE); card.getStyleClass().add("login-card");
        StackPane page = new StackPane(card); page.getStyleClass().add("login-page");
        ustawScene(page, true); login.requestFocus();
    }

    private void pokazPanel() {
        shell = new BorderPane();
        VBox nav = new VBox(7); nav.setPrefWidth(226); nav.getStyleClass().add("sidebar");
        nav.getChildren().addAll(etykieta("HOTEL", "brand"), etykieta("PANEL RECEPCJI", "brand-sub"), new Separator());
        for (String nazwa : new String[]{"Pulpit", "Goście", "Pokoje", "Rezerwacje", "Płatności", "Raporty", "Pracownicy", "Moje konto"}) {
            if (nazwa.equals("Pracownicy") && !admin()) continue;
            Button b = new Button(nazwa); b.setMaxWidth(Double.MAX_VALUE); b.getStyleClass().add("nav");
            b.setId("nav-" + klucz(nazwa));
            b.setOnAction(e -> {
                strona = nazwa;
                for (Node n : nav.getChildren()) if (n instanceof Button) n.getStyleClass().remove("active");
                b.getStyleClass().add("active"); pokazStrone();
            });
            if (nazwa.equals(strona)) b.getStyleClass().add("active");
            nav.getChildren().add(b);
        }
        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS); nav.getChildren().add(spacer);
        VBox osoba = new VBox(5, etykieta(sesja.getImie() + " " + sesja.getNazwisko(), "user-name"),
                etykieta(admin() ? "ADMINISTRATOR" : "RECEPCJONISTA", "user-role"));
        osoba.getStyleClass().add("user-card"); nav.getChildren().add(osoba);
        Button logout = przycisk("Wyloguj się", "", () -> {
            if (potwierdz("Wylogowanie", "Zakończyć bieżącą sesję?")) pokazLogowanie();
        }); logout.setId("logout"); logout.setMaxWidth(Double.MAX_VALUE); nav.getChildren().add(logout);
        shell.setLeft(nav);
        HBox footer = new HBox(status); footer.getStyleClass().add("statusbar"); shell.setBottom(footer);
        pokazStrone(); ustawScene(shell, false);
    }

    private void pokazStrone() {
        Node content = switch (strona) {
            case "Goście" -> stronaGosci();
            case "Pokoje" -> stronaPokoi();
            case "Rezerwacje" -> stronaRezerwacji();
            case "Płatności" -> stronaPlatnosci();
            case "Raporty" -> stronaRaportow();
            case "Pracownicy" -> admin() ? stronaPracownikow() : pulpit();
            case "Moje konto" -> stronaKonta();
            default -> pulpit();
        };
        shell.setCenter(content);
    }

    private VBox strona(String tytul, String opis, Node... elementy) {
        Button refresh = przycisk("Odśwież dane", "", () -> odswiez("Dane zaktualizowane.")); refresh.setId("refresh");
        VBox tekst = new VBox(5, etykieta(tytul, "title"), etykieta(opis, "subtitle"));
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox header = new HBox(12, tekst, gap, refresh); header.setAlignment(Pos.CENTER_LEFT);
        VBox page = new VBox(18, header); page.setPadding(new Insets(28)); page.getChildren().addAll(elementy);
        for (Node element : elementy) if (element instanceof Label l) l.setWrapText(true);
        return page;
    }

    private Node stronaRaportow() {
        DatePicker od = data(LocalDate.now().withDayOfMonth(1)), koniec = data(LocalDate.now());
        od.setId("report-from"); koniec.setId("report-to");
        TabPane zakladki = new TabPane(); zakladki.setId("report-tabs");
        Label opis = etykieta("Wybierz okres i kliknij „Generuj raport”. Daty obejmują oba wybrane dni.", "muted");
        opis.setWrapText(true);
        Raporty.Raport[] wynik = {null};
        Button csv = przycisk("Zapisz tabelę CSV", "", null), html = przycisk("Zapisz cały raport HTML / PDF", "", null);
        csv.setId("report-csv"); html.setId("report-html"); csv.setDisable(true); html.setDisable(true);
        Runnable uniewaznij = () -> {
            if (wynik[0] == null) return;
            wynik[0] = null; zakladki.getTabs().clear(); csv.setDisable(true); html.setDisable(true);
            opis.setText("Zmieniono okres. Wygeneruj raport dla nowych dat.");
        };
        od.getEditor().textProperty().addListener((o,a,b) -> uniewaznij.run());
        koniec.getEditor().textProperty().addListener((o,a,b) -> uniewaznij.run());
        Button generuj = przycisk("Generuj raport", "primary", () -> {
            try {
                LocalDate a=dataZ(od), b=dataZ(koniec);
                uwierzytelnione(false,"Generowanie raportu",() -> Raporty.generuj(a,b), r -> {
                    wynik[0]=r; zakladki.getTabs().clear();
                    for (Raporty.Tabela dane:r.tabele()) {
                        TableView<java.util.List<String>> t = new TableView<>(FXCollections.observableArrayList(dane.wiersze()));
                        for (int i=0;i<dane.kolumny().size();i++) {
                            final int k=i; double szerokosc=dane.kolumny().size()==2 ? (i==0?470:420) : (i==1||i==2?220:150);
                            kol(t,dane.kolumny().get(i),szerokosc,w -> w.get(k));
                        }
                        pusta(t,"Brak danych w wybranym okresie.");
                        Tab tab=new Tab(dane.nazwa(),t); tab.setClosable(false); zakladki.getTabs().add(tab);
                    }
                    csv.setDisable(false); html.setDisable(false);
                    opis.setText("Raport: " + r.od() + " — " + r.doDnia() + " • Wygenerowano: "
                            + r.utworzono().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            + ". CSV eksportuje wybraną zakładkę, HTML zawiera cały raport.");
                    status.setText("Raport gotowy • " + godzina());
                });
            } catch (IllegalArgumentException ex) { blad(ex.getMessage()); }
        }); generuj.setId("report-generate");
        csv.setOnAction(e -> {
            var r=wynik[0]; int i=zakladki.getSelectionModel().getSelectedIndex();
            if(r!=null && i>=0) eksportRaportu(r,i,false);
        });
        html.setOnAction(e -> { if(wynik[0]!=null) eksportRaportu(wynik[0],0,true); });
        Label zasady=etykieta(Raporty.ZASADY,"muted"); zasady.setWrapText(true);
        VBox page = strona("Raporty", "Zestawienia pobytów i transakcji dla wybranego okresu.",
                pasek(new Label("Od"),od,new Label("Do (włącznie)"),koniec,generuj), opis,
                zakladki,pasek(csv,html),zasady);
        VBox.setVgrow(zakladki,Priority.ALWAYS); return page;
    }

    private void eksportRaportu(Raporty.Raport r,int tabela,boolean html) {
        javafx.stage.FileChooser wybor = new javafx.stage.FileChooser();
        wybor.setTitle(html?"Zapisz cały raport":"Zapisz tabelę CSV");
        String rozszerzenie=html?"html":"csv";
        wybor.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(
                html?"Raport HTML (*.html)":"Tabela CSV (*.csv)","*."+rozszerzenie));
        wybor.setInitialFileName("raport_"+r.od()+"_"+r.doDnia()+(html?"":"_"+tabela)+"."+rozszerzenie);
        java.io.File plik=wybor.showSaveDialog(stage); if(plik==null)return;
        java.nio.file.Path cel=plik.toPath();
        if(!cel.toString().toLowerCase(Locale.ROOT).endsWith("."+rozszerzenie))
            cel=java.nio.file.Path.of(cel+"."+rozszerzenie);
        if(java.nio.file.Files.exists(cel) && !potwierdz("Zastąpienie pliku","Zastąpić istniejący plik?\n"+cel)) return;
        final java.nio.file.Path docelowy=cel;
        uwierzytelnione(false,"Zapisywanie raportu",() -> {
            if(html) Raporty.zapiszHtml(r,docelowy); else Raporty.zapiszCsv(r.tabele().get(tabela),docelowy);
            return docelowy;
        }, zapis -> {
            status.setText("Zapisano raport: "+zapis);
            Alert a=new Alert(Alert.AlertType.INFORMATION); a.initOwner(stage); a.setTitle("Raport zapisany");
            a.setHeaderText("Plik jest gotowy");
            a.setContentText(zapis+(html?"\n\nOtwórz plik w przeglądarce. Przycisk „Drukuj / zapisz jako PDF” otworzy drukowanie — wybierz zapis do PDF.":"\n\nOtwórz CSV w Excelu lub LibreOffice. Separator kolumn: średnik; kodowanie: UTF-8."));
            a.showAndWait();
        });
    }

    private Node pulpit() {
        long aktywne = rezerwacje.stream().filter(r -> r.getStatus().equals("POTWIERDZONA") || r.getStatus().equals("ZAMELDOWANA")).count();
        HBox liczby = new HBox(15,
                kafel("Goście", goscie.size(), "Osoby zapisane w bazie"),
                kafel("Pokoje", pokoje.size(), "Wszystkie pokoje hotelu"),
                kafel("Aktywne rezerwacje", aktywne, "Potwierdzone i zameldowane"));
        LocalDate dzis = LocalDate.now();
        TableView<Rezerwacja> tabela = new TableView<>();
        kol(tabela, "ID", 65, Rezerwacja::getId);
        kol(tabela, "Gość", 200, r -> osoba(r.getGosc()));
        kol(tabela, "Pokój", 90, r -> r.getPokoj().getNumer());
        kol(tabela, "Przyjazd", 130, Rezerwacja::getOd);
        kol(tabela, "Wyjazd", 130, Rezerwacja::getDoDnia);
        kol(tabela, "Status", 155, Rezerwacja::getStatus);
        tabela.setItems(FXCollections.observableArrayList(rezerwacje.stream().filter(r ->
                (r.getOd().equals(dzis) && r.getStatus().equals("POTWIERDZONA"))
                || (!r.getDoDnia().isAfter(dzis) && r.getStatus().equals("ZAMELDOWANA"))).toList()));
        pusta(tabela, "Brak przyjazdów i wyjazdów wymagających dziś uwagi."); tabela.setPrefHeight(340);
        Label opis = etykieta("Przyjazdy na dziś oraz zameldowane pobyty z dzisiejszym lub zaległym wyjazdem.", "muted"); opis.setWrapText(true);
        VBox page = strona("Pulpit", "Przegląd pracy hotelu • " + dzis, liczby,
                etykieta("Dzisiaj w hotelu", "section-title"), opis, tabela,
                etykieta("Aby wykonać operację, przejdź do odpowiedniej zakładki. Dane odświeżaj przyciskiem w prawym górnym rogu.", "muted"));
        VBox.setVgrow(tabela, Priority.ALWAYS); return page;
    }
    private VBox kafel(String opis, long wartosc, String podtekst) {
        VBox v = new VBox(7, etykieta(opis, "muted"), etykieta(String.valueOf(wartosc), "metric"), etykieta(podtekst, "muted"));
        v.getStyleClass().add("card"); HBox.setHgrow(v, Priority.ALWAYS); v.setMaxWidth(Double.MAX_VALUE); return v;
    }

    private Node stronaGosci() {
        Button dodaj = przycisk("+ Dodaj gościa", "primary", () -> formularzGoscia(null)); dodaj.setId("add-guest");
        Button edytuj = przycisk("Edytuj zaznaczonego", "", () -> formularzGoscia(tabelaGosci.getSelectionModel().getSelectedItem()));
        edytuj.disableProperty().bind(tabelaGosci.getSelectionModel().selectedItemProperty().isNull());
        HBox bar = pasek(szukajGoscia, dodaj, edytuj);
        VBox page = strona("Goście", "Dane kontaktowe gości hotelowych.", bar, tabelaGosci);
        VBox.setVgrow(tabelaGosci, Priority.ALWAYS); return page;
    }

    private Node stronaPokoi() {
        HBox bar = pasek(szukajPokoju, stanPokoju);
        if (admin()) bar.getChildren().addAll(przycisk("+ Dodaj pokój", "primary", () -> formularzPokoju(null)),
                zaznaczony("Edytuj pokój", tabelaPokoi, this::formularzPokoju));
        Button sprzataj = zaznaczony("Oznacz jako posprzątany", tabelaPokoi, p -> {
            if (!p.getStan().equals("DO_SPRZATANIA")) { blad("Wybierz pokój o stanie DO_SPRZATANIA."); return; }
            if (!potwierdz("Sprzątanie", "Pokój " + p.getNumer() + " został posprzątany?")) return;
            uwierzytelnione(false, "Zmiana stanu pokoju", () -> {
                if (!BazaDanych.oznaczPokojJakoPosprzatany(p.getId())) throw new SQLException("Nie zmieniono stanu. Odśwież dane pokoju.");
                return true;
            }, v -> odswiez("Pokój jest gotowy."));
        });
        HBox dostepnosc = pasek(etykieta("Przyjazd", "form-label"), dostepnyOd,
                etykieta("Wyjazd", "form-label"), dostepnyDo,
                etykieta("Osoby", "form-label"), miejsca,
                przycisk("Sprawdź termin", "", () -> {
                    try {
                        LocalDate a = dataZ(dostepnyOd), b = dataZ(dostepnyDo);
                        termin(a,b); filtrOd = a; filtrDo = b; filtrOsoby = miejsca.getValue();
                    }
                    catch (IllegalArgumentException e) { blad(e.getMessage()); return; }
                    filtrTerminu = true;
                    odswiez("Sprawdzono dostępność według aktualnych rezerwacji.");
                }));
        miejsca.setPrefWidth(72);
        Button reset = przycisk("Wszystkie pokoje", "", () -> { filtrTerminu = false; filtruj(); });
        VBox page = strona("Pokoje", "Cennik, stan przygotowania i wyszukiwanie wolnych terminów.", bar,
                dostepnosc, pasek(filtrInfo, reset), tabelaPokoi, pasek(sprzataj));
        VBox.setVgrow(tabelaPokoi, Priority.ALWAYS); return page;
    }

    private Node stronaRezerwacji() {
        Button dodaj = przycisk("+ Nowa rezerwacja", "primary", this::formularzRezerwacji); dodaj.setId("add-booking");
        HBox bar = pasek(szukajRezerwacji, statusRezerwacji, dodaj);
        Button uczestnicy = zaznaczony("Goście pobytu", tabelaRezerwacji, this::formularzUczestnikow); uczestnicy.setId("booking-participants");
        Button daty = zaznaczony("Zmień termin", tabelaRezerwacji, this::formularzTerminu);
        Button zamelduj = zaznaczony("Zamelduj", tabelaRezerwacji, r -> operacjaRezerwacji(r, "Zameldowanie", () -> BazaDanych.zameldujGoscia(r.getId())));
        Button wymelduj = zaznaczony("Wymelduj", tabelaRezerwacji, r -> operacjaRezerwacji(r, "Wymeldowanie", () -> BazaDanych.wymeldujGoscia(r.getId())));
        Button anuluj = zaznaczony("Anuluj rezerwację", tabelaRezerwacji, r -> operacjaRezerwacji(r, "Anulowanie rezerwacji", () -> BazaDanych.anulujRezerwacje(r.getId())));
        anuluj.getStyleClass().add("danger");
        VBox page = strona("Rezerwacje", "Wybierz pobyt z tabeli, aby zmienić termin lub obsłużyć gościa.", bar,
                tabelaRezerwacji, pasek(uczestnicy, daty, zamelduj, wymelduj, anuluj),
                etykieta("Wymeldowanie wymaga pełnego rozliczenia. Przed anulowaniem zwróć wpłacone środki.", "muted"));
        VBox.setVgrow(tabelaRezerwacji, Priority.ALWAYS); return page;
    }

    private Node stronaPlatnosci() {
        Button pokaz = zaznaczony("Pokaż rozliczenie", tabelaRozliczen, this::pobierzRozliczenie); pokaz.setId("show-balance");
        Button wplata = zaznaczony("+ Wpłata / zaliczka", tabelaRozliczen, r -> formularzPlatnosci(r, false));
        wplata.getStyleClass().add("primary");
        HBox bar = pasek(pokaz, wplata);
        if (admin()) bar.getChildren().add(zaznaczony("Zarejestruj zwrot", tabelaRozliczen, r -> formularzPlatnosci(r, true)));
        tabelaRozliczen.setPrefHeight(240); tabelaPlatnosci.setPrefHeight(210);
        szczegoly.setWrapText(true); szczegoly.getStyleClass().add("card"); szczegoly.setMaxWidth(Double.MAX_VALUE);
        VBox page = strona("Płatności", "Rozliczenia oraz historia wpłat i zwrotów. Operacje rejestrują płatności w bazie.",
                tabelaRozliczen, bar, szczegoly, tabelaPlatnosci);
        VBox.setVgrow(tabelaRozliczen, Priority.ALWAYS); VBox.setVgrow(tabelaPlatnosci, Priority.ALWAYS); return page;
    }

    private Node stronaPracownikow() {
        Button dodaj = przycisk("+ Nowe konto", "primary", this::formularzPracownika); dodaj.setId("add-account");
        Button zablokuj = zaznaczony("Dezaktywuj konto", tabelaKont, k -> {
            if (!k.aktywny()) { blad("To konto jest już nieaktywne."); return; }
            if (k.id() == sesja.getId()) { blad("Nie możesz dezaktywować własnego konta."); return; }
            if (!potwierdz("Dezaktywacja konta", "Zablokować kolejne logowanie konta „" + k.login() + "”?")) return;
            long adminId = sesja.getId();
            uwierzytelnione(true, "Dezaktywacja konta", () -> { BazaDanych.dezaktywujPracownika(k.id(), adminId); return true; },
                    v -> odswiez("Konto zostało dezaktywowane."));
        }); zablokuj.getStyleClass().add("danger");
        VBox page = strona("Pracownicy", "Konta pracowników, role i aktywność. Dane historyczne nie są usuwane.",
                pasek(szukajKonta, dodaj, zablokuj), tabelaKont);
        VBox.setVgrow(tabelaKont, Priority.ALWAYS); return page;
    }

    private Node stronaKonta() {
        Label opis = new Label("Hasło musi mieć 12–128 znaków. Zmiana wymaga podania obecnego hasła."); opis.setWrapText(true);
        VBox karta = new VBox(15, etykieta(osoba(sesja), "section-title"),
                new Label("Login: " + sesja.getLogin()), new Label("Rola: " + sesja.getRola()), opis,
                przycisk("Zmień własne hasło", "primary", this::formularzHasla));
        karta.getStyleClass().add("card"); karta.setMaxWidth(650);
        return strona("Moje konto", "Dane aktualnie zalogowanej osoby.", karta);
    }

    private void formularzGoscia(Gosc g) {
        Formularz f = new Formularz(g == null ? "Nowy gość" : "Edycja gościa", "Dane kontaktowe", "Zapisz gościa");
        TextField imie = f.tekst("Imię", g == null ? "" : g.getImie()); imie.setId("guest-first-name");
        TextField nazwisko = f.tekst("Nazwisko", g == null ? "" : g.getNazwisko()); nazwisko.setId("guest-last-name");
        TextField telefon = f.tekst("Telefon", g == null ? "" : g.getTelefon()); telefon.setId("guest-phone");
        TextField email = f.tekst("E-mail", g == null ? "" : g.getEmail()); email.setId("guest-email");
        f.pokaz(() -> {
            String i = imie.getText().trim(), n = nazwisko.getText().trim(), t = telefon.getText().trim(), e = email.getText().trim();
            GuiRepository.sprawdzGoscia(i, n, t, e);
            uwierzytelnione(false, "Zapis gościa", () -> {
                if (g == null) BazaDanych.zapiszGoscia(i, n, t, e);
                else if (!BazaDanych.edytujGoscia(g.getId(), i, n, t, e)) throw new SQLException("Nie potwierdzono aktualizacji gościa.");
                return true;
            }, v -> { f.zamknij(); odswiez("Zapisano dane gościa."); });
        });
    }

    private void formularzPokoju(Pokoj p) {
        Formularz f = new Formularz(p == null ? "Nowy pokój" : "Edycja pokoju", "Cena dotyczy całego pokoju za jedną noc.", "Zapisz pokój");
        TextField numer = f.tekst("Numer", p == null ? "" : p.getNumer());
        TextField typ = f.tekst("Typ", p == null ? "Standard" : typy.getOrDefault(p.getId(), "Standard"));
        Spinner<Integer> ile = new Spinner<>(1, 10, p == null ? 2 : p.getLiczbaMiejsc());
        ile.setDisable(p != null); f.dodaj("Liczba miejsc", ile);
        TextField cena = f.tekst("Cena za noc (zł)", p == null ? "" : p.getCenaZaNoc().toPlainString());
        if (p != null) f.notatka("Liczba miejsc pozostaje bez zmian, aby zachować zgodność istniejących rezerwacji. Wcześniejsze pobyty zachowają zapisaną stawkę.");
        f.pokaz(() -> {
            String n = numer.getText().trim(), t = typ.getText().trim();
            GuiRepository.tekst(n, 10, "Numer"); GuiRepository.tekst(t, 40, "Typ");
            BigDecimal kwota = GuiRepository.kwota(cena.getText()); int l = ile.getValue();
            uwierzytelnione(true, "Zapis pokoju", () -> {
                if (p == null) BazaDanych.dodajPokoj(n, t, l, kwota);
                else if (!BazaDanych.edytujPokoj(p.getId(), n, t, kwota)) throw new SQLException("Nie potwierdzono aktualizacji pokoju.");
                return true;
            }, v -> { f.zamknij(); odswiez("Zapisano dane pokoju."); });
        });
    }

    private void formularzRezerwacji() {
        // Przed otwarciem formularza pobieramy najnowsze dane.
        odswiezPo("Przygotowanie rezerwacji", () -> {
            if (goscie.isEmpty() || pokoje.isEmpty()) { blad("Najpierw dodaj gościa i pokój."); return; }
            Formularz f = new Formularz("Nowa rezerwacja", "Wybierz gościa, pokój i termin pobytu.", "Utwórz rezerwację");
            ComboBox<Gosc> gosc = new ComboBox<>(goscie); gosc.setId("booking-guest");
            gosc.setConverter(konwerter(g -> "#" + g.getId() + " • " + osoba(g)));
            gosc.getSelectionModel().selectFirst(); f.dodaj("Główny gość", gosc);
            ComboBox<Pokoj> pokoj = new ComboBox<>(FXCollections.observableArrayList(pokoje.stream()
                    .filter(p -> !p.getStan().equals("WYLACZONY")).toList())); pokoj.setId("booking-room");
            pokoj.setConverter(konwerter(p -> p.getNumer() + " • " + p.getLiczbaMiejsc() + " os. • " + zl(p.getCenaZaNoc()) + "/noc"));
            pokoj.getSelectionModel().selectFirst(); f.dodaj("Pokój", pokoj);
            Spinner<Integer> osoby = new Spinner<>(1, 10, 1); osoby.setId("booking-people"); f.dodaj("Liczba osób", osoby);
            PanelUczestnikow panel = new PanelUczestnikow(f);
            osoby.valueProperty().addListener((o,a,b) -> panel.liczba(b - 1));
            panel.liczba(0);
            f.notatka("Liczba osób obejmuje głównego gościa. Dla każdej kolejnej osoby wpisz imię i nazwisko; telefon i e-mail nie są wymagane.");
            DatePicker od = data(LocalDate.now()), koniec = data(LocalDate.now().plusDays(1));
            od.setId("booking-from"); koniec.setId("booking-to"); f.dodaj("Przyjazd", od); f.dodaj("Wyjazd", koniec);
            Label cena = new Label(); cena.setWrapText(true); f.dodaj("Szacowana cena", cena);
            Runnable wycena = () -> {
                Pokoj p = pokoj.getValue();
                if (p == null || od.getValue() == null || koniec.getValue() == null) { cena.setText("Wybierz pokój i daty."); return; }
                long noce = java.time.temporal.ChronoUnit.DAYS.between(od.getValue(), koniec.getValue());
                cena.setText(noce > 0 ? noce + " nocy • " + zl(p.getCenaZaNoc().multiply(BigDecimal.valueOf(noce))) : "Wyjazd musi być po przyjeździe.");
            };
            pokoj.valueProperty().addListener((o,a,b) -> wycena.run()); od.valueProperty().addListener((o,a,b) -> wycena.run());
            koniec.valueProperty().addListener((o,a,b) -> wycena.run()); wycena.run();
            f.notatka("Pobyt: 1–30 nocy. Dostępność i aktualna cena są ponownie sprawdzane w bazie podczas zapisu. Pokój do sprzątania można zarezerwować, ale musi być gotowy przed zameldowaniem.");
            f.pokaz(() -> {
                Gosc g = gosc.getValue(); Pokoj p = pokoj.getValue();
                if (g == null || p == null) throw new IllegalArgumentException("Wybierz gościa i dostępny pokój.");
                LocalDate a = dataZ(od), b = dataZ(koniec); termin(a,b); int liczba = osoby.getValue();
                if (liczba > p.getLiczbaMiejsc()) throw new IllegalArgumentException("Za dużo osób dla wybranego pokoju.");
                var listaOsob = panel.wczytaj();
                long pracownik = sesja.getId();
                uwierzytelnione(false, "Tworzenie rezerwacji", () -> BazaDanych.zapiszRezerwacje(g,p,a,b,liczba,pracownik,listaOsob),
                        r -> { f.zamknij(); odswiez("Utworzono rezerwację #" + r.getId() + "."); });
            });
        });
    }

    private void formularzUczestnikow(Rezerwacja r) {
        boolean edycja = r.getStatus().equals("POTWIERDZONA") || r.getStatus().equals("ZAMELDOWANA");
        if (!edycja) {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.initOwner(stage); a.setTitle("Goście pobytu #" + r.getId());
            a.setHeaderText("Pokój " + r.getPokoj().getNumer()); a.setContentText(r.getGosciePobytu()); a.showAndWait(); return;
        }
        Formularz f = new Formularz("Goście pobytu #" + r.getId(),
                "Główny gość: " + osoba(r.getGosc()) + " • pokój " + r.getPokoj().getNumer(), "Zapisz gości");
        PanelUczestnikow panel = new PanelUczestnikow(f);
        panel.liczba(r.getLiczbaOsob() - 1);
        for (int i = 0; i < Math.min(r.getUczestnicy().size(), panel.imiona.size()); i++) {
            panel.imiona.get(i).setText(r.getUczestnicy().get(i).imie());
            panel.nazwiska.get(i).setText(r.getUczestnicy().get(i).nazwisko());
        }
        f.notatka("Ten formularz uzupełnia dane osób. Nie zmienia liczby osób, terminu ani ceny rezerwacji.");
        f.pokaz(() -> {
            var osoby = panel.wczytaj();
            uwierzytelnione(false, "Zapis gości pobytu", () -> { BazaDanych.zmienUczestnikow(r.getId(), osoby); return true; },
                    v -> { f.zamknij(); odswiez("Zapisano gości pobytu #" + r.getId() + "."); });
        });
    }

    /** Pola pojawiają się po zwiększeniu liczby osób; wpisy zachowujemy przy jej zmianie. */
    private final class PanelUczestnikow {
        final VBox box = new VBox(8);
        final java.util.List<TextField> imiona = new java.util.ArrayList<>();
        final java.util.List<TextField> nazwiska = new java.util.ArrayList<>();
        int aktywne;
        PanelUczestnikow(Formularz f) { f.grid.add(box, 0, f.row++, 2, 1); }
        void liczba(int ile) {
            aktywne = ile; box.getChildren().clear();
            if (ile == 0) box.getChildren().add(new Label("Pobyt jednoosobowy — bez dodatkowych gości."));
            for (int i = 0; i < ile; i++) {
                if (i >= imiona.size()) {
                    TextField a = pole("Imię"), b = pole("Nazwisko");
                    a.setId("participant-first-" + (i+1)); b.setId("participant-last-" + (i+1));
                    imiona.add(a); nazwiska.add(b);
                }
                TextField imie = imiona.get(i), nazwisko = nazwiska.get(i);
                imie.setMinWidth(0); nazwisko.setMinWidth(0);
                imie.setPrefWidth(180); nazwisko.setPrefWidth(180);
                HBox.setHgrow(imie, Priority.ALWAYS); HBox.setHgrow(nazwisko, Priority.ALWAYS);
                HBox row = new HBox(8, imie, nazwisko);
                row.setAlignment(Pos.CENTER_LEFT);
                box.getChildren().add(new VBox(4, new Label("Osoba " + (i+2) + " — imię i nazwisko"), row));
            }
        }
        java.util.List<Uczestnik> wczytaj() {
            java.util.List<Uczestnik> wynik = new java.util.ArrayList<>();
            for (int i = 0; i < aktywne; i++) wynik.add(new Uczestnik(imiona.get(i).getText(), nazwiska.get(i).getText()));
            return java.util.List.copyOf(wynik);
        }
    }

    private void formularzTerminu(Rezerwacja r) {
        if (!r.getStatus().equals("POTWIERDZONA")) { blad("Zmień termin potwierdzonej rezerwacji."); return; }
        Formularz f = new Formularz("Zmiana terminu #" + r.getId(), osoba(r.getGosc()) + " • pokój " + r.getPokoj().getNumer(), "Zapisz termin");
        DatePicker od = data(r.getOd()), koniec = data(r.getDoDnia()); f.dodaj("Nowy przyjazd", od); f.dodaj("Nowy wyjazd", koniec);
        f.notatka("Cena za noc pozostanie bez zmian: " + zl(r.getCenaZaNoc()) + ". Jeśli zaliczka przekroczy nową cenę pobytu, najpierw zarejestruj zwrot nadpłaty.");
        f.pokaz(() -> {
            LocalDate a = dataZ(od), b = dataZ(koniec); termin(a,b);
            uwierzytelnione(false, "Zmiana terminu", () -> { BazaDanych.zmienTerminRezerwacji(r.getId(),a,b); return true; },
                    v -> { f.zamknij(); odswiez("Zmieniono termin rezerwacji #" + r.getId() + "."); });
        });
    }

    private void operacjaRezerwacji(Rezerwacja r, String nazwa, SqlAkcja akcja) {
        String opis = "Rezerwacja #" + r.getId() + " • " + osoba(r.getGosc()) + " • pokój " + r.getPokoj().getNumer();
        if (!potwierdz(nazwa, opis + "\n\nPotwierdzasz operację?")) return;
        uwierzytelnione(false, nazwa, () -> { akcja.wykonaj(); return true; }, v -> odswiez(nazwa + " — zapisano zmianę."));
    }

    private void pobierzRozliczenie(Rezerwacja r) {
        uwierzytelnione(false, "Pobieranie rozliczenia", () -> BazaDanych.pobierzRozliczenie(r.getId()), this::wyswietlRozliczenie);
    }
    private void wyswietlRozliczenie(Rozliczenie r) {
        String saldo = r.saldo().signum() > 0 ? "Do dopłaty: " + zl(r.saldo())
                : r.saldo().signum() < 0 ? "Do zwrotu: " + zl(r.saldo().abs()) : "Rezerwacja rozliczona";
        szczegoly.setText("Rezerwacja #" + r.rezerwacjaId() + " • " + r.status()
                + "\nCena pobytu: " + zl(r.cenaPobytu()) + "    Należność: " + zl(r.naleznosc())
                + "\nWpłaty: " + zl(r.wplaty()) + "    Zwroty: " + zl(r.zwroty())
                + "    Zapłacono netto: " + zl(r.zaplaconoNetto()) + "\n" + saldo);
        tabelaPlatnosci.setItems(FXCollections.observableArrayList(r.platnosci()));
        status.setText("Rozliczenie #" + r.rezerwacjaId() + " • " + godzina());
    }

    private void formularzPlatnosci(Rezerwacja r, boolean zwrot) {
        uwierzytelnione(zwrot, "Sprawdzanie salda", () -> BazaDanych.pobierzRozliczenie(r.getId()), roz -> {
            BigDecimal limit = zwrot ? roz.zaplaconoNetto() : roz.saldo();
            if (!zwrot && roz.status().equals("ANULOWANA")) { blad("Nie można wpłacać na anulowaną rezerwację."); return; }
            if (limit.signum() <= 0) { blad(zwrot ? "Brak środków do zwrotu." : "Brak kwoty do dopłaty."); return; }
            Formularz f = new Formularz(zwrot ? "Rejestracja zwrotu" : "Wpłata lub zaliczka",
                    "Rezerwacja #" + r.getId() + " • " + osoba(r.getGosc()), zwrot ? "Zapisz zwrot" : "Zapisz wpłatę");
            f.notatka((zwrot ? "Maksymalnie do zwrotu: " : "Pozostało do zapłaty: ") + zl(limit));
            TextField kwota = f.tekst("Kwota (zł)", limit.toPlainString()); kwota.setId("payment-amount");
            ComboBox<String> metoda = wybor("GOTOWKA", "KARTA", "PRZELEW"); f.dodaj("Metoda", metoda);
            f.notatka(zwrot ? "Zwrot nie zmniejsza ceny aktywnego pobytu — zwiększa kwotę do dopłaty. Zapis w programie nie wykonuje przelewu bankowego."
                    : "Zapis rejestruje otrzymaną płatność. Nie łączy się z bankiem ani terminalem.");
            f.pokaz(() -> {
                BigDecimal k = GuiRepository.kwota(kwota.getText());
                if (k.compareTo(limit) > 0) throw new IllegalArgumentException("Kwota przekracza " + zl(limit) + ".");
                String m = metoda.getValue(); long id = sesja.getId();
                if (!potwierdz(zwrot ? "Potwierdzenie zwrotu" : "Potwierdzenie wpłaty", "Zapisać " + zl(k) + " dla rezerwacji #" + r.getId() + "?")) return;
                uwierzytelnione(zwrot, "Zapis płatności", () -> {
                    if (zwrot) BazaDanych.zapiszZwrot(r.getId(),k,m,id);
                    else BazaDanych.zapiszWplate(r.getId(),k,m,id);
                    return true;
                }, v -> {
                    f.zamknij();
                    odswiezPo("Płatność została zapisana.", () -> pobierzRozliczenie(r));
                });
            });
        });
    }

    private void formularzPracownika() {
        Formularz f = new Formularz("Nowe konto pracownika", "Dostęp do programu i uprawnienia.", "Utwórz konto");
        TextField imie = f.tekst("Imię", ""), nazwisko = f.tekst("Nazwisko", ""), login = f.tekst("Login", "");
        PasswordField haslo = f.haslo("Hasło"), powtorz = f.haslo("Powtórz hasło");
        ComboBox<String> rola = wybor("RECEPCJA", "ADMIN"); f.dodaj("Rola", rola);
        f.notatka("Login: 3–60 znaków (litery bez polskich znaków, cyfry, kropka, _ lub -). Hasło: 12–128 znaków. W bazie zostanie zapisany skrót PBKDF2.");
        f.pokaz(() -> {
            String i = imie.getText().trim(), n = nazwisko.getText().trim(), l = login.getText().trim(), h = haslo.getText(), ro = rola.getValue();
            GuiRepository.tekst(i,60,"Imię"); GuiRepository.tekst(n,80,"Nazwisko");
            if (!l.matches("[A-Za-z0-9._-]{3,60}")) throw new IllegalArgumentException("Login ma niepoprawny format.");
            sprawdzHasla(h,powtorz.getText());
            uwierzytelnione(true, "Tworzenie konta", () -> { BazaDanych.dodajPracownika(i,n,l,h,ro); return true; },
                    v -> { haslo.clear(); powtorz.clear(); f.zamknij(); odswiez("Utworzono konto pracownika."); });
        });
    }

    private void formularzHasla() {
        Formularz f = new Formularz("Zmiana własnego hasła", "Konto: " + sesja.getLogin(), "Zmień hasło");
        PasswordField stare = f.haslo("Obecne hasło"), nowe = f.haslo("Nowe hasło"), powtorz = f.haslo("Powtórz nowe hasło");
        f.notatka("Nowe hasło: 12–128 znaków. Spacje są zachowywane. Zmiana nie kończy automatycznie innych już otwartych sesji.");
        f.pokaz(() -> {
            String s = stare.getText(), n = nowe.getText(); sprawdzHasla(n,powtorz.getText());
            if (s.isEmpty()) throw new IllegalArgumentException("Podaj obecne hasło.");
            long id = sesja.getId();
            uwierzytelnione(false, "Zmiana hasła", () -> { BazaDanych.zmienHaslo(id,s,n); return true; },
                    v -> { stare.clear(); nowe.clear(); powtorz.clear(); f.zamknij(); status.setText("Hasło zmieniono. Przy następnym logowaniu użyj nowego hasła."); });
        });
    }

    private void konfigurujTabele() {
        tabelaGosci.setId("guests-table"); tabelaPokoi.setId("rooms-table"); tabelaRezerwacji.setId("bookings-table");
        tabelaKont.setId("accounts-table"); tabelaRozliczen.setId("balances-table"); tabelaPlatnosci.setId("payments-table");
        kol(tabelaGosci,"ID",60,Gosc::getId); kol(tabelaGosci,"Imię",140,Gosc::getImie);
        kol(tabelaGosci,"Nazwisko",180,Gosc::getNazwisko); kol(tabelaGosci,"Telefon",180,Gosc::getTelefon); kol(tabelaGosci,"E-mail",260,Gosc::getEmail);
        kol(tabelaPokoi,"ID",60,Pokoj::getId); kol(tabelaPokoi,"Numer",100,Pokoj::getNumer);
        kol(tabelaPokoi,"Typ",180,p -> typy.getOrDefault(p.getId(),"")); kol(tabelaPokoi,"Miejsca",90,Pokoj::getLiczbaMiejsc);
        kol(tabelaPokoi,"Cena za noc (zł)",150,Pokoj::getCenaZaNoc); kol(tabelaPokoi,"Stan",180,Pokoj::getStan);
        tabelaRezerwacji(tabelaRezerwacji); tabelaRezerwacji(tabelaRozliczen);
        kol(tabelaKont,"ID",60,KontoPracownika::id); kol(tabelaKont,"Imię",130,KontoPracownika::imie);
        kol(tabelaKont,"Nazwisko",170,KontoPracownika::nazwisko); kol(tabelaKont,"Login",150,KontoPracownika::login);
        kol(tabelaKont,"Rola",130,KontoPracownika::rola); kol(tabelaKont,"Konto",130,k -> k.aktywny() ? "AKTYWNE" : "NIEAKTYWNE");
        kol(tabelaPlatnosci,"ID",65,Platnosc::id); kol(tabelaPlatnosci,"Data i czas",200,p -> p.data().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        kol(tabelaPlatnosci,"Rodzaj",145,Platnosc::rodzaj); kol(tabelaPlatnosci,"Kwota (zł)",145,Platnosc::kwota); kol(tabelaPlatnosci,"Metoda",150,Platnosc::metoda);
        podlacz(tabelaGosci,widoczniGoscie); podlacz(tabelaPokoi,widocznePokoje); podlacz(tabelaRezerwacji,widoczneRezerwacje);
        podlacz(tabelaKont,widoczneKonta);
        SortedList<Rezerwacja> salda = new SortedList<>(rezerwacje); salda.comparatorProperty().bind(tabelaRozliczen.comparatorProperty()); tabelaRozliczen.setItems(salda);
        pusta(tabelaGosci,"Brak gości pasujących do wyszukiwania."); pusta(tabelaPokoi,"Brak pokoi pasujących do filtrów.");
        pusta(tabelaRezerwacji,"Brak rezerwacji pasujących do filtrów."); pusta(tabelaKont,"Brak kont.");
        pusta(tabelaRozliczen,"Brak rezerwacji."); pusta(tabelaPlatnosci,"Brak płatności lub nie wybrano rozliczenia.");
        for (TextField t : new TextField[]{szukajGoscia,szukajPokoju,szukajRezerwacji,szukajKonta})
            t.textProperty().addListener((o,a,b) -> filtruj());
        stanPokoju.valueProperty().addListener((o,a,b) -> filtruj()); statusRezerwacji.valueProperty().addListener((o,a,b) -> filtruj());
        tabelaRozliczen.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> {
            tabelaPlatnosci.getItems().clear(); szczegoly.setText("Kliknij „Pokaż rozliczenie”, aby pobrać aktualne saldo.");
        });
    }

    private void tabelaRezerwacji(TableView<Rezerwacja> t) {
        kol(t,"ID",60,Rezerwacja::getId); kol(t,"Goście pobytu",270,Rezerwacja::getGosciePobytu);
        kol(t,"Pokój",75,r -> r.getPokoj().getNumer()); kol(t,"Przyjazd",115,Rezerwacja::getOd);
        kol(t,"Wyjazd",115,Rezerwacja::getDoDnia); kol(t,"Osoby",65,Rezerwacja::getLiczbaOsob);
        kol(t,"Cena (zł)",110,Rezerwacja::obliczCene); kol(t,"Status",165,Rezerwacja::getStatus);
    }

    private void filtruj() {
        widoczniGoscie.setPredicate(g -> pasuje(szukajGoscia.getText(), osoba(g) + " " + g.getTelefon() + " " + g.getEmail() + " " + g.getId()));
        widoczneRezerwacje.setPredicate(r -> pasuje(szukajRezerwacji.getText(), r.getId() + " " + r.getGosciePobytu() + " " + r.getPokoj().getNumer())
                && (statusRezerwacji.getValue() == null || statusRezerwacji.getValue().equals("Wszystkie statusy") || r.getStatus().equals(statusRezerwacji.getValue())));
        widoczneKonta.setPredicate(k -> pasuje(szukajKonta.getText(), k.id() + " " + k.imie() + " " + k.nazwisko() + " " + k.login()));
        // Używamy ostatnio zatwierdzonych wartości DatePicker. Walidacja następuje przed włączeniem filtra.
        LocalDate od = filtrOd, koniec = filtrDo; int osoby = filtrOsoby;
        widocznePokoje.setPredicate(p -> {
            if (!pasuje(szukajPokoju.getText(), p.getNumer() + " " + typy.getOrDefault(p.getId(),""))) return false;
            if (stanPokoju.getValue() != null && !stanPokoju.getValue().equals("Wszystkie stany") && !p.getStan().equals(stanPokoju.getValue())) return false;
            if (!filtrTerminu) return true;
            return od != null && koniec != null && !p.getStan().equals("WYLACZONY") && p.getLiczbaMiejsc() >= osoby
                    && rezerwacje.stream().noneMatch(r -> r.kolidacja(p.getNumer(),od,koniec));
        });
        filtrInfo.setText(filtrTerminu ? "Wolne w terminie " + od + " → " + koniec + " • osób: " + osoby
                : "Bez filtra terminu • wszystkie pokoje zgodne z wyszukiwaniem.");
    }

    private void odswiez(String komunikat) { odswiezPo(komunikat, () -> {}); }
    private void odswiezPo(String komunikat, Runnable potem) {
        uwierzytelnione(false, "Odświeżanie danych", () -> GuiRepository.pobierz(sesja), d -> {
            typy = d.typy(); goscie.setAll(d.goscie()); pokoje.setAll(d.pokoje()); rezerwacje.setAll(d.rezerwacje()); konta.setAll(d.konta());
            filtruj(); tabelaPokoi.refresh(); pokazStrone(); status.setText(komunikat + " • " + godzina()); potem.run();
        });
    }

    private <T> void uwierzytelnione(boolean tylkoAdmin, String nazwa, Callable<T> praca, Consumer<T> sukces) {
        Pracownik osoba = sesja;
        zadanie(nazwa, () -> { GuiRepository.sprawdzSesje(osoba,tylkoAdmin); return praca.call(); }, sukces);
    }

    private <T> void zadanie(String nazwa, Callable<T> praca, Consumer<T> sukces) {
        if (busy.get()) return;
        busy.set(true); status.setText(nazwa + "…");
        Task<T> task = new Task<>() { @Override protected T call() throws Exception { return praca.call(); } };
        task.setOnSucceeded(e -> { busy.set(false); sukces.accept(task.getValue()); });
        task.setOnFailed(e -> {
            busy.set(false); Throwable blad = task.getException();
            if (blad instanceof GuiRepository.SesjaWygasla) {
                if (aktywnyDialog != null) aktywnyDialog.close();
                pokazLogowanie();
            }
            status.setText("Operacja nie została potwierdzona.");
            blad(komunikatBledu(blad));
        });
        executor.submit(task);
    }

    private String komunikatBledu(Throwable e) {
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

    private final class Formularz {
        final Dialog<ButtonType> dialog = new Dialog<>();
        final GridPane grid = new GridPane();
        final Label error = etykieta("", "error-text");
        final ButtonType zapisz;
        int row;
        Formularz(String tytul, String opis, String przycisk) {
            dialog.setTitle(tytul); dialog.setHeaderText(opis); dialog.initOwner(stage);
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/hotel.css").toExternalForm());
            dialog.setResizable(true); dialog.getDialogPane().setPrefWidth(590);
            zapisz = new ButtonType(przycisk, ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(zapisz,ButtonType.CANCEL);
            ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Anuluj");
            dialog.getDialogPane().lookupButton(zapisz).getStyleClass().add("primary");
            dialog.getDialogPane().lookupButton(zapisz).setId("form-save");
            grid.setHgap(18); grid.setVgap(12); grid.setPadding(new Insets(12));
            ColumnConstraints l = new ColumnConstraints(140), r = new ColumnConstraints(); r.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().addAll(l,r); error.setWrapText(true); error.setMaxWidth(520);
            VBox content = new VBox(8,grid,error); content.setPadding(new Insets(4));
            ScrollPane scroll = new ScrollPane(content); scroll.setFitToWidth(true); scroll.setPrefViewportHeight(390);
            dialog.getDialogPane().setContent(scroll);
            dialog.getDialogPane().disableProperty().bind(busy);
            dialog.setOnCloseRequest(e -> { if (busy.get()) e.consume(); });
            dialog.setOnHidden(e -> { if (aktywnyDialog == dialog) aktywnyDialog = null; });
        }
        void dodaj(String opis, Control pole) { pole.setMaxWidth(Double.MAX_VALUE); grid.add(etykieta(opis,"form-label"),0,row); grid.add(pole,1,row++); }
        TextField tekst(String opis, String wartosc) { TextField t = new TextField(wartosc); dodaj(opis,t); return t; }
        PasswordField haslo(String opis) { PasswordField p = new PasswordField(); dodaj(opis,p); return p; }
        void notatka(String tekst) { Label l = etykieta(tekst,"muted"); l.setWrapText(true); l.setMaxWidth(480); grid.add(l,0,row++,2,1); }
        void pokaz(Runnable zapis) {
            dialog.getDialogPane().lookupButton(zapisz).addEventFilter(javafx.event.ActionEvent.ACTION,e -> {
                e.consume(); error.setText("");
                try { zapis.run(); } catch (IllegalArgumentException ex) { error.setText(ex.getMessage()); }
            });
            aktywnyDialog = dialog; dialog.show();
        }
        void zamknij() { dialog.close(); }
    }

    private void blad(String tekst) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle("Nie można wykonać operacji"); a.setHeaderText("Sprawdź dane i spróbuj ponownie");
        a.setContentText(tekst); a.initOwner(wlasciciel()); a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE); a.showAndWait();
    }
    private boolean potwierdz(String tytul, String tekst) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION); a.setTitle(tytul); a.setHeaderText(tytul); a.setContentText(tekst);
        a.initOwner(wlasciciel()); a.getButtonTypes().setAll(ButtonType.YES,ButtonType.NO);
        ((Button) a.getDialogPane().lookupButton(ButtonType.YES)).setText("Tak");
        ((Button) a.getDialogPane().lookupButton(ButtonType.NO)).setText("Nie");
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
    private Window wlasciciel() { return aktywnyDialog != null && aktywnyDialog.isShowing() ? aktywnyDialog.getDialogPane().getScene().getWindow() : stage; }
    private boolean admin() { return sesja != null && sesja.getRola().equals("ADMIN"); }
    private static String osoba(Osoba o) { return o.getImie() + " " + o.getNazwisko(); }
    private static String godzina() { return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")); }
    private static String zl(BigDecimal k) { return k.setScale(2).toPlainString().replace('.',',') + " zł"; }
    private static String klucz(String s) { return s.toLowerCase(Locale.ROOT).replace('ł','l').replace('ś','s').replace('ć','c').replace('ą','a').replace(' ','-'); }
    private static boolean pasuje(String szukane, String tekst) { return tekst.toLowerCase(Locale.ROOT).contains(szukane.trim().toLowerCase(Locale.ROOT)); }
    private static TextField pole(String podpowiedz) { TextField t = new TextField(); t.setPromptText(podpowiedz); t.setPrefWidth(280); return t; }
    private static Label etykieta(String tekst, String styl) { Label l = new Label(tekst); l.getStyleClass().add(styl); return l; }
    private static Button przycisk(String tekst, String styl, Runnable akcja) {
        Button b = new Button(tekst); if (!styl.isEmpty()) b.getStyleClass().add(styl); if (akcja != null) b.setOnAction(e -> akcja.run()); return b;
    }
    private static HBox pasek(Node... dzieci) { HBox h = new HBox(10,dzieci); h.setAlignment(Pos.CENTER_LEFT); return h; }
    private static ComboBox<String> wybor(String... opcje) {
        ComboBox<String> c = new ComboBox<>(FXCollections.observableArrayList(opcje)); c.getSelectionModel().selectFirst(); return c;
    }
    private static DatePicker data(LocalDate wartosc) {
        DatePicker d = new DatePicker(wartosc); d.setPromptText("RRRR-MM-DD"); d.setPrefWidth(145);
        d.setConverter(new StringConverter<>() {
            public String toString(LocalDate v) { return v == null ? "" : v.toString(); }
            public LocalDate fromString(String s) { try { return s == null || s.isBlank() ? null : LocalDate.parse(s.trim()); } catch (DateTimeParseException e) { return null; } }
        });
        d.getEditor().setText(d.getConverter().toString(wartosc));
        return d;
    }
    private static LocalDate dataZ(DatePicker d) {
        try {
            LocalDate v = LocalDate.parse(d.getEditor().getText().trim()); d.setValue(v); return v;
        } catch (DateTimeParseException e) { throw new IllegalArgumentException("Wybierz poprawną datę w formacie RRRR-MM-DD."); }
    }
    private static void termin(LocalDate a, LocalDate b) {
        if (a.isBefore(LocalDate.now()) || !b.isAfter(a) || java.time.temporal.ChronoUnit.DAYS.between(a,b) > 30)
            throw new IllegalArgumentException("Przyjazd nie może być w przeszłości. Pobyt musi trwać od 1 do 30 nocy.");
    }
    private static void sprawdzHasla(String a, String b) {
        if (a.length() < 12 || a.length() > 128) throw new IllegalArgumentException("Hasło musi mieć od 12 do 128 znaków.");
        if (!a.equals(b)) throw new IllegalArgumentException("Powtórzone hasło jest inne.");
    }
    private static <T> StringConverter<T> konwerter(Function<T,String> opis) {
        return new StringConverter<>() { public String toString(T v) { return v == null ? "" : opis.apply(v); } public T fromString(String s) { return null; } };
    }
    private <T> Button zaznaczony(String tekst, TableView<T> tabela, Consumer<T> akcja) {
        Button b = przycisk(tekst,"",() -> { T item = tabela.getSelectionModel().getSelectedItem(); if (item != null) akcja.accept(item); });
        b.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull()); return b;
    }
    private static <T> void pusta(TableView<T> t, String tekst) { t.setPlaceholder(new Label(tekst)); }
    private static <T> void podlacz(TableView<T> t, FilteredList<T> lista) {
        SortedList<T> sorted = new SortedList<>(lista); sorted.comparatorProperty().bind(t.comparatorProperty()); t.setItems(sorted);
    }
    private static <T> void kol(TableView<T> t, String nazwa, double szerokosc, Function<T,?> wartosc) {
        TableColumn<T,Object> c = new TableColumn<>(nazwa); c.setPrefWidth(szerokosc);
        c.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(wartosc.apply(v.getValue())));
        c.setComparator(HotelGUI::porownaj);
        t.getColumns().add(c);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int porownaj(Object a, Object b) {
        if (a == b) return 0; if (a == null) return -1; if (b == null) return 1;
        if (a instanceof Comparable && a.getClass().isInstance(b)) return ((Comparable)a).compareTo(b);
        return a.toString().compareToIgnoreCase(b.toString());
    }
    @FunctionalInterface private interface SqlAkcja { void wykonaj() throws SQLException; }
    @Override public void stop() { executor.shutdownNow(); }
}
