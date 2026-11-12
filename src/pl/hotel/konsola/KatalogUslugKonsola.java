package pl.hotel.konsola;
import pl.hotel.baza.BazaDanych;
import pl.hotel.bezpieczenstwo.KomunikatyBledu;
import pl.hotel.model.UslugaHotelowa;
import java.sql.SQLException;
final class KatalogUslugKonsola {
    private KatalogUslugKonsola(){}
    static boolean pokaz(){
        if(!KontaKonsola.wymagaAdministratora())return true;
        try{
            var lista=BazaDanych.pobierzKatalogUslug(true);for(var u:lista)System.out.println(u.id()+" | "+u.nazwa()+" | "+u.cena()+" zł / "+u.jednostka()+" | "+(u.aktywna()?"aktywna":"nieaktywna"));
            String wybor=WejscieKonsoli.wczytajNiepustyTekst("1. Dodaj | 2. Edytuj/wyłącz | 0. Powrót: ");if(wybor==null)return false;
            if(!wybor.equals("1")&&!wybor.equals("2"))return true;UslugaHotelowa stara=null;
            if(wybor.equals("2")){
                Integer id=WejscieKonsoli.wczytajLiczbe("ID usługi: ");if(id==null)return false;
                stara=lista.stream().filter(x->x.id()==id).findFirst().orElseThrow(()->new IllegalArgumentException("Brak usługi."));
            }
            String nazwa=WejscieKonsoli.wczytajNiepustyTekst("Nazwa: ");if(nazwa==null)return false;
            String jednostka=WejscieKonsoli.wczytajNiepustyTekst("Jednostka: ");if(jednostka==null)return false;
            var cena=WejscieKonsoli.wczytajCene("Cena: ");if(cena==null)return false;
            String a=WejscieKonsoli.wczytajNiepustyTekst("Aktywna? TAK/NIE: ");if(a==null)return false;
            if(!a.equalsIgnoreCase("TAK")&&!a.equalsIgnoreCase("NIE"))throw new IllegalArgumentException("Wpisz TAK albo NIE.");
            var u=BazaDanych.zapiszUsluge(stara,nazwa,jednostka,cena,a.equalsIgnoreCase("TAK"));System.out.println("Zapisano katalog: #"+u.id()+" "+u.nazwa());
        }catch(SQLException|IllegalArgumentException e){System.out.println(KomunikatyBledu.dlaUzytkownika(e));}return true;
    }
}
