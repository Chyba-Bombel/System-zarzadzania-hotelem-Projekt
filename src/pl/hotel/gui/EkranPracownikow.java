package pl.hotel.gui;

import pl.hotel.baza.BazaDanych;
import pl.hotel.serwis.GuiRepository;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import static pl.hotel.gui.NarzedziaGUI.*;

final class EkranPracownikow {
    private final HotelGUI app;
    EkranPracownikow(HotelGUI app) { this.app = app; }

    Node stronaPracownikow() {
        Button dodaj = przycisk("+ Nowe konto", "primary", this::formularzPracownika); dodaj.setId("add-account");
        Button zablokuj = app.zaznaczony("Dezaktywuj konto", app.stan.tabelaKont, k -> {
            if (!k.aktywny()) { app.komunikatyGUI.blad("To konto jest już nieaktywne."); return; }
            if (k.id() == app.stan.sesja.getId()) { app.komunikatyGUI.blad("Nie możesz dezaktywować własnego konta."); return; }
            if (!app.komunikatyGUI.potwierdz("Dezaktywacja konta", "Zablokować kolejne logowanie konta „" + k.login() + "”?")) return;
            long adminId = app.stan.sesja.getId();
            app.zadaniaGUI.uwierzytelnione(true, "Dezaktywacja konta", () -> { BazaDanych.dezaktywujPracownika(k.id(), adminId); return true; },
                    v -> app.zadaniaGUI.odswiez("Konto zostało dezaktywowane."));
        }); zablokuj.getStyleClass().add("danger");
        VBox page = app.strona("Pracownicy", "Konta pracowników, role i aktywność. Dane historyczne nie są usuwane.",
                pasek(app.stan.szukajKonta, dodaj, zablokuj), app.stan.tabelaKont);
        VBox.setVgrow(app.stan.tabelaKont, Priority.ALWAYS); return page;
    }

    void formularzPracownika() {
        Formularz f = new Formularz(app, "Nowe konto pracownika", "Dostęp do programu i uprawnienia.", "Utwórz konto");
        TextField imie = f.tekst("Imię", ""), nazwisko = f.tekst("Nazwisko", ""), login = f.tekst("Login", "");
        PasswordField haslo = f.haslo("Hasło"), powtorz = f.haslo("Powtórz hasło");
        ComboBox<String> rola = wybor("RECEPCJA", "ADMIN"); f.dodaj("Rola", rola);
        f.notatka("Login: 3–60 znaków (litery bez polskich znaków, cyfry, kropka, _ lub -). Hasło: 12–128 znaków. W bazie zostanie zapisany skrót PBKDF2.");
        f.pokaz(() -> {
            String i = imie.getText().trim(), n = nazwisko.getText().trim(), l = login.getText().trim(), h = haslo.getText(), ro = rola.getValue();
            GuiRepository.tekst(i,60,"Imię"); GuiRepository.tekst(n,80,"Nazwisko");
            if (!l.matches("[A-Za-z0-9._-]{3,60}")) throw new IllegalArgumentException("Login ma niepoprawny format.");
            sprawdzHasla(h,powtorz.getText());
            app.zadaniaGUI.uwierzytelnione(true, "Tworzenie konta", () -> { BazaDanych.dodajPracownika(i,n,l,h,ro); return true; },
                    v -> { haslo.clear(); powtorz.clear(); f.zamknij(); app.zadaniaGUI.odswiez("Utworzono konto pracownika."); });
        });
    }
}
