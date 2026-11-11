package pl.hotel.baza;

import pl.hotel.model.Pracownik;
import pl.hotel.bezpieczenstwo.Hasla;
import java.security.GeneralSecurityException;
import java.sql.*;

final class LogowanieDAO {
    private LogowanieDAO() {}
    private static final String PUSTY_HASH = "pbkdf2_sha256$600000$nRCrfCjzpxXsWYGfmp3rig==$VK5AdfVz4k8seDb2Q/sqbbsnvNdG0a/TahDWSGCIrko=";
    public static Pracownik zaloguj(String login, String haslo) throws SQLException {
        KontrolaDostepu.wyloguj();
        if (login == null || !login.matches("[A-Za-z0-9._-]{3,60}") || haslo == null || haslo.isEmpty() || haslo.length() > 128) return null;
        try (Connection c = PolaczenieBazy.polacz()) {
            c.setAutoCommit(false);
            try {
                long id; String imie, nazwisko, rola, hash; boolean aktywny, zablokowany; int bledy;
                try (PreparedStatement p = c.prepareStatement("SELECT id,imie,nazwisko,rola,hash_hasla,aktywny, "
                        + "CASE WHEN blokada_do IS NOT NULL AND blokada_do <= CURRENT_TIMESTAMP THEN 0 ELSE nieudane_logowania END proby, "
                        + "(blokada_do > CURRENT_TIMESTAMP) zablokowany FROM pracownicy WHERE login=? FOR UPDATE")) {
                    p.setString(1,login);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) { sprawdzHaslo(haslo,PUSTY_HASH); c.commit(); return null; }
                        id=r.getLong("id"); imie=r.getString("imie"); nazwisko=r.getString("nazwisko");
                        rola=r.getString("rola"); hash=r.getString("hash_hasla"); aktywny=r.getBoolean("aktywny");
                        zablokowany=r.getBoolean("zablokowany"); bledy=r.getInt("proby");
                    }
                }
                if (!aktywny || zablokowany) { sprawdzHaslo(haslo,PUSTY_HASH); c.commit(); return null; }
                if (!sprawdzHaslo(haslo,hash)) {
                    try (PreparedStatement p = c.prepareStatement("UPDATE pracownicy SET nieudane_logowania=?, "
                            + "blokada_do=CASE WHEN ? >= 5 THEN DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 5 MINUTE) ELSE NULL END WHERE id=?")) {
                        p.setInt(1,Math.min(bledy+1,5)); p.setInt(2,bledy+1); p.setLong(3,id); p.executeUpdate();
                    }
                    c.commit(); return null;
                }
                if (!rola.equals("ADMIN") && !rola.equals("RECEPCJA")) { c.commit(); return null; }
                try (PreparedStatement p=c.prepareStatement("UPDATE pracownicy SET nieudane_logowania=0, blokada_do=NULL WHERE id=?")) {
                    p.setLong(1,id); p.executeUpdate();
                }
                c.commit();
                Pracownik pracownik=new Pracownik(id,imie,nazwisko,login,rola);
                KontrolaDostepu.otworz(pracownik,hash);
                return pracownik;
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); } throw e;
            }
        }
    }
    private static boolean sprawdzHaslo(String haslo,String hash) throws SQLException {
        try { return Hasla.sprawdz(haslo,hash); }
        catch (GeneralSecurityException e) { throw new SQLException("Nie udało się sprawdzić danych logowania.",e); }
    }
}
