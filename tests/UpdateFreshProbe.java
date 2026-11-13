import pl.hotel.konfiguracja.KonfiguracjaAplikacji;

public final class UpdateFreshProbe {
    public static void main(String[] args) throws Exception {
        var p = KonfiguracjaAplikacji.wczytajPlik();
        if (!p.getProperty("password").isEmpty()) throw new AssertionError("Nowa konfiguracja ma zawierac puste haslo");
        System.out.println("FRESH_OK");
    }
}
