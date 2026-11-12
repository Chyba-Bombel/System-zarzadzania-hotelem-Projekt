package pl.hotel.gui;

import pl.hotel.model.*;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import static pl.hotel.gui.NarzedziaGUI.*;

final class TabeleGUI {
    private final HotelGUI app;
    TabeleGUI(HotelGUI app) { this.app = app; }

    void konfigurujTabele() {
        app.stan.tabelaGosci.setId("guests-table"); app.stan.tabelaPokoi.setId("rooms-table"); app.stan.tabelaRezerwacji.setId("bookings-table");
        app.stan.tabelaKont.setId("accounts-table"); app.stan.tabelaRozliczen.setId("balances-table"); app.stan.tabelaPlatnosci.setId("payments-table");
        kol(app.stan.tabelaGosci,"ID",60,Gosc::getId); kol(app.stan.tabelaGosci,"Imię",140,Gosc::getImie);
        kol(app.stan.tabelaGosci,"Nazwisko",180,Gosc::getNazwisko); kol(app.stan.tabelaGosci,"Telefon",180,Gosc::getTelefon); kol(app.stan.tabelaGosci,"E-mail",260,Gosc::getEmail);
        kol(app.stan.tabelaGosci,"Kartoteka",110,g->g.isArchiwalny()?"ARCHIWALNA":"AKTYWNA");
        kol(app.stan.tabelaGosci,"Archiwizowano",180,Gosc::getArchiwizowano);
        kol(app.stan.tabelaGosci,"Archiwizował",140,Gosc::getArchiwizowalLogin);
        kol(app.stan.tabelaPokoi,"ID",60,Pokoj::getId); kol(app.stan.tabelaPokoi,"Numer",100,Pokoj::getNumer);
        kol(app.stan.tabelaPokoi,"Typ",180,p -> app.stan.typy.getOrDefault(p.getId(),"")); kol(app.stan.tabelaPokoi,"Miejsca",90,Pokoj::getLiczbaMiejsc);
        kol(app.stan.tabelaPokoi,"Cena za noc (zł)",150,Pokoj::getCenaZaNoc); kol(app.stan.tabelaPokoi,"Stan",180,Pokoj::getStan);
        tabelaRezerwacji(app.stan.tabelaRezerwacji); tabelaRezerwacji(app.stan.tabelaRozliczen);
        kol(app.stan.tabelaKont,"ID",60,KontoPracownika::id); kol(app.stan.tabelaKont,"Imię",130,KontoPracownika::imie);
        kol(app.stan.tabelaKont,"Nazwisko",170,KontoPracownika::nazwisko); kol(app.stan.tabelaKont,"Login",150,KontoPracownika::login);
        kol(app.stan.tabelaKont,"Rola",130,KontoPracownika::rola); kol(app.stan.tabelaKont,"Konto",130,k -> k.aktywny() ? "AKTYWNE" : "NIEAKTYWNE");
        kol(app.stan.tabelaPlatnosci,"ID",65,Platnosc::id); kol(app.stan.tabelaPlatnosci,"Data i czas",200,p -> p.data().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        kol(app.stan.tabelaPlatnosci,"Rodzaj",145,Platnosc::rodzaj); kol(app.stan.tabelaPlatnosci,"Kwota (zł)",145,Platnosc::kwota); kol(app.stan.tabelaPlatnosci,"Metoda",150,Platnosc::metoda);
        podlacz(app.stan.tabelaGosci,app.stan.widoczniGoscie); podlacz(app.stan.tabelaPokoi,app.stan.widocznePokoje); podlacz(app.stan.tabelaRezerwacji,app.stan.widoczneRezerwacje);
        podlacz(app.stan.tabelaKont,app.stan.widoczneKonta);
        SortedList<Rezerwacja> salda = new SortedList<>(app.stan.rezerwacje); salda.comparatorProperty().bind(app.stan.tabelaRozliczen.comparatorProperty()); app.stan.tabelaRozliczen.setItems(salda);
        pusta(app.stan.tabelaGosci,"Brak gości pasujących do wyszukiwania."); pusta(app.stan.tabelaPokoi,"Brak pokoi pasujących do filtrów.");
        pusta(app.stan.tabelaRezerwacji,"Brak rezerwacji pasujących do filtrów."); pusta(app.stan.tabelaKont,"Brak kont.");
        pusta(app.stan.tabelaRozliczen,"Brak rezerwacji."); pusta(app.stan.tabelaPlatnosci,"Brak płatności lub nie wybrano rozliczenia.");
        for (TextField t : new TextField[]{app.stan.szukajGoscia,app.stan.szukajPokoju,app.stan.szukajRezerwacji,app.stan.szukajKonta})
            t.textProperty().addListener((o,a,b) -> filtruj());
        app.stan.stanGoscia.setId("guest-state");
        app.stan.stanGoscia.valueProperty().addListener((o,a,b)->filtruj());
        app.stan.stanPokoju.valueProperty().addListener((o,a,b) -> filtruj()); app.stan.statusRezerwacji.valueProperty().addListener((o,a,b) -> filtruj());
        app.stan.tabelaRozliczen.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> {
            app.stan.tabelaPlatnosci.getItems().clear(); app.stan.szczegoly.setText("Kliknij „Pokaż rozliczenie”, aby pobrać aktualne saldo.");
        });
    }

