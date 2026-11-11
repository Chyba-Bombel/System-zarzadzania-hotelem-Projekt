package pl.hotel.bezpieczenstwo;

import java.sql.SQLException;

public final class KomunikatyBledu {
    private KomunikatyBledu() {}
    public static String dlaUzytkownika(Throwable e) {
        if(e instanceof SQLException s) {
            if(s.getErrorCode()==1062) return "Taki numer pokoju lub login już istnieje.";
            if(s.getErrorCode()==1045) return "Odmowa połączenia. Sprawdź konto techniczne MySQL.";
            if(s.getErrorCode()==1049) return "Nie znaleziono bazy. Sprawdź konfigurację połączenia.";
            if(s.getErrorCode()==1054 || s.getErrorCode()==1146) return "Schemat bazy wymaga sprawdzenia. Wykonaj odpowiednią aktualizację SQL z folderu db.";
            if(s.getErrorCode()==1213 || s.getErrorCode()==1205) return "Dane zmienia inny użytkownik. Odśwież je i spróbuj ponownie.";
            if(s.getSQLState()!=null && s.getSQLState().startsWith("08")) return "Utracono połączenie. Sprawdź MySQL. Po zapisie najpierw odśwież dane: operacja mogła zostać wykonana.";
            if(s.getSQLState()!=null) return "Operacja bazy nie powiodła się. Sprawdź dane, połączenie i uprawnienia konta technicznego.";
        }
        if(e instanceof SQLException || e instanceof IllegalArgumentException || e instanceof SecurityException)
            return e.getMessage()==null ? "Nie można wykonać operacji." : e.getMessage();
        return "Nie udało się wykonać operacji. Odśwież dane i spróbuj ponownie.";
    }
}
