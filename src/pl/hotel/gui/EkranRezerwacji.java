package pl.hotel.gui;

import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import static pl.hotel.gui.NarzedziaGUI.*;

final class EkranRezerwacji {
    private final HotelGUI app;
    private final KartaRezerwacjiGUI karta;
    EkranRezerwacji(HotelGUI app) { this.app = app; this.karta = new KartaRezerwacjiGUI(app); }

    Node stronaRezerwacji() {
        Button dodaj = przycisk("+ Nowa rezerwacja", "primary", this::formularzRezerwacji); dodaj.setId("add-booking");
        Button szukaj=przycisk("Wyszukiwanie zaawansowane","",()->new WyszukiwanieGUI(app).pokaz());szukaj.setId("booking-search");
        FlowPane bar=new FlowPane(10,10,app.stan.szukajRezerwacji,app.stan.statusRezerwacji,dodaj,szukaj);
        Button uczestnicy = app.zaznaczony("Goście pobytu", app.stan.tabelaRezerwacji, this::formularzUczestnikow); uczestnicy.setId("booking-participants");
        Button daty = app.zaznaczony("Zmień termin", app.stan.tabelaRezerwacji, this::formularzTerminu);
        Button pokoj = app.zaznaczony("Zmień pokój", app.stan.tabelaRezerwacji, r -> new ZmianaPokojuGUI(app).pokaz(r));
        pokoj.setId("booking-change-room");
        Button historia = app.zaznaczony("Historia", app.stan.tabelaRezerwacji, r -> new HistoriaRezerwacjiGUI(app).pokaz(r));
        historia.setId("booking-history");
        Button potwierdzenie = app.zaznaczony("Potwierdzenie", app.stan.tabelaRezerwacji, r -> new PotwierdzenieGUI(app).pokaz(r));
        potwierdzenie.setId("booking-confirmation");
        Button notatka = app.zaznaczony("Notatka recepcji", app.stan.tabelaRezerwacji, r -> new NotatkiGUI(app).pokaz(r));
        notatka.setId("booking-notes");
        Button uslugi=app.zaznaczony("Usługi pobytu",app.stan.tabelaRezerwacji,r->new UslugiGUI(app).pokaz(r));uslugi.setId("booking-services");
        Button katalog=przycisk("Katalog usług","",()->new KatalogUslugGUI(app).pokaz());katalog.setId("services-manage");katalog.setVisible(app.admin());katalog.setManaged(app.admin());
        Button zamelduj = app.zaznaczony("Zamelduj", app.stan.tabelaRezerwacji, r -> operacjaRezerwacji(r, "Zameldowanie", () -> BazaDanych.zameldujGoscia(r.getId())));
        Button wymelduj = app.zaznaczony("Wymelduj", app.stan.tabelaRezerwacji, r -> operacjaRezerwacji(r, "Wymeldowanie", () -> BazaDanych.wymeldujGoscia(r.getId())));
        Button anuluj = app.zaznaczony("Anuluj rezerwację", app.stan.tabelaRezerwacji, r -> operacjaRezerwacji(r, "Anulowanie rezerwacji", () -> BazaDanych.anulujRezerwacje(r.getId())));
        anuluj.getStyleClass().add("danger");
        VBox page = app.strona("Rezerwacje", "Wybierz pobyt z tabeli, aby zmienić termin lub obsłużyć gościa.", bar,
                app.stan.tabelaRezerwacji, new FlowPane(10, 10, uczestnicy, daty, pokoj, historia, potwierdzenie, notatka, uslugi, katalog, zamelduj, wymelduj, anuluj),
                karta.widok(), etykieta("Wymeldowanie wymaga pełnego rozliczenia. Przed anulowaniem zwróć wpłacone środki.", "muted"));
        VBox.setVgrow(app.stan.tabelaRezerwacji, Priority.ALWAYS); return page;
    }

    void formularzRezerwacji() { formularzRezerwacji(null, LocalDate.now()); }

