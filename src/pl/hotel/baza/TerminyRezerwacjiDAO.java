package pl.hotel.baza;

import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Operacje obszaru: TerminyRezerwacjiDAO. */
final class TerminyRezerwacjiDAO {
    private TerminyRezerwacjiDAO() {}

    public static void zmienTerminRezerwacji(int id, LocalDate noweOd, LocalDate noweDo) throws SQLException {
        if (noweOd.isBefore(LocalDate.now()) || !noweDo.isAfter(noweOd) || ChronoUnit.DAYS.between(noweOd, noweDo) > 30){
            throw new SQLException("Pobyt musi trwać do 30 nocy i nie może zaczynać sie w przeszłości");
        }
        try (Connection polaczenie = PolaczenieBazy.polacz()){
            polaczenie.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            polaczenie.setAutoCommit(false);
            try {
                long pokojID;
                BigDecimal stawka;

                String sqlPokojID =  "SELECT pokoj_id FROM rezerwacje WHERE id = ?";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlPokojID)){
                    zapytanie.setInt(1, id);

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if(!wynik.next()){
                            throw new SQLException("Nie znaleizono rezerwacji.");

                        }
                        pokojID = wynik.getLong("pokoj_id");


                    }

                }
                String sqlPokoj =  "SELECT stan FROM pokoje WHERE id = ? FOR UPDATE";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlPokoj)){
                    zapytanie.setLong(1,pokojID);
                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if (!wynik.next()){
                            throw new SQLException("Pokój nie istnieje");
                        }

                        if (wynik.getString("stan").equals("WYLACZONY")){
                            throw new SQLException("Pokój jest wyłączony z użytkownia.");

                        }

                    }

                }
                String sqlRezerwacja = "SELECT status, pokoj_id, cena_za_noc "
                                + "FROM rezerwacje WHERE id = ? FOR UPDATE";
                try(PreparedStatement zapytanie = polaczenie.prepareStatement(sqlRezerwacja)) {
                    zapytanie.setInt(1,id);

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if (!wynik.next()){
                            throw new SQLException("Nie znaleziono rezerwacji.");

                        }

                        if (wynik.getLong("pokoj_id") != pokojID){
                            throw new SQLException("Zmieniono pokój rezerwacji. Spróbuj ponownie.");
                        }
                        if (!wynik.getString("status").equals("POTWIERDZONA")){
                            throw new SQLException("Termin można zmienić tylko w potwierdzonej rezerwacji");

                        }

                        stawka = wynik.getBigDecimal("cena_za_noc");

                    }

                }
                long noweNoce = ChronoUnit.DAYS.between(noweOd, noweDo);

                BigDecimal nowaNaleznosc =
                        stawka.multiply(BigDecimal.valueOf(noweNoce));

                BigDecimal netto = RozliczeniaDAO.pobierzNetto(polaczenie, id);

                if (netto.signum() < 0) {
                    throw new SQLException(
                            "Nieprawidłowe rozliczenie: zwroty przekraczają wpłaty."
                    );
                }

                if (netto.compareTo(nowaNaleznosc) > 0) {
                    BigDecimal nadplata = netto.subtract(nowaNaleznosc);

                    throw new SQLException(
                            "Nowy pobyt kosztowałby " + nowaNaleznosc
                                    + " zł. Najpierw zarejestruj zwrot "
                                    + nadplata + " zł w opcji 19."
                    );
                }
                String sqlDostepnosc = "SELECT id FROM rezerwacje "
                        + "WHERE pokoj_id = ? "
                        + "AND id <> ? "
                        + "AND status IN ('POTWIERDZONA', 'ZAMELDOWANA') "
                        + "AND data_od < ? "
                        + "AND data_do > ?";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlDostepnosc)){
                    zapytanie.setLong(1,pokojID);
                    zapytanie.setInt(2,id);
                    zapytanie.setDate(3, Date.valueOf(noweDo));
                    zapytanie.setDate(4, Date.valueOf(noweOd));

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if (wynik.next()){
                            throw new SQLException("Pokój jest zajęty w nowym terminie");
                        }

                    }

                }
                String sqlZmiana = "UPDATE rezerwacje "
                        + "SET data_od = ?, data_do = ? "
                        + "WHERE id = ?";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlZmiana)){
                    zapytanie.setDate(1,Date.valueOf(noweOd));
                    zapytanie.setDate(2, Date.valueOf(noweDo));
                    zapytanie.setInt(3, id);

                    zapytanie.executeUpdate();

                }

                polaczenie.commit();
            } catch (SQLException | RuntimeException e){
                try {
                    polaczenie.rollback();
                } catch (SQLException bladWycofania){
                    e.addSuppressed(bladWycofania);
                }
                throw e;
            }


        }

    }
}
