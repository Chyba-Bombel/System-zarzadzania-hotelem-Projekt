import pl.hotel.gui.HotelGUI;
import javafx.application.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.stage.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

public final class SettingsGuiProbe extends Application {
    private HotelGUI app;
    private int ile;

    @Override public void start(Stage stage) {
        app = new HotelGUI(); app.start(stage);
        new Thread(() -> {
            try {
                scenariusz();
                System.out.println("SETTINGS_GUI_OK checks=" + ile);
                Platform.runLater(() -> { app.stop(); Platform.exit(); });
            } catch (Throwable e) { e.printStackTrace(); System.exit(2); }
        }, "settings-probe").start();
    }

    private void scenariusz() throws Exception {
        Path config = Path.of(System.getProperty("hotel.config"));
        byte[] oryginal = Files.readAllBytes(config);
        open("config-open"); czekaj(() -> node("config-test") != null);
        fx(() -> { ((Button)node("config-test")).fire(); return null; });
        check(fx(() -> 1) == 1, "Watek GUI odpowiada podczas testu");
        czekaj(() -> ((Label)node("config-result")).getText().startsWith("Połączenie działa"));
        fx(() -> {
            Path dir = Path.of(System.getProperty("hotel.screens", "target/settings-screens")); Files.createDirectories(dir);
            var scena = node("config-test").getScene(); scena.getRoot().applyCss(); scena.getRoot().layout();
            javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(scena.snapshot(null), null), "png", dir.resolve("ustawienia-polaczenia.png").toFile());
            return null;
        });
        check(java.util.Arrays.equals(oryginal, Files.readAllBytes(config)), "Test z GUI nie zapisuje ustawien");
        fx(() -> { ((PasswordField)node("config-password")).setText("NiepoprawneHaslo102!"); ((Button)node("config-test")).fire(); return null; });
        czekaj(() -> ((Label)node("config-result")).getText().contains("Odmowa"));
        check(java.util.Arrays.equals(oryginal, Files.readAllBytes(config)), "Bledny test nie zapisuje ustawien");
        fx(() -> { ((Button)node("config-save")).fire(); return null; });
        czekaj(() -> node("config-open") != null && node("config-save") == null);
        check(!java.util.Arrays.equals(oryginal, Files.readAllBytes(config)), "Zapis GUI");
        open("config-open"); czekaj(() -> node("config-restore") != null);
        open("config-restore");
        czekaj(() -> wybor() != null);
        fx(() -> { ((Button)wybor().lookupButton(ButtonType.OK)).fire(); return null; });
        czekaj(() -> ((Label)node("config-result")).getText().startsWith("Przywrócono"));
        check(((String)fx(() -> ((PasswordField)node("config-password")).getText())).isEmpty(), "Przywrocono haslo testowego konta");
        fx(() -> { DialogPane pane = (DialogPane)node("config-save").getScene().getRoot(); ((Button)pane.lookupButton(ButtonType.CANCEL)).fire(); return null; });
        open("about-open"); czekaj(() -> node("about-copy") != null);
        fx(() -> { ((Button)node("about-copy")).fire(); return null; });
        String schowek = fx(() -> Clipboard.getSystemClipboard().getString());
        check(schowek != null && schowek.contains("1.0.2") && !schowek.contains("jdbc:") && !schowek.contains("NiepoprawneHaslo"), "Schowek bez danych polaczenia");
        fx(() -> { ((Button)node("about-logs")).fire(); return null; });
        check(!fx(() -> ((Label)node("about-result")).getText()).isBlank(), "Folder logow lub czytelna sciezka awaryjna");
        fx(() -> { DialogPane pane = (DialogPane)node("about-copy").getScene().getRoot(); ((Button)pane.lookupButton(ButtonType.OK)).fire(); return null; });
        Files.writeString(config, "url=" + "\\uZZZZ");
        open("config-open"); czekaj(() -> node("config-restore") != null);
        check(fx(() -> ((Label)node("config-result")).getText()).contains("Nie odczytano"), "Uszkodzony plik nie blokuje przywracania");
        fx(() -> { DialogPane pane = (DialogPane)node("config-save").getScene().getRoot(); ((Button)pane.lookupButton(ButtonType.CANCEL)).fire(); return null; });
        Files.write(config, oryginal);
    }

    private static DialogPane wybor() {
        for (Window w : List.copyOf(Window.getWindows()))
            if (w.isShowing() && w.getScene().getRoot() instanceof DialogPane p && p.getHeaderText() != null && p.getHeaderText().startsWith("Wybierz kopię")) return p;
        return null;
    }

    private static Node node(String id) {
        for (Window w : List.copyOf(Window.getWindows())) {
            if (!w.isShowing() || w.getScene() == null) continue;
            Node n = w.getScene().lookup("#" + id); if (n != null) return n;
        }
        return null;
    }

    private static <T> T fx(Callable<T> c) throws Exception {
        FutureTask<T> f = new FutureTask<>(c); Platform.runLater(f); return f.get(10, TimeUnit.SECONDS);
    }

    private static void open(String id) { Platform.runLater(() -> ((Button)node(id)).fire()); }

    private static void czekaj(BooleanSupplier warunek) throws Exception {
        for (int i = 0; i < 150; i++) {
            if (fx(warunek::getAsBoolean)) return;
            Thread.sleep(100);
        }
        throw new AssertionError("Przekroczono czas oczekiwania GUI");
    }

    private void check(boolean p, String opis) { if (!p) throw new AssertionError(opis); ile++; }
}