    void formularzRezerwacji(Long pokojId, LocalDate przyjazd) {

        app.zadaniaGUI.odswiezPo("Przygotowanie rezerwacji", () -> {
            if (app.stan.goscie.stream().noneMatch(g->!g.isArchiwalny()) || app.stan.pokoje.isEmpty()) { app.komunikatyGUI.blad("Najpierw dodaj lub przywróć gościa i przygotuj pokój."); return; }
            Pokoj wybrany = pokojId == null ? null : app.stan.pokoje.stream().filter(p -> p.getId() == pokojId).findFirst().orElse(null);
            if (pokojId != null) {
                if (wybrany == null || !new pl.hotel.kalendarz.PlanKalendarza(java.time.YearMonth.from(przyjazd), app.stan.rezerwacje)
                        .dzien(wybrany, przyjazd).moznaRezerwowac(LocalDate.now())) {
                    app.komunikatyGUI.blad("Wybrana noc nie jest już dostępna. Sprawdź odświeżony kalendarz."); return;
                }
            }
            Formularz f = new Formularz(app, "Nowa rezerwacja", "Wybierz gościa, pokój i termin pobytu.", "Utwórz rezerwację");
            ComboBox<Gosc> gosc = new ComboBox<>(FXCollections.observableArrayList(app.stan.goscie.stream().filter(g->!g.isArchiwalny()).toList())); gosc.setId("booking-guest");
            gosc.setConverter(konwerter(g -> "#" + g.getId() + " • " + osoba(g)));
            gosc.getSelectionModel().selectFirst(); f.dodaj("Główny gość", gosc);
            ComboBox<Pokoj> pokoj = new ComboBox<>(FXCollections.observableArrayList(app.stan.pokoje.stream()
                    .filter(p -> !p.getStan().equals("WYLACZONY")).toList())); pokoj.setId("booking-room");
            pokoj.setConverter(konwerter(p -> p.getNumer() + " • " + p.getLiczbaMiejsc() + " os. • " + zl(p.getCenaZaNoc()) + "/noc"));
            if (wybrany == null) pokoj.getSelectionModel().selectFirst(); else pokoj.getSelectionModel().select(wybrany);
            f.dodaj("Pokój", pokoj);
            Spinner<Integer> osoby = new Spinner<>(1, 10, 1); osoby.setId("booking-people"); f.dodaj("Liczba osób", osoby);
            PanelUczestnikow panel = new PanelUczestnikow(f);
            osoby.valueProperty().addListener((o,a,b) -> panel.liczba(b - 1));
            panel.liczba(0);
            f.notatka("Liczba osób obejmuje głównego gościa. Dla każdej kolejnej osoby wpisz imię i nazwisko; telefon i e-mail nie są wymagane.");
            DatePicker od = data(przyjazd), koniec = data(przyjazd.plusDays(1));
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
                long pracownik = app.stan.sesja.getId();
                app.zadaniaGUI.uwierzytelnione(false, "Tworzenie rezerwacji", () -> BazaDanych.zapiszRezerwacje(g,p,a,b,liczba,pracownik,listaOsob),
                        r -> { f.zamknij(); app.zadaniaGUI.odswiez("Utworzono rezerwację #" + r.getId() + "."); });
            });
        });
    }

    void formularzUczestnikow(Rezerwacja r) {
        boolean edycja = r.getStatus().equals("POTWIERDZONA") || r.getStatus().equals("ZAMELDOWANA");
        if (!edycja) {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.initOwner(app.stan.stage); a.setTitle("Goście pobytu #" + r.getId());
            a.setHeaderText("Pokój " + r.getPokoj().getNumer()); a.setContentText(r.getGosciePobytu()); a.showAndWait(); return;
        }
        Formularz f = new Formularz(app, "Goście pobytu #" + r.getId(),
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
            app.zadaniaGUI.uwierzytelnione(false, "Zapis gości pobytu", () -> { BazaDanych.zmienUczestnikow(r.getId(), osoby); return true; },
                    v -> { f.zamknij(); app.zadaniaGUI.odswiez("Zapisano gości pobytu #" + r.getId() + "."); });
        });
    }

    final class PanelUczestnikow {
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

    void formularzTerminu(Rezerwacja r) {
        if (!r.getStatus().equals("POTWIERDZONA")) { app.komunikatyGUI.blad("Zmień termin potwierdzonej rezerwacji."); return; }
        Formularz f = new Formularz(app, "Zmiana terminu #" + r.getId(), osoba(r.getGosc()) + " • pokój " + r.getPokoj().getNumer(), "Zapisz termin");
        DatePicker od = data(r.getOd()), koniec = data(r.getDoDnia()); f.dodaj("Nowy przyjazd", od); f.dodaj("Nowy wyjazd", koniec);
        f.notatka("Cena za noc pozostanie bez zmian: " + zl(r.getCenaZaNoc()) + ". Ilości usług pozostają bez zmian. Jeśli zaliczka przekroczy nową cenę pobytu, najpierw zarejestruj zwrot nadpłaty.");
        f.pokaz(() -> {
            LocalDate a = dataZ(od), b = dataZ(koniec); termin(a,b);
            app.zadaniaGUI.uwierzytelnione(false, "Zmiana terminu", () -> { BazaDanych.zmienTerminRezerwacji(r.getId(),a,b); return true; },
                    v -> { f.zamknij(); app.zadaniaGUI.odswiez("Zmieniono termin rezerwacji #" + r.getId() + "."); });
        });
    }

    void operacjaRezerwacji(Rezerwacja r, String nazwa, SqlAkcja akcja) {
        String opis = "Rezerwacja #" + r.getId() + " • " + osoba(r.getGosc()) + " • pokój " + r.getPokoj().getNumer();
        if (!app.komunikatyGUI.potwierdz(nazwa, opis + "\n\nPotwierdzasz operację?")) return;
        app.zadaniaGUI.uwierzytelnione(false, nazwa, () -> { akcja.wykonaj(); return true; }, v -> app.zadaniaGUI.odswiez(nazwa + " — zapisano zmianę."));
    }
}
