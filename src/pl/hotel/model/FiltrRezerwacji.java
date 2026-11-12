package pl.hotel.model;
import java.time.LocalDate;
import java.util.Set;
public record FiltrRezerwacji(String nazwisko,String pokoj,String status,LocalDate od,LocalDate doDnia,TrybDat tryb,StanRozliczenia rozliczenie) {
    public enum TrybDat {POBYT,PRZYJAZD,WYJAZD}
    public enum StanRozliczenia {WSZYSTKIE,DO_ZAPLATY,NIEOPLACONA,CZESCIOWO_OPLACONA,ROZLICZONA,NADPLATA}
    public FiltrRezerwacji {
        nazwisko=nazwisko==null?"":nazwisko.trim();pokoj=pokoj==null?"":pokoj.trim();status=status==null?"":status;
        if(nazwisko.length()>80||pokoj.length()>10||!Set.of("","POTWIERDZONA","ZAMELDOWANA","ZAKONCZONA","ANULOWANA").contains(status)
                ||tryb==null||rozliczenie==null||od!=null&&(od.getYear()<1900||od.getYear()>9998)
                ||doDnia!=null&&(doDnia.getYear()<1900||doDnia.getYear()>9998)||od!=null&&doDnia!=null&&doDnia.isBefore(od))
            throw new IllegalArgumentException("Sprawdź filtry: nazwisko do 80 znaków, pokój do 10, daty 1900–9998 w poprawnej kolejności.");
    }
    public static FiltrRezerwacji wszystkie(){return new FiltrRezerwacji("","","",null,null,TrybDat.POBYT,StanRozliczenia.WSZYSTKIE);}
}
