package pl.hotel.bezpieczenstwo;

public final class SesjaWygasla extends SecurityException {
    public SesjaWygasla() { super("Sesja wygasła lub uprawnienia konta uległy zmianie. Zaloguj się ponownie."); }
}
