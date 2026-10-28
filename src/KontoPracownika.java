public record KontoPracownika(
        long id,
        String imie,
        String nazwisko,
        String login,
        String rola,
        boolean aktywny
) {
    public void wyswietlDane(){
        System.out.println("ID: " + id + " | " + imie + " " + nazwisko + " | Login: " + login + " | Rola: " + rola + " | " + (aktywny ? "AKTYWNE" : "NIEAKTYWNE"));
    }
}
