import java.math.BigDecimal;
public class Pokoj {
    private final long id;
    private final String numer;
    private final int liczbaMiejsc;
    private final BigDecimal cenaZaNoc;
    private final String stan;

    public Pokoj(long id, String numer, int liczbaMiejsc, BigDecimal cenaZaNoc, String stan){
        this.id = id;
        this.numer = numer;
        this.liczbaMiejsc = liczbaMiejsc;
        this.cenaZaNoc = cenaZaNoc;
        this.stan = stan;
    }

    public long getId() {
        return id;
    }

    public String getNumer() {
        return numer;
    }

    public int getLiczbaMiejsc() {
        return liczbaMiejsc;
    }

    public BigDecimal getCenaZaNoc() {
        return cenaZaNoc;
    }

    public String getStan() {
        return stan;
    }

    public void wyswietlDane() {
        System.out.println(
                "Pokój " + numer
                        + " | Miejsca: " + liczbaMiejsc
                        + " | Cena za noc: " + cenaZaNoc + " zł"
                        + " | Stan: " + stan
        );
    }

}
