package pl.hotel.konfiguracja;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class KonfiguracjaAplikacji {
    private KonfiguracjaAplikacji() {}

    public static synchronized Path przygotuj() throws IOException {
        Path cel = SciezkiAplikacji.konfiguracja();
        if (Files.exists(cel)) return cel;
        if (System.getProperty("hotel.config") != null && !System.getProperty("hotel.config").isBlank())
            throw new IOException("Nie istnieje jawnie wskazany plik konfiguracji.");
        Files.createDirectories(cel.getParent());
        for (Path stary : starszePliki()) {
            if (!stary.toAbsolutePath().normalize().equals(cel.toAbsolutePath().normalize()) && Files.isRegularFile(stary)) {
                Properties dane = odczytaj(stary);
                if (dane.containsKey("url") && dane.containsKey("user") && dane.containsKey("password")) {
                    zapiszJesliBrak(cel, Files.readString(stary, StandardCharsets.UTF_8));
                    return cel;
                }
            }
        }
        zapiszJesliBrak(cel, "url=jdbc:mysql://localhost:3306/hotel_manager?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=10000\nuser=hotel_app\npassword=\n");
        return cel;
    }

    private static List<Path> starszePliki() {
        List<Path> lista = new ArrayList<>();
        String stary = System.getProperty("hotel.legacy.config");
        if (stary != null && !stary.isBlank()) lista.add(Path.of(stary));
        try {
            Path kod = Path.of(KonfiguracjaAplikacji.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            lista.add((Files.isDirectory(kod) ? kod : kod.getParent()).resolve("db.properties"));
        } catch (java.net.URISyntaxException | SecurityException ignored) {}
        lista.add(Path.of("db.properties"));
        return lista;
    }

    private static Properties odczytaj(Path plik) throws IOException {
        Properties p = new Properties();
        try (var czytnik = Files.newBufferedReader(plik, StandardCharsets.UTF_8)) {
            czytnik.mark(1);
            if (czytnik.read() != 0xFEFF) czytnik.reset();
            p.load(czytnik);
        }
        return p;
    }

    public static Properties wczytajPlik() throws IOException {
        return odczytaj(przygotuj());
    }

    public static synchronized void zapisz(Properties dane) throws IOException {
        Path cel = przygotuj();
        Path tmp = Files.createTempFile(cel.getParent(), "config-", ".tmp");
        try {
            try { Files.setPosixFilePermissions(tmp, PosixFilePermissions.fromString("rw-------")); }
            catch (UnsupportedOperationException ignored) {}
            try (var out = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) { dane.store(out, null); }
            try { Files.move(tmp, cel, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(tmp, cel, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(tmp); }
    }

    public static Properties wczytaj() throws BladKonfiguracji {
        try {
            Properties p = odczytaj(przygotuj());
            for (String[] para : new String[][]{{"url", "HOTEL_DB_URL"}, {"user", "HOTEL_DB_USER"}, {"password", "HOTEL_DB_PASSWORD"}}) {
                String env = System.getenv(para[1]);
                if (env != null) p.setProperty(para[0], env);
                if (!p.containsKey(para[0])) throw new IOException("Brak wymaganego pola.");
            }
            return p;
        } catch (IOException | IllegalArgumentException | SecurityException e) {
            throw new BladKonfiguracji("Nie można odczytać konfiguracji bazy. Otwórz ustawienia połączenia i sprawdź plik db.properties oraz uprawnienia do jego folderu.", e);
        }
    }

    private static void zapiszJesliBrak(Path cel, String tekst) throws IOException {
        Path tymczasowy = Files.createTempFile(cel.getParent(), "config-", ".tmp");
        try {
            try { Files.setPosixFilePermissions(tymczasowy, PosixFilePermissions.fromString("rw-------")); }
            catch (UnsupportedOperationException ignored) {}
            Files.writeString(tymczasowy, tekst, StandardCharsets.UTF_8);
            try { Files.move(tymczasowy, cel); }
            catch (FileAlreadyExistsException ignored) {}
        } finally { Files.deleteIfExists(tymczasowy); }
    }
}
