package pl.hotel.baza;

import pl.hotel.konfiguracja.KonfiguracjaAplikacji;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public final class UpdateConnectionProbe {
    private static Properties ostatnie;
    private static int ile;

    public static void main(String[] args) throws Exception {
        Path plik = Files.createTempFile("hotel-connection-", ".properties");
        System.setProperty("hotel.config", plik.toString());
        Driver atrapowy = new Driver() {
            public Connection connect(String url, Properties info) {
                ostatnie = new Properties(); ostatnie.putAll(info);
                return (Connection) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class}, (proxy, metoda, parametry) -> {
                    if (metoda.getName().equals("isClosed")) return false;
                    return null;
                });
            }
            public boolean acceptsURL(String url) { return url.startsWith("jdbc:mysql://"); }
            public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) { return new DriverPropertyInfo[0]; }
            public int getMajorVersion() { return 1; }
            public int getMinorVersion() { return 0; }
            public boolean jdbcCompliant() { return false; }
            public Logger getParentLogger() { return Logger.getGlobal(); }
        };
        DriverManager.registerDriver(atrapowy);
        try {
            Properties p = new Properties();
            p.setProperty("url", "jdbc:mysql://localhost:3306/hotel_test?sslMode=DISABLED");
            p.setProperty("user", "test-user"); p.setProperty("password", "test-secret");
            KonfiguracjaAplikacji.zapisz(p);
            try (Connection c = PolaczenieBazy.polacz()) { check(c != null, "Utworzono polaczenie przez sterownik"); }
            check(ostatnie.getProperty("user").equals("test-user"), "Przekazano konto z nowej konfiguracji");
            check(ostatnie.getProperty("allowMultiQueries").equals("false"), "Ochrona multi queries");
            check(ostatnie.getProperty("paranoid").equals("true"), "Ochrona komunikatow sterownika");
            p.setProperty("url", "jdbc:mysql://example.invalid/hotel_test?sslMode=DISABLED");
            KonfiguracjaAplikacji.zapisz(p);
            try { PolaczenieBazy.polacz(); throw new AssertionError("Zdalne polaczenie bez TLS"); }
            catch (SQLException e) { check(e.getMessage().contains("VERIFY_IDENTITY"), "Weryfikacja TLS pozostala aktywna"); }
            p.setProperty("url", "jdbc:mysql://localhost/hotel_test?allowMultiQueries=true");
            KonfiguracjaAplikacji.zapisz(p);
            try { PolaczenieBazy.polacz(); throw new AssertionError("Niedozwolona opcja JDBC"); }
            catch (SQLException e) { check(e.getMessage().contains("niedozwoloną"), "Nie mozna wlaczyc multi queries"); }
            System.out.println("UPDATE_CONNECTION_PROBE_OK checks=" + ile);
        } finally { DriverManager.deregisterDriver(atrapowy); Files.deleteIfExists(plik); }
    }

    private static void check(boolean warunek, String tekst) {
        if (!warunek) throw new AssertionError(tekst);
        ile++;
    }
}
