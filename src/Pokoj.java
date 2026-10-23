import java.math.BigDecimal;
public class Pokoj {
    private final int numer;
    private final int liczbaMiejsc;
    private final BigDecimal cenaZaNoc;

    public Pokoj(int numer, int liczbaMiejsc, String cenaZaNoc){
        this.cenaZaNoc = new BigDecimal(cenaZaNoc).setScale(2);
        if (numer <= 0 || liczbaMiejsc <= 0 || this.cenaZaNoc.signum() <= 0){
            throw new IllegalArgumentException("Dane pokoju muszą być dodatnie.");
        }
        this.numer = numer;
        this.liczbaMiejsc = liczbaMiejsc;
    }

    public int getNumer(){return numer;}
    public int getLiczbaMiejsc(){ return liczbaMiejsc;}
    public BigDecimal getCenaZaNoc(){ return cenaZaNoc;}

    public void wyswietlDane(){
        System.out.println("Pokój " + numer + " | Miejsca: " + liczbaMiejsc + " | Cena za noc: " + cenaZaNoc + " zł");
    }

}
