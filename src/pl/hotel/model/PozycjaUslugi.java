package pl.hotel.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record PozycjaUslugi(long id,int rezerwacjaId,long uslugaId,String nazwa,String jednostka,BigDecimal cena,int ilosc,String status,LocalDateTime utworzono,LocalDateTime anulowano) {
    public BigDecimal wartosc(){return cena.multiply(BigDecimal.valueOf(ilosc));}
    public boolean aktywna(){return status.equals("AKTYWNA");}
    public String opis(){return "#"+id+" | "+nazwa+" | "+ilosc+" × "+cena+" zł / "+jednostka+" | "+status;}
}
