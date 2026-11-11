package pl.hotel.raporty;

import pl.hotel.model.*;
import pl.hotel.baza.BazaDanych;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class Raporty {
    private Raporty() {}
    public record Tabela(String nazwa, List<String> kolumny, List<List<String>> wiersze) {
        public Tabela { kolumny=List.copyOf(kolumny); wiersze=wiersze.stream().map(List::copyOf).toList(); }
    }
    public record Raport(LocalDate od, LocalDate doDnia, LocalDateTime utworzono, List<Tabela> tabele) {
        public Raport { tabele=List.copyOf(tabele); }
    }
    public static final String ZASADY = "Okres obejmuje oba wybrane dni. Noc przypisujemy do dnia jej rozpoczęcia; "
            + "dzień wyjazdu nie jest noclegiem. Obłożenie rezerwacyjne obejmuje pobyty potwierdzone, zameldowane "
            + "i zakończone, bez anulowanych. Mianownik: wszystkie aktualne pokoje × dni okresu (także pokoje wyłączone); "
            + "baza nie przechowuje historycznej liczby pokoi. Wartość noclegów to wycena części pobytów w okresie, "
            + "a nie otrzymane pieniądze. Płatności liczymy według daty transakcji, niezależnie od terminu pobytu.";
    public static Raport generuj(LocalDate od,LocalDate koniec) throws SQLException {
        return pl.hotel.baza.BazaDanych.generujRaport(od,koniec);
    }

    public static void zapiszCsv(Tabela t, Path plik) throws java.io.IOException {
        StringBuilder s=new StringBuilder("\uFEFF");
        csvWiersz(s,t.kolumny()); for(var w:t.wiersze()) csvWiersz(s,w);
        zapiszAtomowo(plik,s.toString());
    }
    private static void csvWiersz(StringBuilder s,List<String> pola) {
        for(int i=0;i<pola.size();i++) {
            if(i>0)s.append(';'); String v=pola.get(i);

            String poczatek=v.stripLeading().replaceFirst("^[\\p{Cf}\\p{Z}\\s]+", "");
            if(!poczatek.isEmpty() && ("=+@-".indexOf(poczatek.charAt(0))>=0 || v.startsWith("\t") || v.startsWith("\r"))
                    && !v.matches("-?\\d+(,\\d+)?")) v="'"+v;
            s.append('"').append(v.replace("\"","\"\"")).append('"');
        } s.append("\r\n");
    }
    private static String html(String v) { return v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;"); }
    public static void zapiszHtml(Raport r,Path plik) throws java.io.IOException {
        StringBuilder s=new StringBuilder("<!doctype html><html lang='pl'><meta charset='utf-8'><title>Raport hotelu</title><style>"
                + "body{font:14px Arial,sans-serif;color:#20324d;margin:36px}h1{color:#294d82}h2{margin-top:30px}table{border-collapse:collapse;width:100%;font-size:12px}"
                + "th,td{padding:8px;border:1px solid #ccd6e3;text-align:left;overflow-wrap:anywhere}th{background:#eaf0f8}tr:nth-child(even){background:#f6f8fb}"
                + "small{color:#52647d}thead{display:table-header-group}tr{break-inside:avoid}@media print{body{margin:0}button{display:none}@page{size:A4 landscape;margin:12mm}}"
                + "</style><h1>Raport hotelu</h1><p>Okres: ");
        s.append(r.od()).append(" — ").append(r.doDnia()).append(" (oba dni włącznie)</p><p>Wygenerowano: ").append(r.utworzono()).append("</p>");
        s.append("<button onclick='window.print()'>Drukuj / zapisz jako PDF</button><p><small>").append(html(ZASADY)).append("</small></p>");
        for(Tabela t:r.tabele()) {
            s.append("<h2>").append(html(t.nazwa())).append("</h2><table><thead><tr>");
            for(String h:t.kolumny()) s.append("<th>").append(html(h)).append("</th>");
            s.append("</tr></thead><tbody>");
            for(var w:t.wiersze()) { s.append("<tr>");for(String v:w)s.append("<td>").append(html(v)).append("</td>");s.append("</tr>"); }
            if(t.wiersze().isEmpty())s.append("<tr><td colspan='").append(t.kolumny().size()).append("'>Brak danych w tym okresie.</td></tr>");
            s.append("</tbody></table>");
        } s.append("</html>"); zapiszAtomowo(plik,s.toString());
    }

    private static void zapiszAtomowo(Path plik,String tekst) throws java.io.IOException {
        pl.hotel.pliki.ZapisPliku.tekst(plik, tekst);
    }
}
