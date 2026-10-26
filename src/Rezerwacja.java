import java.lang.classfile.attribute.LocalVariableTableAttribute;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

public class Rezerwacja {
    private final int id;
    private final Gosc gosc;
    private final Pokoj pokoj;
    private final LocalDate od;
    private final LocalDate doDnia;
    private final int liczbaOsob;
    private String status;
    private final BigDecimal cenaZaNoc;

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
        this.status = "POTWIERDZONA";
        this.cenaZaNoc = pokoj.getCenaZaNoc();
    }
    public Rezerwacja(int id, Gosc gosc, Pokoj pokoj, LocalDate od ,LocalDate doDnia, int liczbaOsob, BigDecimal cenaZaNoc, String status){
        this.id = id;
        this.gosc = gosc;
        this.pokoj = pokoj;
        this.od = od;
        this.doDnia = doDnia;
        this.liczbaOsob = liczbaOsob;
        this.cenaZaNoc = cenaZaNoc;
        this.status = status;
    }

    public int getId() {
        return id;
    }
    public String getStatus(){
        return status;
    }
    public boolean isAnulowana(){
        return status.equals("ANULOWANA");
    }
    public void anuluj(){
        status = "ANULOWANA";
    }
    public BigDecimal obliczCene(){
        return cenaZaNoc.multiply(BigDecimal.valueOf(liczbaNocy()));
    }

    public long liczbaNocy() {
        return ChronoUnit.DAYS.between(od, doDnia);
    }


    public boolean kolidacja(String numerPokoju, LocalDate poczatek, LocalDate koniec) {
        boolean blokuePokoj = status.equals("POTWIERDZONA") || status.equals("ZAMELDOWANA");

        return blokuePokoj && pokoj.getNumer().equalsIgnoreCase(numerPokoju) && poczatek.isBefore(doDnia) && koniec.isAfter(od);


    }

    public void wyswietlDane() {
        System.out.println("Rezerwacja " + id + " | " + gosc.getImie() + " " + gosc.getNazwisko() + " | Pokoj " + pokoj.getNumer() + " | " + od + " -> " + doDnia + " | Osoby: " + liczbaOsob + " | Noce: " + liczbaNocy() + " | Cena " + obliczCene() + "zł | " + status);
    }

}
