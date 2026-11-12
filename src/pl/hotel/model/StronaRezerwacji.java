package pl.hotel.model;
import java.util.List;
public record StronaRezerwacji(List<WynikRezerwacji> wyniki,boolean maStarsze){public StronaRezerwacji{wyniki=List.copyOf(wyniki);}}
