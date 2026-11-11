package pl.hotel.kalendarz;

import pl.hotel.model.Pokoj;
import pl.hotel.model.Rezerwacja;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public final class PlanKalendarza {
    public record PoleDnia(Pokoj pokoj, LocalDate data, List<Rezerwacja> rezerwacje) {
        public PoleDnia { rezerwacje = List.copyOf(rezerwacje); }
        public List<Rezerwacja> aktywne() {
            return rezerwacje.stream().filter(PlanKalendarza::blokuje).toList();
        }
        public boolean konflikt() { return aktywne().size() > 1; }
        public boolean moznaRezerwowac(LocalDate dzisiaj) {
            return !data.isBefore(dzisiaj) && !pokoj.getStan().equals("WYLACZONY") && aktywne().isEmpty();
        }
        public Rezerwacja widoczna() {
            return aktywne().stream().findFirst().orElse(rezerwacje.isEmpty() ? null : rezerwacje.get(0));
        }
    }
    private record Klucz(long pokojId, LocalDate data) {}
    private final Map<Klucz, List<Rezerwacja>> indeks = new HashMap<>();
    private final YearMonth miesiac;

    public PlanKalendarza(YearMonth miesiac, List<Rezerwacja> rezerwacje) {
        this.miesiac = Objects.requireNonNull(miesiac);
        LocalDate poczatek = miesiac.atDay(1), koniec = miesiac.plusMonths(1).atDay(1);
        for (Rezerwacja r : rezerwacje) {
            if (r.getStatus().equals("ANULOWANA")) continue;
            LocalDate od = r.getOd().isAfter(poczatek) ? r.getOd() : poczatek;
            LocalDate doDnia = r.getDoDnia().isBefore(koniec) ? r.getDoDnia() : koniec;
            for (LocalDate d = od; d.isBefore(doDnia); d = d.plusDays(1))
                indeks.computeIfAbsent(new Klucz(r.getPokoj().getId(), d), k -> new ArrayList<>()).add(r);
        }
        indeks.replaceAll((klucz, lista) -> List.copyOf(lista));
    }
    public PoleDnia dzien(Pokoj pokoj, LocalDate data) {
        if (!YearMonth.from(data).equals(miesiac)) throw new IllegalArgumentException("Data spoza miesiąca kalendarza.");
        return new PoleDnia(pokoj, data, indeks.getOrDefault(new Klucz(pokoj.getId(), data), List.of()));
    }
    private static boolean blokuje(Rezerwacja r) {
        return r.getStatus().equals("POTWIERDZONA") || r.getStatus().equals("ZAMELDOWANA");
    }
}
