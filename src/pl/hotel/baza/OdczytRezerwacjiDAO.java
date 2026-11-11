package pl.hotel.baza;

import pl.hotel.model.*;
import java.sql.*;
import java.util.ArrayList;

final class OdczytRezerwacjiDAO {
    private OdczytRezerwacjiDAO() {}

    public static ArrayList<Rezerwacja> pobierzRezerwacje(ArrayList<Gosc> goscie, ArrayList<Pokoj> pokoje) throws SQLException{
        ArrayList<Rezerwacja> lista = new ArrayList<>();

        String sql = "SELECT id, gosc_id, pokoj_id, data_od, data_do, "
                + "liczba_osob, cena_za_noc, status "
                + "FROM rezerwacje ORDER BY id";
        try (
                Connection polaczenie = PolaczenieBazy.polacz();

                PreparedStatement zapytanie = polaczenie.prepareStatement(sql);
                ResultSet wynik = zapytanie.executeQuery();
                ){
            while (wynik.next()){
                long idZBazy = wynik.getLong("id");
                long idGoscia = wynik.getLong("gosc_id");
                long idPokoju = wynik.getLong("pokoj_id");

                if (idZBazy < 1 || idZBazy > Integer.MAX_VALUE){
                    throw new SQLException(
                            "ID rezerwacji nie mieści sie w typie INT. "
                    );
                }
                Gosc znalezionyGosc = null;

                for (Gosc gosc: goscie){
                    if (gosc.getId() == idGoscia){
                        znalezionyGosc = gosc;
                        break;
                    }
                }
                Pokoj znalezionyPokoj = null;
                for (Pokoj pokoj : pokoje){
                    if (pokoj.getId() == idPokoju){
                        znalezionyPokoj = pokoj;
                        break;
                    }
                }
                if (znalezionyGosc == null || znalezionyPokoj == null){
                    throw new SQLException(
                            "Nie znaleziono goscia lub pokoju "+ "dla rezerwacji" + idZBazy
                    );
                }
                Rezerwacja rezerwacja = new Rezerwacja(
                        (int) idZBazy,
                        znalezionyGosc,
                        znalezionyPokoj,
                        wynik.getDate("data_od").toLocalDate(),
                        wynik.getDate("data_do").toLocalDate(),
                        wynik.getInt("liczba_osob"),
                        wynik.getBigDecimal("cena_za_noc"),
                        wynik.getString("status")
                );

                lista.add(rezerwacja);
            }

        }
        java.util.Map<Integer, Rezerwacja> mapa = new java.util.HashMap<>();
        for (Rezerwacja r : lista) mapa.put(r.getId(), r);
        java.util.Map<Integer, ArrayList<Uczestnik>> osoby = new java.util.HashMap<>();
        try (Connection c = PolaczenieBazy.polacz(); PreparedStatement p = c.prepareStatement(
                "SELECT rezerwacja_id, imie, nazwisko FROM uczestnicy_rezerwacji ORDER BY rezerwacja_id, pozycja");
             ResultSet r = p.executeQuery()) {
            while (r.next()) osoby.computeIfAbsent(r.getInt("rezerwacja_id"), k -> new ArrayList<>())
                    .add(new Uczestnik(r.getString("imie"), r.getString("nazwisko")));
        }
        osoby.forEach((id, listaOsob) -> { if (mapa.containsKey(id)) mapa.get(id).ustawUczestnikow(listaOsob); });
        return lista;
    }
}
