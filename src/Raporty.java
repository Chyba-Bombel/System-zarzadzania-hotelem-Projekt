import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Raport jest spójnym odczytem bazy, eksportuje te same dane co podgląd. */
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
    private static String kwota(BigDecimal v) { return v.setScale(2,RoundingMode.HALF_UP).toPlainString().replace('.',','); }
    private static String procent(long zajete, long dostepne) {
        return dostepne==0 ? "0,00" : kwota(BigDecimal.valueOf(zajete).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(dostepne),2,RoundingMode.HALF_UP));
    }
    public static Raport generuj(LocalDate od, LocalDate koniec) throws SQLException {
        if (od==null || koniec==null || koniec.isBefore(od) || ChronoUnit.DAYS.between(od,koniec)>365)
            throw new IllegalArgumentException("Wybierz okres od 1 do 366 dni. Data końcowa nie może być wcześniejsza.");
        LocalDate doWylacznie=koniec.plusDays(1); long dni=ChronoUnit.DAYS.between(od,doWylacznie);
        List<List<String>> rezerwacje=new ArrayList<>(), platnosci=new ArrayList<>(), pokoje=new ArrayList<>();
        Map<Long,Long> nocePokoi=new HashMap<>();
        BigDecimal wartosc=BigDecimal.ZERO, wplaty=BigDecimal.ZERO, zwroty=BigDecimal.ZERO;
        long noce=0, osobonoce=0; int aktywne=0, anulowane=0;
        try (Connection c=BazaDanych.polacz()) {
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            c.setReadOnly(true); c.setAutoCommit(false);
            try {
                Map<Long,List<String>> uczestnicy=new HashMap<>();
                try (PreparedStatement p=c.prepareStatement("SELECT u.rezerwacja_id, u.imie, u.nazwisko "
                        + "FROM uczestnicy_rezerwacji u JOIN rezerwacje r ON r.id=u.rezerwacja_id "
                        + "WHERE r.data_od < ? AND r.data_do > ? ORDER BY u.rezerwacja_id,u.pozycja")) {
                    p.setDate(1,java.sql.Date.valueOf(doWylacznie)); p.setDate(2,java.sql.Date.valueOf(od));
                    try(ResultSet r=p.executeQuery()) { while(r.next()) uczestnicy.computeIfAbsent(r.getLong(1),k->new ArrayList<>())
                            .add(r.getString(2)+" "+r.getString(3)); }
                }
                try (PreparedStatement p=c.prepareStatement("SELECT r.*, g.imie, g.nazwisko, p.numer FROM rezerwacje r "
                        + "JOIN goscie g ON g.id=r.gosc_id JOIN pokoje p ON p.id=r.pokoj_id "
                        + "WHERE r.data_od < ? AND r.data_do > ? ORDER BY r.data_od,r.id")) {
                    p.setDate(1,java.sql.Date.valueOf(doWylacznie)); p.setDate(2,java.sql.Date.valueOf(od));
                    try(ResultSet r=p.executeQuery()) { while(r.next()) {
                        long id=r.getLong("id"), pokoj=r.getLong("pokoj_id");
                        LocalDate a=r.getDate("data_od").toLocalDate(), b=r.getDate("data_do").toLocalDate();
                        long n=ChronoUnit.DAYS.between(a.isAfter(od)?a:od,b.isBefore(doWylacznie)?b:doWylacznie);
                        boolean an=r.getString("status").equals("ANULOWANA");
                        BigDecimal cena=an?BigDecimal.ZERO:r.getBigDecimal("cena_za_noc").multiply(BigDecimal.valueOf(n));
                        int osoby=r.getInt("liczba_osob");
                        if(an) anulowane++; else { aktywne++; noce+=n; osobonoce+=n*osoby; wartosc=wartosc.add(cena); nocePokoi.merge(pokoj,n,Long::sum); }
                        String glowny=r.getString("imie")+" "+r.getString("nazwisko");
                        List<String> dodatkowi=uczestnicy.getOrDefault(id,List.of());
                        int brak=Math.max(0,osoby-1-dodatkowi.size());
                        rezerwacje.add(List.of(""+id,glowny,String.join(", ",dodatkowi)+(brak>0?" [brak danych "+brak+" os.]":""),
                                r.getString("numer"),a.toString(),b.toString(),""+osoby,r.getString("status"),""+(an?0:n),kwota(cena)));
                    } }
                }
                try (PreparedStatement p=c.prepareStatement("SELECT id, numer, liczba_miejsc, stan FROM pokoje ORDER BY numer");
                     ResultSet r=p.executeQuery()) {
                    while(r.next()) { long zajete=nocePokoi.getOrDefault(r.getLong("id"),0L);
                        pokoje.add(List.of(r.getString("numer"),""+r.getInt("liczba_miejsc"),r.getString("stan"),""+zajete,""+dni,procent(zajete,dni))); }
                }
                try (PreparedStatement p=c.prepareStatement("SELECT id,rezerwacja_id,data_platnosci,rodzaj,metoda,kwota FROM platnosci "
                        + "WHERE data_platnosci >= ? AND data_platnosci < ? ORDER BY data_platnosci,id")) {
                    p.setTimestamp(1,Timestamp.valueOf(od.atStartOfDay())); p.setTimestamp(2,Timestamp.valueOf(doWylacznie.atStartOfDay()));
                    try(ResultSet r=p.executeQuery()) { while(r.next()) {
                        BigDecimal k=r.getBigDecimal("kwota");
                        if(r.getString("rodzaj").equals("WPLATA")) wplaty=wplaty.add(k); else zwroty=zwroty.add(k);
                        platnosci.add(List.of(""+r.getLong("id"),""+r.getLong("rezerwacja_id"),r.getTimestamp("data_platnosci").toLocalDateTime().toString(),
                                r.getString("rodzaj"),r.getString("metoda"),kwota(k)));
                    } }
                }
                c.commit();
            } catch(SQLException | RuntimeException e) {
                try { c.rollback(); } catch(SQLException rollback) { e.addSuppressed(rollback); } throw e;
            }
        }
        long dostepne=dni*pokoje.size();
        Tabela suma=new Tabela("Podsumowanie",List.of("Wskaźnik","Wartość"),List.of(
                List.of("Okres (oba dni włącznie)",od+" — "+koniec),List.of("Liczba dni",""+dni),
                List.of("Pobyty nieanulowane przecinające okres",""+aktywne),List.of("Anulowane przecinające okres",""+anulowane),
                List.of("Zarezerwowane pokojonoce",""+noce),List.of("Osobonoce",""+osobonoce),
                List.of("Dostępne pokojonoce (aktualna baza pokoi)",""+dostepne),List.of("Obłożenie rezerwacyjne (%)",procent(noce,dostepne)),
                List.of("Wartość noclegów w okresie (zł)",kwota(wartosc)),List.of("Wpłaty w okresie (zł)",kwota(wplaty)),
                List.of("Zwroty w okresie (zł)",kwota(zwroty)),List.of("Wpłaty minus zwroty (zł)",kwota(wplaty.subtract(zwroty)))));
        return new Raport(od,koniec,LocalDateTime.now(),List.of(suma,
                new Tabela("Rezerwacje i goście",List.of("ID","Główny gość","Dodatkowi goście","Pokój","Przyjazd","Wyjazd","Osoby","Status","Noce w okresie","Wartość w okresie (zł)"),rezerwacje),
                new Tabela("Obłożenie pokoi",List.of("Pokój","Miejsca","Stan obecnie","Zarezerwowane noce","Noce okresu","Obłożenie (%)"),pokoje),
                new Tabela("Płatności",List.of("ID","Rezerwacja ID","Data","Rodzaj","Metoda","Kwota (zł)"),platnosci)));
    }
    /** UTF-8 BOM i średnik ułatwiają otwieranie polskich danych w Excelu. */
    public static void zapiszCsv(Tabela t, Path plik) throws java.io.IOException {
        StringBuilder s=new StringBuilder("\uFEFF");
        csvWiersz(s,t.kolumny()); for(var w:t.wiersze()) csvWiersz(s,w);
        zapiszAtomowo(plik,s.toString());
    }
    private static void csvWiersz(StringBuilder s,List<String> pola) {
        for(int i=0;i<pola.size();i++) {
            if(i>0)s.append(';'); String v=pola.get(i);
            // Neutralizujemy formuły w danych tekstowych, zachowując liczby ujemne.
            String poczatek=v.stripLeading();
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
    /** Przy błędzie nie pozostawiamy częściowo nadpisanego raportu. */
    private static void zapiszAtomowo(Path plik,String tekst) throws java.io.IOException {
        Path cel=plik.toAbsolutePath(); Path tmp=Files.createTempFile(cel.getParent(),".hotel-raport-",".tmp");
        try {
            Files.writeString(tmp,tekst,StandardCharsets.UTF_8);
            try { Files.move(tmp,cel,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException e) { Files.move(tmp,cel,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(tmp); }
    }
}
