package pl.hotel.diagnostyka;

import pl.hotel.konfiguracja.SciezkiAplikacji;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.logging.*;

public final class DziennikBledow {
    private static Logger logger;
    private static boolean inicjowano;
    private DziennikBledow() {}

    private static synchronized Logger logger() {
        if (inicjowano) return logger;
        inicjowano = true;
        try {
            Files.createDirectories(SciezkiAplikacji.logi());
            FileHandler plik = new FileHandler(SciezkiAplikacji.logi().resolve("bledy-%g.log").toString(), 1048576, 3, true);
            plik.setEncoding("UTF-8");
            plik.setFormatter(new Formatter() {
                @Override public String format(LogRecord r) { return Instant.ofEpochMilli(r.getMillis()) + " " + r.getMessage() + System.lineSeparator(); }
            });
            logger = Logger.getLogger("pl.hotel.diagnostyka");
            logger.setUseParentHandlers(false);
            logger.addHandler(plik);
        } catch (IOException | SecurityException e) {
            System.err.println("Nie można utworzyć dziennika błędów HotelManager.");
        }
        return logger;
    }

    public static void zapisz(Throwable blad) {
        Logger l = logger();
        if (l == null || blad == null) return;
        StringBuilder wpis = new StringBuilder("BLAD");
        Set<Throwable> odwiedzone = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable e = blad; e != null && odwiedzone.size() < 8 && odwiedzone.add(e); e = e.getCause()) {
            wpis.append(" typ=").append(e.getClass().getName());
            if (e instanceof SQLException s) {
                String stan = s.getSQLState();
                wpis.append(" kod=").append(s.getErrorCode());
                if (stan != null && stan.matches("[A-Z0-9]{5}")) wpis.append(" sqlState=").append(stan);
            }
            int ile = 0;
            for (StackTraceElement ramka : e.getStackTrace()) {
                if (ile++ >= 12) break;
                wpis.append(" at=").append(ramka.getClassName()).append('.').append(ramka.getMethodName()).append(':').append(ramka.getLineNumber());
            }
        }
        l.warning(wpis.toString());
    }
}
