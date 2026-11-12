package pl.hotel.gui;
import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import pl.hotel.serwis.GuiRepository;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import static pl.hotel.gui.NarzedziaGUI.*;
final class KatalogUslugGUI {
    private final HotelGUI app;
    KatalogUslugGUI(HotelGUI app){this.app=app;}
    void pokaz(){
        TableView<UslugaHotelowa> t=new TableView<>();t.setId("catalog-table");t.setPrefHeight(210);
        kol(t,"Nazwa",230,UslugaHotelowa::nazwa);kol(t,"Jednostka",100,UslugaHotelowa::jednostka);kol(t,"Cena (zł)",100,UslugaHotelowa::cena);kol(t,"Aktywna",90,x->x.aktywna()?"TAK":"NIE");
        TextField nazwa=pole("Nazwa"),jednostka=pole("Jednostka, np. porcja"),cena=pole("Cena w zł");nazwa.setId("catalog-name");jednostka.setId("catalog-unit");cena.setId("catalog-price");
        CheckBox aktywna=new CheckBox("Aktywna");aktywna.setId("catalog-active");aktywna.setSelected(true);
        UslugaHotelowa[] stara={null};Label opis=new Label("Nowa usługa");
        t.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{if(b!=null){stara[0]=b;nazwa.setText(b.nazwa());jednostka.setText(b.jednostka());cena.setText(b.cena().toPlainString());aktywna.setSelected(b.aktywna());opis.setText("Edycja #"+b.id());}});
        Runnable nowa=()->{stara[0]=null;t.getSelectionModel().clearSelection();nazwa.clear();jednostka.clear();cena.clear();aktywna.setSelected(true);opis.setText("Nowa usługa");};
        Runnable czytaj=()->app.zadaniaGUI.uwierzytelnione(true,"Odczyt cennika",()->BazaDanych.pobierzKatalogUslug(true),lista->{t.getItems().setAll(lista);nowa.run();});
        Button zapisz=przycisk("Zapisz usługę","primary",()->{try{
            var kwota=GuiRepository.kwota(cena.getText());var poprzednia=stara[0];String n=nazwa.getText().trim(),j=jednostka.getText().trim();boolean a=aktywna.isSelected();
            app.zadaniaGUI.uwierzytelnione(true,"Zapis cennika",()->BazaDanych.zapiszUsluge(poprzednia,n,j,kwota,a),u->czytaj.run());
        }catch(IllegalArgumentException e){app.komunikatyGUI.blad(e.getMessage());}});zapisz.setId("catalog-save");
        Label uwaga=etykieta("Nieaktywna usługa znika z wyboru nowych pozycji. Zmiana cennika nie zmienia cen i nazw zapisanych wcześniej do pobytów.","muted");uwaga.setWrapText(true);uwaga.setMaxWidth(650);
        OknoDanych.pokaz(app,"Katalog usług dodatkowych","catalog-close",new VBox(10,t,opis,nazwa,pasek(jednostka,cena),aktywna,pasek(przycisk("Nowa","",nowa),zapisz,przycisk("Odśwież","",czytaj)),uwaga),710);czytaj.run();
    }
}
