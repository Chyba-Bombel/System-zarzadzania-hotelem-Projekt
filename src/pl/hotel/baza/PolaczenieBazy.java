package pl.hotel.baza;

import java.sql.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class PolaczenieBazy {
    private PolaczenieBazy() {}
    public static Connection polacz() throws SQLException {
        return polacz(pl.hotel.konfiguracja.KonfiguracjaAplikacji.wczytaj());
    }

    static Connection polacz(Properties p) throws SQLException {
        String url=p.getProperty("url");
        Map<String,String> param=new HashMap<>(); URI uri;
        try {
            if(!url.startsWith("jdbc:mysql://"))throw new IllegalArgumentException();
            uri=URI.create(url.substring(5));
            if(uri.getHost()==null || uri.getUserInfo()!=null || uri.getFragment()!=null)throw new IllegalArgumentException();
            String q=uri.getRawQuery();
            if(q!=null)for(String czesc:q.split("&")) {
                String[] para=czesc.split("=",2); String k=URLDecoder.decode(para[0],StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT);
                String v=para.length==2 ? URLDecoder.decode(para[1],StandardCharsets.UTF_8) : "";
                if(param.putIfAbsent(k,v)!=null)throw new IllegalArgumentException();
            }
        } catch(IllegalArgumentException e) { throw new SQLException("Niepoprawny URL JDBC: użyj jednego serwera jdbc:mysql://host:port/baza i unikalnych parametrów."); }
        Properties ustawienia=new Properties(); ustawienia.setProperty("user",p.getProperty("user")); ustawienia.setProperty("password",p.getProperty("password"));
        for(String klucz:List.of("autoDeserialize","allowLoadLocalInfile","allowUrlInLocalInfile","allowMultiQueries")) {
            String v=param.get(klucz.toLowerCase(Locale.ROOT));
            if(v!=null && !v.equalsIgnoreCase("false"))throw new SQLException("Konfiguracja JDBC włącza niedozwoloną funkcję sterownika.");
            ustawienia.setProperty(klucz,"false");
        }
        if(param.containsKey("allowloadlocalinfileinpath"))throw new SQLException("Lokalny import plików jest wyłączony.");
        if(param.containsKey("paranoid")&&!param.get("paranoid").equalsIgnoreCase("true"))throw new SQLException("Nie można wyłączyć ochrony danych w komunikatach sterownika.");
        ustawienia.setProperty("paranoid","true");
        for(String klucz:List.of("connectTimeout","socketTimeout")) {
            String v=param.getOrDefault(klucz.toLowerCase(Locale.ROOT),klucz.equals("connectTimeout")?"5000":"15000");
            try { int n=Integer.parseInt(v); if(n<1||n>30000)throw new NumberFormatException(); }
            catch(NumberFormatException e) { throw new SQLException("Timeout JDBC musi wynosić 1–30000 ms."); }
            ustawienia.setProperty(klucz,v);
        }
        String host=uri.getHost(); boolean lokalny=List.of("localhost","127.0.0.1","[::1]","::1").contains(host.toLowerCase(Locale.ROOT));
        if(!lokalny) {
            if(param.containsKey("sslmode")&&!param.get("sslmode").equalsIgnoreCase("VERIFY_IDENTITY"))throw new SQLException("Zdalna baza wymaga sslMode=VERIFY_IDENTITY i zaufanego certyfikatu.");
            if("true".equalsIgnoreCase(param.get("allowpublickeyretrieval")))throw new SQLException("Dla zdalnej bazy wyłącz allowPublicKeyRetrieval i użyj zweryfikowanego TLS.");
            ustawienia.setProperty("sslMode","VERIFY_IDENTITY");
        }
        return DriverManager.getConnection(url,ustawienia);
    }
}
