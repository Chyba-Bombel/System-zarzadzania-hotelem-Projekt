import java.sql.*;
import java.util.Properties;
import java.nio.file.*;

public final class TestDatabase {
    private TestDatabase() {}
    public static void wymagajTestowejBazy() throws Exception {
        Properties p=konfiguracja();
        String baza=java.net.URI.create(p.getProperty("url").substring(5)).getPath().substring(1);
        if(!Boolean.getBoolean("hotel.tests") || !baza.matches("[A-Za-z0-9_]*_test(?:_[0-9]+)?"))
            throw new IllegalStateException("Testy wymagają -Dhotel.tests=true i osobnej bazy kończącej się _test lub _test_<liczba>.");
    }
    private static Properties konfiguracja() throws Exception {
        Properties p=new Properties();
        try(var r=Files.newBufferedReader(Path.of(System.getProperty("hotel.config","db.properties")))) { p.load(r); }
        return p;
    }
    public static Connection polacz() throws Exception {
        wymagajTestowejBazy(); Properties p=konfiguracja();
        return DriverManager.getConnection(p.getProperty("url"),p.getProperty("user"),p.getProperty("password"));
    }
}
