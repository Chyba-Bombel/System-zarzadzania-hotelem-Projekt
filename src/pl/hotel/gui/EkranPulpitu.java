package pl.hotel.gui;

import pl.hotel.model.*;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import static pl.hotel.gui.NarzedziaGUI.*;

final class EkranPulpitu {
    private final HotelGUI app;
    EkranPulpitu(HotelGUI app) { this.app = app; }

    Node pulpit() {
        long aktywne = app.stan.rezerwacje.stream().filter(r -> r.getStatus().equals("POTWIERDZONA") || r.getStatus().equals("ZAMELDOWANA")).count();
        HBox liczby = new HBox(15,
                kafel("Goście", app.stan.goscie.size(), "Osoby zapisane w bazie"),
                kafel("Pokoje", app.stan.pokoje.size(), "Wszystkie pokoje hotelu"),
                kafel("Aktywne rezerwacje", aktywne, "Potwierdzone i zameldowane"));
        LocalDate dzis = LocalDate.now();
        TableView<Rezerwacja> tabela = new TableView<>();
        kol(tabela, "ID", 65, Rezerwacja::getId);
        kol(tabela, "Gość", 200, r -> osoba(r.getGosc()));
        kol(tabela, "Pokój", 90, r -> r.getPokoj().getNumer());
        kol(tabela, "Przyjazd", 130, Rezerwacja::getOd);
        kol(tabela, "Wyjazd", 130, Rezerwacja::getDoDnia);
        kol(tabela, "Status", 155, Rezerwacja::getStatus);
        tabela.setItems(FXCollections.observableArrayList(app.stan.rezerwacje.stream().filter(r ->
                (r.getOd().equals(dzis) && r.getStatus().equals("POTWIERDZONA"))
                || (!r.getDoDnia().isAfter(dzis) && r.getStatus().equals("ZAMELDOWANA"))).toList()));
        pusta(tabela, "Brak przyjazdów i wyjazdów wymagających dziś uwagi."); tabela.setPrefHeight(340);
        Label opis = etykieta("Przyjazdy na dziś oraz zameldowane pobyty z dzisiejszym lub zaległym wyjazdem.", "muted"); opis.setWrapText(true);
        VBox page = app.strona("Pulpit", "Przegląd pracy hotelu • " + dzis, liczby,
                etykieta("Dzisiaj w hotelu", "section-title"), opis, tabela,
                etykieta("Aby wykonać operację, przejdź do odpowiedniej zakładki. Dane odświeżaj przyciskiem w prawym górnym rogu.", "muted"));
        VBox.setVgrow(tabela, Priority.ALWAYS); return page;
    }

    VBox kafel(String opis, long wartosc, String podtekst) {
        VBox v = new VBox(7, etykieta(opis, "muted"), etykieta(String.valueOf(wartosc), "metric"), etykieta(podtekst, "muted"));
        v.getStyleClass().add("card"); HBox.setHgrow(v, Priority.ALWAYS); v.setMaxWidth(Double.MAX_VALUE); return v;
    }
}
