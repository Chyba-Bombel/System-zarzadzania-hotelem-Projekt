package pl.hotel.gui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.sql.SQLException;
import static pl.hotel.gui.NarzedziaGUI.*;

@FunctionalInterface interface SqlAkcja { void wykonaj() throws SQLException; }
