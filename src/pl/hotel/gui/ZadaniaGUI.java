package pl.hotel.gui;

import pl.hotel.model.*;
import pl.hotel.serwis.GuiRepository;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import static pl.hotel.gui.NarzedziaGUI.*;

/** Kontroler: ZadaniaGUI. */
final class ZadaniaGUI {
    private final HotelGUI app;
    ZadaniaGUI(HotelGUI app) { this.app = app; }

    void odswiez(String komunikat) { odswiezPo(komunikat, () -> {}); }

    void odswiezPo(String komunikat, Runnable potem) {
        uwierzytelnione(false, "Odświeżanie danych", () -> GuiRepository.pobierz(app.stan.sesja), d -> {
            app.stan.typy = d.typy(); app.stan.goscie.setAll(d.goscie()); app.stan.pokoje.setAll(d.pokoje()); app.stan.rezerwacje.setAll(d.rezerwacje()); app.stan.konta.setAll(d.konta());
            app.tabeleGUI.filtruj(); app.stan.tabelaPokoi.refresh(); app.pokazStrone(); app.stan.status.setText(komunikat + " • " + godzina()); potem.run();
        });
    }

    <T> void uwierzytelnione(boolean tylkoAdmin, String nazwa, Callable<T> praca, Consumer<T> sukces) {
        Pracownik osoba = app.stan.sesja;
        zadanie(nazwa, () -> { GuiRepository.sprawdzSesje(osoba,tylkoAdmin); return praca.call(); }, sukces);
    }

    <T> void zadanie(String nazwa, Callable<T> praca, Consumer<T> sukces) {
        if (app.stan.busy.get()) return;
        app.stan.busy.set(true); app.stan.status.setText(nazwa + "…");
        Task<T> task = new Task<>() { @Override protected T call() throws Exception { return praca.call(); } };
        task.setOnSucceeded(e -> { app.stan.busy.set(false); sukces.accept(task.getValue()); });
        task.setOnFailed(e -> {
            app.stan.busy.set(false); Throwable blad = task.getException();
            if (blad instanceof GuiRepository.SesjaWygasla) {
                if (app.stan.aktywnyDialog != null) app.stan.aktywnyDialog.close();
                app.ekranLogowania.pokazLogowanie();
            }
            app.stan.status.setText("Operacja nie została potwierdzona.");
            app.komunikatyGUI.blad(app.komunikatyGUI.komunikatBledu(blad));
        });
        app.stan.executor.submit(task);
    }
}
