package pl.hotel.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Rezerwacja {
    private java.util.List<Uczestnik> uczestnicy = java.util.List.of();
    public java.util.List<Uczestnik> getUczestnicy() { return uczestnicy; }
    public void ustawUczestnikow(java.util.List<Uczestnik> osoby) { uczestnicy = java.util.List.copyOf(osoby); }
    public String getGosciePobytu() {
        String nazwy = gosc.getImie() + " " + gosc.getNazwisko();
        for (Uczestnik u : uczestnicy) nazwy += ", " + u;
        int brak = liczbaOsob - 1 - uczestnicy.size();
        return nazwy + (brak > 0 ? " (brak danych " + brak + " os.)" : "");
    }
    private BigDecimal kosztUslug=BigDecimal.ZERO,netto=BigDecimal.ZERO;
    public void ustawRozliczenie(BigDecimal uslugi,BigDecimal netto){this.kosztUslug=uslugi;this.netto=netto;}
    public BigDecimal getKosztUslug(){return kosztUslug;}
    public BigDecimal getNetto(){return netto;}
    public BigDecimal getNaleznosc(){return isAnulowana()?BigDecimal.ZERO:obliczCene();}
    public BigDecimal getSaldo(){return getNaleznosc().subtract(netto);}
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
        return cenaZaNoc.multiply(BigDecimal.valueOf(liczbaNocy())).add(kosztUslug);
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

    public Gosc getGosc() { return gosc; }
    public Pokoj getPokoj() { return pokoj; }
    public LocalDate getOd() { return od; }
    public LocalDate getDoDnia() { return doDnia; }
    public int getLiczbaOsob() { return liczbaOsob; }
    public BigDecimal getCenaZaNoc() { return cenaZaNoc; }
}
