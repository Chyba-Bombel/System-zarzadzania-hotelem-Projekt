package pl.hotel.bezpieczenstwo;

import java.sql.SQLException;

public final class KomunikatyBledu {
    private KomunikatyBledu() {}
    private static String znanyBlad(SQLException s) {
        return switch (s.getErrorCode()) {
            case 1062 -> "Taki numer pokoju lub login już istnieje.";
            case 1044, 1045 -> "Odmowa dostępu do bazy. Sprawdź użytkownika, hasło i uprawnienia konta MySQL w ustawieniach połączenia.";
            case 1049 -> "Nie znaleziono bazy. Sprawdź nazwę bazy w ustawieniach połączenia lub zaimportuj właściwy skrypt SQL.";
            case 1054, 1146 -> "Brakuje tabeli lub kolumny. Struktura bazy nie odpowiada wersji aplikacji; wykonaj właściwą aktualizację SQL z folderu db po zrobieniu kopii bazy.";
            case 1213, 1205 -> "Dane zmienia inny użytkownik. Odśwież je i spróbuj ponownie.";
            default -> null;
        };
    }

    public static String dlaUzytkownika(Throwable e) {
        if (e instanceof pl.hotel.konfiguracja.BladKonfiguracji) return e.getMessage();
        java.util.Set<Throwable> widziane = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Throwable obecny = e; obecny != null && widziane.size() < 16 && widziane.add(obecny); obecny = obecny.getCause()) {
            if (obecny instanceof java.net.ConnectException || obecny instanceof java.net.SocketTimeoutException)
                return "Brak połączenia z serwerem bazy. Sprawdź działanie MySQL/MariaDB, adres i port. Po błędzie zapisu odśwież dane przed ponowieniem: operacja mogła zostać wykonana.";
            if (obecny instanceof javax.net.ssl.SSLException)
                return "Nie można zweryfikować bezpiecznego połączenia z bazą. Sprawdź certyfikat, nazwę serwera i konfigurację TLS.";
            if (obecny instanceof SQLException s) {
                String znany = znanyBlad(s);
                if (znany != null) return znany;
                java.util.Set<SQLException> lancuch = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
                for (SQLException nastepny = s.getNextException(); nastepny != null && lancuch.size() < 16 && lancuch.add(nastepny); nastepny = nastepny.getNextException()) {
                    znany = znanyBlad(nastepny);
                    if (znany != null) return znany;
                }
            }
        }
        if(e instanceof SQLException s) {
            if(s.getSQLState()!=null && s.getSQLState().startsWith("08")) return "Utracono połączenie. Sprawdź MySQL. Po zapisie najpierw odśwież dane: operacja mogła zostać wykonana.";
            if(s.getSQLState()!=null) return "Operacja bazy nie powiodła się. Sprawdź dane, połączenie i uprawnienia konta technicznego.";
        }
        if(e instanceof SQLException || e instanceof IllegalArgumentException || e instanceof SecurityException)
            return e.getMessage()==null ? "Nie można wykonać operacji." : e.getMessage();
        return "Nie udało się wykonać operacji. Odśwież dane i spróbuj ponownie.";
    }
}
