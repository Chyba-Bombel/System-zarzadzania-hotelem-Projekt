package pl.hotel.gui;
import javafx.scene.Node;
import javafx.scene.control.*;
final class OknoDanych {
    private OknoDanych(){}
    static Dialog<ButtonType> pokaz(HotelGUI app,String tytul,String id,Node tresc,double szerokosc){
        Dialog<ButtonType> d=new Dialog<>();d.setTitle(tytul);d.setHeaderText(tytul);d.initOwner(app.stan.stage);d.setResizable(true);
        d.getDialogPane().getStylesheets().add(OknoDanych.class.getResource("/hotel.css").toExternalForm());
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button b=(Button)d.getDialogPane().lookupButton(ButtonType.CLOSE);b.setText("Zamknij");b.setId(id);
        d.getDialogPane().setContent(tresc);d.getDialogPane().setPrefWidth(szerokosc);d.getDialogPane().disableProperty().bind(app.stan.busy);
        d.setOnCloseRequest(e->{if(app.stan.busy.get())e.consume();});d.setOnHidden(e->{if(app.stan.aktywnyDialog==d)app.stan.aktywnyDialog=null;});
        app.stan.aktywnyDialog=d;d.show();return d;
    }
}
