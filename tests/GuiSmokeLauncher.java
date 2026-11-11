import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import pl.hotel.gui.HotelGUI;
import pl.hotel.serwis.GuiRepository;
import pl.hotel.raporty.Raporty;
public class GuiSmokeLauncher {
    public static void main(String[] args) throws Exception { TestDatabase.wymagajTestowejBazy(); javafx.application.Application.launch(GuiSmoke.class,args); }
}
