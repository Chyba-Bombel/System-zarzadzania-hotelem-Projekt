package pl.hotel.gui;

import pl.hotel.model.Gosc;
import pl.hotel.baza.BazaDanych;

final class ArchiwumGosciGUI {
    private final HotelGUI app;
    ArchiwumGosciGUI(HotelGUI app){this.app=app;}
    void pokaz(Gosc g,boolean archiwizuj){
        if(g==null)return;
        String tytul=archiwizuj?"Archiwizacja gościa":"Przywracanie gościa";
        Formularz f=new Formularz(app,tytul,"#"+g.getId()+" • "+g.getImie()+" "+g.getNazwisko(),archiwizuj?"Archiwizuj":"Przywróć");
        f.notatka(archiwizuj
            ?"Gość zniknie z aktywnych kartotek i wyboru nowej rezerwacji. Dotychczasowe pobyty, płatności i historia pozostaną. Potwierdzone lub zameldowane rezerwacje blokują archiwizację."
            :"Kartoteka wróci do aktywnych gości i wyboru nowej rezerwacji. Dotychczasowe pobyty pozostaną bez zmian.");
        f.pokaz(()->app.zadaniaGUI.uwierzytelnione(false,tytul,()->{
            if(archiwizuj)BazaDanych.archiwizujGoscia(g.getId());else BazaDanych.przywrocGoscia(g.getId());return true;
        },v->{f.zamknij();app.zadaniaGUI.odswiez(archiwizuj?"Zarchiwizowano gościa.":"Przywrócono gościa.");}));
    }
}
