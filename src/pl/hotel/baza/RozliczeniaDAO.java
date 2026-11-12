package pl.hotel.baza;

import pl.hotel.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

final class RozliczeniaDAO {
    private RozliczeniaDAO() {}

    public static Rozliczenie pobierzRozliczenie(int id) throws SQLException{
        try(Connection c=PolaczenieBazy.polacz()){
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setReadOnly(true);c.setAutoCommit(false);
            try{var r=pobierz(c,id);c.commit();return r;}
            catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException b){e.addSuppressed(b);}throw e;}
        }
    }
    static Rozliczenie pobierz(Connection polaczenie,int id)throws SQLException {
        String sql =
                "SELECT r.id AS rezerwacja_id, r.status, "
                        + "r.data_od, r.data_do, r.cena_za_noc, "
                        + "p.id AS platnosc_id, p.kwota, p.rodzaj, "
                        + "p.metoda, p.data_platnosci "
                        + "FROM rezerwacje r "
                        + "LEFT JOIN platnosci p ON p.rezerwacja_id = r.id "
                        + "WHERE r.id = ? "
                        + "ORDER BY p.data_platnosci, p.id";
        try (PreparedStatement zapytanie = polaczenie.prepareStatement(sql)){
            zapytanie.setInt(1, id);

            try (ResultSet wynik = zapytanie.executeQuery()){
                if (!wynik.next()){
                    throw new SQLException("Nie znaleziono rezerwacji.");
                }

                String status = wynik.getString("status");
                LocalDate od = wynik.getDate("data_od").toLocalDate();
                LocalDate doDnia = wynik.getDate("data_do").toLocalDate();

                long liczbaNocy = ChronoUnit.DAYS.between(od,doDnia);
                BigDecimal cenaPobytu = wynik.getBigDecimal("cena_za_noc").multiply(BigDecimal.valueOf(liczbaNocy));
                BigDecimal uslugi=KosztyDAO.sumaUslug(polaczenie,id);
                BigDecimal naleznosc=status.equals("ANULOWANA")?new BigDecimal("0.00"):cenaPobytu.add(uslugi);

                BigDecimal wplaty = new BigDecimal("0.00");
                BigDecimal zwroty = new BigDecimal("0.00");

                ArrayList<Platnosc> platnosi = new ArrayList<>();

                do {
                    long platnoscId = wynik.getLong("platnosc_id");

                    if (wynik.wasNull()){
                        continue;
                    }
                    BigDecimal kwota = wynik.getBigDecimal("kwota");
                    String rodzaj = wynik.getString("rodzaj");

                    if (rodzaj.equals("WPLATA")){
                        wplaty = wplaty.add(kwota);
                    } else if (rodzaj.equals("ZWROT")) {
                        zwroty = zwroty.add(kwota);
                    }

                    Platnosc platnosc = new Platnosc(platnoscId, kwota, rodzaj, wynik.getString("metoda"), wynik.getTimestamp("data_platnosci").toLocalDateTime());

                    platnosi.add(platnosc);
                } while (wynik.next());

                return new Rozliczenie(id,status,cenaPobytu,uslugi,naleznosc,wplaty,zwroty,platnosi);

            }

        }
    }

    static BigDecimal pobierzNetto(Connection polaczenie, int rezerwacjaId) throws SQLException {
        String sql = "SELECT COALESCE(SUM("
                        + "CASE WHEN rodzaj = 'WPLATA' THEN kwota "
                        + "ELSE -kwota END), 0) AS netto "
                        + "FROM platnosci WHERE rezerwacja_id = ?";

        try (PreparedStatement zapytanie = polaczenie.prepareStatement(sql)) {

            zapytanie.setInt(1, rezerwacjaId);

            try (ResultSet wynik = zapytanie.executeQuery()) {
                wynik.next();
                return wynik.getBigDecimal("netto");
            }
        }
    }
}
