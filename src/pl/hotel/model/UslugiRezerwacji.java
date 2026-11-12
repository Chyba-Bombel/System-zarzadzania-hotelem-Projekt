package pl.hotel.model;
import java.util.List;
public record UslugiRezerwacji(Rozliczenie rozliczenie,List<PozycjaUslugi> pozycje) {
    public UslugiRezerwacji {pozycje=List.copyOf(pozycje);}
    public boolean edytowalne(){return java.util.Set.of("POTWIERDZONA","ZAMELDOWANA").contains(rozliczenie.status());}
}
