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

/** Uruchamiany w izolowanej bazie przez skrypt weryfikacyjny; nie dotyka bazy użytkownika. */
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
    static Object pole(String nazwa) throws Exception { Field f = HotelGUI.class.getDeclaredField(nazwa); f.setAccessible(true); return f.get(app); }
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
    static void scenariusz() throws Exception {
        screen("01-logowanie"); loguj("admin","Admin123!");
        if(fx(() -> pole("sesja"))==null)throw new AssertionError("Logowanie nieudane");
        screen("02-pulpit");
        for(String p:new String[]{"goscie","pokoje","rezerwacje","platnosci","pracownicy","moje konto"}){
            klik("nav-"+p.replace(' ','-')); screen("03-"+p.replace(' ','-'));
        }
        klik("nav-goscie"); klik("add-guest"); klik("form-save");
        if(fx(() -> pole("aktywnyDialog"))==null)throw new AssertionError("Pusty formularz został zapisany");
        wpisz("guest-first-name","GUI");wpisz("guest-last-name","Testowy");wpisz("guest-phone","500123456");wpisz("guest-email","gui@example.com");
        klik("form-save");czekaj();
        if(fx(() -> pole("aktywnyDialog"))!=null)throw new AssertionError("Formularz nie zamknął się po zapisie");
        if(BazaDanych.pobierzGosci().stream().noneMatch(g -> g.getEmail().equals("gui@example.com")))throw new AssertionError("Brak zapisu gościa");
        klik("nav-rezerwacje"); klik("add-booking");czekaj();
        screen("04-formularz-rezerwacji");
        // Domyślnie: pierwszy gość, pokój 101 i dzisiejszy termin; test DB przygotowuje go jako wolny.
        klik("form-save");czekaj();
        if(fx(() -> pole("aktywnyDialog"))!=null)throw new AssertionError("Rezerwacja nie została zapisana");
        klik("nav-platnosci");
        fx(() -> { TableView<?> t=(TableView<?>)node("balances-table");t.getSelectionModel().selectLast();return null; });
        klik("show-balance");czekaj();screen("05-rozliczenie");
        // Wylogowanie bez potwierdzenia modalnego: bezpośrednio wywołujemy tę samą zmianę widoku.
        fx(() -> { var m=HotelGUI.class.getDeclaredMethod("pokazLogowanie");m.setAccessible(true);m.invoke(app);return null; });
        loguj("recepcja","Recepcja123!");
        boolean ukryte=fx(() -> okno.getScene().lookup("#nav-pracownicy")==null);
        if(!ukryte)throw new AssertionError("Recepcja widzi administrację");
        screen("06-recepcja");
        // Wygaszenie konta w DB musi blokować kolejną operację GUI.
        try(var c=BazaDanych.polacz();var p=c.prepareStatement("UPDATE pracownicy SET aktywny=FALSE WHERE login='recepcja'")){p.executeUpdate();}
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
