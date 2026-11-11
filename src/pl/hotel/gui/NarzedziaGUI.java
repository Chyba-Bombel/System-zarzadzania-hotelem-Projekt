package pl.hotel.gui;

import pl.hotel.model.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.function.Function;

/** Wspólne formatowanie, kontrolki i walidacja formularzy. */
final class NarzedziaGUI {
    private NarzedziaGUI() {}

    static String osoba(Osoba o) { return o.getImie() + " " + o.getNazwisko(); }

    static String godzina() { return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")); }

    static String zl(BigDecimal k) { return k.setScale(2).toPlainString().replace('.',',') + " zł"; }

    static String klucz(String s) { return s.toLowerCase(Locale.ROOT).replace('ł','l').replace('ś','s').replace('ć','c').replace('ą','a').replace(' ','-'); }

    static boolean pasuje(String szukane, String tekst) { return tekst.toLowerCase(Locale.ROOT).contains(szukane.trim().toLowerCase(Locale.ROOT)); }

    static TextField pole(String podpowiedz) { TextField t = new TextField(); t.setPromptText(podpowiedz); t.setPrefWidth(280); return t; }

    static Label etykieta(String tekst, String styl) { Label l = new Label(tekst); l.getStyleClass().add(styl); return l; }

    static Button przycisk(String tekst, String styl, Runnable akcja) {
        Button b = new Button(tekst); if (!styl.isEmpty()) b.getStyleClass().add(styl); if (akcja != null) b.setOnAction(e -> akcja.run()); return b;
    }

    static HBox pasek(Node... dzieci) { HBox h = new HBox(10,dzieci); h.setAlignment(Pos.CENTER_LEFT); return h; }

    static ComboBox<String> wybor(String... opcje) {
        ComboBox<String> c = new ComboBox<>(FXCollections.observableArrayList(opcje)); c.getSelectionModel().selectFirst(); return c;
    }

    static DatePicker data(LocalDate wartosc) {
        DatePicker d = new DatePicker(wartosc); d.setPromptText("RRRR-MM-DD"); d.setPrefWidth(145);
        d.setConverter(new StringConverter<>() {
            public String toString(LocalDate v) { return v == null ? "" : v.toString(); }
            public LocalDate fromString(String s) { try { return s == null || s.isBlank() ? null : LocalDate.parse(s.trim()); } catch (DateTimeParseException e) { return null; } }
        });
        d.getEditor().setText(d.getConverter().toString(wartosc));
        return d;
    }

    static LocalDate dataZ(DatePicker d) {
        try {
            LocalDate v = LocalDate.parse(d.getEditor().getText().trim()); d.setValue(v); return v;
        } catch (DateTimeParseException e) { throw new IllegalArgumentException("Wybierz poprawną datę w formacie RRRR-MM-DD."); }
    }

    static void termin(LocalDate a, LocalDate b) {
        if (a.isBefore(LocalDate.now()) || !b.isAfter(a) || java.time.temporal.ChronoUnit.DAYS.between(a,b) > 30)
            throw new IllegalArgumentException("Przyjazd nie może być w przeszłości. Pobyt musi trwać od 1 do 30 nocy.");
    }

    static void sprawdzHasla(String a, String b) {
        if (a.length() < 12 || a.length() > 128) throw new IllegalArgumentException("Hasło musi mieć od 12 do 128 znaków.");
        if (!a.equals(b)) throw new IllegalArgumentException("Powtórzone hasło jest inne.");
    }

    static <T> StringConverter<T> konwerter(Function<T,String> opis) {
        return new StringConverter<>() { public String toString(T v) { return v == null ? "" : opis.apply(v); } public T fromString(String s) { return null; } };
    }

    static <T> void pusta(TableView<T> t, String tekst) { t.setPlaceholder(new Label(tekst)); }

    static <T> void podlacz(TableView<T> t, FilteredList<T> lista) {
        SortedList<T> sorted = new SortedList<>(lista); sorted.comparatorProperty().bind(t.comparatorProperty()); t.setItems(sorted);
    }

    static <T> void kol(TableView<T> t, String nazwa, double szerokosc, Function<T,?> wartosc) {
        TableColumn<T,Object> c = new TableColumn<>(nazwa); c.setPrefWidth(szerokosc);
        c.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(wartosc.apply(v.getValue())));
        c.setComparator(NarzedziaGUI::porownaj);
        t.getColumns().add(c);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static int porownaj(Object a, Object b) {
        if (a == b) return 0; if (a == null) return -1; if (b == null) return 1;
        if (a instanceof Comparable && a.getClass().isInstance(b)) return ((Comparable)a).compareTo(b);
        return a.toString().compareToIgnoreCase(b.toString());
    }
}
