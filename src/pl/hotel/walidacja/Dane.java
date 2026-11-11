package pl.hotel.walidacja;

import java.sql.SQLException;

public final class Dane {
    private Dane() {}
    public static void tekst(String s,int max,String nazwa) throws SQLException {
        if(s==null || s.isBlank() || s.length()>max || s.chars().anyMatch(c->c<32 || c==127))
            throw new SQLException(nazwa+": podaj od 1 do "+max+" znaków bez znaków sterujących.");
    }
    public static void gosc(String imie,String nazwisko,String telefon,String email) throws SQLException {
        tekst(imie,60,"Imię"); tekst(nazwisko,80,"Nazwisko"); tekst(telefon,30,"Telefon");
        if(email==null || email.length()>120 || !email.matches("^[A-Za-z0-9_%+-]+(?:\\.[A-Za-z0-9_%+-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$"))
            throw new SQLException("Podaj poprawny adres e-mail (do 120 znaków).");
    }
}
