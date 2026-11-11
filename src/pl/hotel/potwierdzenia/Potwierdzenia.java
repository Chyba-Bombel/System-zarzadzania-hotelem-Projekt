package pl.hotel.potwierdzenia;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import pl.hotel.model.PotwierdzenieRezerwacji;
import pl.hotel.pliki.ZapisPliku;

public final class Potwierdzenia {
    private static final Locale PL = Locale.forLanguageTag("pl-PL");
    private Potwierdzenia() {}

    public static String kwota(BigDecimal kwota) { return String.format(PL, "%,.2f zł", kwota); }
    public static String czas(PotwierdzenieRezerwacji p) {
        return p.utworzono().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }
    public static String uwaga(PotwierdzenieRezerwacji p) {
        return p.anulowana() ? "REZERWACJA ANULOWANA — dokument archiwalny. Nie potwierdza aktywnego pobytu."
                : p.status().equals("ZAKONCZONA") ? "Pobyt zakończony — dokument przedstawia dane archiwalnego pobytu."
                : "Dokument przedstawia stan rezerwacji w chwili pobrania danych.";
    }
    public static List<List<String>> pobyt(PotwierdzenieRezerwacji p) {
        return List.of(List.of("Numer rezerwacji", "#" + p.id()), List.of("Status", p.opisStatusu()),
                List.of("Główny gość", p.glownyGosc()), List.of("Pokój", p.pokoj() + " • " + p.typ()),
                List.of("Przyjazd", p.od().toString()), List.of("Wyjazd", p.doDnia().toString()),
                List.of("Liczba nocy", String.valueOf(p.noce())), List.of("Liczba osób", String.valueOf(p.liczbaOsob())));
    }
    public static List<List<String>> rozliczenie(PotwierdzenieRezerwacji p) {
        return List.of(List.of("Cena pokoju za noc", kwota(p.cenaZaNoc())),
                List.of("Wartość noclegów", kwota(p.wartoscPobytu())), List.of("Należność", kwota(p.naleznosc())),
                List.of("Wpłaty", kwota(p.wplaty())), List.of("Zwroty", kwota(p.zwroty())),
                List.of("Wpłaty minus zwroty", kwota(p.netto())),
                List.of(p.saldo().signum() < 0 ? "Do zwrotu" : p.saldo().signum() > 0 ? "Do zapłaty" : "Pozostałe saldo",
                        kwota(p.saldo().abs())));
    }
    public static List<String> goscie(PotwierdzenieRezerwacji p) {
        ArrayList<String> osoby = new ArrayList<>();
        osoby.add(p.glownyGosc() + " — główny gość"); osoby.addAll(p.uczestnicy());
        if (p.brakujaceOsoby() > 0) osoby.add("Brak danych dodatkowych osób: " + p.brakujaceOsoby());
        return List.copyOf(osoby);
    }
    public static String nazwaPliku(PotwierdzenieRezerwacji p) {
        return "rezerwacja_" + p.id() + "_" + p.od() + ".html";
    }
    private static String html(String tekst) {
        return tekst.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
    private static void tabela(StringBuilder s, List<List<String>> wiersze) {
        s.append("<table><tbody>");
        for (var w : wiersze) s.append("<tr><th scope='row'>").append(html(w.get(0)))
                .append("</th><td>").append(html(w.get(1))).append("</td></tr>");
        s.append("</tbody></table>");
    }
    public static String html(PotwierdzenieRezerwacji p) {
        StringBuilder s = new StringBuilder("""
                <!doctype html>
                <html lang="pl"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'">
                <title>Potwierdzenie danych rezerwacji</title>
                <style>
                *{box-sizing:border-box}body{margin:0;background:#eef2f7;color:#20324d;font:14px Arial,sans-serif;line-height:1.45}
                .sheet{max-width:840px;margin:28px auto;padding:32px;background:white;border:1px solid #d8e0eb;border-radius:12px}
                header{border-bottom:3px solid #315e9e;padding-bottom:16px}h1{margin:0;font-size:27px;color:#234b83}
                h2{font-size:17px;margin:24px 0 8px}p{margin:8px 0}.muted,footer{color:#5a6a80;font-size:12px}
                .notice{background:#edf3fb;border-left:4px solid #315e9e;padding:12px;margin:18px 0;overflow-wrap:anywhere}
                .cancelled{background:#fff0f0;border-color:#b42335;color:#8a1b2c;font-weight:bold}
                table{width:100%;border-collapse:collapse}th,td{border-bottom:1px solid #e0e6ef;padding:7px 9px;text-align:left;overflow-wrap:anywhere}
                th{width:43%;font-weight:normal;color:#566780}tr:last-child td{font-weight:bold}li{padding:3px 0;overflow-wrap:anywhere}
                footer{margin-top:24px;border-top:1px solid #d8e0eb;padding-top:12px}.print-help{max-width:840px;margin:18px auto;padding:0 12px}
                @page{size:A4;margin:14mm}@media print{body{background:white;font-size:11px}.sheet{border:0;border-radius:0;margin:0;padding:0}
                h1{font-size:23px}h2{margin-top:17px}.print-help{display:none}tr,li,header,.notice{break-inside:avoid}th,td{padding:5px 7px}}
                </style></head><body><p class="print-help">Drukowanie: naciśnij Ctrl+P i wybierz drukarkę lub „Zapisz jako PDF”.</p>
                <main class="sheet"><header><h1>Potwierdzenie danych rezerwacji</h1>
                """);
        s.append("<p>Rezerwacja <strong>#").append(p.id()).append("</strong></p><p class='muted'>Stan na: ")
                .append(html(czas(p))).append(" (czas serwera MySQL)</p></header>");
        s.append("<p class='notice").append(p.anulowana() ? " cancelled" : "").append("'>").append(html(uwaga(p))).append("</p>");
        s.append("<h2>Dane pobytu</h2>"); tabela(s, pobyt(p));
        s.append("<h2>Goście pobytu</h2><ol>");
        for (String g : goscie(p)) s.append("<li>").append(html(g)).append("</li>");
        s.append("</ol><h2>Rozliczenie w PLN</h2>"); tabela(s, rozliczenie(p));
        s.append("<footer><p>Data wyjazdu nie jest noclegiem. Cena dotyczy całego pokoju za noc.</p>")
                .append("<p>Dokument jest informacją o rezerwacji. Nie jest fakturą ani bankowym potwierdzeniem płatności. ")
                .append("Kwoty wpłat i zwrotów pochodzą z ewidencji hotelu.</p>")
                .append("<p>Po zmianie pobytu lub rozliczenia pobierz nowe dane i przygotuj nowy dokument.</p></footer></main></body></html>");
        return s.toString();
    }
    public static void zapiszHtml(PotwierdzenieRezerwacji p, Path plik) throws IOException {
        ZapisPliku.tekst(plik, html(p));
    }
}
