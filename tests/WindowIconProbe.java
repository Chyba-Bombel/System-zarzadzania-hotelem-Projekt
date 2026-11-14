package pl.hotel.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import java.util.List;

public final class WindowIconProbe {
    public static void main(String[] args) { Application.launch(Probe.class,args); }
    public static final class Probe extends Application {
        @Override public void start(Stage stage) {
            HotelGUI app = new HotelGUI();
            try {
                app.start(stage);
                sprawdz(stage);
                if (!stage.getTitle().contains("1.1.1")) throw new AssertionError("Niepoprawna wersja okna");
                Dialog<ButtonType> d = new Dialog<>(); d.initOwner(stage);
                d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE); d.show();
                sprawdz((Stage)d.getDialogPane().getScene().getWindow());
                d.close();
                app.wyglad.przelacznik().fire(); sprawdz(stage);
                System.out.println("WINDOW_ICON_OK main, dialog, theme, version, sizes, pixels");
                app.stop(); stage.close(); Platform.exit();
            } catch(Throwable e) { e.printStackTrace(); app.stop(); System.exit(2); }
        }
        private static void sprawdz(Stage s) {
            if(s.getIcons().size()!=6)throw new AssertionError("Brak ikon");
            List<Integer> sizes=List.of(16,32,48,64,128,256);
            for(int i=0;i<sizes.size();i++) {
                var im=s.getIcons().get(i);
                if(im.isError()||im.getWidth()!=sizes.get(i)||im.getHeight()!=sizes.get(i))throw new AssertionError("Niepoprawny rozmiar");
            }
            var color=s.getIcons().get(5).getPixelReader().getColor(0,0);
            String expected=System.getProperty("test.icon","default");
            if(expected.equals("default")&&color.getOpacity()!=0)throw new AssertionError("Nie wybrano ikony domyslnej");
            if(expected.equals("red")&&color.getRed()<.9)throw new AssertionError("Nie wybrano ikona.png");
            if(expected.equals("green")&&color.getGreen()<.9)throw new AssertionError("Nie wybrano hotel.png");
        }
    }
}
