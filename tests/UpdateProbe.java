import pl.hotel.konfiguracja.*;
import pl.hotel.diagnostyka.DziennikBledow;
import pl.hotel.bezpieczenstwo.KomunikatyBledu;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Properties;

public final class UpdateProbe {
    private static int licznik;

    private static void check(boolean warunek, String nazwa) {
        if (!warunek) throw new AssertionError(nazwa);
        licznik++;
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("env")) {
            Properties p = KonfiguracjaAplikacji.wczytaj();
            check(p.getProperty("user").equals("konto_z_env"), "Pierwszenstwo zmiennej srodowiskowej");
            System.out.println("ENV_OK");
            return;
        }
        Path root = Files.createTempDirectory("hotel-update-test-");
        String staryData = System.getProperty("hotel.data");
        String staryConfig = System.getProperty("hotel.config");
        String staryLegacy = System.getProperty("hotel.legacy.config");
        try {
            System.clearProperty("hotel.config");
            System.setProperty("hotel.data", root.resolve("dane").toString());
            Path zrodlo = root.resolve("stara instalacja").resolve("db.properties");
            Files.createDirectories(zrodlo.getParent());
            String sekret = "HASLO_TESTOWE_TAJNE";
            String dane = "url=jdbc:mysql://localhost:3306/hotel_test\nuser=konto\npassword=" + sekret + "\n";
            Files.writeString(zrodlo, dane, StandardCharsets.UTF_8);
            System.setProperty("hotel.legacy.config", zrodlo.toString());
            Path cel = KonfiguracjaAplikacji.przygotuj();
            check(Files.readString(cel).equals(dane), "Migracja starej konfiguracji");
            check(Files.readString(zrodlo).equals(dane), "Zrodlo pozostaje nietkniete");
            Files.writeString(zrodlo, dane.replace("konto", "inne"));
            KonfiguracjaAplikacji.przygotuj();
            check(Files.readString(cel).equals(dane), "Nie nadpisuje istniejacej konfiguracji");
            Properties zmiana = KonfiguracjaAplikacji.wczytajPlik();
            zmiana.setProperty("password", "Żółć: = \\ & ?");
            KonfiguracjaAplikacji.zapisz(zmiana);
            check(KonfiguracjaAplikacji.wczytajPlik().getProperty("password").equals("Żółć: = \\ & ?"), "Zapis znakow specjalnych i UTF-8");
            Path wlasny = root.resolve("wlasne.properties");
            Files.writeString(wlasny, dane);
            System.setProperty("hotel.config", wlasny.toString());
            check(KonfiguracjaAplikacji.przygotuj().equals(wlasny.toAbsolutePath()), "Jawny plik dla testow i narzedzi");
            check(KonfiguracjaAplikacji.wczytaj().getProperty("password").equals(sekret), "Odczyt jawnego pliku");
            Files.writeString(wlasny, "\uFEFF" + dane, StandardCharsets.UTF_8);
            check(KonfiguracjaAplikacji.wczytaj().getProperty("url").startsWith("jdbc:mysql://"), "UTF-8 BOM z Notatnika Windows");
            ProcessBuilder env = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-Dhotel.config=" + wlasny, "-cp", System.getProperty("java.class.path"), "UpdateProbe", "env");
            env.environment().put("HOTEL_DB_USER", "konto_z_env");
            Process dziecko = env.redirectErrorStream(true).start();
            String wynik = new String(dziecko.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            check(dziecko.waitFor() == 0 && wynik.contains("ENV_OK"), "Zmienne srodowiskowe");
            System.setProperty("hotel.config", root.resolve("brak.properties").toString());
            try { KonfiguracjaAplikacji.wczytaj(); throw new AssertionError("Brak pliku powinien byc bledem"); }
            catch (BladKonfiguracji e) { check(e.getMessage().contains("konfiguracji"), "Czytelny blad konfiguracji"); }
            System.clearProperty("hotel.config");
            System.setProperty("hotel.data", root.resolve("nowe-dane").toString());
            Path czysty = root.resolve("pusty katalog");
            Files.createDirectories(czysty);
            ProcessBuilder nowy = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-Dhotel.data=" + root.resolve("czysta-instalacja"), "-cp", absolutnyClasspath(), "UpdateFreshProbe");
            nowy.directory(czysty.toFile());
            Process pierwsze = nowy.redirectErrorStream(true).start();
            String pierwszyWynik = new String(pierwsze.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            check(pierwsze.waitFor() == 0 && pierwszyWynik.contains("FRESH_OK"), "Czysta instalacja nie kopiuje hasla demo");
            check(KomunikatyBledu.dlaUzytkownika(new SQLException(sekret, "28000", 1045)).contains("Odmowa"), "Bledne konto bazy");
            check(KomunikatyBledu.dlaUzytkownika(new RuntimeException(new SQLException(sekret, "42000", 1049))).contains("Nie znaleziono bazy"), "Zagniezdzony blad bazy");
            SQLException lancuch = new SQLException("zewnetrzny", "HY000");
            lancuch.setNextException(new SQLException(sekret, "42S22", 1054));
            check(KomunikatyBledu.dlaUzytkownika(lancuch).contains("Brakuje tabeli lub kolumny"), "Lancuch wyjatkow JDBC");
            check(KomunikatyBledu.dlaUzytkownika(new java.net.ConnectException(sekret)).contains("Brak połączenia"), "Brak serwera");
            check(KomunikatyBledu.dlaUzytkownika(new javax.net.ssl.SSLException(sekret)).contains("certyfikat"), "Blad TLS");
            SQLException blad = new SQLException("password=" + sekret + " gosc=Jan Kowalski SQL SELECT", "42000", 1064);
            blad.addSuppressed(new IllegalArgumentException(sekret));
            DziennikBledow.zapisz(blad);
            try (var logi = Files.list(SciezkiAplikacji.logi())) {
                StringBuilder tekst = new StringBuilder();
                for (Path log : logi.filter(f -> f.toString().endsWith(".log")).toList()) tekst.append(Files.readString(log));
                check(tekst.toString().contains("kod=1064"), "Log zawiera kod diagnostyczny");
                check(!tekst.toString().contains(sekret) && !tekst.toString().contains("Kowalski") && !tekst.toString().contains("SELECT"), "Log nie zapisuje hasel, danych gosci ani SQL");
            }
            check(WersjaAplikacji.NUMER.equals("1.0.1"), "Wersja z zasobu aplikacji");
            System.out.println("UPDATE_PROBE_OK checks=" + licznik);
        } finally {
            przywroc("hotel.data", staryData);
            przywroc("hotel.config", staryConfig);
            przywroc("hotel.legacy.config", staryLegacy);
        }
    }

    private static String absolutnyClasspath() {
        return java.util.Arrays.stream(System.getProperty("java.class.path").split(java.io.File.pathSeparator))
                .map(s -> Path.of(s).toAbsolutePath().toString()).collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));
    }

    private static void przywroc(String klucz, String wartosc) {
        if (wartosc == null) System.clearProperty(klucz); else System.setProperty(klucz, wartosc);
    }
}