    void tabelaRezerwacji(TableView<Rezerwacja> t) {
        kol(t,"ID",60,Rezerwacja::getId); kol(t,"Goście pobytu",270,Rezerwacja::getGosciePobytu);
        kol(t,"Pokój",75,r -> r.getPokoj().getNumer()); kol(t,"Przyjazd",115,Rezerwacja::getOd);
        kol(t,"Wyjazd",115,Rezerwacja::getDoDnia); kol(t,"Osoby",65,Rezerwacja::getLiczbaOsob);
        kol(t,"Razem (zł)",110,Rezerwacja::obliczCene); kol(t,"Saldo (zł)",110,Rezerwacja::getSaldo); kol(t,"Status",165,Rezerwacja::getStatus);
    }

    void filtruj() {
        app.stan.widoczniGoscie.setPredicate(g -> pasuje(app.stan.szukajGoscia.getText(), osoba(g) + " " + g.getTelefon() + " " + g.getEmail() + " " + g.getId())
                && (app.stan.stanGoscia.getSelectionModel().getSelectedIndex()==2 || g.isArchiwalny()==(app.stan.stanGoscia.getSelectionModel().getSelectedIndex()==1)));
        app.stan.widoczneRezerwacje.setPredicate(r -> pasuje(app.stan.szukajRezerwacji.getText(), r.getId() + " " + r.getGosciePobytu() + " " + r.getPokoj().getNumer())
                && (app.stan.statusRezerwacji.getValue() == null || app.stan.statusRezerwacji.getValue().equals("Wszystkie statusy") || r.getStatus().equals(app.stan.statusRezerwacji.getValue())));
        app.stan.widoczneKonta.setPredicate(k -> pasuje(app.stan.szukajKonta.getText(), k.id() + " " + k.imie() + " " + k.nazwisko() + " " + k.login()));

        LocalDate od = app.stan.filtrOd, koniec = app.stan.filtrDo; int osoby = app.stan.filtrOsoby;
        app.stan.widocznePokoje.setPredicate(p -> {
            if (!pasuje(app.stan.szukajPokoju.getText(), p.getNumer() + " " + app.stan.typy.getOrDefault(p.getId(),""))) return false;
            if (app.stan.stanPokoju.getValue() != null && !app.stan.stanPokoju.getValue().equals("Wszystkie stany") && !p.getStan().equals(app.stan.stanPokoju.getValue())) return false;
            if (!app.stan.filtrTerminu) return true;
            return od != null && koniec != null && !p.getStan().equals("WYLACZONY") && p.getLiczbaMiejsc() >= osoby
                    && app.stan.rezerwacje.stream().noneMatch(r -> r.kolidacja(p.getNumer(),od,koniec));
        });
        app.stan.filtrInfo.setText(app.stan.filtrTerminu ? "Wolne w terminie " + od + " → " + koniec + " • osób: " + osoby
                : "Bez filtra terminu • wszystkie pokoje zgodne z wyszukiwaniem.");
    }
}
