package pl.hotel.gui;

import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import pl.hotel.serwis.GuiRepository;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.sql.SQLException;
import static pl.hotel.gui.NarzedziaGUI.*;

final class EkranGosci {
    private final HotelGUI app;
    EkranGosci(HotelGUI app) { this.app = app; }

    Node stronaGosci() {
        Button dodaj = przycisk("+ Dodaj gościa", "primary", () -> formularzGoscia(null)); dodaj.setId("add-guest");
        Button edytuj = przycisk("Edytuj zaznaczonego", "", () -> formularzGoscia(app.stan.tabelaGosci.getSelectionModel().getSelectedItem()));
        edytuj.disableProperty().bind(app.stan.tabelaGosci.getSelectionModel().selectedItemProperty().isNull());
        HBox bar = pasek(app.stan.szukajGoscia, dodaj, edytuj);
        VBox page = app.strona("Goście", "Dane kontaktowe gości hotelowych.", bar, app.stan.tabelaGosci);
        VBox.setVgrow(app.stan.tabelaGosci, Priority.ALWAYS); return page;
    }

    void formularzGoscia(Gosc g) {
        Formularz f = new Formularz(app, g == null ? "Nowy gość" : "Edycja gościa", "Dane kontaktowe", "Zapisz gościa");
        TextField imie = f.tekst("Imię", g == null ? "" : g.getImie()); imie.setId("guest-first-name");
        TextField nazwisko = f.tekst("Nazwisko", g == null ? "" : g.getNazwisko()); nazwisko.setId("guest-last-name");
        TextField telefon = f.tekst("Telefon", g == null ? "" : g.getTelefon()); telefon.setId("guest-phone");
        TextField email = f.tekst("E-mail", g == null ? "" : g.getEmail()); email.setId("guest-email");
        f.pokaz(() -> {
            String i = imie.getText().trim(), n = nazwisko.getText().trim(), t = telefon.getText().trim(), e = email.getText().trim();
            GuiRepository.sprawdzGoscia(i, n, t, e);
            app.zadaniaGUI.uwierzytelnione(false, "Zapis gościa", () -> {
                if (g == null) BazaDanych.zapiszGoscia(i, n, t, e);
                else if (!BazaDanych.edytujGoscia(g.getId(), i, n, t, e)) throw new SQLException("Nie potwierdzono aktualizacji gościa.");
                return true;
            }, v -> { f.zamknij(); app.zadaniaGUI.odswiez("Zapisano dane gościa."); });
        });
    }
}
