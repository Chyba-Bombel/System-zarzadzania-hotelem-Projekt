package pl.hotel.model;

import java.math.BigDecimal;
import java.util.List;

public record Rozliczenie (
        long rezerwacjaId,
        String status,
        BigDecimal cenaPobytu,
        BigDecimal uslugi,
        BigDecimal naleznosc,
        BigDecimal wplaty,
        BigDecimal zwroty,
        List<Platnosc> platnosci
){

    public Rozliczenie {platnosci=List.copyOf(platnosci);}
    public Rozliczenie(long id,String status,BigDecimal noclegi,BigDecimal naleznosc,BigDecimal wplaty,BigDecimal zwroty,List<Platnosc> lista){
        this(id,status,noclegi,BigDecimal.ZERO,naleznosc,wplaty,zwroty,lista);
    }
    public BigDecimal zaplaconoNetto(){
        return wplaty.subtract(zwroty);
    }
    public BigDecimal saldo(){
        return naleznosc.subtract(zaplaconoNetto());
    }
}
