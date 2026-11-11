package pl.hotel.baza;

import java.sql.*;
import java.math.BigDecimal;

final class WplatyDAO {
    private WplatyDAO() {}

    public static void zapiszWplate(
            int rezerwacjaId,
            BigDecimal kwota,
            String metoda,
            long pracownikId
    ) throws SQLException{
        if (kwota == null || kwota.signum() <= 0 || kwota.scale() > 2 || kwota.compareTo(new BigDecimal("99999999.99")) > 0){
            throw new SQLException("Niepoprawna kwota wpłaty.");
        }
        if (!"GOTOWKA".equals(metoda) && !"KARTA".equals(metoda) && !"PRZELEW".equals(metoda)){
            throw new SQLException("Niepoprawna metoda płatości.");
        }
        try (Connection polaczenie = PolaczenieBazy.polacz()){
            polaczenie.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            polaczenie.setAutoCommit(false);
            try {
                BigDecimal naleznosc;

                String sqlRezerwacja = "SELECT status, cena_za_noc, "
                        + "DATEDIFF(data_do, data_od) AS noce "
                        + "FROM rezerwacje WHERE id = ? FOR UPDATE";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlRezerwacja)){
                    zapytanie.setInt(1,rezerwacjaId);

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if (!wynik.next()){
                            throw new SQLException("Nie znaleziono rezerwacji.");
                        }

                        if (wynik.getString("status").equals("ANULOWANA")){
                            throw new SQLException("Nie można wpłacać na anulowaną rezerwację.");
                        }

                        naleznosc = wynik.getBigDecimal("cena_za_noc").multiply(BigDecimal.valueOf(wynik.getLong("noce")));

                    }

                }
                String sqlPracownik = "SELECT id FROM pracownicy "
                        + "WHERE id = ? AND aktywny = TRUE";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlPracownik)){
                    zapytanie.setLong(1,pracownikId);

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if (!wynik.next()){
                            throw new SQLException("Nie znaleziono aktywnego pracownika.");
                        }

                    }

                }
                String sqlSaldo =  "SELECT COALESCE(SUM("
                        + "CASE WHEN rodzaj = 'WPLATA' THEN kwota "
                        + "ELSE -kwota END), 0) AS netto "
                        + "FROM platnosci WHERE rezerwacja_id = ?";

                BigDecimal zaplaconoNetto;

                try(PreparedStatement zapytanie = polaczenie.prepareStatement(sqlSaldo)) {
                    zapytanie.setInt(1,rezerwacjaId);

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        wynik.next();
                        zaplaconoNetto = wynik.getBigDecimal("netto");

                    }

                }
                BigDecimal pozostalo = naleznosc.subtract(zaplaconoNetto);

                if (kwota.compareTo(pozostalo) > 0){
                    throw new SQLException("Za duża wpłata. Pozostało do zapłaty: " + pozostalo + " zł");
                }

                String sqlZapis = "INSERT INTO platnosci "
                        + "(rezerwacja_id, pracownik_id, kwota, "
                        + "rodzaj, metoda) "
                        + "VALUES (?, ?, ?, 'WPLATA', ?)";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlZapis)){
                    zapytanie.setInt(1,rezerwacjaId);
                    zapytanie.setLong(2, pracownikId);
                    zapytanie.setBigDecimal(3,kwota);
                    zapytanie.setString(4,metoda);

                    zapytanie.executeUpdate();

                }

                polaczenie.commit();
            } catch (SQLException | RuntimeException e){
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
