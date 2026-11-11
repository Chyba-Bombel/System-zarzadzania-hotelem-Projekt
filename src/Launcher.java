import javafx.application.Application;
import pl.hotel.gui.HotelGUI;

public final class Launcher {
    private Launcher() {}
    public static void main(String[] args) {
        Application.launch(HotelGUI.class, args);
    }
}
