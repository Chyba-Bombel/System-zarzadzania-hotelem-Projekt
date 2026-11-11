package pl.hotel.gui;

import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import pl.hotel.serwis.GuiRepository;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.math.BigDecimal;
import static pl.hotel.gui.NarzedziaGUI.*;

final class EkranPlatnosci {
    private final HotelGUI app;
    EkranPlatnosci(HotelGUI app) { this.app = app; }

    Node stronaPlatnosci() {
        Button pokaz = app.zaznaczony("Pokaż rozliczenie", app.stan.tabelaRozliczen, this::pobierzRozliczenie); pokaz.setId("show-balance");
        Button wplata = app.zaznaczony("+ Wpłata / zaliczka", app.stan.tabelaRozliczen, r -> formularzPlatnosci(r, false));
        wplata.getStyleClass().add("primary");
        HBox bar = pasek(pokaz, wplata);
        if (app.admin()) bar.getChildren().add(app.zaznaczony("Zarejestruj zwrot", app.stan.tabelaRozliczen, r -> formularzPlatnosci(r, true)));
        app.stan.tabelaRozliczen.setPrefHeight(240); app.stan.tabelaPlatnosci.setPrefHeight(210);
        app.stan.szczegoly.setWrapText(true); app.stan.szczegoly.getStyleClass().add("card"); app.stan.szczegoly.setMaxWidth(Double.MAX_VALUE);
        VBox page = app.strona("Płatności", "Rozliczenia oraz historia wpłat i zwrotów. Operacje rejestrują płatności w bazie.",
                app.stan.tabelaRozliczen, bar, app.stan.szczegoly, app.stan.tabelaPlatnosci);
        VBox.setVgrow(app.stan.tabelaRozliczen, Priority.ALWAYS); VBox.setVgrow(app.stan.tabelaPlatnosci, Priority.ALWAYS); return page;
    }

    void pobierzRozliczenie(Rezerwacja r) {
        app.zadaniaGUI.uwierzytelnione(false, "Pobieranie rozliczenia", () -> BazaDanych.pobierzRozliczenie(r.getId()), this::wyswietlRozliczenie);
    }

    void wyswietlRozliczenie(Rozliczenie r) {
        String saldo = r.saldo().signum() > 0 ? "Do dopłaty: " + zl(r.saldo())
                : r.saldo().signum() < 0 ? "Do zwrotu: " + zl(r.saldo().abs()) : "Rezerwacja rozliczona";
        app.stan.szczegoly.setText("Rezerwacja #" + r.rezerwacjaId() + " • " + r.status()
                + "\nCena pobytu: " + zl(r.cenaPobytu()) + "    Należność: " + zl(r.naleznosc())
                + "\nWpłaty: " + zl(r.wplaty()) + "    Zwroty: " + zl(r.zwroty())
                + "    Zapłacono netto: " + zl(r.zaplaconoNetto()) + "\n" + saldo);
        app.stan.tabelaPlatnosci.setItems(FXCollections.observableArrayList(r.platnosci()));
        app.stan.status.setText("Rozliczenie #" + r.rezerwacjaId() + " • " + godzina());
    }

    void formularzPlatnosci(Rezerwacja r, boolean zwrot) {
        app.zadaniaGUI.uwierzytelnione(zwrot, "Sprawdzanie salda", () -> BazaDanych.pobierzRozliczenie(r.getId()), roz -> {
            BigDecimal limit = zwrot ? roz.zaplaconoNetto() : roz.saldo();
            if (!zwrot && roz.status().equals("ANULOWANA")) { app.komunikatyGUI.blad("Nie można wpłacać na anulowaną rezerwację."); return; }
            if (limit.signum() <= 0) { app.komunikatyGUI.blad(zwrot ? "Brak środków do zwrotu." : "Brak kwoty do dopłaty."); return; }
            Formularz f = new Formularz(app, zwrot ? "Rejestracja zwrotu" : "Wpłata lub zaliczka",
                    "Rezerwacja #" + r.getId() + " • " + osoba(r.getGosc()), zwrot ? "Zapisz zwrot" : "Zapisz wpłatę");
            f.notatka((zwrot ? "Maksymalnie do zwrotu: " : "Pozostało do zapłaty: ") + zl(limit));
            TextField kwota = f.tekst("Kwota (zł)", limit.toPlainString()); kwota.setId("payment-amount");
            ComboBox<String> metoda = wybor("GOTOWKA", "KARTA", "PRZELEW"); f.dodaj("Metoda", metoda);
            f.notatka(zwrot ? "Zwrot nie zmniejsza ceny aktywnego pobytu — zwiększa kwotę do dopłaty. Zapis w programie nie wykonuje przelewu bankowego."
                    : "Zapis rejestruje otrzymaną płatność. Nie łączy się z bankiem ani terminalem.");
            f.pokaz(() -> {
                BigDecimal k = GuiRepository.kwota(kwota.getText());
                if (k.compareTo(limit) > 0) throw new IllegalArgumentException("Kwota przekracza " + zl(limit) + ".");
                String m = metoda.getValue(); long id = app.stan.sesja.getId();
                if (!app.komunikatyGUI.potwierdz(zwrot ? "Potwierdzenie zwrotu" : "Potwierdzenie wpłaty", "Zapisać " + zl(k) + " dla rezerwacji #" + r.getId() + "?")) return;
                app.zadaniaGUI.uwierzytelnione(zwrot, "Zapis płatności", () -> {
                    if (zwrot) BazaDanych.zapiszZwrot(r.getId(),k,m,id);
                    else BazaDanych.zapiszWplate(r.getId(),k,m,id);
                    return true;
                }, v -> {
                    f.zamknij();
                    app.zadaniaGUI.odswiezPo("Płatność została zapisana.", () -> pobierzRozliczenie(r));
                });
            });
        });
    }
}
