package pl.hotel.gui;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

final class StatusyGUI {
    private StatusyGUI() {}

    static Label etykieta(String stan) {
        String tekst = switch (stan) {
            case "POTWIERDZONA" -> "Potwierdzona";
            case "ZAMELDOWANA" -> "Zameldowana";
            case "ZAKONCZONA" -> "Zakończona";
            case "ANULOWANA" -> "Anulowana";
            case "GOTOWY" -> "Gotowy";
            case "DO_SPRZATANIA" -> "Do sprzątania";
            case "WYLACZONY" -> "Wyłączony";
            case "AKTYWNA" -> "Aktywna";
            case "AKTYWNE" -> "Aktywne";
            case "ARCHIWALNA" -> "Archiwalna";
            case "NIEAKTYWNE" -> "Nieaktywne";
            case "ADMIN" -> "Administrator";
            case "RECEPCJA" -> "Recepcja";
            default -> stan;
        };
        String kolor = switch (stan) {
            case "POTWIERDZONA", "GOTOWY", "AKTYWNA", "AKTYWNE" -> "positive";
            case "ZAMELDOWANA", "ADMIN" -> "info";
            case "DO_SPRZATANIA" -> "warning";
            case "ANULOWANA", "NIEAKTYWNE" -> "negative";
            default -> "neutral";
        };
        Label l = new Label(tekst); l.getStyleClass().addAll("status-badge", "badge-" + kolor);
        l.setTooltip(new Tooltip(stan)); return l;
    }
}
