import pl.hotel.kalendarz.PlanKalendarza;
import pl.hotel.model.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.List;

public class CalendarProbe {
    static void check(boolean ok,String opis) { if (!ok) throw new AssertionError(opis); }
    static Rezerwacja r(int id, Pokoj p, LocalDate od, LocalDate koniec, String status) {
        return new Rezerwacja(id,new Gosc(1,"Jan","Test","123","jan@example.com"),p,od,koniec,1,new BigDecimal("100"),status);
    }
    public static void main(String[] args) {
        Pokoj p = new Pokoj(1,"101",2,new BigDecimal("100"),"GOTOWY");
        Pokoj off = new Pokoj(2,"102",2,new BigDecimal("100"),"WYLACZONY");
        LocalDate d = LocalDate.of(2040,2,28);
        var a=r(1,p,d,d.plusDays(1),"POTWIERDZONA");
        var b=r(2,p,d.plusDays(1),d.plusDays(3),"ZAMELDOWANA");
        var anulowana=r(3,p,d,d.plusDays(2),"ANULOWANA");
        var plan=new PlanKalendarza(YearMonth.from(d),List.of(a,b,anulowana));
        check(!plan.dzien(p,d).moznaRezerwowac(d),"Początek pobytu zajęty");
        check(plan.dzien(p,d.plusDays(1)).rezerwacje().size()==1,"Wyjazd pierwszego i przyjazd drugiego tego samego dnia");
        check(plan.dzien(p,d.plusDays(1)).widoczna().getId()==2,"29 lutego w roku przestępnym");
        check(plan.dzien(off,d).rezerwacje().isEmpty() && !plan.dzien(off,d).moznaRezerwowac(d),"Wyłączony pokój bez rezerwacji");
        check(!plan.dzien(p,d.minusDays(1)).moznaRezerwowac(d),"Przeszłość nie przyjmuje nowych pobytów");
        var marzec=new PlanKalendarza(YearMonth.of(2040,3),List.of(b));
        check(!marzec.dzien(p,LocalDate.of(2040,3,1)).rezerwacje().isEmpty(),"Pobyt przecina miesiąc");
        check(marzec.dzien(p,LocalDate.of(2040,3,2)).moznaRezerwowac(d),"Dzień wyjazdu jest wolny");
        var zakonczona=r(4,p,d,d.plusDays(1),"ZAKONCZONA");
        var historia=new PlanKalendarza(YearMonth.from(d),List.of(zakonczona));
        check(historia.dzien(p,d).widoczna().getId()==4 && historia.dzien(p,d).moznaRezerwowac(d),"Historia nie blokuje pokoju");
        var konflikt=new PlanKalendarza(YearMonth.from(d),List.of(zakonczona,a,r(5,p,d,d.plusDays(1),"ZAMELDOWANA")));
        check(konflikt.dzien(p,d).konflikt(),"Wykrycie dwóch aktywnych rezerwacji");
        check(konflikt.dzien(p,d).widoczna().getId()==1,"Aktywna rezerwacja przed historią");
        check(!new PlanKalendarza(YearMonth.from(d),List.of(a,zakonczona)).dzien(p,d).konflikt(),"Historia z aktywną nie oznacza konfliktu");
        check(YearMonth.of(2041,2).lengthOfMonth()==28,"Luty bez przestępnego dnia");
        check(new PlanKalendarza(YearMonth.of(2040,1),List.of()).dzien(p,LocalDate.of(2040,1,31)).moznaRezerwowac(LocalDate.of(2040,1,1)),"31 dni");
        try { plan.dzien(p,LocalDate.of(2040,3,1)); throw new AssertionError("Data spoza miesiąca przyjęta"); } catch (IllegalArgumentException ok) {}
        try { plan.dzien(p,d).rezerwacje().clear(); throw new AssertionError("Modyfikowalny indeks"); } catch (UnsupportedOperationException ok) {}
        System.out.println("CALENDAR_OK: granice dat, statusy, konflikty, przeszłość i miesiące.");
    }
}
