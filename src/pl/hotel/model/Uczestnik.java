package pl.hotel.model;

public record Uczestnik(String imie, String nazwisko) {
    public Uczestnik {
        imie = imie == null ? "" : imie.trim();
        nazwisko = nazwisko == null ? "" : nazwisko.trim();
        if (imie.chars().anyMatch(c -> c < 32 || c == 127) || nazwisko.chars().anyMatch(c -> c < 32 || c == 127) || imie.isEmpty() || imie.length() > 60 || nazwisko.isEmpty() || nazwisko.length() > 80)
            throw new IllegalArgumentException("Podaj imię (do 60 znaków) i nazwisko (do 80 znaków) każdej dodatkowej osoby.");
    }
    public String toString() { return imie + " " + nazwisko; }
}
