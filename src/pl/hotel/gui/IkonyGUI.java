package pl.hotel.gui;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

final class IkonyGUI {
    private IkonyGUI() {}

    static Node ikona(String nazwa) {
        String d = switch (nazwa) {
            case "Pulpit" -> "M3 10 L12 3 L21 10 V21 H15 V14 H9 V21 H3 Z";
            case "Goście", "Pracownicy" -> "M9 12 A4 4 0 1 0 9 4 A4 4 0 1 0 9 12 M2 21 V19 Q2 14 9 14 Q16 14 16 19 V21 M17 4 Q23 6 18 11 M19 15 Q23 16 23 21";
            case "Pokoje", "Zajętość" -> "M2 18 V7 M2 14 H22 V21 M2 18 H22 M5 9 H10 V14 H5 Z M12 8 H19 Q22 8 22 12 V14 H12 Z";
            case "Rezerwacje", "Kalendarz" -> "M5 5 H19 Q21 5 21 7 V20 H3 V7 Q3 5 5 5 Z M7 2 V8 M17 2 V8 M3 11 H21 M7 15 H9 M12 15 H14 M17 15 H19 M7 18 H9 M12 18 H14";
            case "Płatności" -> "M3 5 H21 V20 H3 Z M3 10 H21 M6 15 H11";
            case "Raporty" -> "M4 21 V14 M10 21 V9 M16 21 V4 M22 21 V11";
            case "Moje konto" -> "M12 12 A5 5 0 1 0 12 2 A5 5 0 1 0 12 12 M3 22 Q3 15 12 15 Q21 15 21 22 Z";
            case "przyjazd" -> "M14 3 H21 V21 H14 M2 12 H16 M10 6 L16 12 L10 18";
            case "wyjazd", "Wyloguj się" -> "M10 3 H3 V21 H10 M8 12 H22 M16 6 L22 12 L16 18";
            case "sprzatanie" -> "M14 3 L8 14 M6 12 L15 17 L12 23 L2 18 Z M5 17 L4 20 M9 19 L8 22";
            case "odswiez" -> "M20 7 A9 9 0 1 0 21 15 M20 2 V8 H14";
            case "plus" -> "M12 3 V21 M3 12 H21";
            case "slonce" -> "M12 17 A5 5 0 1 0 12 7 A5 5 0 1 0 12 17 M12 1 V4 M12 20 V23 M1 12 H4 M20 12 H23 M4 4 L6 6 M18 18 L20 20 M4 20 L6 18 M18 6 L20 4";
            case "ksiezyc" -> "M21 14 A9 9 0 0 1 10 3 A9 9 0 1 0 21 14 Z";
            case "Wygląd" -> "M12 3 Q2 3 2 12 Q2 21 11 21 H14 Q16 21 15 18 Q14 15 18 15 H20 Q23 14 21 9 Q19 3 12 3 Z M7 8 H8 M12 6 H13 M17 9 H18 M6 13 H7";
            case "O programie" -> "M12 22 A10 10 0 1 0 12 2 A10 10 0 1 0 12 22 M12 10 V17 M12 6 V7";
            default -> "M5 12 H19 M13 6 L19 12 L13 18";
        };
        SVGPath sciezka = new SVGPath(); sciezka.setContent(d); sciezka.getStyleClass().add("icon");
        sciezka.setScaleX(.78); sciezka.setScaleY(.78); sciezka.setMouseTransparent(true);
        StackPane obszar = new StackPane(sciezka); obszar.setMinSize(24,24); obszar.setPrefSize(24,24); obszar.setMaxSize(24,24);
        obszar.setMouseTransparent(true); return obszar;
    }
}
