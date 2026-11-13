package pl.hotel.konfiguracja;

import java.nio.file.Path;
import java.util.Locale;

public final class SciezkiAplikacji {
    private SciezkiAplikacji() {}

    public static Path dane() {
        String wymuszony = System.getProperty("hotel.data");
        if (wymuszony != null && !wymuszony.isBlank()) return Path.of(wymuszony).toAbsolutePath().normalize();
        String dom = System.getProperty("user.home");
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) {
            String lokalny = System.getenv("LOCALAPPDATA");
            return (lokalny == null || lokalny.isBlank() ? Path.of(dom, "AppData", "Local") : Path.of(lokalny)).resolve("HotelManagerData");
        }
        String xdg = System.getenv("XDG_CONFIG_HOME");
        return (xdg == null || xdg.isBlank() ? Path.of(dom, ".config") : Path.of(xdg)).resolve("HotelManagerData");
    }

    public static Path konfiguracja() {
        String wymuszony = System.getProperty("hotel.config");
        return wymuszony == null || wymuszony.isBlank()
                ? dane().resolve("config").resolve("db.properties")
                : Path.of(wymuszony).toAbsolutePath().normalize();
    }

    public static Path logi() { return dane().resolve("logs"); }
}
