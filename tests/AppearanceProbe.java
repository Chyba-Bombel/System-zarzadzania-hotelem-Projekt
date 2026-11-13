import pl.hotel.konfiguracja.PreferencjeWygladu;
import pl.hotel.konfiguracja.PreferencjeWygladu.Motyw;
import pl.hotel.model.*;
import pl.hotel.pulpit.PodsumowaniePulpitu;
import java.nio.file.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class AppearanceProbe {
    private static int ile;
    private static void check(boolean p,String opis) { if(!p) throw new AssertionError(opis); ile++; }

    public static void main(String[] args) throws Exception {
        if(args.length>0) {
            var p=PreferencjeWygladu.wczytaj();
            if(p.motyw()!=Motyw.CIEMNY||p.skala()!=120) throw new AssertionError("Preferencje innego procesu");
            System.out.println("APPEARANCE_RESTART_OK"); return;
        }
        Path dir=Files.createTempDirectory("hotel-appearance-"); System.setProperty("hotel.data",dir.toString());
        Path config=dir.resolve("config/db.properties"); Files.createDirectories(config.getParent()); Files.writeString(config,"password=TEST_SECRET\n");
        check(PreferencjeWygladu.wczytaj().equals(new PreferencjeWygladu(Motyw.JASNY,100)),"Domyslny jasny motyw");
        new PreferencjeWygladu(Motyw.CIEMNY,120).zapisz();
        check(PreferencjeWygladu.wczytaj().equals(new PreferencjeWygladu(Motyw.CIEMNY,120)),"Zapis motywu i skali");
        check(Files.readString(config).equals("password=TEST_SECRET\n"),"Wyglad nie zmienia konfiguracji bazy");
        String cp=java.util.Arrays.stream(System.getProperty("java.class.path").split(java.io.File.pathSeparator)).map(s->Path.of(s).toAbsolutePath().toString()).collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));
        Process proces=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin/java").toString(),"-Dhotel.data="+dir,"-cp",cp,"AppearanceProbe","restart").redirectErrorStream(true).start();
        String wynik=new String(proces.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        check(proces.waitFor()==0&&wynik.contains("APPEARANCE_RESTART_OK"),"Zachowanie po ponownym uruchomieniu");
        Files.writeString(PreferencjeWygladu.plik(),"motyw=INNY\nskala=900\n");
        check(PreferencjeWygladu.wczytaj().motyw()==Motyw.JASNY,"Bezpieczny powrot po uszkodzeniu preferencji");
        try { new PreferencjeWygladu(Motyw.JASNY,900); throw new AssertionError("Zla skala"); }
        catch(IllegalArgumentException e) { check(true,"Odmowa niepoprawnej skali"); }
        LocalDate dzis=LocalDate.of(2026,11,13);
        Gosc g=new Gosc(1,"Anna","Nowak","500000000","anna@example.invalid");
        Pokoj a=new Pokoj(1,"101",2,new BigDecimal("200"),"GOTOWY");
        Pokoj b=new Pokoj(2,"102",2,new BigDecimal("200"),"DO_SPRZATANIA");
        Pokoj c=new Pokoj(3,"103",2,new BigDecimal("200"),"WYLACZONY");
        List<Rezerwacja> rezerwacje=List.of(
            new Rezerwacja(1,g,a,dzis.minusDays(3),dzis.minusDays(1),1,new BigDecimal("200"),"ZAMELDOWANA"),
            new Rezerwacja(2,g,a,dzis.minusDays(2),dzis.plusDays(2),1,new BigDecimal("200"),"ZAMELDOWANA"),
            new Rezerwacja(3,g,b,dzis,dzis.plusDays(2),1,new BigDecimal("200"),"POTWIERDZONA"),
            new Rezerwacja(4,g,b,dzis.plusDays(1),dzis.plusDays(3),1,new BigDecimal("200"),"POTWIERDZONA"),
            new Rezerwacja(5,g,c,dzis,dzis.plusDays(3),1,new BigDecimal("200"),"ANULOWANA"));
        var p=PodsumowaniePulpitu.oblicz(List.of(a,b,c),rezerwacje,dzis);
        check(p.zajete()==1,"Pokoj liczony raz przy konflikcie");
        check(p.wyjazdy().size()==1,"Zalegly zameldowany pobyt wymaga wyjazdu");
        check(p.przyjazdy().size()==1,"Przyjazdy tylko dzisiejsze potwierdzone");
        check(p.dostepne()==2&&p.wylaczone()==1,"Wylaczone poza mianownikiem zajetosci");
        check(p.zajetosc()==.5,"Zajetosc fizycznie zameldowanych pokoi");
        check(p.zajete()+p.gotowe()+p.sprzatanie()+p.wylaczone()==p.wszystkie(),"Kategorie wykresu bez podwojnego liczenia");
        var pusty=PodsumowaniePulpitu.oblicz(List.of(),List.of(),dzis);
        check(pusty.zajetosc()==0&&!Double.isNaN(pusty.zajetosc()),"Pusty hotel bez dzielenia przez zero");
        System.out.println("APPEARANCE_PROBE_OK checks="+ile);
    }
}
