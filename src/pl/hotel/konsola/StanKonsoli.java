package pl.hotel.konsola;

import pl.hotel.model.*;
import java.util.ArrayList;
import java.util.Scanner;

/** Dane bieżącej sesji wcześniejszej wersji konsolowej. */
final class StanKonsoli {
    private StanKonsoli() {}
    static ArrayList<Gosc> goscie = new ArrayList<>();
    static ArrayList<Pracownik> pracownicy = new ArrayList<>();
    static ArrayList<Pokoj> pokoje = new ArrayList<>();
    static ArrayList<Rezerwacja> rezerwacje = new ArrayList<>();
    static Scanner scanner = new Scanner(System.in);
    static Pracownik zalogowanyPracownik;
}
