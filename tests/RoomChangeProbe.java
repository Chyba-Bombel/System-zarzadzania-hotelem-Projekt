import pl.hotel.baza.BazaDanych;
import pl.hotel.model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

public final class RoomChangeProbe {
    private static int assertions;
    private static void check(boolean ok, String opis) {
        assertions++;
        if (!ok) throw new AssertionError(opis);
    }
    @FunctionalInterface interface Akcja { void run() throws Exception; }
    private static void odmowa(Akcja akcja, String fragment) throws Exception {
        try { akcja.run(); throw new AssertionError("Przyjęto niedozwoloną zmianę: " + fragment); }
        catch (SQLException e) { check(e.getMessage().contains(fragment), e.toString()); }
    }
    private static Pokoj pokoj(String nr, int miejsca) throws Exception {
        BazaDanych.dodajPokoj(nr, "Zmiana", miejsca, new BigDecimal("999.00"));
        return IntegrationProbe.pokoj(nr);
    }
    private static void stan(Pokoj p, String stan) throws Exception {
        try (Connection c = TestDatabase.polacz(); PreparedStatement q = c.prepareStatement("UPDATE pokoje SET stan = ? WHERE id = ?")) {
            q.setString(1, stan); q.setLong(2, p.getId()); q.executeUpdate();
        }
    }
    private static void przenies(Rezerwacja r, Pokoj p) throws Exception {
        BazaDanych.zmienPokojRezerwacji(r.getId(), r.getPokoj().getId(), p.getId());
    }
    public static void main(String[] args) throws Exception {
        TestDatabase.wymagajTestowejBazy();
        BazaDanych.wyloguj();
        try { BazaDanych.zmienPokojRezerwacji(1, 1, 2); throw new AssertionError("Brak ochrony sesji"); }
        catch (SecurityException ok) { assertions++; }
        Pracownik admin = BazaDanych.zaloguj("admin", "Admin123!");
        Gosc g = BazaDanych.zapiszGoscia("Test", "Przeniesienie", "500100200", "room@example.com");
        Pokoj a = pokoj("M100", 3), b = pokoj("M101", 3), maly = pokoj("M102", 1);
        Pokoj wylaczony = pokoj("M103", 3), zajety = pokoj("M104", 3);
        stan(wylaczony, "WYLACZONY");
        BazaDanych.edytujPokoj(a.getId(), a.getNumer(), "Zmiana", new BigDecimal("120.00"));
        a = IntegrationProbe.pokoj("M100");
        LocalDate od = LocalDate.now().plusDays(5), koniec = od.plusDays(2);
        var osoby = List.of(new Uczestnik("Anna", "Druga"));
        Rezerwacja r = BazaDanych.zapiszRezerwacje(g, a, od, koniec, 2, admin.getId(), osoby);
        BazaDanych.zapiszWplate(r.getId(), new BigDecimal("50"), "KARTA", admin.getId());
        BazaDanych.zapiszRezerwacje(g, zajety, od, koniec, 1, admin.getId());
        odmowa(() -> przenies(r, maly), "za mało");
        odmowa(() -> przenies(r, wylaczony), "wyłączony");
        odmowa(() -> przenies(r, zajety), "zajęty");
        Pokoj stary = a;
        odmowa(() -> przenies(r, stary), "inny pokój");
        check(IntegrationProbe.rezerwacja(r.getId()).getPokoj().getId() == a.getId(), "Odmowy zachowują pokój");
        BazaDanych.zapiszRezerwacje(g, b, koniec, koniec.plusDays(1), 1, admin.getId());
        Pracownik recepcja = BazaDanych.zaloguj("recepcja", "Recepcja123!");
        check(recepcja != null, "Logowanie recepcji");
        przenies(r, b);
        Rezerwacja po = IntegrationProbe.rezerwacja(r.getId());
        check(po.getPokoj().getId() == b.getId(), "Przeniesienie i stykające się terminy");
        check(po.obliczCene().compareTo(new BigDecimal("240")) == 0, "Cena uzgodniona zachowana");
        check(po.getOd().equals(od) && po.getDoDnia().equals(koniec), "Daty zachowane");
        check(po.getUczestnicy().equals(osoby) && po.getLiczbaOsob() == 2, "Goście zachowani");
        check(po.getStatus().equals("POTWIERDZONA"), "Status zachowany");
        check(BazaDanych.pobierzRozliczenie(r.getId()).saldo().compareTo(new BigDecimal("190")) == 0, "Wpłata zachowana");
        check(IntegrationProbe.pokoj("M100").getStan().equals("GOTOWY"), "Potwierdzona nie brudzi pokoju");
        odmowa(() -> przenies(r, maly), "został zmieniony");
        BazaDanych.zaloguj("admin", "Admin123!");
        Pokoj liveA = pokoj("M110", 2), liveB = pokoj("M111", 2);
        Rezerwacja live = BazaDanych.zapiszRezerwacje(g, liveA, LocalDate.now(), LocalDate.now().plusDays(2), 1, admin.getId());
        BazaDanych.zameldujGoscia(live.getId());
        stan(liveB, "DO_SPRZATANIA");
        odmowa(() -> przenies(live, liveB), "musi być gotowy");
        check(IntegrationProbe.pokoj("M110").getStan().equals("GOTOWY"), "Odmowa nie brudzi pokoju");
        stan(liveB, "GOTOWY");
        try (Connection c = TestDatabase.polacz(); Statement q = c.createStatement()) {
            q.execute("CREATE TRIGGER test_move BEFORE UPDATE ON pokoje FOR EACH ROW BEGIN "
                    + "IF NEW.numer='M110' AND NEW.stan='DO_SPRZATANIA' THEN "
                    + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='wymuszony rollback'; END IF; END");
        }
        odmowa(() -> przenies(live, liveB), "wymuszony rollback");
        check(IntegrationProbe.rezerwacja(live.getId()).getPokoj().getId() == liveA.getId(), "Rollback zmiany rezerwacji po błędzie sprzątania");
        try (Connection c = TestDatabase.polacz(); Statement q = c.createStatement()) { q.execute("DROP TRIGGER test_move"); }
        przenies(live, liveB);
        check(IntegrationProbe.rezerwacja(live.getId()).getStatus().equals("ZAMELDOWANA"), "Przeniesiony pobyt pozostaje zameldowany");
        check(IntegrationProbe.pokoj("M110").getStan().equals("DO_SPRZATANIA"), "Stary pokój do sprzątania");
        check(IntegrationProbe.pokoj("M111").getStan().equals("GOTOWY"), "Nowy pokój gotowy");
        BazaDanych.zapiszWplate(live.getId(), new BigDecimal("1998"), "KARTA", admin.getId());
        BazaDanych.wymeldujGoscia(live.getId());
        Rezerwacja zakonczona = IntegrationProbe.rezerwacja(live.getId());
        odmowa(() -> przenies(zakonczona, liveA), "tylko w potwierdzonej");
        Rezerwacja anulowana = BazaDanych.zapiszRezerwacje(g, liveA, od, koniec, 1, admin.getId());
        BazaDanych.anulujRezerwacje(anulowana.getId());
        odmowa(() -> przenies(anulowana, b), "tylko w potwierdzonej");
        Pokoj x = pokoj("M120", 2), y = pokoj("M121", 2);
        Rezerwacja rx = BazaDanych.zapiszRezerwacje(g, x, od, koniec, 1, admin.getId());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Future<Boolean> move = pool.submit(() -> {
                BazaDanych.zaloguj("recepcja", "Recepcja123!"); start.await();
                try { przenies(rx, y); return true; }
                catch (SQLException e) { if (!e.getMessage().contains("zajęty")) throw e; return false; }
            });
            Future<Boolean> book = pool.submit(() -> {
                Pracownik worker = BazaDanych.zaloguj("recepcja", "Recepcja123!"); start.await();
                try { BazaDanych.zapiszRezerwacje(g, y, od, koniec, 1, worker.getId()); return true; }
                catch (SQLException e) { if (!e.getMessage().contains("zajęty")) throw e; return false; }
            });
            start.countDown();
            check((move.get(15, TimeUnit.SECONDS) ? 1 : 0) + (book.get(15, TimeUnit.SECONDS) ? 1 : 0) == 1,
                    "Równoczesne przeniesienie i nowa rezerwacja mają jednego zwycięzcę");
        } finally { pool.shutdownNow(); }
        Pokoj swapA = pokoj("M130", 2), swapB = pokoj("M131", 2);
        Rezerwacja ra = BazaDanych.zapiszRezerwacje(g, swapA, od, koniec, 1, admin.getId());
        Rezerwacja rb = BazaDanych.zapiszRezerwacje(g, swapB, koniec, koniec.plusDays(2), 1, admin.getId());
        ExecutorService swap = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Future<?> fa = swap.submit(() -> {
                BazaDanych.zaloguj("recepcja", "Recepcja123!"); start.await(); przenies(ra, swapB); return null;
            });
            Future<?> fb = swap.submit(() -> {
                BazaDanych.zaloguj("recepcja", "Recepcja123!"); start.await(); przenies(rb, swapA); return null;
            });
            start.countDown(); fa.get(15, TimeUnit.SECONDS); fb.get(15, TimeUnit.SECONDS);
            check(IntegrationProbe.rezerwacja(ra.getId()).getPokoj().getId() == swapB.getId()
                    && IntegrationProbe.rezerwacja(rb.getId()).getPokoj().getId() == swapA.getId(),
                    "Równoczesne ruchy w przeciwnych kierunkach bez zakleszczenia");
        } finally { swap.shutdownNow(); }
        System.out.println("ROOM_CHANGE_OK: " + assertions + " asercji, transakcja, cena, wpłaty, goście, sprzątanie i wyścig zapisów.");
    }
}
