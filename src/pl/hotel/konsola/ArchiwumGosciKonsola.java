package pl.hotel.konsola;

import pl.hotel.baza.BazaDanych;
import pl.hotel.bezpieczenstwo.KomunikatyBledu;
import java.sql.SQLException;

final class ArchiwumGosciKonsola {
    private ArchiwumGosciKonsola(){}
    static void pokaz(){
        try{StanKonsoli.goscie=BazaDanych.pobierzGosci();lista(true);}
        catch(SQLException e){System.out.println(KomunikatyBledu.dlaUzytkownika(e));}
    }
    private static void lista(boolean archiwum){
        int ile=0;
        for(var g:StanKonsoli.goscie)if(g.isArchiwalny()==archiwum){
            g.wyswietlDane();ile++;
            if(archiwum)System.out.println("Archiwizowano: "+g.getArchiwizowano()+" | Pracownik: "+g.getArchiwizowalLogin());
        }
        if(ile==0)System.out.println("Brak gości w wybranej grupie.");
    }
    static boolean zmien(boolean archiwizuj){
        try{
            StanKonsoli.goscie=BazaDanych.pobierzGosci();lista(!archiwizuj);
            Integer id=WejscieKonsoli.wczytajLiczbe("ID gościa: ");if(id==null)return false;
            var g=StanKonsoli.goscie.stream().filter(x->x.getId()==id).findFirst().orElse(null);
            if(g==null||g.isArchiwalny()==archiwizuj){System.out.println("Gościa nie ma w wybranej grupie. Odśwież listę.");return true;}
            System.out.println(archiwizuj?"Archiwizacja ukrywa kartotekę, zachowując rezerwacje. Aktywne pobyty blokują operację.":"Kartoteka wróci do aktywnych gości.");
            String tak=WejscieKonsoli.wczytajNiepustyTekst("Potwierdzasz? TAK/NIE: ");if(tak==null)return false;
            if(!tak.equalsIgnoreCase("TAK"))return true;
            if(archiwizuj)BazaDanych.archiwizujGoscia(id);else BazaDanych.przywrocGoscia(id);
            System.out.println(archiwizuj?"Zarchiwizowano gościa.":"Przywrócono gościa.");
            StanKonsoli.goscie=BazaDanych.pobierzGosci();
            StanKonsoli.rezerwacje=BazaDanych.pobierzRezerwacje(StanKonsoli.goscie,StanKonsoli.pokoje);
        }catch(SQLException e){System.out.println(KomunikatyBledu.dlaUzytkownika(e));System.out.println("Przed ponowieniem sprawdź aktualny stan kartoteki.");}
        return true;
    }
}
