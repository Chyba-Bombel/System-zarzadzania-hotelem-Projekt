package pl.hotel.gui;

import pl.hotel.kalendarz.PlanKalendarza;
import pl.hotel.model.*;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static pl.hotel.gui.NarzedziaGUI.*;

final class PodgladKalendarzaGUI {
    private final HotelGUI app;
    PodgladKalendarzaGUI(HotelGUI app) { this.app = app; }

    VBox utworz() {
        LocalDate dzis = LocalDate.now();
        Map<YearMonth,PlanKalendarza> plany = new HashMap<>();
        GridPane siatka = new GridPane(); siatka.setHgap(5); siatka.setVgap(5); siatka.setMinWidth(700);
        ColumnConstraints numer = new ColumnConstraints(68); siatka.getColumnConstraints().add(numer);
        for(int i=0;i<7;i++) { ColumnConstraints c = new ColumnConstraints(); c.setHgrow(Priority.ALWAYS); c.setPercentWidth(13); siatka.getColumnConstraints().add(c); }
        siatka.add(etykieta("Pokój","muted"),0,0);
        DateTimeFormatter naglowek = DateTimeFormatter.ofPattern("d EEE",Locale.forLanguageTag("pl-PL"));
        for(int i=0;i<7;i++) { Label l = etykieta(naglowek.format(dzis.plusDays(i)),"week-day"); l.setMaxWidth(Double.MAX_VALUE); if(i==0) l.getStyleClass().add("today"); siatka.add(l,i+1,0); }
        List<Pokoj> pokoje = app.stan.pokoje.stream().sorted(Comparator.comparing(Pokoj::getNumer)).limit(4).toList();
        for(int row=0;row<pokoje.size();row++) {
            Pokoj pokoj = pokoje.get(row); siatka.add(etykieta(pokoj.getNumer(),"strong"),0,row+1);
            int dzien=0;
            while(dzien<7) {
                LocalDate data = dzis.plusDays(dzien);
                var pole = plany.computeIfAbsent(YearMonth.from(data), m -> new PlanKalendarza(m,app.stan.rezerwacje)).dzien(pokoj,data);
                Rezerwacja r = pole.widoczna(); int span=1;
                if(r != null && !pole.konflikt()) while(dzien+span<7) {
                    LocalDate kolejna = dzis.plusDays(dzien+span);
                    var p = plany.computeIfAbsent(YearMonth.from(kolejna), m -> new PlanKalendarza(m,app.stan.rezerwacje)).dzien(pokoj,kolejna);
                    if(p.konflikt() || p.widoczna()==null || p.widoczna().getId()!=r.getId()) break;
                    span++;
                }
                String kolor = pole.konflikt()?"conflict":r==null?(pole.moznaRezerwowac(dzis)?"free":"unavailable"):
                        r.getStatus().equals("ZAMELDOWANA")?"checked-in":r.getStatus().equals("POTWIERDZONA")?"confirmed":"finished";
                Button b = new Button(r == null ? (pole.moznaRezerwowac(dzis)?"+":"—") : (pole.konflikt()?"! ":"")+osoba(r.getGosc()));
                b.getStyleClass().addAll("week-slot","calendar-"+kolor); b.setMaxWidth(Double.MAX_VALUE); b.setMinWidth(0); b.setAlignment(Pos.CENTER_LEFT);
                b.setTooltip(new Tooltip(r==null?"Pokój " + pokoj.getNumer() + " • " + data:r.getGosciePobytu()+"\n"+r.getOd()+" → "+r.getDoDnia()));
                b.setDisable(r==null&&!pole.moznaRezerwowac(dzis));
                b.setOnAction(e -> { if(r != null) app.otworzRezerwacje(r); else app.ekranRezerwacji.formularzRezerwacji(pokoj.getId(),data); });
                siatka.add(b,dzien+1,row+1,span,1); dzien+=span;
            }
        }
        Button otworz = przycisk("Otwórz kalendarz →","text-button",() -> app.przejdz("Kalendarz")); otworz.setId("dashboard-calendar");
        Region gap=new Region(); HBox.setHgrow(gap,Priority.ALWAYS); HBox header=new HBox(10,etykieta("Najbliższe rezerwacje","section-title"),gap,otworz); header.setAlignment(Pos.CENTER_LEFT);
        ScrollPane scroll=new ScrollPane(siatka); scroll.setFitToWidth(true); scroll.setMinHeight(205); scroll.setPrefHeight(205); scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); scroll.getStyleClass().add("transparent-scroll");
        VBox karta=new VBox(12,header,scroll,etykieta("Najbliższe 7 nocy • pierwsze " + pokoje.size() + " z " + app.stan.pokoje.size() + " pokoi. Pełny plan w kalendarzu.","muted"));
        if(pokoje.isEmpty()) karta.getChildren().set(1,etykieta("Dodaj pokoje, aby zobaczyć plan rezerwacji.","muted"));
        karta.getStyleClass().add("card"); return karta;
    }
}
