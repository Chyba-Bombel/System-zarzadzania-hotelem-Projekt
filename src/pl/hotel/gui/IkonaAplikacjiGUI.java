package pl.hotel.gui;

import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class IkonaAplikacjiGUI {
    private static List<Image> ikony;
    private IkonaAplikacjiGUI() {}

    static void ustaw(Stage okno) {
        if (ikony == null) ikony = wczytaj();
        if (!ikony.isEmpty()) okno.getIcons().setAll(ikony);
    }

    private static List<Image> wczytaj() {
        for (String zasob : new String[]{"/ikona.png", "/hotel.png", "/hotel-default-icon.png"}) {
            List<Image> wynik = new ArrayList<>();
            for (int rozmiar : new int[]{16, 32, 48, 64, 128, 256}) {
                try (var strumien = IkonaAplikacjiGUI.class.getResourceAsStream(zasob)) {
                    if (strumien == null) break;
                    Image obraz = new Image(strumien, rozmiar, rozmiar, false, true);
                    if (obraz.isError()) break;
                    wynik.add(obraz);
                } catch (IOException | IllegalArgumentException e) { break; }
            }
            if (wynik.size() == 6) return List.copyOf(wynik);
        }
        return List.of();
    }
}
