package pl.hotel.baza;

import pl.hotel.model.Pracownik;
import pl.hotel.bezpieczenstwo.SesjaWygasla;
import java.sql.*;

final class KontrolaDostepu {
    private record Sesja(long id, String rola, String hash) {}
    private static final ThreadLocal<Sesja> SESJA = new ThreadLocal<>();
    private KontrolaDostepu() {}
    static void otworz(Pracownik p, String hash) { SESJA.set(new Sesja(p.getId(),p.getRola(),hash)); }
    static long identyfikator() {
        Sesja sesja = SESJA.get();
        if (sesja == null) throw new SesjaWygasla();
        return sesja.id();
    }
    static void wyloguj() { SESJA.remove(); }
    static long wymagaj(boolean admin) throws SQLException {
        Sesja s = SESJA.get();
        if (s == null) throw new SesjaWygasla();
        try (Connection c = PolaczenieBazy.polacz(); PreparedStatement p = c.prepareStatement(
                "SELECT rola, aktywny, hash_hasla FROM pracownicy WHERE id=?")) {
            p.setLong(1,s.id());
            try (ResultSet r = p.executeQuery()) {
                if (!r.next() || !r.getBoolean("aktywny") || !s.rola().equals(r.getString("rola")) || !s.hash().equals(r.getString("hash_hasla"))) {
                    wyloguj(); throw new SesjaWygasla();
                }
            }
        }
        if (admin && !s.rola().equals("ADMIN")) throw new SecurityException("Ta operacja wymaga administratora.");
        return s.id();
    }
    static void tozsamosc(long id, boolean admin) throws SQLException {
        if (wymagaj(admin) != id) throw new SecurityException("Nie można wykonać operacji w imieniu innego pracownika.");
    }
    static void sprawdz(Pracownik p, boolean admin) throws SQLException {
        long id = wymagaj(admin);
        if (p == null || p.getId() != id || !SESJA.get().rola().equals(p.getRola())) throw new SesjaWygasla();
    }
}
