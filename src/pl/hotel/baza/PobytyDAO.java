package pl.hotel.baza;

import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDate;

final class PobytyDAO {
    private PobytyDAO() {}

    public static void zameldujGoscia(int id) throws SQLException{
        try (Connection polaczenie = PolaczenieBazy.polacz()){

            polaczenie.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            polaczenie.setAutoCommit(false);

            try {
                long pokojId;

                String sqlPokojId = "SELECT pokoj_id FROM rezerwacje WHERE id = ?";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlPokojId)){
                    zapytanie.setInt(1,id);
                    try(ResultSet wynik = zapytanie.executeQuery()) {
                        if (!wynik.next()){
                            throw new SQLException("Nie znaleziono rezerwacji.");
                        }

                        pokojId = wynik.getLong("pokoj_id");

                    }
                }

                String sqlPokoj = "SELECT stan FROM pokoje WHERE id = ? FOR UPDATE";

                try (PreparedStatement zapytanie  = polaczenie.prepareStatement(sqlPokoj)){
                    zapytanie.setLong(1, pokojId);

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if (!wynik.next()){
                            throw new SQLException("Pokój nie istnieje");
                        }

                        if (!wynik.getString("stan").equals("GOTOWY")){
                            throw new SQLException("Pokój nie jest gotowy do zameldowania");
                        }
                    }

                }
                String sqlRezerwacja = "SELECT pokoj_id, status, data_od, data_do " + "FROM rezerwacje WHERE id = ? FOR UPDATE";

                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlRezerwacja)){
                    zapytanie.setInt(1,id);

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if (!wynik.next()){
                            throw new SQLException("Nie znaleziono rezerwacji.");
                        }
                        if (wynik.getLong("pokoj_id") != pokojId){
                            throw new SQLException("Zmieniono pokój rezerwacji. Spróbuj ponownie.");
                        }
                        if (!wynik.getString("status").equals("POTWIERDZONA")){
                            throw new SQLException("Zameldować można tylko potwierdzoną rezerwację.");
                        }

                        LocalDate dzis = LocalDate.now();
                        LocalDate od = wynik.getDate("data_od").toLocalDate();
                        LocalDate doDnia = wynik.getDate("data_do").toLocalDate();

                        if (dzis.isBefore(od) || !dzis.isBefore(doDnia)){
                            throw new SQLException("Dzisiejsza data nie przypada w terminie pobytu");
                        }

                    }

                }
                String sqlZajety =
                        "SELECT id FROM rezerwacje " + "WHERE pokoj_id = ? "
                                + "AND status = 'ZAMELDOWANA' "
                                + "AND id <> ?";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlZajety)){
                    zapytanie.setLong(1,pokojId);
                    zapytanie.setInt(2,id);

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if (wynik.next()){
                            throw new SQLException("W tym pokoju jest już zameldowany gość");
                        }

                    }

                }
                String przedZmiana = HistoriaDAO.stan(polaczenie, id);
                String sqlZmiana =
                        "UPDATE rezerwacje "
                                + "SET status = 'ZAMELDOWANA' "
                                + "WHERE id = ?";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlZmiana)){
                    zapytanie.setInt(1,id);
                    zapytanie.executeUpdate();

                }
                HistoriaDAO.zapisz(polaczenie, id, "ZAMELDOWANIE", przedZmiana);
                polaczenie.commit();
            } catch (SQLException | RuntimeException e){
                try {
                    polaczenie.rollback();
                }catch (SQLException bladWycofania){
                    e.addSuppressed(bladWycofania);
                }
                throw e;
            }

        }
    }

    public static void wymeldujGoscia(int id)
            throws SQLException {

        try (Connection polaczenie = PolaczenieBazy.polacz()) {

            polaczenie.setTransactionIsolation(
                    Connection.TRANSACTION_READ_COMMITTED
            );

            polaczenie.setAutoCommit(false);

            try {
                long pokojId;

                String sqlPokojId =
                        "SELECT pokoj_id FROM rezerwacje WHERE id = ?";

                try (PreparedStatement zapytanie =
                             polaczenie.prepareStatement(sqlPokojId)) {

                    zapytanie.setInt(1, id);

                    try (ResultSet wynik = zapytanie.executeQuery()) {
                        if (!wynik.next()) {
                            throw new SQLException(
                                    "Nie znaleziono rezerwacji."
                            );
                        }

                        pokojId = wynik.getLong("pokoj_id");
                    }
                }

                String sqlPokoj =
                        "SELECT stan FROM pokoje WHERE id = ? FOR UPDATE";

                String stanPokoju;

                try (PreparedStatement zapytanie =
                             polaczenie.prepareStatement(sqlPokoj)) {

                    zapytanie.setLong(1, pokojId);

                    try (ResultSet wynik = zapytanie.executeQuery()) {
                        if (!wynik.next()) {
                            throw new SQLException("Pokój nie istnieje.");
                        }

                        stanPokoju = wynik.getString("stan");
                    }
                }

                String sqlRezerwacja = "SELECT pokoj_id, status, cena_za_noc, "
                                + "DATEDIFF(data_do, data_od) AS noce "
                                + "FROM rezerwacje WHERE id = ? FOR UPDATE";

                try (PreparedStatement zapytanie =
                             polaczenie.prepareStatement(sqlRezerwacja)) {

                    zapytanie.setInt(1, id);

                    try (ResultSet wynik = zapytanie.executeQuery()) {
                        if (!wynik.next()) {
                            throw new SQLException(
                                    "Nie znaleziono rezerwacji."
                            );
                        }

                        if (wynik.getLong("pokoj_id") != pokojId) {
                            throw new SQLException("Zmieniono pokój rezerwacji. Spróbuj ponownie.");
                        }

                        if (!wynik.getString("status").equals("ZAMELDOWANA")) {

                            throw new SQLException("Wymeldować można tylko zameldowaną rezerwację.");
                        }
                        BigDecimal naleznosc = wynik.getBigDecimal("cena_za_noc").multiply(BigDecimal.valueOf(wynik.getLong("noce")));

                        naleznosc=naleznosc.add(KosztyDAO.sumaUslug(polaczenie,id));
                        BigDecimal zaplacono = RozliczeniaDAO.pobierzNetto(polaczenie, id);
                        BigDecimal saldo = naleznosc.subtract(zaplacono);

                        if (saldo.signum() > 0) {
                            throw new SQLException("Nie można wymeldować. Pozostało do zapłaty: " + saldo + " zł.");
                        }

                        if (saldo.signum() < 0) {
                            throw new SQLException("Przed wymeldowaniem zarejestruj zwrot nadpłaty: " + saldo.abs() + " zł.");
                        }
                    }
                }

                String przedZmiana = HistoriaDAO.stan(polaczenie, id);
                String sqlZakonczenie =
                        "UPDATE rezerwacje "
                                + "SET status = 'ZAKONCZONA' "
                                + "WHERE id = ?";

                try (PreparedStatement zapytanie =
                             polaczenie.prepareStatement(sqlZakonczenie)) {

                    zapytanie.setInt(1, id);
                    zapytanie.executeUpdate();
                }

                if (!stanPokoju.equals("WYLACZONY")) {

                    String sqlSprzatanie =
                            "UPDATE pokoje "
                                    + "SET stan = 'DO_SPRZATANIA' "
                                    + "WHERE id = ?";

                    try (PreparedStatement zapytanie =
                                 polaczenie.prepareStatement(sqlSprzatanie)) {

                        zapytanie.setLong(1, pokojId);
                        zapytanie.executeUpdate();
                    }
                }

                HistoriaDAO.zapisz(polaczenie, id, "WYMELDOWANIE", przedZmiana);
                polaczenie.commit();

            } catch (SQLException | RuntimeException e) {
                try {
                    polaczenie.rollback();
                } catch (SQLException bladWycofania) {
                    e.addSuppressed(bladWycofania);
                }

                throw e;
            }
        }
    }
}
