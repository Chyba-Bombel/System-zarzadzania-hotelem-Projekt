import pl.hotel.gui.HotelGUI;
import pl.hotel.konfiguracja.PreferencjeWygladu;
import pl.hotel.konfiguracja.PreferencjeWygladu.Motyw;
import javafx.application.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.*;
import java.util.List;
import java.util.concurrent.Callable;

public final class ThemeGuiProbe extends Application {
    private int ile;
    @Override public void start(Stage stage) {
        GuiSmoke.app=new HotelGUI(); GuiSmoke.okno=stage; GuiSmoke.app.start(stage);
        new Thread(()->{
            try { scenariusz(); System.out.println("THEME_GUI_OK checks="+ile); Platform.runLater(()->{GuiSmoke.app.stop();Platform.exit();}); }
            catch(Throwable e) { e.printStackTrace();System.exit(2); }
        },"theme-probe").start();
    }

    private void scenariusz() throws Exception {
        check(!fx(()->GuiSmoke.okno.getScene().getRoot().getStyleClass().contains("theme-dark")),"Start jasny");
        GuiSmoke.screen("login-jasny"); GuiSmoke.klik("theme-toggle");
        check(PreferencjeWygladu.wczytaj().motyw()==Motyw.CIEMNY,"Zapis szybkiego przelacznika");
        GuiSmoke.screen("login-ciemny"); GuiSmoke.loguj("admin","Admin123!");
        Thread.sleep(180);
        check(fx(()->GuiSmoke.okno.getScene().getRoot().getStyleClass().contains("theme-dark")),"Motyw po logowaniu");
        check(bezNakladania(),"Karty pulpitu bez nakladania");
        GuiSmoke.klik("dashboard-departures");
        check(fx(()->((TableView<?>)GuiSmoke.node("dashboard-stays")).getItems().size()==2),"Dzisiejsze i zalegle wyjazdy");
        GuiSmoke.klik("dashboard-rooms");
        check(fx(()->((ComboBox<?>)GuiSmoke.pole("stanPokoju")).getValue().equals("DO_SPRZATANIA")),"Skrot filtrowania pokoi");
        GuiSmoke.klik("nav-pulpit");
        GuiSmoke.klik("dashboard-calendar"); czekaj("calendar-table"); GuiSmoke.klik("nav-pulpit");
        GuiSmoke.screen("pulpit-ciemny"); GuiSmoke.klik("theme-toggle");Thread.sleep(100);GuiSmoke.screen("pulpit-jasny");
        for(String id:new String[]{"goscie","pokoje","rezerwacje","kalendarz","platnosci","raporty","pracownicy","moje-konto"}) {
            GuiSmoke.klik("nav-"+id); Thread.sleep(150); GuiSmoke.screen("jasny-"+id);
            check(!fx(()->GuiSmoke.okno.getScene().getRoot().getStyleClass().contains("theme-dark")),"Jasny ekran "+id);
        }
        GuiSmoke.klik("nav-goscie"); GuiSmoke.klik("add-guest");
        GuiSmoke.klik("theme-toggle");
        check(fx(()->Window.getWindows().stream().filter(w->w.isShowing()).allMatch(w->w.getScene().getRoot().getStyleClass().contains("theme-dark"))),"Istniejacy dialog zmienia motyw");
        GuiSmoke.screen("formularz-ciemny");
        fx(()-> { Dialog<?> d=(Dialog<?>)GuiSmoke.pole("aktywnyDialog"); ((Button)d.getDialogPane().lookupButton(ButtonType.CANCEL)).fire(); return null; });
        open("appearance-open"); czekaj("font-scale");
        fx(()->{ ((ComboBox<?>)GuiSmoke.node("theme-choice")).show();return null; });
        check(fx(()->Window.getWindows().stream().filter(w->w.isShowing()).allMatch(w->w.getScene().getRoot().getStyleClass().contains("theme-dark"))),"Motyw listy rozwijanej");
        fx(()->{ ((ComboBox<?>)GuiSmoke.node("theme-choice")).hide();return null; });
        fx(()->{ @SuppressWarnings("unchecked") ComboBox<Integer> c=(ComboBox<Integer>)GuiSmoke.node("font-scale"); c.setValue(120);return null; });
        check(PreferencjeWygladu.wczytaj().skala()==120,"Zapis rozmiaru tekstu");
        fx(()->{ for(Window w:List.copyOf(Window.getWindows()))if(w.isShowing()&&w.getScene().getRoot() instanceof DialogPane d&&d.lookup("#font-scale")!=null)((Button)d.lookupButton(ButtonType.CLOSE)).fire();return null; });
        GuiSmoke.klik("nav-pulpit"); Thread.sleep(160); GuiSmoke.screen("pulpit-ciemny-duzy-tekst");
        fx(()->{ GuiSmoke.okno.setWidth(1040);GuiSmoke.okno.setHeight(720);return null; });Thread.sleep(180);GuiSmoke.screen("mniejsze-okno-ciemne");
        check(bezNakladania(),"Powiekszone karty bez nakladania w malym oknie");
        check(fx(()->GuiSmoke.okno.getScene().getRoot().lookupAll(".page-scroll").stream().findFirst().map(n->((ScrollPane)n).isFitToWidth()).orElse(false)),"Przewijanie w malym oknie");
        fx(()->{ GuiSmoke.okno.setWidth(1440);GuiSmoke.okno.setHeight(990);return null; });
        for(String id:new String[]{"goscie","pokoje","rezerwacje","kalendarz","platnosci","raporty","pracownicy","moje-konto"}) {
            GuiSmoke.klik("nav-"+id);Thread.sleep(150);GuiSmoke.screen("ciemny-"+id);
        }
        GuiSmoke.klik("nav-kalendarz");
        fx(()->{ ((DatePicker)GuiSmoke.node("calendar-month")).show();return null; });
        check(fx(()->Window.getWindows().stream().filter(w->w.isShowing()).allMatch(w->w.getScene().getRoot().getStyleClass().contains("theme-dark"))),"Motyw kalendarzyka");
        fx(()->{ ((DatePicker)GuiSmoke.node("calendar-month")).hide();return null; });
        GuiSmoke.klik("nav-pulpit");
        check(fx(()-> { Label l=(Label)GuiSmoke.okno.getScene().lookup(".title"); Color c=(Color)l.getTextFill();return c.getBrightness()>.8; }),"Jasny tekst w ciemnym motywie");
        open("logout");Thread.sleep(100);
        check(fx(()->Window.getWindows().stream().filter(w->w.isShowing()).allMatch(w->w.getScene().getRoot().getStyleClass().contains("theme-dark"))),"Motyw potwierdzenia");
        fx(()->{ for(Window w:List.copyOf(Window.getWindows()))if(w.isShowing()&&w.getScene().getRoot() instanceof DialogPane d&&d.getButtonTypes().contains(ButtonType.YES))((Button)d.lookupButton(ButtonType.YES)).fire();return null; });
        GuiSmoke.loguj("recepcja","Recepcja123!");Thread.sleep(120);
        check(fx(()->GuiSmoke.okno.getScene().lookup("#nav-pracownicy")==null),"Role zachowane po przebudowie menu");
        check(fx(()->GuiSmoke.okno.getScene().getRoot().getStyleClass().contains("theme-dark")),"Motyw zachowany po wylogowaniu");
    }

    private static boolean bezNakladania() throws Exception {
        return fx(()->{ var k=List.copyOf(GuiSmoke.okno.getScene().getRoot().lookupAll(".metric-card"));
            if(k.size()!=4)return false;
            for(int i=0;i<k.size();i++)for(int j=i+1;j<k.size();j++)if(k.get(i).localToParent(k.get(i).getLayoutBounds()).intersects(k.get(j).localToParent(k.get(j).getLayoutBounds())))return false;
            return true; });
    }

    private static <T> T fx(Callable<T> c) throws Exception { return GuiSmoke.fx(c); }
    private static void open(String id) { Platform.runLater(()->((Button)GuiSmoke.node(id)).fire()); }
    private static void czekaj(String id) throws Exception {
        for(int i=0;i<100;i++) { if(fx(()->Window.getWindows().stream().filter(w->w.isShowing()).anyMatch(w->w.getScene().lookup("#"+id)!=null)))return;Thread.sleep(80); }
        throw new AssertionError("Brak elementu "+id);
    }
    private void check(boolean p,String opis) { if(!p)throw new AssertionError(opis);ile++; }
}
