package pl.hotel.gui;

import pl.hotel.pulpit.PodsumowaniePulpitu;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import static pl.hotel.gui.NarzedziaGUI.*;

final class EkranPulpitu {
    private final HotelGUI app;
    EkranPulpitu(HotelGUI app) { this.app = app; }

    Node pulpit() {
        PodsumowaniePulpitu p = PodsumowaniePulpitu.oblicz(app.stan.pokoje,app.stan.rezerwacje,LocalDate.now());
        TilePane liczby = new TilePane(14,14); liczby.setPrefColumns(4); liczby.setPrefTileHeight(Region.USE_COMPUTED_SIZE);
        liczby.prefTileWidthProperty().bind(Bindings.createDoubleBinding(() -> {
            double w=liczby.getWidth(); int cols=w>=920?4:2; return w<=0?230:Math.floor((w-14*(cols-1))/cols)-1;
        },liczby.widthProperty()));
        liczby.getChildren().addAll(
                KafelPulpituGUI.utworz("Zajętość hotelu",Math.round(p.zajetosc()*100)+"%",p.zajete()+" zameldowanych / "+p.dostepne()+" dostępnych pokoi","Zajętość",p.zajetosc()),
                KafelPulpituGUI.utworz("Przyjazdy dzisiaj",String.valueOf(p.przyjazdy().size()),"Potwierdzone pobyty do obsłużenia","przyjazd",null),
                KafelPulpituGUI.utworz("Wyjazdy do obsługi",String.valueOf(p.wyjazdy().size()),"Dzisiejsze i zaległe wymeldowania","wyjazd",null),
                KafelPulpituGUI.utworz("Do sprzątania",String.valueOf(p.sprzatanie()),"Niezajęte pokoje do przygotowania","sprzatanie",null));
        GridPane srodek = new GridPane(); srodek.setHgap(16); srodek.setVgap(16);
        ColumnConstraints a=new ColumnConstraints(),b=new ColumnConstraints(); a.setPercentWidth(67); b.setPercentWidth(33); srodek.getColumnConstraints().addAll(a,b);
        VBox pobyty=TabelaPobytowPulpituGUI.utworz(app,p), pokoje=StanPokoiPulpituGUI.utworz(app,p);
        srodek.add(pobyty,0,0); srodek.add(pokoje,1,0);
        srodek.widthProperty().addListener((o,stare,nowe) -> {
            boolean wasko=nowe.doubleValue()<900;
            GridPane.setColumnSpan(pobyty,wasko?2:1); GridPane.setColumnIndex(pokoje,wasko?0:1); GridPane.setRowIndex(pokoje,wasko?1:0); GridPane.setColumnSpan(pokoje,wasko?2:1);
        });
        String imie=app.stan.sesja==null?"":", "+app.stan.sesja.getImie();
        VBox page=app.strona("Dzień dobry"+imie,"Oto dzisiejsza sytuacja w hotelu.",liczby,srodek,new PodgladKalendarzaGUI(app).utworz());
        Button nowa=przycisk("Nowa rezerwacja","primary",app.ekranRezerwacji::formularzRezerwacji); nowa.setGraphic(IkonyGUI.ikona("plus")); nowa.setId("dashboard-new-booking");
        Label data=etykieta(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy",Locale.forLanguageTag("pl-PL"))),"muted");
        data.visibleProperty().bind(page.widthProperty().greaterThan(1100)); data.managedProperty().bind(data.visibleProperty());
        HBox header=(HBox)page.getChildren().get(0); header.getChildren().addAll(data,nowa); header.setAlignment(Pos.CENTER_LEFT);
        return page;
    }
}
