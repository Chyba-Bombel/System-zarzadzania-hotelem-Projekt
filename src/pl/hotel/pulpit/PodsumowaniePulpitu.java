package pl.hotel.pulpit;

import pl.hotel.model.*;
import java.time.LocalDate;
import java.util.*;

public record PodsumowaniePulpitu(int wszystkie, int dostepne, int zajete, int gotowe, int sprzatanie, int wylaczone,
                                  List<Rezerwacja> przyjazdy, List<Rezerwacja> wyjazdy) {
    public double zajetosc() { return dostepne == 0 ? 0 : Math.min(1, zajete / (double)dostepne); }

    public static PodsumowaniePulpitu oblicz(List<Pokoj> pokoje, List<Rezerwacja> rezerwacje, LocalDate dzien) {
        Set<Long> zameldowane = new HashSet<>();
        for (Rezerwacja r : rezerwacje) if ("ZAMELDOWANA".equals(r.getStatus())) zameldowane.add(r.getPokoj().getId());
        int zajete = 0, gotowe = 0, sprzatanie = 0, wylaczone = 0;
        for (Pokoj p : pokoje) {
            if ("WYLACZONY".equals(p.getStan())) wylaczone++;
            else if (zameldowane.contains(p.getId())) zajete++;
            else if ("DO_SPRZATANIA".equals(p.getStan())) sprzatanie++;
            else gotowe++;
        }
        Comparator<Rezerwacja> kolejnosc = Comparator.comparing(Rezerwacja::getDoDnia).thenComparingInt(Rezerwacja::getId);
        List<Rezerwacja> przyjazdy = rezerwacje.stream().filter(r -> "POTWIERDZONA".equals(r.getStatus()) && dzien.equals(r.getOd())).sorted(Comparator.comparingInt(Rezerwacja::getId)).toList();
        List<Rezerwacja> wyjazdy = rezerwacje.stream().filter(r -> "ZAMELDOWANA".equals(r.getStatus()) && !r.getDoDnia().isAfter(dzien)).sorted(kolejnosc).toList();
        return new PodsumowaniePulpitu(pokoje.size(), pokoje.size() - wylaczone, zajete, gotowe, sprzatanie, wylaczone, przyjazdy, wyjazdy);
    }
}
