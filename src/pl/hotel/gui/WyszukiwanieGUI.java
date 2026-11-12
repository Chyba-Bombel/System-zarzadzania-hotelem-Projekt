package pl.hotel.gui;
import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import static pl.hotel.gui.NarzedziaGUI.*;
final class WyszukiwanieGUI {
    private final HotelGUI app;
    WyszukiwanieGUI(HotelGUI app){this.app=app;}
    void pokaz(){
        TextField nazwisko=pole("Nazwisko także dodatkowego gościa"),pokoj=pole("Dokładny numer pokoju");nazwisko.setId("search-guest");pokoj.setId("search-room");
        ComboBox<String> status=wybor("Wszystkie statusy","POTWIERDZONA","ZAMELDOWANA","ZAKONCZONA","ANULOWANA");status.setId("search-status");
        DatePicker od=data(null),doDnia=data(null);od.setId("search-from");doDnia.setId("search-to");
        ComboBox<FiltrRezerwacji.TrybDat> tryb=new ComboBox<>();tryb.getItems().setAll(FiltrRezerwacji.TrybDat.values());tryb.getSelectionModel().selectFirst();tryb.setId("search-date-mode");
        tryb.setConverter(konwerter(x->switch(x){case POBYT->"Pobyt przecina okres";case PRZYJAZD->"Przyjazd w okresie";case WYJAZD->"Wyjazd w okresie";}));
        ComboBox<FiltrRezerwacji.StanRozliczenia> roz=new ComboBox<>();roz.getItems().setAll(FiltrRezerwacji.StanRozliczenia.values());roz.getSelectionModel().selectFirst();roz.setId("search-balance");
        roz.setConverter(konwerter(x->switch(x){case WSZYSTKIE->"Każde rozliczenie";case DO_ZAPLATY->"Do zapłaty";case NIEOPLACONA->"Bez wpłat netto";case CZESCIOWO_OPLACONA->"Częściowo opłacona";case ROZLICZONA->"Saldo zero";case NADPLATA->"Nadpłata / do zwrotu";}));
        TableView<WynikRezerwacji> t=new TableView<>();t.setId("search-table");t.setPrefHeight(280);
        kol(t,"ID",65,WynikRezerwacji::id);kol(t,"Główny gość",180,WynikRezerwacji::gosc);kol(t,"Pokój",85,WynikRezerwacji::pokoj);kol(t,"Przyjazd",110,WynikRezerwacji::od);kol(t,"Wyjazd",110,WynikRezerwacji::doDnia);kol(t,"Status",150,WynikRezerwacji::status);kol(t,"Należność",110,WynikRezerwacji::naleznosc);kol(t,"Netto",100,WynikRezerwacji::netto);kol(t,"Saldo",110,WynikRezerwacji::saldo);pusta(t,"Brak wyników dla wybranych filtrów.");
        Label opis=new Label();opis.setId("search-info");Button starsze=new Button("Starsze wyniki");starsze.setId("search-older");starsze.setDisable(true);
        FiltrRezerwacji[] filtr={null};long[] kursor={0};
        java.util.function.BiConsumer<FiltrRezerwacji,Long> czytaj=(f,c)->app.zadaniaGUI.uwierzytelnione(false,"Wyszukiwanie",()->BazaDanych.wyszukajRezerwacje(f,c),s->{
            filtr[0]=f;t.getSortOrder().clear();t.getItems().setAll(s.wyniki());starsze.setDisable(!s.maStarsze());kursor[0]=s.wyniki().isEmpty()?0:s.wyniki().get(s.wyniki().size()-1).id();opis.setText("Wyników na stronie: "+s.wyniki().size()+(s.maStarsze()?" • dostępne kolejne strony":" • koniec wyników"));
        });
        Runnable szukaj=()->{try{czytaj.accept(new FiltrRezerwacji(nazwisko.getText(),pokoj.getText(),status.getSelectionModel().getSelectedIndex()==0?"":status.getValue(),opcjonalna(od),opcjonalna(doDnia),tryb.getValue(),roz.getValue()),0L);}catch(IllegalArgumentException e){app.komunikatyGUI.blad(e.getMessage());}};
        Runnable zmiana=()->{filtr[0]=null;t.getItems().clear();starsze.setDisable(true);opis.setText("Zmieniono filtry. Kliknij Szukaj.");};
        nazwisko.textProperty().addListener((o,a,b)->zmiana.run());pokoj.textProperty().addListener((o,a,b)->zmiana.run());status.valueProperty().addListener((o,a,b)->zmiana.run());tryb.valueProperty().addListener((o,a,b)->zmiana.run());roz.valueProperty().addListener((o,a,b)->zmiana.run());od.getEditor().textProperty().addListener((o,a,b)->zmiana.run());doDnia.getEditor().textProperty().addListener((o,a,b)->zmiana.run());
        starsze.setOnAction(e->{if(filtr[0]!=null)czytaj.accept(filtr[0],kursor[0]);});
        Button wyszukaj=przycisk("Szukaj","primary",szukaj);wyszukaj.setId("search-run");
        Button reset=przycisk("Wyczyść filtry","",()->{nazwisko.clear();pokoj.clear();status.getSelectionModel().selectFirst();od.setValue(null);od.getEditor().clear();doDnia.setValue(null);doDnia.getEditor().clear();tryb.getSelectionModel().selectFirst();roz.getSelectionModel().selectFirst();szukaj.run();});reset.setId("search-reset");
        Button otworz=new Button("Otwórz rezerwację");otworz.setId("search-open");otworz.disableProperty().bind(t.getSelectionModel().selectedItemProperty().isNull());
        Label uwaga=etykieta("Daty graniczne są włączne. W trybie pobytu badamy noce; dzień wyjazdu nie jest noclegiem. Saldo obejmuje aktywne usługi. Saldo zero obejmuje rozliczone anulowania.","muted");uwaga.setWrapText(true);uwaga.setMaxWidth(930);
        VBox box=new VBox(10,new FlowPane(10,10,nazwisko,pokoj,status),new FlowPane(10,10,new Label("Od"),od,new Label("Do"),doDnia,tryb,roz),pasek(wyszukaj,reset),t,opis,pasek(starsze,otworz),uwaga);
        ScrollPane scroll=new ScrollPane(box);scroll.setFitToWidth(true);scroll.setPrefViewportHeight(540);
        var dialog=OknoDanych.pokaz(app,"Wyszukiwanie rezerwacji","search-close",scroll,1000);
        otworz.setOnAction(e->{var w=t.getSelectionModel().getSelectedItem();if(w==null)return;dialog.close();app.zadaniaGUI.odswiezPo("Otwarto wynik",()->{
            var r=app.stan.rezerwacje.stream().filter(x->x.getId()==w.id()).findFirst().orElse(null);if(r==null){app.komunikatyGUI.blad("Rezerwacja nie jest już dostępna.");return;}
            app.stan.szukajRezerwacji.clear();app.stan.statusRezerwacji.getSelectionModel().selectFirst();app.stan.strona="Rezerwacje";app.pokazPanel();app.stan.tabelaRezerwacji.getSelectionModel().select(r);app.stan.tabelaRezerwacji.scrollTo(r);
        });});szukaj.run();
    }
    private static LocalDate opcjonalna(DatePicker d){
        String s=d.getEditor().getText().trim();if(s.isEmpty())return null;
        try{return LocalDate.parse(s);}catch(java.time.format.DateTimeParseException e){throw new IllegalArgumentException("Podaj datę RRRR-MM-DD albo pozostaw pole puste.");}
    }
}
