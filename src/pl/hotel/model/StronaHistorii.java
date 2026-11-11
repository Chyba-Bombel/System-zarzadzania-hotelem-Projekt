package pl.hotel.model;

import java.util.List;

public record StronaHistorii(List<ZmianaRezerwacji> wpisy, boolean maStarsze) {
    public StronaHistorii { wpisy = List.copyOf(wpisy); }
}
