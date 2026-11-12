package pl.hotel.konsola;
import pl.hotel.baza.BazaDanych;
import pl.hotel.model.FiltrRezerwacji;
import pl.hotel.bezpieczenstwo.KomunikatyBledu;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Locale;
final class WyszukiwanieKonsola {
    private WyszukiwanieKonsola(){}
    private static String pole(String opis){System.out.print(opis+" (Enter = bez ograniczenia): ");return StanKonsoli.scanner.hasNextLine()?StanKonsoli.scanner.nextLine().trim():null;}
    static boolean pokaz(){
        try{
            String n=pole("Nazwisko głównego/dodatkowego gościa");if(n==null)return false;
            String p=pole("Dokładny numer pokoju");if(p==null)return false;
            String s=pole("Status POTWIERDZONA/ZAMELDOWANA/ZAKONCZONA/ANULOWANA");if(s==null)return false;
            String od=pole("Od RRRR-MM-DD");if(od==null)return false;
            String koniec=pole("Do RRRR-MM-DD");if(koniec==null)return false;
            String t=pole("Tryb POBYT/PRZYJAZD/WYJAZD, domyślnie POBYT");if(t==null)return false;
            String roz=pole("Rozliczenie DO_ZAPLATY/NIEOPLACONA/CZESCIOWO_OPLACONA/ROZLICZONA/NADPLATA");if(roz==null)return false;
            var f=new FiltrRezerwacji(n,p,s.toUpperCase(Locale.ROOT),od.isEmpty()?null:LocalDate.parse(od),koniec.isEmpty()?null:LocalDate.parse(koniec),t.isEmpty()?FiltrRezerwacji.TrybDat.POBYT:FiltrRezerwacji.TrybDat.valueOf(t.toUpperCase(Locale.ROOT)),roz.isEmpty()?FiltrRezerwacji.StanRozliczenia.WSZYSTKIE:FiltrRezerwacji.StanRozliczenia.valueOf(roz.toUpperCase(Locale.ROOT)));
            long kursor=0;
            while(true){
                var strona=BazaDanych.wyszukajRezerwacje(f,kursor);
                for(var r:strona.wyniki())System.out.println("#"+r.id()+" | "+r.gosc()+" | Pokój "+r.pokoj()+" | "+r.od()+" – "+r.doDnia()+" | "+r.status()+" | Należność: "+r.naleznosc()+" | Netto: "+r.netto()+" | Saldo: "+r.saldo());
                System.out.println("Wyników na stronie: "+strona.wyniki().size());if(!strona.maStarsze())return true;
                String dalej=WejscieKonsoli.wczytajNiepustyTekst("Starsze wyniki? TAK/NIE: ");if(dalej==null)return false;if(!dalej.equalsIgnoreCase("TAK"))return true;
                kursor=strona.wyniki().get(strona.wyniki().size()-1).id();
            }
        }catch(SQLException|IllegalArgumentException|java.time.format.DateTimeParseException e){System.out.println("Sprawdź filtry: "+KomunikatyBledu.dlaUzytkownika(e));return true;}
    }
}
