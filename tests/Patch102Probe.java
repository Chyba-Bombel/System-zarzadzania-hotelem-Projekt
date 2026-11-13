import pl.hotel.konfiguracja.*;
import pl.hotel.diagnostyka.InformacjeDiagnostyczne;
import java.nio.file.*;
import java.util.Properties;

public final class Patch102Probe {
    private static int ile;
    private static void check(boolean p, String opis) { if (!p) throw new AssertionError(opis); ile++; }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("hotel-102-");
        Path config = dir.resolve("db.properties");
        System.setProperty("hotel.config", config.toString());
        String stare = "url=jdbc:mysql://localhost/hotel_patch_test\nuser=test-user\npassword=SECRET_TEST_ONLY\n";
        Files.writeString(config, stare);
        Properties p = KonfiguracjaAplikacji.wczytajPlik();
        p.setProperty("password", "Nowe Żółć = \\ hasło");
        KonfiguracjaAplikacji.zapisz(p);
        check(KopieKonfiguracji.lista().size() == 1, "Pierwsza kopia");
        Path kopia = KopieKonfiguracji.lista().get(0);
        check(Files.readString(kopia).equals(stare), "Kopia dokladnych poprzednich bajtow");
        check(KonfiguracjaAplikacji.wczytajPlik().getProperty("password").equals(p.getProperty("password")), "Zapis UTF-8");
        KonfiguracjaAplikacji.przywroc(kopia);
        check(KonfiguracjaAplikacji.wczytajPlik().getProperty("password").equals("SECRET_TEST_ONLY"), "Przywrocenie poprzedniego hasla");
        check(KopieKonfiguracji.lista().size() == 2, "Przywrocenie tez zachowuje stan");
        boolean maNowe = false;
        for (Path f : KopieKonfiguracji.lista()) {
            Properties x = new Properties(); try (var r = Files.newBufferedReader(f)) { x.load(r); }
            if (p.getProperty("password").equals(x.getProperty("password"))) maNowe = true;
        }
        check(maNowe, "Mozna cofnac przywrocenie");
        for (int i = 0; i < 10; i++) { p.setProperty("user", "test-" + i); KonfiguracjaAplikacji.zapisz(p); }
        check(KopieKonfiguracji.lista().size() == 5, "Rotacja pieciu kopii");
        Path doNaprawy = KopieKonfiguracji.lista().get(0);
        Files.writeString(config, "url=" + "\\uZZZZ");
        KonfiguracjaAplikacji.przywroc(doNaprawy);
        check(KonfiguracjaAplikacji.wczytajPlik().getProperty("user").startsWith("test-"), "Przywrocenie uszkodzonego pliku");
        Path obcy = dir.resolve("obcy.properties"); Files.writeString(obcy, stare);
        try { KonfiguracjaAplikacji.przywroc(obcy); throw new AssertionError("Obca kopia"); }
        catch (java.io.IOException e) { check(true, "Odmowa pliku spoza folderu kopii"); }
        String przed = Files.readString(config);
        Path folder = dir.resolve("backups"), przesuniety = dir.resolve("backups-stare");
        Files.move(folder, przesuniety); Files.writeString(folder, "blokada folderu");
        try { KonfiguracjaAplikacji.zapisz(p); throw new AssertionError("Zapis bez mozliwosci kopii"); }
        catch (java.io.IOException e) { check(Files.readString(config).equals(przed), "Awaria kopii nie zmienia konfiguracji"); }
        finally { Files.delete(folder); Files.move(przesuniety, folder); }
        try (var pliki = Files.list(dir)) { check(pliki.noneMatch(f -> f.getFileName().toString().endsWith(".tmp")), "Brak osieroconych plikow tymczasowych"); }
        if (Files.getFileStore(config).supportsFileAttributeView("posix")) {
            check(Files.getPosixFilePermissions(config).equals(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")), "Prawa konfiguracji");
            check(Files.getPosixFilePermissions(KopieKonfiguracji.lista().get(0)).equals(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")), "Prawa kopii");
            Path link = folder.resolve("db-20260101-000000-000-00000000-0000-0000-0000-000000000000.properties");
            Files.createSymbolicLink(link, obcy);
            try { KonfiguracjaAplikacji.przywroc(link); throw new AssertionError("Dowiazanie"); }
            catch (java.io.IOException e) { check(true, "Odmowa dowiazania"); }
        }
        String info = InformacjeDiagnostyczne.tekst();
        check(info.contains("HotelManager 1.0.2") && info.contains("Java:") && info.contains("System:"), "Informacje o wersji");
        check(!info.contains("SECRET_TEST_ONLY") && !info.contains("jdbc:") && !info.contains("test-user"), "Informacje bez ustawien bazy");
        System.out.println("PATCH102_OK checks=" + ile);
    }
}
