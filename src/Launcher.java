import javafx.application.Application;
import pl.hotel.gui.HotelGUI;

public final class Launcher {
    private Launcher() {}
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((watek, blad) -> {
            pl.hotel.diagnostyka.DziennikBledow.zapisz(blad);
            System.err.println("Nieoczekiwany błąd HotelManager. Sprawdź dziennik błędów w folderze użytkownika.");
        });
        Application.launch(HotelGUI.class, args);
    }
}
