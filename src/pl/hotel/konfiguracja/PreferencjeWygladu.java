package pl.hotel.konfiguracja;

import pl.hotel.diagnostyka.DziennikBledow;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;

public record PreferencjeWygladu(Motyw motyw, int skala) {
    public enum Motyw {
        JASNY("Jasny"), CIEMNY("Ciemny");
        private final String opis;
        Motyw(String opis) { this.opis = opis; }
        @Override public String toString() { return opis; }
    }

    public PreferencjeWygladu {
        if (motyw == null || (skala != 100 && skala != 110 && skala != 120))
            throw new IllegalArgumentException("Niepoprawne ustawienia wyglądu.");
    }

    public static Path plik() { return SciezkiAplikacji.dane().resolve("wyglad.properties"); }

    public static PreferencjeWygladu wczytaj() {
        if (!Files.exists(plik())) return new PreferencjeWygladu(Motyw.JASNY, 100);
        try (var r = Files.newBufferedReader(plik(), StandardCharsets.UTF_8)) {
            Properties p = new Properties(); p.load(r);
            return new PreferencjeWygladu(Motyw.valueOf(p.getProperty("motyw", "JASNY")), Integer.parseInt(p.getProperty("skala", "100")));
        } catch (IOException | IllegalArgumentException | SecurityException e) {
            DziennikBledow.zapisz(e); return new PreferencjeWygladu(Motyw.JASNY, 100);
        }
    }

    public void zapisz() throws IOException {
        Path cel = plik(); Files.createDirectories(cel.getParent());
        Path tmp = Files.createTempFile(cel.getParent(), "wyglad-", ".tmp");
        try {
            try { Files.setPosixFilePermissions(tmp, PosixFilePermissions.fromString("rw-------")); }
            catch (UnsupportedOperationException ignored) {}
            Files.writeString(tmp, "motyw=" + motyw.name() + "\nskala=" + skala + "\n", StandardCharsets.UTF_8);
            try { Files.move(tmp, cel, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(tmp, cel, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(tmp); }
    }
}
