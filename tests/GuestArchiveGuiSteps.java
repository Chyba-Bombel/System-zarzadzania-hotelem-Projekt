import pl.hotel.baza.BazaDanych;
import pl.hotel.model.*;
import javafx.scene.control.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class GuestArchiveGuiSteps extends GuiSmoke {
    private static int id;
    private static Gosc odczytaj()throws Exception{return BazaDanych.pobierzGosci().stream().filter(x->x.getId()==id).findFirst().orElseThrow();}
    private static void wybierz(int grupa)throws Exception{
        fx(()->{((ComboBox<?>)node("guest-state")).getSelectionModel().select(grupa);((TextField)pole("szukajGoscia")).setText("GUIArchiwum");
            var t=(TableView<Gosc>)node("guests-table");t.getSelectionModel().select(t.getItems().stream().filter(x->x.getId()==id).findFirst().orElseThrow());return null;});
    }
    static void admin()throws Exception{
        var g=BazaDanych.zapiszGoscia("Jan","GUIArchiwum","500123456","gui-archive@example.com");id=g.getId();
        BazaDanych.dodajPokoj("GUAR","GUI archiwum",1,new BigDecimal("100"));var d=LocalDate.now().plusDays(240);
        var r=BazaDanych.zapiszRezerwacje(g,IntegrationProbe.pokoj("GUAR"),d,d.plusDays(1),1,1);BazaDanych.anulujRezerwacje(r.getId());
        klik("refresh");czekaj();klik("nav-goscie");wybierz(0);klik("guest-archive");screen("18-archiwizacja-formularz");klik("form-save");czekaj();
        if(!odczytaj().isArchiwalny()||!odczytaj().getArchiwizowalLogin().equals("admin"))throw new AssertionError("Archiwizacja GUI");
        if(!fx(()->((TableView<?>)node("guests-table")).getItems().isEmpty()))throw new AssertionError("Archiwum widoczne w aktywnych");
        wybierz(1);if(!fx(()->((Button)node("guest-edit")).isDisabled()))throw new AssertionError("Edycja archiwum aktywna");screen("19-archiwum-gosci");
        klik("nav-rezerwacje");klik("add-booking");czekaj();
        if(fx(()->((ComboBox<Gosc>)node("booking-guest")).getItems().stream().anyMatch(x->x.getId()==id)))throw new AssertionError("Archiwalny gość w nowej rezerwacji");
        fx(()->{((Dialog<?>)pole("aktywnyDialog")).close();return null;});
        klik("nav-goscie");wybierz(1);klik("guest-restore");fx(()->{((Dialog<?>)pole("aktywnyDialog")).close();return null;});
        if(!odczytaj().isArchiwalny())throw new AssertionError("Anulowanie formularza przywróciło kartotekę");
        wybierz(1);klik("guest-restore");klik("form-save");czekaj();if(odczytaj().isArchiwalny())throw new AssertionError("Przywrócenie GUI");
        wybierz(0);klik("guest-archive");klik("form-save");czekaj();
        fx(()->{((TextField)pole("szukajGoscia")).clear();return null;});
        System.out.println("GUI_GUEST_ARCHIVE_ADMIN_OK");
    }
    static void recepcja()throws Exception{
        klik("nav-goscie");wybierz(1);klik("guest-restore");klik("form-save");czekaj();
        if(odczytaj().isArchiwalny())throw new AssertionError("Przywrócenie przez recepcję GUI");wybierz(0);screen("20-przywrocony-gosc");
        fx(()->{((TextField)pole("szukajGoscia")).clear();return null;});
        System.out.println("GUI_GUEST_ARCHIVE_RECEPTION_OK");
    }
}
