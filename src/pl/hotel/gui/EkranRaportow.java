package pl.hotel.gui;

import pl.hotel.raporty.Raporty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import static pl.hotel.gui.NarzedziaGUI.*;

/** Kontroler: EkranRaportow. */
final class EkranRaportow {
    private final HotelGUI app;
    EkranRaportow(HotelGUI app) { this.app = app; }

    Node stronaRaportow() {
        DatePicker od = data(LocalDate.now().withDayOfMonth(1)), koniec = data(LocalDate.now());
        od.setId("report-from"); koniec.setId("report-to");
        TabPane zakladki = new TabPane(); zakladki.setId("report-tabs");
        Label opis = etykieta("Wybierz okres i kliknij „Generuj raport”. Daty obejmują oba wybrane dni.", "muted");
        opis.setWrapText(true);
        Raporty.Raport[] wynik = {null};
        Button csv = przycisk("Zapisz tabelę CSV", "", null), html = przycisk("Zapisz cały raport HTML / PDF", "", null);
        csv.setId("report-csv"); html.setId("report-html"); csv.setDisable(true); html.setDisable(true);
        Runnable uniewaznij = () -> {
            if (wynik[0] == null) return;
            wynik[0] = null; zakladki.getTabs().clear(); csv.setDisable(true); html.setDisable(true);
            opis.setText("Zmieniono okres. Wygeneruj raport dla nowych dat.");
        };
        od.getEditor().textProperty().addListener((o,a,b) -> uniewaznij.run());
        koniec.getEditor().textProperty().addListener((o,a,b) -> uniewaznij.run());
        Button generuj = przycisk("Generuj raport", "primary", () -> {
            try {
                LocalDate a=dataZ(od), b=dataZ(koniec);
                app.zadaniaGUI.uwierzytelnione(false,"Generowanie raportu",() -> Raporty.generuj(a,b), r -> {
                    wynik[0]=r; zakladki.getTabs().clear();
                    for (Raporty.Tabela dane:r.tabele()) {
                        TableView<java.util.List<String>> t = new TableView<>(FXCollections.observableArrayList(dane.wiersze()));
                        for (int i=0;i<dane.kolumny().size();i++) {
                            final int k=i; double szerokosc=dane.kolumny().size()==2 ? (i==0?470:420) : (i==1||i==2?220:150);
                            kol(t,dane.kolumny().get(i),szerokosc,w -> w.get(k));
                        }
                        pusta(t,"Brak danych w wybranym okresie.");
                        Tab tab=new Tab(dane.nazwa(),t); tab.setClosable(false); zakladki.getTabs().add(tab);
                    }
                    csv.setDisable(false); html.setDisable(false);
                    opis.setText("Raport: " + r.od() + " — " + r.doDnia() + " • Wygenerowano: "
                            + r.utworzono().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            + ". CSV eksportuje wybraną zakładkę, HTML zawiera cały raport.");
                    app.stan.status.setText("Raport gotowy • " + godzina());
                });
            } catch (IllegalArgumentException ex) { app.komunikatyGUI.blad(ex.getMessage()); }
        }); generuj.setId("report-generate");
        csv.setOnAction(e -> {
            var r=wynik[0]; int i=zakladki.getSelectionModel().getSelectedIndex();
            if(r!=null && i>=0) eksportRaportu(r,i,false);
        });
        html.setOnAction(e -> { if(wynik[0]!=null) eksportRaportu(wynik[0],0,true); });
        Label zasady=etykieta(Raporty.ZASADY,"muted"); zasady.setWrapText(true);
        VBox page = app.strona("Raporty", "Zestawienia pobytów i transakcji dla wybranego okresu.",
                pasek(new Label("Od"),od,new Label("Do (włącznie)"),koniec,generuj), opis,
                zakladki,pasek(csv,html),zasady);
        VBox.setVgrow(zakladki,Priority.ALWAYS); return page;
    }

    void eksportRaportu(Raporty.Raport r,int tabela,boolean html) {
        javafx.stage.FileChooser wybor = new javafx.stage.FileChooser();
        wybor.setTitle(html?"Zapisz cały raport":"Zapisz tabelę CSV");
        String rozszerzenie=html?"html":"csv";
        wybor.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(
                html?"Raport HTML (*.html)":"Tabela CSV (*.csv)","*."+rozszerzenie));
        wybor.setInitialFileName("raport_"+r.od()+"_"+r.doDnia()+(html?"":"_"+tabela)+"."+rozszerzenie);
        java.io.File plik=wybor.showSaveDialog(app.stan.stage); if(plik==null)return;
        java.nio.file.Path cel=plik.toPath();
        if(!cel.toString().toLowerCase(Locale.ROOT).endsWith("."+rozszerzenie))
            cel=java.nio.file.Path.of(cel+"."+rozszerzenie);
        if(java.nio.file.Files.exists(cel) && !app.komunikatyGUI.potwierdz("Zastąpienie pliku","Zastąpić istniejący plik?\n"+cel)) return;
        final java.nio.file.Path docelowy=cel;
        app.zadaniaGUI.uwierzytelnione(false,"Zapisywanie raportu",() -> {
            if(html) Raporty.zapiszHtml(r,docelowy); else Raporty.zapiszCsv(r.tabele().get(tabela),docelowy);
            return docelowy;
        }, zapis -> {
            app.stan.status.setText("Zapisano raport: "+zapis);
            Alert a=new Alert(Alert.AlertType.INFORMATION); a.initOwner(app.stan.stage); a.setTitle("Raport zapisany");
            a.setHeaderText("Plik jest gotowy");
            a.setContentText(zapis+(html?"\n\nOtwórz plik w przeglądarce. Przycisk „Drukuj / zapisz jako PDF” otworzy drukowanie — wybierz zapis do PDF.":"\n\nOtwórz CSV w Excelu lub LibreOffice. Separator kolumn: średnik; kodowanie: UTF-8."));
            a.showAndWait();
        });
    }
}
