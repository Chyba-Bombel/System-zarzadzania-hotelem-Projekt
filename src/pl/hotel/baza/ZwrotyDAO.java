package pl.hotel.baza;

import java.sql.*;
import java.math.BigDecimal;

final class ZwrotyDAO {
    private ZwrotyDAO() {}

    public static void zapiszZwrot(int rezerwacjaId, BigDecimal kwota, String metoda, long pracownikId) throws SQLException{
        if (kwota == null
                || kwota.signum() <= 0
                || kwota.scale() > 2
                || kwota.compareTo(new BigDecimal("99999999.99")) > 0) {

            throw new SQLException("Niepoprawna kwota zwrotu.");
        }
        if (!"GOTOWKA".equals(metoda)
                && !"KARTA".equals(metoda)
                && !"PRZELEW".equals(metoda)) {

            throw new SQLException("Niepoprawna metoda zwrotu.");
        }
        try (Connection polaczenie = PolaczenieBazy.polacz()){

            polaczenie.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            polaczenie.setAutoCommit(false);

            try {
                String sqlRezerwacja = "SELECT id FROM rezerwacje "
                                + "WHERE id = ? FOR UPDATE";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlRezerwacja)){
                    zapytanie.setInt(1,rezerwacjaId);

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if (!wynik.next()){
                            throw new SQLException("Nie znaleziono rezerwacji.");
                        }

                    }

                }
                String sqlPracownik = "SELECT id FROM pracownicy "
                                + "WHERE id = ? AND aktywny = TRUE AND rola = 'ADMIN'";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlPracownik)){

                    zapytanie.setLong(1, pracownikId);

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if(!wynik.next()){
                            throw new SQLException("Nie znaleziono aktywnego pracownika.");
                        }

                    }

                }
                String sqlSaldo = "SELECT COALESCE(SUM("
                        + "CASE WHEN rodzaj = 'WPLATA' THEN kwota "
                        + "ELSE -kwota END), 0) AS netto "
                        + "FROM platnosci WHERE rezerwacja_id = ?";

                BigDecimal dostepneDoZwrotu;

                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlSaldo)){

                    zapytanie.setInt(1,rezerwacjaId);

                    try (ResultSet wynik = zapytanie.executeQuery()){

                        wynik.next();

                        dostepneDoZwrotu = wynik.getBigDecimal("netto");

                    }

                }
                if (kwota.compareTo(dostepneDoZwrotu) > 0){
                    throw new SQLException("Za duży zwrot. Maksymalnie można zwrócić: " + dostepneDoZwrotu + " zł.");

                }
                String sqlZapis = "INSERT INTO platnosci "
                                + "(rezerwacja_id, pracownik_id, kwota, "
                                + "rodzaj, metoda) "
                                + "VALUES (?, ?, ?, 'ZWROT', ?)";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlZapis)){
                    zapytanie.setInt(1, rezerwacjaId);
                    zapytanie.setLong(2, pracownikId);
                    zapytanie.setBigDecimal(3, kwota);
                    zapytanie.setString(4, metoda);

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
