package pl.hotel.konfiguracja;

import pl.hotel.diagnostyka.DziennikBledow;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class KopieKonfiguracji {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);
    private KopieKonfiguracji() {}

    private static Path folder() {
        return SciezkiAplikacji.konfiguracja().toAbsolutePath().getParent().resolve("backups");
    }

    public static List<Path> lista() throws IOException {
        Path dir = folder();
        if (!Files.exists(dir)) return List.of();
        if (Files.isSymbolicLink(dir)) throw new IOException("Folder kopii nie może być dowiązaniem.");
        try (var pliki = Files.list(dir)) {
            return pliki.filter(f -> Files.isRegularFile(f, LinkOption.NOFOLLOW_LINKS))
                    .filter(f -> f.getFileName().toString().matches("db-[0-9]{8}-[0-9]{6}-[0-9]{3}-[a-f0-9-]{36}\\.properties"))
                    .sorted(Comparator.comparing((Path f) -> f.getFileName().toString()).reversed()).toList();
        }
    }

    static Path sprawdz(Path plik) throws IOException {
        Path cel = plik.toAbsolutePath().normalize();
        if (!lista().contains(cel)) throw new IOException("Wybierz istniejącą kopię z folderu konfiguracji.");
        return cel;
    }

    static void zachowaj(Path plik) throws IOException {
        Path dir = folder();
        if (Files.isSymbolicLink(dir)) throw new IOException("Folder kopii nie może być dowiązaniem.");
        Files.createDirectories(dir);
        Path tymczasowy = Files.createTempFile(dir, "backup-", ".tmp");
        try {
            try { Files.setPosixFilePermissions(tymczasowy, PosixFilePermissions.fromString("rw-------")); }
            catch (UnsupportedOperationException ignored) {}
            Files.write(tymczasowy, Files.readAllBytes(plik));
            Path cel = dir.resolve("db-" + DATA.format(Instant.now()) + "-" + UUID.randomUUID() + ".properties");
            Files.move(tymczasowy, cel);
        } finally { Files.deleteIfExists(tymczasowy); }
    }

    static void ogranicz() {
        try {
            List<Path> kopie = lista();
            for (int i = 5; i < kopie.size(); i++) Files.deleteIfExists(kopie.get(i));
        } catch (IOException | SecurityException e) { DziennikBledow.zapisz(e); }
    }
}
