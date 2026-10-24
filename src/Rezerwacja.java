import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Rezerwacja {
    private final int id;
    private final Gosc gosc;
    private final Pokoj pokoj;
    private final LocalDate od;
    private final LocalDate doDnia;
    private final int liczbaOsob;
    private boolean anulowana;

    public Rezerwacja(int id, Gosc gosc, Pokoj pokoj, LocalDate od, LocalDate doDnia, int liczbaOsob) {
        if (gosc == null || pokoj == null || od == null || doDnia == null || !doDnia.isAfter(od) || id <= 0 || liczbaOsob <= 0 || liczbaOsob > pokoj.getLiczbaMiejsc()) {
            throw new IllegalArgumentException("Niepoprawne dane rezerwacji");
        }
        this.id = id;
        this.gosc = gosc;
        this.pokoj = pokoj;
        this.od = od;
        this.doDnia = doDnia;
        this.liczbaOsob = liczbaOsob;
    }

    public int GetId() {
        return id;
    }

    public boolean isAnulowana() {
        return anulowana;
    }

    public void anuluj() {
        anulowana = true;
    }

    public long liczbaNocy() {
        return ChronoUnit.DAYS.between(od, doDnia);
    }

    public BigDecimal obliczCene() {
        return pokoj.getCenaZaNoc().multiply(BigDecimal.valueOf(liczbaNocy()));
    }

    public boolean kolidacja(String numerPokoju, LocalDate poczatek, LocalDate koniec) {
        return !anulowana && pokoj.getNumer().equalsIgnoreCase(numerPokoju) && poczatek.isBefore(doDnia) && koniec.isAfter(od);

    }

    public void wyswietlDane() {
        System.out.println("Rezerwacja " + id + " | " + gosc.getImie() + " " + gosc.getNazwisko() + " | Pokoj " + pokoj.getNumer() + " | " + od + " -> " + doDnia + " | Osoby: " + liczbaOsob + " | Noce: " + liczbaNocy() + " | Cena " + obliczCene() + "zł | " + (anulowana ? "ANULOWANA" : "AKTYWNA"));
    }

}
