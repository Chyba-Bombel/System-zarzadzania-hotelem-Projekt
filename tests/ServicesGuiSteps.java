import pl.hotel.baza.BazaDanych;
import pl.hotel.model.*;
import javafx.application.Platform;
import javafx.scene.control.*;
import java.math.BigDecimal;
public final class ServicesGuiSteps extends GuiSmoke {
    static void wykonaj(int id)throws Exception {
        klik("services-manage");czekaj();wpisz("catalog-name","GUI śniadanie");wpisz("catalog-unit","porcja");wpisz("catalog-price","12,50");klik("catalog-save");czekaj();
        var u=BazaDanych.pobierzKatalogUslug(true).stream().filter(x->x.nazwa().equals("GUI śniadanie")).findFirst().orElseThrow();
        fx(()->{var t=(TableView<UslugaHotelowa>)node("catalog-table");t.getSelectionModel().select(t.getItems().stream().filter(x->x.id()==u.id()).findFirst().orElseThrow());return null;});
        wpisz("catalog-price","15,00");klik("catalog-save");czekaj();screen("14-katalog-uslug");klik("catalog-close");
        fx(()->{var t=(TableView<Rezerwacja>)node("bookings-table");t.getSelectionModel().select(t.getItems().stream().filter(x->x.getId()==id).findFirst().orElseThrow());return null;});
        klik("booking-services");czekaj();fx(()->{var cb=(ComboBox<UslugaHotelowa>)node("services-catalog");cb.getSelectionModel().select(cb.getItems().stream().filter(x->x.id()==u.id()).findFirst().orElseThrow());((Spinner<Integer>)node("services-quantity")).getValueFactory().setValue(2);return null;});
        klik("services-add");czekaj();if(BazaDanych.pobierzRozliczenie(id).uslugi().compareTo(new BigDecimal("30"))!=0)throw new AssertionError("Usługi GUI: należność");screen("15-uslugi-pobytu");
        fx(()->{((TableView<?>)node("services-table")).getSelectionModel().selectFirst();Platform.runLater(()->((Button)node("services-cancel")).fire());return null;});potwierdzSprzatanie();czekaj();
        if(BazaDanych.pobierzRozliczenie(id).uslugi().signum()!=0)throw new AssertionError("Usługi GUI: anulowanie");klik("services-add");czekaj();klik("services-close");
        klik("booking-confirmation");czekaj();fx(()->{((TabPane)node("confirmation-tabs")).getSelectionModel().select(3);return null;});
        if(fx(()->((ListView<?>)node("confirmation-services")).getItems().size())!=2)throw new AssertionError("Karta usług");screen("16-karta-uslugi");klik("confirmation-close");
        klik("booking-search");czekaj();wpisz("search-guest","Pruska");wpisz("search-room","106");
        fx(()->{((ComboBox<FiltrRezerwacji.StanRozliczenia>)node("search-balance")).setValue(FiltrRezerwacji.StanRozliczenia.CZESCIOWO_OPLACONA);((DatePicker)node("search-from")).getEditor().setText(java.time.LocalDate.now().toString());((DatePicker)node("search-to")).getEditor().setText(java.time.LocalDate.now().toString());return null;});
        klik("search-run");czekaj();if(fx(()->((TableView<WynikRezerwacji>)node("search-table")).getItems().stream().filter(x->x.id()==id).count())!=1)throw new AssertionError("Filtry GUI");screen("17-wyszukiwanie");
        wpisz("search-guest","NIE_MA_TAKIEGO_GOSCIA");if(!fx(()->((TableView<?>)node("search-table")).getItems().isEmpty()))throw new AssertionError("Stare wyniki po zmianie filtrów");klik("search-reset");czekaj();
        fx(()->{var t=(TableView<WynikRezerwacji>)node("search-table");t.getSelectionModel().select(t.getItems().stream().filter(x->x.id()==id).findFirst().orElseThrow());return null;});klik("search-open");czekaj();
        if(fx(()->((Rezerwacja)((TableView<?>)node("bookings-table")).getSelectionModel().getSelectedItem()).getId())!=id)throw new AssertionError("Otwarcie wyniku");
        klik("booking-history");czekaj();boolean h=fx(()->{var t=(TableView<ZmianaRezerwacji>)node("history-table");return t.getItems().stream().anyMatch(x->x.akcja().equals("WPLATA"))&&t.getItems().stream().anyMatch(x->x.akcja().equals("DODANIE_USLUGI"));});if(!h)throw new AssertionError("Historia wpłaty/usług GUI");klik("history-close");
        System.out.println("GUI_SERVICES_SEARCH_OK: katalog, cena, usługi, karta, filtry, saldo, nazwisko dodatkowe i otwarcie wyniku.");
    }
    static void recepcja()throws Exception {
        klik("nav-rezerwacje");if(fx(()->{var n=okno.getScene().lookup("#services-manage");return n!=null&&n.isVisible();}))throw new AssertionError("Cennik dostępny recepcji");klik("booking-search");czekaj();klik("search-close");
    }
}
