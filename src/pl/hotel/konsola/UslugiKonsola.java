package pl.hotel.konsola;
import pl.hotel.baza.BazaDanych;
import pl.hotel.bezpieczenstwo.KomunikatyBledu;
import java.sql.SQLException;
final class UslugiKonsola {
    private UslugiKonsola(){}
    static boolean pokaz(){
        Integer id=WejscieKonsoli.wczytajLiczbe("ID rezerwacji: ");if(id==null)return false;
        try{
            var dane=BazaDanych.pobierzUslugiRezerwacji(id);for(var p:dane.pozycje())System.out.println(p.opis());
            var r=dane.rozliczenie();System.out.println("Noclegi: "+r.cenaPobytu()+" zł | Usługi: "+r.uslugi()+" zł | Należność: "+r.naleznosc()+" zł | Saldo: "+r.saldo()+" zł");
            if(!dane.edytowalne()){System.out.println("Archiwalny pobyt: tylko odczyt.");return true;}
            String wybor=WejscieKonsoli.wczytajNiepustyTekst("1. Dodaj usługę | 2. Anuluj pozycję | 0. Powrót: ");if(wybor==null)return false;
            if(wybor.equals("1")){
                var lista=BazaDanych.pobierzKatalogUslug(false);for(var u:lista)System.out.println(u.id()+" | "+u.nazwa()+" | "+u.cena()+" zł / "+u.jednostka());
                if(lista.isEmpty()){System.out.println("Brak aktywnych usług.");return true;}
                Integer uid=WejscieKonsoli.wczytajLiczbe("ID usługi: ");if(uid==null)return false;
                var u=lista.stream().filter(x->x.id()==uid).findFirst().orElseThrow(()->new IllegalArgumentException("Brak usługi."));
                System.out.println("Ilość dla całego pobytu. Zmiana dat nie zmienia ilości, dostawka nie zwiększa pojemności pokoju.");
                Integer ile=WejscieKonsoli.wczytajLiczbe("Ilość 1–1000: ");if(ile==null)return false;
                var dodana=BazaDanych.dodajUslugeDoRezerwacji(id,u,ile,StanKonsoli.zalogowanyPracownik.getId());System.out.println("Dodano usługę: "+dodana.opis());
            }else if(wybor.equals("2")){
                Integer pid=WejscieKonsoli.wczytajLiczbe("ID pozycji pobytu, nie katalogu: ");if(pid==null)return false;
                String tak=WejscieKonsoli.wczytajNiepustyTekst("Potwierdzasz anulowanie? TAK/NIE: ");if(tak==null)return false;
                if(tak.equalsIgnoreCase("TAK")){BazaDanych.anulujUslugeRezerwacji(id,pid);System.out.println("Anulowano usługę.");}
            }
            StanKonsoli.rezerwacje=BazaDanych.pobierzRezerwacje(StanKonsoli.goscie,StanKonsoli.pokoje);
        }catch(SQLException|IllegalArgumentException e){System.out.println(KomunikatyBledu.dlaUzytkownika(e));}return true;
    }
}
