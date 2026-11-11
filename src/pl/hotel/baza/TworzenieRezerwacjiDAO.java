package pl.hotel.baza;

import pl.hotel.model.*;
import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Operacje obszaru: TworzenieRezerwacjiDAO. */
final class TworzenieRezerwacjiDAO {
    private TworzenieRezerwacjiDAO() {}

    public static Rezerwacja zapiszRezerwacje(
            Gosc gosc,
            Pokoj pokoj,
            LocalDate od,
            LocalDate doDnia,
            int liczbaOsob,
            long pracownikId
    ) throws SQLException {
        return zapiszRezerwacje(gosc, pokoj, od, doDnia, liczbaOsob, pracownikId, null);
    }

    public static Rezerwacja zapiszRezerwacje(Gosc gosc, Pokoj pokoj, LocalDate od,
            LocalDate doDnia, int liczbaOsob, long pracownikId,
            java.util.List<Uczestnik> uczestnicy) throws SQLException {
        java.util.List<Uczestnik> osoby = uczestnicy == null ? null : java.util.List.copyOf(uczestnicy);
        if (osoby != null && osoby.size() != liczbaOsob - 1)
            throw new IllegalArgumentException("Uzupełnij dane wszystkich dodatkowych osób.");
        if(od.isBefore(LocalDate.now()) || !doDnia.isAfter(od) || ChronoUnit.DAYS.between(od, doDnia) > 30 ){
            throw new SQLException("Pobyt nie może trwać więcej niż 30 nocy i nie może zaczynać sie w przeszłości");
        }
        if (liczbaOsob < 1){
            throw new SQLException("Liczba osób musi być zawsze większa niż 0");
        }

        try (Connection polaczenie = PolaczenieBazy.polacz()) {
            polaczenie.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            polaczenie.setAutoCommit(false);

            try {
                BigDecimal cenaZaNoc;

                String sqlPokoj = "SELECT liczba_miejsc, cena_za_noc, stan "
                        + "FROM pokoje WHERE id = ? FOR UPDATE";

                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlPokoj)) {
                    zapytanie.setLong(1,pokoj.getId());

                    try(ResultSet wynik = zapytanie.executeQuery()) {

                        if(!wynik.next()){
                            throw new SQLException("Pokój nie istnieje");
                        }
                        if (wynik.getString("stan").equals("WYLACZONY")){
                            throw new SQLException("Pokój jest wyłaczony z uzytkownia");
                        }
                        if (liczbaOsob > wynik.getInt("liczba_miejsc")){
                            throw new SQLException("Za dużo osób dla wybranego pokoju");
                        }

                        cenaZaNoc = wynik.getBigDecimal("cena_za_noc");
                    }

                }
                String sqlPracownik =
                        "SELECT id FROM pracownicy "
                                + "WHERE id = ? AND aktywny = TRUE";

                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlPracownik)){
                    zapytanie.setLong(1, pracownikId);

                    try(ResultSet wynik = zapytanie.executeQuery()){
                        if (!wynik.next()){
                            throw new SQLException("Nie znaleziono aktywnego pracownika");
                        }
                    }

                }
                String sqlDostepnosc =
                        "SELECT id FROM rezerwacje "
                                + "WHERE pokoj_id = ? "
                                + "AND status IN ('POTWIERDZONA', 'ZAMELDOWANA') "
                                + "AND data_od < ? "
                                + "AND data_do > ?";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(sqlDostepnosc)){

                    zapytanie.setLong(1,pokoj.getId());
                    zapytanie.setDate(2, Date.valueOf(doDnia));
                    zapytanie.setDate(3, Date.valueOf(od));

                    try (ResultSet wynik = zapytanie.executeQuery()){
                        if (wynik.next()){
                            throw new SQLException("Pokój jest zajęty w wybranym terminie");
                        }
                    }

                }
                String sqlZapis =
                        "INSERT INTO rezerwacje "
                                + "(gosc_id, pokoj_id, pracownik_id, "
                                + "data_od, data_do, liczba_osob, "
                                + "cena_za_noc, status) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, 'POTWIERDZONA')";
                try (PreparedStatement zapytanie = polaczenie.prepareStatement(
                        sqlZapis,
                        Statement.RETURN_GENERATED_KEYS
                )){
                    zapytanie.setLong(1, gosc.getId());
                    zapytanie.setLong(2, pokoj.getId());
                    zapytanie.setLong(3, pracownikId);
                    zapytanie.setDate(4, Date.valueOf(od));
                    zapytanie.setDate(5, Date.valueOf(doDnia));
                    zapytanie.setInt(6, liczbaOsob);
                    zapytanie.setBigDecimal(7, cenaZaNoc);

                    zapytanie.executeUpdate();

                    try (ResultSet wynik = zapytanie.getGeneratedKeys()){
                        if (!wynik.next()){
                            throw new SQLException("Baza nie zwróciła id.");
                        }

                        long idZbazy = wynik.getLong(1);

                        if (idZbazy < 1 || idZbazy > Integer.MAX_VALUE){
                            throw new SQLException("ID rezerwacji nie mieści się w typie int.");
                        }

                        Rezerwacja nowa = new Rezerwacja(
                                (int) idZbazy,
                                gosc,
                                pokoj,
                                od,
                                doDnia,
                                liczbaOsob,
                                cenaZaNoc,
                                "POTWIERDZONA"
                        );

                        if (osoby != null) {
                            UczestnicyDAO.zapiszUczestnikow(polaczenie, (int) idZbazy, osoby);
                            nowa.ustawUczestnikow(osoby);
                        }
                        polaczenie.commit();
                        return nowa;

                    }
                }
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
