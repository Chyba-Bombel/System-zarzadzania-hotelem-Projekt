import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import pl.hotel.gui.HotelGUI;
import pl.hotel.serwis.GuiRepository;
import pl.hotel.raporty.Raporty;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javax.imageio.ImageIO;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

public class GuiSmoke extends Application {
    static HotelGUI app;
    static Stage okno;
    @Override public void start(Stage stage) {
        try {
            app = new HotelGUI(); okno = stage; app.start(stage);
            new Thread(() -> {
                try { scenariusz(); System.out.println("GUI_SMOKE_OK"); Platform.runLater(() -> { app.stop(); Platform.exit(); }); }
                catch (Throwable e) { e.printStackTrace(); System.exit(2); }
            },"test-ui").start();
        } catch (Throwable e) { e.printStackTrace(); System.exit(3); }
    }
    static Object pole(String nazwa) throws Exception {
        Field stan = HotelGUI.class.getDeclaredField("stan"); stan.setAccessible(true);
        Object dane = stan.get(app);
        Field f = dane.getClass().getDeclaredField(nazwa); f.setAccessible(true); return f.get(dane);
    }
    static <T> T fx(Callable<T> c) throws Exception { FutureTask<T> t = new FutureTask<>(c); Platform.runLater(t); return t.get(15,TimeUnit.SECONDS); }
    static void czekaj() throws Exception {
        for (int i=0;i<200;i++) {
            boolean gotowe = fx(() -> !((SimpleBooleanProperty)pole("busy")).get());
            if (gotowe) { Thread.sleep(120); if(fx(() -> !((SimpleBooleanProperty)pole("busy")).get())) return; }
            Thread.sleep(60);
        }
        throw new AssertionError("Przekroczono czas operacji GUI");
    }
    static Node node(String id) {
        for (Window w : Window.getWindows()) if (w.isShowing() && w.getScene()!=null) {
            Node n = w.getScene().lookup("#"+id); if(n!=null) return n;
        }
        throw new AssertionError("Brak kontrolki "+id);
    }
    static void klik(String id) throws Exception { fx(() -> { ((Button)node(id)).fire(); return null; }); }
    static void wpisz(String id,String tekst) throws Exception { fx(() -> { ((TextInputControl)node(id)).setText(tekst); return null; }); }
    static void screen(String nazwa) throws Exception {
        fx(() -> {
            okno.getScene().getRoot().applyCss(); okno.getScene().getRoot().layout();
            Path p=Path.of(System.getProperty("hotel.screens","../tmp/gui-screens"));Files.createDirectories(p);
            var d=(Dialog<?>)pole("aktywnyDialog");
            var scena=d!=null && d.isShowing() ? d.getDialogPane().getScene() : okno.getScene();
            ImageIO.write(SwingFXUtils.fromFXImage(scena.snapshot(null),null),"png",p.resolve(nazwa+".png").toFile()); return null;
        });
    }
    static void loguj(String login,String haslo) throws Exception { wpisz("login",login);wpisz("password",haslo);klik("login-button");czekaj(); }
    static void zamknijBladNotatki() throws Exception {
        for(int i=0;i<100;i++) {
            boolean zamknieto=fx(() -> {
                for(Window w:java.util.List.copyOf(Window.getWindows())) {
                    if(w.isShowing() && w!=okno && w.getScene().getRoot() instanceof DialogPane d
                            && d.lookup("#notes-editor")==null && d.getButtonTypes().contains(ButtonType.OK)) {
                        ((Button)d.lookupButton(ButtonType.OK)).fire();return true;
                    }
                }
                return false;
            });
            if(zamknieto)return;
            Thread.sleep(100);
        }
        throw new AssertionError("Brak komunikatu konfliktu notatki");
    }
    static void scenariusz() throws Exception {
        screen("01-logowanie"); loguj("admin","Admin123!");
        if(fx(() -> pole("sesja"))==null)throw new AssertionError("Logowanie nieudane");
        screen("02-pulpit");
        for(String p:new String[]{"goscie","pokoje","rezerwacje","kalendarz","platnosci","raporty","pracownicy","moje konto"}){
            klik("nav-"+p.replace(' ','-')); screen("03-"+p.replace(' ','-'));
        }
        klik("nav-goscie"); klik("add-guest"); klik("form-save");
        if(fx(() -> pole("aktywnyDialog"))==null)throw new AssertionError("Pusty formularz został zapisany");
        wpisz("guest-first-name","GUI");wpisz("guest-last-name","Testowy");wpisz("guest-phone","500123456");wpisz("guest-email","gui@example.com");
        klik("form-save");czekaj();
        if(fx(() -> pole("aktywnyDialog"))!=null)throw new AssertionError("Formularz nie zamknął się po zapisie");
        BazaDanych.zaloguj("admin","Admin123!");
        if(BazaDanych.pobierzGosci().stream().noneMatch(g -> g.getEmail().equals("gui@example.com")))throw new AssertionError("Brak zapisu gościa");
        klik("nav-rezerwacje"); klik("add-booking");czekaj();
        fx(() -> {
            ((ComboBox<?>)node("booking-room")).getSelectionModel().select(3);
            ((Spinner<Integer>)node("booking-people")).getValueFactory().setValue(2); return null;
        });
        klik("form-save");
        if(fx(() -> pole("aktywnyDialog"))==null)throw new AssertionError("Przyjęto puste nazwiska");
        wpisz("participant-first-1","Błażej"); wpisz("participant-last-1","Wencel");

        fx(() -> { ((Spinner<Integer>)node("booking-people")).getValueFactory().setValue(1);
            ((Spinner<Integer>)node("booking-people")).getValueFactory().setValue(2); return null; });
        if(!fx(() -> ((TextField)node("participant-first-1")).getText()).equals("Błażej"))throw new AssertionError("Utrata wpisów");
        screen("04-formularz-rezerwacji");

        klik("form-save");czekaj();
        if(fx(() -> pole("aktywnyDialog"))!=null)throw new AssertionError("Rezerwacja nie została zapisana");
        BazaDanych.zaloguj("admin","Admin123!");
        var zapisane = BazaDanych.pobierzRezerwacje(BazaDanych.pobierzGosci(),BazaDanych.pobierzPokoje());
        var ostatnia=zapisane.get(zapisane.size()-1);
        if(!ostatnia.getUczestnicy().equals(java.util.List.of(new Uczestnik("Błażej","Wencel"))))throw new AssertionError("Brak trwałego zapisu uczestnika z GUI");
        fx(() -> { ((TableView<?>)node("bookings-table")).getSelectionModel().selectLast(); return null; });
        klik("booking-participants");
        if(!fx(() -> ((TextField)node("participant-last-1")).getText()).equals("Wencel"))throw new AssertionError("Brak danych w edycji");
        wpisz("participant-first-1","Anna"); wpisz("participant-last-1","Pruska");
        klik("form-save");czekaj();
        var edytowane=BazaDanych.pobierzRezerwacje(BazaDanych.pobierzGosci(),BazaDanych.pobierzPokoje()).stream()
            .filter(r->r.getId()==ostatnia.getId()).findFirst().orElseThrow();
        if(!edytowane.getUczestnicy().get(0).nazwisko().equals("Pruska"))throw new AssertionError("Edycja GUI nie zapisana");
        fx(() -> { ((TableView<?>)node("bookings-table")).getSelectionModel().selectLast(); return null; });
        klik("booking-change-room"); czekaj();
        long nowyPokoj = fx(() -> {
            ComboBox<Pokoj> wybor = (ComboBox<Pokoj>)node("change-room-target");
            Pokoj wolny = wybor.getItems().stream().filter(p -> p.getNumer().equals("106")).findFirst().orElseThrow();
            wybor.getSelectionModel().select(wolny); return wolny.getId();
        });
        screen("04a-zmiana-pokoju");
        klik("form-save"); czekaj();
        if (fx(() -> pole("aktywnyDialog")) != null) throw new AssertionError("Formularz zmiany pokoju nie zamknął się");
        var przeniesiona = IntegrationProbe.rezerwacja(ostatnia.getId());
        if (przeniesiona.getPokoj().getId() != nowyPokoj || !przeniesiona.obliczCene().equals(ostatnia.obliczCene())
                || !przeniesiona.getUczestnicy().get(0).nazwisko().equals("Pruska"))
            throw new AssertionError("GUI zmiany pokoju: pokój, cena lub uczestnicy");
        klik("nav-kalendarz");
        screen("04b-kalendarz-po-zmianie");
        klik("calendar-cell-" + nowyPokoj + "-" + java.time.LocalDate.now());
        if (!fx(() -> ((Label)node("calendar-details")).getText()).contains("Rezerwacja #" + ostatnia.getId()))
            throw new AssertionError("Kalendarz nie odświeżył pokoju po przeniesieniu");
        klik("nav-rezerwacje");
        fx(() -> { ((TableView<?>)node("bookings-table")).getSelectionModel().selectLast(); return null; });
        klik("booking-history"); czekaj();
        int ileWpisow=fx(() -> ((TableView<?>)node("history-table")).getItems().size());
        if(ileWpisow!=2)throw new AssertionError("Historia GUI: oczekiwano utworzenia i zmiany pokoju, otrzymano "+ileWpisow);
        if(!fx(() -> ((TextArea)node("history-details")).getText()).contains("106"))throw new AssertionError("Brak szczegółów zmiany pokoju w historii");
        if(!fx(() -> ((Button)node("history-older")).isDisabled()))throw new AssertionError("Starsze wpisy aktywne bez starszej strony");
        screen("04c-historia-rezerwacji");
        klik("history-refresh"); czekaj(); klik("history-close");
        if(fx(() -> pole("aktywnyDialog"))!=null)throw new AssertionError("Historia nie zamknęła się");
        klik("booking-confirmation"); czekaj();
        var danePobytu=fx(() -> ((TableView<java.util.List<String>>)node("confirmation-stay")).getItems());
        if(danePobytu.stream().noneMatch(w->w.get(0).equals("Pokój")&&w.get(1).contains("106")))throw new AssertionError("Nieaktualny pokój w potwierdzeniu");
        var gosciePobytu=fx(() -> ((ListView<String>)node("confirmation-guests")).getItems());
        if(gosciePobytu.stream().noneMatch(g->g.equals("Anna Pruska")))throw new AssertionError("Brak uczestnika w potwierdzeniu");
        if(fx(() -> ((Button)node("confirmation-save")).isDisabled()))throw new AssertionError("Eksport poprawnego potwierdzenia nieaktywny");
        screen("04d-potwierdzenie-pobyt");
        BazaDanych.zapiszWplate(ostatnia.getId(),new java.math.BigDecimal("50"),"KARTA",1);
        klik("confirmation-refresh"); czekaj();
        var kwoty=fx(() -> ((TableView<java.util.List<String>>)node("confirmation-balance")).getItems());
        if(kwoty.stream().noneMatch(w->w.get(0).equals("Wpłaty")&&w.get(1).equals("50,00 zł")))throw new AssertionError("Brak wpłaty po odświeżeniu potwierdzenia");
        fx(() -> { ((TabPane)node("confirmation-tabs")).getSelectionModel().select(2); return null; });
        screen("04e-potwierdzenie-rozliczenie");
        klik("confirmation-close");
        if(fx(() -> pole("aktywnyDialog"))!=null)throw new AssertionError("Potwierdzenie nie zamknęło się");
        klik("booking-notes"); czekaj();
        if(!fx(() -> ((TextArea)node("notes-editor")).isEditable()))throw new AssertionError("Notatka aktywnego pobytu nieedytowalna");
        wpisz("notes-editor","Późny przyjazd o 23:00\nProśba o ciszę");
        wpisz("notes-editor","a".repeat(501));
        if(fx(() -> ((TextArea)node("notes-editor")).getText().length())>500)throw new AssertionError("Brak limitu pola notatki");
        String pierwsza="Późny przyjazd o 23:00\nProśba o ciszę";
        wpisz("notes-editor",pierwsza); klik("notes-save"); czekaj();
        if(!BazaDanych.pobierzNotatkeRezerwacji(ostatnia.getId()).tresc().equals(pierwsza))throw new AssertionError("Brak zapisu notatki z GUI");
        wpisz("notes-editor","Mój szkic");
        BazaDanych.zapiszNotatkeRezerwacji(ostatnia.getId(),pierwsza,"Zmiana z innego stanowiska");
        klik("notes-save"); zamknijBladNotatki(); czekaj();
        if(!fx(() -> ((TextArea)node("notes-editor")).getText()).equals("Mój szkic"))throw new AssertionError("Konflikt usuwa szkic notatki");
        if(!BazaDanych.pobierzNotatkeRezerwacji(ostatnia.getId()).tresc().equals("Zmiana z innego stanowiska"))throw new AssertionError("Konflikt nadpisuje nowszą notatkę");
        klik("notes-refresh"); czekaj();
        if(!fx(() -> ((TextArea)node("notes-current")).getText()).equals("Zmiana z innego stanowiska")
                || !fx(() -> ((TextArea)node("notes-editor")).getText()).equals("Mój szkic"))throw new AssertionError("Odświeżenie nie zachowuje szkicu i aktualnej treści");
        String scalona="Zmiana z innego stanowiska\nMój szkic";
        wpisz("notes-editor",scalona); klik("notes-save"); czekaj();
        screen("04f-notatka-recepcji");
        wpisz("notes-editor","Bez zapisu"); klik("notes-close");
        klik("booking-notes"); czekaj();
        if(!fx(() -> ((TextArea)node("notes-editor")).getText()).equals(scalona))throw new AssertionError("Zamknięcie zapisuje szkic bez polecenia");
        klik("notes-close");
        fx(() -> {
            TableView<Rezerwacja> t=(TableView<Rezerwacja>)node("bookings-table");
            t.getSelectionModel().select(t.getItems().stream().filter(x->x.getId()==1).findFirst().orElseThrow()); return null;
        });
        klik("booking-notes"); czekaj();
        if(fx(() -> ((TextArea)node("notes-editor")).isEditable()) || !fx(() -> ((Button)node("notes-save")).isDisabled()))
            throw new AssertionError("Notatka anulowanej rezerwacji edytowalna w GUI");
        screen("04g-notatka-archiwalna"); klik("notes-close");
        System.out.println("GUI_NOTES_OK");
        System.out.println("GUI_CONFIRMATION_OK");
        System.out.println("GUI_HISTORY_OK");
        System.out.println("GUI_ROOM_CHANGE_OK");
        klik("nav-platnosci");
        fx(() -> { TableView<?> t=(TableView<?>)node("balances-table");t.getSelectionModel().selectLast();return null; });
        klik("show-balance");czekaj();screen("05-rozliczenie");
        klik("nav-raporty");
        if(!fx(() -> ((Button)node("report-csv")).isDisabled()))throw new AssertionError("Eksport bez raportu aktywny");
        klik("report-generate");czekaj();
        if(fx(() -> ((TabPane)node("report-tabs")).getTabs().size())!=4)throw new AssertionError("Brak zakładek raportu");
        if(fx(() -> ((Button)node("report-html")).isDisabled()))throw new AssertionError("Eksport po generowaniu nieaktywny");
        screen("07-raport-podsumowanie");
        fx(() -> { ((TabPane)node("report-tabs")).getSelectionModel().select(1);return null; });
        screen("08-raport-rezerwacje");
        fx(() -> { ((DatePicker)node("report-from")).getEditor().setText(java.time.LocalDate.now().minusDays(1).toString());return null; });
        if(!fx(() -> ((Button)node("report-csv")).isDisabled()))throw new AssertionError("Zmiana okresu nie unieważniła raportu");

        klik("nav-kalendarz");
        screen("09-kalendarz");
        int dni=java.time.YearMonth.now().lengthOfMonth();
        if(fx(() -> ((TableView<?>)node("calendar-table")).getColumns().size())!=dni+1)throw new AssertionError("Zła liczba dni miesiąca");
        klik("calendar-next");
        if(fx(() -> ((TableView<?>)node("calendar-table")).getColumns().size())!=java.time.YearMonth.now().plusMonths(1).lengthOfMonth()+1)
            throw new AssertionError("Zmiana miesiąca nie przebudowała kolumn");
        klik("calendar-today");

        screen("10-kalendarz-dzisiaj");
        klik("calendar-cell-3-"+java.time.LocalDate.now());
        if(!fx(() -> ((Label)node("calendar-details")).getText()).contains("Rezerwacja #3"))throw new AssertionError("Brak szczegółów rezerwacji");
        klik("calendar-open");
        if(fx(() -> ((TableView<?>)node("bookings-table")).getSelectionModel().getSelectedItem())==null)throw new AssertionError("Brak zaznaczenia rezerwacji po nawigacji");
        klik("nav-kalendarz");
        wpisz("calendar-search","101 Standard");
        if(fx(() -> ((TableView<?>)node("calendar-table")).getItems().size())!=1)throw new AssertionError("Filtr pokoju nie działa");
        screen("11-kalendarz-filtr");
        klik("calendar-cell-1-"+java.time.LocalDate.now());czekaj();
        if(!fx(() -> ((DatePicker)node("booking-from")).getValue()).equals(java.time.LocalDate.now()))throw new AssertionError("Brak daty z kalendarza");
        if(fx(() -> ((Pokoj)((ComboBox<?>)node("booking-room")).getValue()).getId())!=1L)throw new AssertionError("Brak pokoju z kalendarza");
        klik("form-save");czekaj();
        var poKalendarzu=BazaDanych.pobierzRezerwacje(BazaDanych.pobierzGosci(),BazaDanych.pobierzPokoje());
        if(poKalendarzu.stream().noneMatch(r->r.getPokoj().getId()==1&&r.getOd().equals(java.time.LocalDate.now())&&r.getStatus().equals("POTWIERDZONA")))
            throw new AssertionError("Rezerwacja z kalendarza nie zapisana");
        screen("12-kalendarz-zapis");

        fx(() -> { var m=HotelGUI.class.getDeclaredMethod("pokazLogowanie");m.setAccessible(true);m.invoke(app);return null; });
        loguj("recepcja","Recepcja123!");
        boolean ukryte=fx(() -> okno.getScene().lookup("#nav-pracownicy")==null);
        if(!ukryte)throw new AssertionError("Recepcja widzi administrację");
        screen("06-recepcja");

        try(var c=TestDatabase.polacz();var p=c.prepareStatement("UPDATE pracownicy SET aktywny=FALSE WHERE login='recepcja'")){p.executeUpdate();}
        klik("refresh");
        for(int i=0;i<100;i++){
            boolean error=fx(() -> Window.getWindows().stream().anyMatch(w -> w.isShowing() && w!=okno && w.getScene().getRoot() instanceof DialogPane));
            if(error)break;Thread.sleep(100);
        }
        fx(() -> { for(Window w:java.util.List.copyOf(Window.getWindows())) if(w.isShowing()&&w!=okno)w.hide();return null; });
        czekaj();if(fx(() -> pole("sesja"))!=null)throw new AssertionError("Sesja nieaktywnego konta nie została zamknięta");
    }
    public static void main(String[] args) { launch(args); }
}
