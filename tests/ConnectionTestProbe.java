import pl.hotel.baza.TestPolaczenia;
import pl.hotel.bezpieczenstwo.KomunikatyBledu;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.Properties;

public final class ConnectionTestProbe {
    private static int ile;
    private static void check(boolean p, String opis) { if (!p) throw new AssertionError(opis); ile++; }

    public static void main(String[] args) throws Exception {
        TestDatabase.wymagajTestowejBazy();
        Path config = Path.of(System.getProperty("hotel.config"));
        byte[] przed = Files.readAllBytes(config);
        Properties p = new Properties(); try (var r = Files.newBufferedReader(config)) { p.load(r); }
        Properties oryginal = new Properties(); oryginal.putAll(p);
        String stanPrzed = stan();
        check(TestPolaczenia.sprawdz(p).startsWith("Połączenie działa"), "Rzeczywisty SELECT 1");
        check(p.equals(oryginal), "Nie zmienia formularza");
        check(java.util.Arrays.equals(przed, Files.readAllBytes(config)), "Nie zapisuje pliku konfiguracji");
        check(stanPrzed.equals(stan()), "Nie zmienia rekordow ani blokad kont pracownikow");
        p.setProperty("password", "NiepoprawneHaslo102!");
        try { TestPolaczenia.sprawdz(p); throw new AssertionError("Bledne haslo"); }
        catch (SQLException e) { check(KomunikatyBledu.dlaUzytkownika(e).contains("Odmowa"), "Bledne haslo serwera"); }
        p.putAll(oryginal);
        String sciezka = java.net.URI.create(p.getProperty("url").substring(5)).getPath();
        p.setProperty("url", p.getProperty("url").replace(sciezka, "/nieistniejaca_patch102_test"));
        try { TestPolaczenia.sprawdz(p); throw new AssertionError("Brak bazy"); }
        catch (SQLException e) { check(KomunikatyBledu.dlaUzytkownika(e).contains("Nie znaleziono bazy"), "Brak bazy"); }
        p.putAll(oryginal);
        p.setProperty("url", "jdbc:mysql://127.0.0.1:1/hotel_patch_test?connectTimeout=1000&socketTimeout=1000&sslMode=DISABLED");
        long start = System.nanoTime();
        try { TestPolaczenia.sprawdz(p); throw new AssertionError("Brak serwera"); }
        catch (SQLException e) { check(KomunikatyBledu.dlaUzytkownika(e).contains("połączeni"), "Brak serwera"); }
        check((System.nanoTime() - start) / 1000000 < 5000, "Ograniczony czas polaczenia");
        p.setProperty("url", "jdbc:mysql://localhost/hotel_patch_test?allowMultiQueries=true");
        try { TestPolaczenia.sprawdz(p); throw new AssertionError("Niebezpieczna opcja"); }
        catch (SQLException e) { check(e.getMessage().contains("niedozwoloną"), "Test zachowuje zabezpieczenia JDBC"); }
        check(java.util.Arrays.equals(przed, Files.readAllBytes(config)), "Bledne testy tez nie zapisuja ustawien");
        System.out.println("CONNECTION_TEST_OK checks=" + ile);
    }

    private static String stan() throws Exception {
        try (var c = TestDatabase.polacz(); var s = c.createStatement(); var r = s.executeQuery("SELECT (SELECT COUNT(*) FROM goscie), (SELECT COUNT(*) FROM rezerwacje), COALESCE(SUM(nieudane_logowania),0) FROM pracownicy")) {
            r.next(); return r.getLong(1) + ":" + r.getLong(2) + ":" + r.getLong(3);
        }
    }
}
