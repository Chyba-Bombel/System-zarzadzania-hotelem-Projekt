package pl.hotel.gui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import static pl.hotel.gui.NarzedziaGUI.*;

final class Formularz {
        private final HotelGUI app;
        final Dialog<ButtonType> dialog = new Dialog<>();
        final GridPane grid = new GridPane();
        final Label error = etykieta("", "error-text");
        final ButtonType zapisz;
        int row;
        Formularz(HotelGUI app, String tytul, String opis, String przycisk) {
            this.app = app;
            dialog.setTitle(tytul); dialog.setHeaderText(opis); dialog.initOwner(app.stan.stage);
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/hotel.css").toExternalForm());
            dialog.setResizable(true); dialog.getDialogPane().setPrefWidth(590);
            zapisz = new ButtonType(przycisk, ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(zapisz,ButtonType.CANCEL);
            ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Anuluj");
            dialog.getDialogPane().lookupButton(zapisz).getStyleClass().add("primary");
            dialog.getDialogPane().lookupButton(zapisz).setId("form-save");
            grid.setHgap(18); grid.setVgap(12); grid.setPadding(new Insets(12));
            ColumnConstraints l = new ColumnConstraints(140), r = new ColumnConstraints(); r.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().addAll(l,r); error.setWrapText(true); error.setMaxWidth(520);
            VBox content = new VBox(8,grid,error); content.setPadding(new Insets(4));
            ScrollPane scroll = new ScrollPane(content); scroll.setFitToWidth(true); scroll.setPrefViewportHeight(390);
            dialog.getDialogPane().setContent(scroll);
            dialog.getDialogPane().disableProperty().bind(app.stan.busy);
            dialog.setOnCloseRequest(e -> { if (app.stan.busy.get()) e.consume(); });
            dialog.setOnHidden(e -> { if (app.stan.aktywnyDialog == dialog) app.stan.aktywnyDialog = null; });
        }
        void dodaj(String opis, Control pole) { pole.setMaxWidth(Double.MAX_VALUE); grid.add(etykieta(opis,"form-label"),0,row); grid.add(pole,1,row++); }
        TextField tekst(String opis, String wartosc) { TextField t = new TextField(wartosc); ogranicz(t,256); dodaj(opis,t); return t; }
        PasswordField haslo(String opis) { PasswordField p = new PasswordField(); ogranicz(p,128); dodaj(opis,p); return p; }
        void notatka(String tekst) { Label l = etykieta(tekst,"muted"); l.setWrapText(true); l.setMaxWidth(480); grid.add(l,0,row++,2,1); }
        void pokaz(Runnable zapis) {
            dialog.getDialogPane().lookupButton(zapisz).addEventFilter(javafx.event.ActionEvent.ACTION,e -> {
                e.consume(); error.setText("");
                try { zapis.run(); } catch (IllegalArgumentException ex) { error.setText(ex.getMessage()); }
            });
            app.stan.aktywnyDialog = dialog; dialog.show();
        }
        void zamknij() { dialog.close(); }
    }
