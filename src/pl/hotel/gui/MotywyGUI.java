package pl.hotel.gui;

import pl.hotel.konfiguracja.PreferencjeWygladu;
import pl.hotel.konfiguracja.PreferencjeWygladu.Motyw;
import pl.hotel.diagnostyka.DziennikBledow;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Window;
import java.util.*;

final class MotywyGUI {
    private final ReadOnlyObjectWrapper<PreferencjeWygladu> wyglad = new ReadOnlyObjectWrapper<>(PreferencjeWygladu.wczytaj());
    private final Map<Window, ChangeListener<Scene>> okna = new HashMap<>();
    private final Map<Scene, ChangeListener<Parent>> sceny = new HashMap<>();
    private final Map<Parent, String> style = new WeakHashMap<>();
    private final String css = Objects.requireNonNull(getClass().getResource("/hotel.css")).toExternalForm();
    private final ListChangeListener<Window> zmiany = c -> {
        while (c.next()) {
            if (c.wasRemoved()) c.getRemoved().forEach(this::usun);
            if (c.wasAdded()) c.getAddedSubList().forEach(this::obserwuj);
        }
    };

    void uruchom() {
        Window.getWindows().addListener(zmiany);
        List.copyOf(Window.getWindows()).forEach(this::obserwuj);
    }

    PreferencjeWygladu wartosc() { return wyglad.get(); }
    ReadOnlyObjectProperty<PreferencjeWygladu> property() { return wyglad.getReadOnlyProperty(); }

    boolean zmien(PreferencjeWygladu nowy) {
        wyglad.set(nowy);
        List.copyOf(sceny.keySet()).forEach(this::zastosuj);
        try { nowy.zapisz(); return true; }
        catch (java.io.IOException | SecurityException e) { DziennikBledow.zapisz(e); return false; }
    }


    void zastosuj(Scene scena) {
        if (scena == null) return;
        if (!scena.getStylesheets().contains(css)) scena.getStylesheets().add(css);
        Parent root = scena.getRoot();
        if (!root.getStyleClass().contains("hotel-root")) root.getStyleClass().add("hotel-root");
        root.getStyleClass().remove("theme-dark");
        if (wyglad.get().motyw() == Motyw.CIEMNY) root.getStyleClass().add("theme-dark");
        String bazowy = style.computeIfAbsent(root, r -> r.getStyle());
        root.setStyle((bazowy.isBlank() ? "" : bazowy + "; ") + "-fx-font-size: " + (13.0 * wyglad.get().skala() / 100.0) + "px;");
    }

    private void obserwuj(Window okno) {
        if (okna.containsKey(okno)) return;
        if (okno instanceof javafx.stage.Stage stage) IkonaAplikacjiGUI.ustaw(stage);
        ChangeListener<Scene> listener = (o, stare, nowe) -> { usunScene(stare); obserwujScene(nowe); };
        okna.put(okno, listener); okno.sceneProperty().addListener(listener); obserwujScene(okno.getScene());
    }

    private void obserwujScene(Scene scena) {
        if (scena == null || sceny.containsKey(scena)) return;
        ChangeListener<Parent> listener = (o, stare, nowe) -> zastosuj(scena);
        sceny.put(scena, listener); scena.rootProperty().addListener(listener); zastosuj(scena);
    }

    private void usunScene(Scene scena) {
        if (scena == null) return;
        ChangeListener<Parent> listener = sceny.remove(scena);
        if (listener != null) scena.rootProperty().removeListener(listener);
    }

    private void usun(Window okno) {
        ChangeListener<Scene> listener = okna.remove(okno);
        if (listener != null) okno.sceneProperty().removeListener(listener);
        usunScene(okno.getScene());
    }

    void zamknij() {
        Window.getWindows().removeListener(zmiany);
        List.copyOf(okna.keySet()).forEach(this::usun);
        style.clear();
    }
}
