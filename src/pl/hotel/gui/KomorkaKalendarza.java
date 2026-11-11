package pl.hotel.gui;

import pl.hotel.kalendarz.PlanKalendarza.PoleDnia;
import pl.hotel.model.Pokoj;
import javafx.scene.control.*;
import java.time.LocalDate;
import java.util.function.Consumer;

/** Jedna komórka planu; resetuje stan przy ponownym użyciu przez TableView. */
final class KomorkaKalendarza extends TableCell<Pokoj, PoleDnia> {
    private final Consumer<PoleDnia> klik;
    KomorkaKalendarza(Consumer<PoleDnia> klik) { this.klik = klik; getStyleClass().add("calendar-cell"); }

    @Override protected void updateItem(PoleDnia pole, boolean empty) {
        super.updateItem(pole, empty);
        setText(null); setGraphic(null); setTooltip(null);
        if (empty || pole == null) return;
        var r = pole.widoczna();
        boolean wolne = r == null, dostepne = pole.moznaRezerwowac(LocalDate.now());
        String kolor = pole.konflikt() ? "conflict" : wolne ? (dostepne ? "free" : "unavailable")
                : r.getStatus().equals("POTWIERDZONA") ? "confirmed"
                : r.getStatus().equals("ZAMELDOWANA") ? "checked-in" : "finished";
        Button b = new Button(pole.konflikt() ? "!" : wolne ? (dostepne ? "+" : "—") : "#" + r.getId());
        b.setId("calendar-cell-" + pole.pokoj().getId() + "-" + pole.data());
        b.getStyleClass().addAll("calendar-slot", "calendar-" + kolor);
        b.setMaxWidth(Double.MAX_VALUE); b.setMaxHeight(Double.MAX_VALUE);
        b.setDisable(wolne && !dostepne);
        String opis = "Pokój " + pole.pokoj().getNumer() + " • noc " + pole.data();
        if (r != null) opis += "\n" + r.getGosciePobytu() + "\n" + r.getOd() + " → " + r.getDoDnia() + "\n" + r.getStatus();
        else if (pole.data().isBefore(LocalDate.now())) opis += "\nTermin w przeszłości.";
        else if (pole.pokoj().getStan().equals("WYLACZONY")) opis += "\nPokój obecnie wyłączony z użytkowania.";
        else opis += "\nKliknij, aby rozpocząć rezerwację.";
        if (pole.konflikt()) opis += "\nUWAGA: kilka aktywnych rezerwacji na tę samą noc.";
        b.setTooltip(new Tooltip(opis)); setTooltip(new Tooltip(opis)); b.setAccessibleText(opis);
        b.setOnAction(e -> klik.accept(pole)); setGraphic(b);
    }
}
