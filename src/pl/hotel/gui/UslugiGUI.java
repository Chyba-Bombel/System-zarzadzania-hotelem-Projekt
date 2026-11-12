package pl.hotel.gui;
import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import static pl.hotel.gui.NarzedziaGUI.*;
final class UslugiGUI {
    private final HotelGUI app;
    private UslugiRezerwacji dane;
    private record Odczyt(UslugiRezerwacji pobyt,java.util.List<UslugaHotelowa> katalog){}
    UslugiGUI(HotelGUI app){this.app=app;}
    void pokaz(Rezerwacja r){
        TableView<PozycjaUslugi> t=new TableView<>();t.setId("services-table");t.setPrefHeight(250);
        kol(t,"ID",65,PozycjaUslugi::id);kol(t,"Usługa",200,PozycjaUslugi::nazwa);kol(t,"Ilość",65,PozycjaUslugi::ilosc);
        kol(t,"Jednostka",90,PozycjaUslugi::jednostka);kol(t,"Cena (zł)",100,PozycjaUslugi::cena);kol(t,"Wartość (zł)",110,PozycjaUslugi::wartosc);kol(t,"Status",110,PozycjaUslugi::status);
        pusta(t,"Brak usług tego pobytu.");
        ComboBox<UslugaHotelowa> u=new ComboBox<>();u.setId("services-catalog");u.setPrefWidth(400);u.setConverter(konwerter(x->x.nazwa()+" • "+zl(x.cena())+" / "+x.jednostka()));
        Spinner<Integer> ile=new Spinner<>(1,1000,1);ile.setId("services-quantity");ile.setPrefWidth(100);
        Label suma=new Label();suma.setId("services-balance");suma.setWrapText(true);
        Button dodaj=new Button("Dodaj usługę"),anuluj=new Button("Anuluj pozycję");dodaj.setId("services-add");anuluj.setId("services-cancel");dodaj.getStyleClass().add("primary");
        Runnable przyciski=()->{dodaj.setDisable(dane==null||!dane.edytowalne()||u.getValue()==null);var p=t.getSelectionModel().getSelectedItem();anuluj.setDisable(dane==null||!dane.edytowalne()||p==null||!p.aktywna());};
        t.getSelectionModel().selectedItemProperty().addListener((o,a,b)->przyciski.run());u.valueProperty().addListener((o,a,b)->przyciski.run());przyciski.run();
        Runnable wczytaj=()->app.zadaniaGUI.uwierzytelnione(false,"Odczyt usług",()->new Odczyt(BazaDanych.pobierzUslugiRezerwacji(r.getId()),BazaDanych.pobierzKatalogUslug(false)),d->{
            dane=d.pobyt();Long id=u.getValue()==null?null:u.getValue().id();t.getItems().setAll(dane.pozycje());u.getItems().setAll(d.katalog());
            if(id==null)u.getSelectionModel().selectFirst();else u.setValue(d.katalog().stream().filter(x->x.id()==id).findFirst().orElse(d.katalog().isEmpty()?null:d.katalog().get(0)));
            var roz=dane.rozliczenie();suma.setText("Usługi: "+zl(roz.uslugi())+" • Noclegi: "+zl(roz.cenaPobytu())+" • Należność: "+zl(roz.naleznosc())+" • Saldo: "+zl(roz.saldo())+" • "+roz.status());przyciski.run();
        });
        Runnable odswiez=()->app.zadaniaGUI.odswiezPo("Zapisano usługi.",()->{app.stan.rezerwacje.stream().filter(x->x.getId()==r.getId()).findFirst().ifPresent(x->app.stan.tabelaRezerwacji.getSelectionModel().select(x));wczytaj.run();});
        dodaj.setOnAction(e->{var wybrana=u.getValue();if(wybrana==null)return;int n=ile.getValue();long prac=app.stan.sesja.getId();app.zadaniaGUI.uwierzytelnione(false,"Dodawanie usługi",()->BazaDanych.dodajUslugeDoRezerwacji(r.getId(),wybrana,n,prac),v->odswiez.run());});
        anuluj.setOnAction(e->{var p=t.getSelectionModel().getSelectedItem();if(p==null||!app.komunikatyGUI.potwierdz("Anulowanie usługi",p.opis()+"\nAnulować pozycję?"))return;
            app.zadaniaGUI.uwierzytelnione(false,"Anulowanie usługi",()->{BazaDanych.anulujUslugeRezerwacji(r.getId(),p.id());return true;},v->odswiez.run());});
        Label uwaga=etykieta("Ilość dotyczy całego pobytu: 2 osoby × 3 śniadania = 6 porcji. Zmiana terminu nie zmienia ilości. Dostawka nie zwiększa pojemności pokoju. Obniżenie opłaconego rachunku może wymagać wcześniejszego zwrotu.","muted");uwaga.setWrapText(true);uwaga.setMaxWidth(850);
        OknoDanych.pokaz(app,"Usługi pobytu #"+r.getId(),"services-close",new VBox(10,t,suma,pasek(u,ile,dodaj),pasek(przycisk("Odśwież","",wczytaj),anuluj),uwaga),910);wczytaj.run();
    }
}
