package pl.hotel.baza;

import java.sql.*;
import java.util.ArrayList;
import java.util.Objects;
import pl.hotel.model.StronaHistorii;
import pl.hotel.model.ZmianaRezerwacji;

final class HistoriaDAO {
    private HistoriaDAO() {}

    static String stan(Connection c, int id) throws SQLException {
        try (PreparedStatement q = c.prepareStatement(
                "SELECT r.pokoj_id, p.numer, r.data_od, r.data_do, r.status, r.liczba_osob, r.cena_za_noc "
                        + "FROM rezerwacje r JOIN pokoje p ON p.id=r.pokoj_id WHERE r.id=?")) {
            q.setInt(1, id);
            try (ResultSet r = q.executeQuery()) {
                if (!r.next()) throw new SQLException("Nie znaleziono rezerwacji.");
                return "Pokój: " + r.getString("numer") + " (ID " + r.getLong("pokoj_id") + ")"
                        + " | Termin: " + r.getDate("data_od") + " – " + r.getDate("data_do")
                        + " | Status: " + r.getString("status") + " | Osoby: " + r.getInt("liczba_osob")
                        + " | Stawka za noc: " + r.getBigDecimal("cena_za_noc").toPlainString() + " zł";
            }
        }
    }

    static void zapisz(Connection c, int id, String akcja, String przed) throws SQLException {
        String po = stan(c, id);
        if (Objects.equals(przed, po)) return;
        opis(c,id,akcja,przed,po,null);
    }
    static void opis(Connection c,int id,String akcja,String przed,String po,Long platnoscId)throws SQLException {
        if(Objects.equals(przed,po))return;
        try(PreparedStatement q=c.prepareStatement("INSERT INTO historia_rezerwacji(rezerwacja_id,pracownik_id,login_pracownika,akcja,przed,po,platnosc_id) SELECT ?,id,login,?,?,?,? FROM pracownicy WHERE id=?")){
            q.setInt(1,id);q.setString(2,akcja);q.setString(3,przed);q.setString(4,po);
            if(platnoscId==null)q.setNull(5,Types.BIGINT);else q.setLong(5,platnoscId);
            q.setLong(6,KontrolaDostepu.identyfikator());
            if(q.executeUpdate()!=1)throw new SQLException("Nie udało się zapisać historii.");
        }
    }

    static StronaHistorii pobierz(int id, long przedId) throws SQLException {
        if (id < 1 || przedId < 0) throw new SQLException("Podaj poprawne ID rezerwacji i strony historii.");
        try (Connection c = PolaczenieBazy.polacz()) {
            try (PreparedStatement q = c.prepareStatement("SELECT id FROM rezerwacje WHERE id=?")) {
                q.setInt(1, id);
                try (ResultSet r = q.executeQuery()) {
                    if (!r.next()) throw new SQLException("Nie znaleziono rezerwacji.");
                }
            }
            Timestamp czas=null;
            if(przedId!=0)try(PreparedStatement q=c.prepareStatement("SELECT utworzono FROM historia_rezerwacji WHERE rezerwacja_id=? AND id=?")){
                q.setInt(1,id);q.setLong(2,przedId);try(ResultSet r=q.executeQuery()){
                    if(!r.next())throw new SQLException("Nieprawidłowa strona historii.");czas=r.getTimestamp(1);
                }
            }
            String sql = "SELECT id, pracownik_id, login_pracownika, akcja, przed, po, utworzono "
                    + "FROM historia_rezerwacji WHERE rezerwacja_id=? "
                    + (przedId == 0 ? "" : "AND (utworzono < ? OR (utworzono = ? AND id < ?)) ") + "ORDER BY utworzono DESC,id DESC LIMIT 101";
            ArrayList<ZmianaRezerwacji> wpisy = new ArrayList<>();
            try (PreparedStatement q = c.prepareStatement(sql)) {
                q.setInt(1, id);
                if(przedId!=0){q.setTimestamp(2,czas);q.setTimestamp(3,czas);q.setLong(4,przedId);}
                try (ResultSet r = q.executeQuery()) {
                    while (r.next()) wpisy.add(new ZmianaRezerwacji(r.getLong("id"), r.getLong("pracownik_id"),
                            r.getString("login_pracownika"), r.getString("akcja"), r.getString("przed"),
                            r.getString("po"), r.getTimestamp("utworzono").toLocalDateTime()));
                }
            }
            boolean starsze = wpisy.size() > 100;
            if (starsze) wpisy.remove(100);
            return new StronaHistorii(wpisy, starsze);
        }
    }
}
