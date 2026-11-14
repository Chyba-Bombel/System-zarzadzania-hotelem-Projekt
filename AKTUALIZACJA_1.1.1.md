# Łatka 1.1.1 — ikona okna

Naprawiono domyślną ikonę wyświetlaną w pasku tytułu aplikacji. JavaFX wymaga ustawienia `Stage.getIcons()` niezależnie od ikony pliku EXE nadawanej przez jpackage.

## Instalacja łatki na źródła 1.1.0

1. Zamknij aplikację i zachowaj kopię projektu.
2. Rozpakuj zawartość tej paczki do głównego folderu swojego projektu, obok `pom.xml`. Scal foldery `src`, `resources` i `tests`; zastąp dołączone pliki. To nakładka, nie kompletny projekt.
3. Zachowaj własne `resources/ikona.png`, ikonę instalatora `hotel.ico`, konfigurację bazy i pliki Flatpaka. Łatka nie zawiera ich zamienników.
4. Uruchom `uruchom_windows.bat` albo `mvn javafx:run`. Sprawdź w tytule wersję 1.1.1.
5. Jeśli korzystasz z zainstalowanej aplikacji, zbuduj ponownie instalator przez `buduj_instalator.ps1`, zamknij poprzedni program i zainstaluj nowe wydanie. Stary EXE nie zaktualizuje się od samego skopiowania źródeł. Zachowano dotychczasowy UUID instalatora.

Nie trzeba aktualizować bazy danych. Folderów `out-gui`, `target` i `dist` nie kopiujemy do źródeł łatki.

## Własna ikona

Pliki są odczytywane z zasobów programu w tej kolejności:

1. `resources/ikona.png` — Twoja własna ikona, jeżeli istnieje i daje się odczytać.
2. `resources/hotel.png` — alternatywna ikona.
3. `resources/hotel-default-icon.png` — dołączony granatowo-złoty monogram H.

Ikona musi być rzeczywistym PNG, najlepiej kwadratowym 256 × 256. Wystarczy zmienić ją w `resources`, a potem ponownie zbudować program. Nie edytuj jedynie kopii w `out-gui`.

Ikona jest ładowana z classpath/JAR, bez zależności od bieżącego katalogu. Aplikacja udostępnia rozmiary 16, 32, 48, 64, 128 i 256 pikseli. Ikony otrzymują okno główne oraz dodatkowe okna JavaFX. Zmiana motywu nie usuwa ikony.

`uruchom_windows.bat` kopiuje teraz również PNG z `resources` do `out-gui`. Maven już wcześniej pakował zasoby z tego folderu do JAR-a.

`hotel.ico` nadal służy jpackage do ikony pliku EXE i skrótów. Nie zastępuje ikony okna JavaFX. Natywne okna systemowe, np. wybór pliku, są obsługiwane przez system.

## Weryfikacja

- Kompilacja JDK 17 / JavaFX 17.0.21.
- Uruchomienie testu z rzeczywistego JAR-a, z innego katalogu roboczego.
- Sześć poprawnych rozmiarów ikon dla głównego okna i dialogu.
- Zachowanie ikony po zmianie motywu oraz tytuł wersji 1.1.1.
- Pierwszeństwo własnej `ikona.png`.
- Uszkodzona `ikona.png`: przejście do poprawnej `hotel.png` lub domyślnej ikony.
- Testy JavaFX uruchomiono w trybie Monocle Headless na Linuxie. Nie jest to natywny test dekoracji okien ani instalatora na Windows.

Test: `tests/WindowIconProbe.java`. Wymaga kompilacji testów i zależności testowych tak jak pozostałe scenariusze opisane w `TESTOWANIE.md`. Domyślny scenariusz uruchamiaj bez własnej ikony w testowym classpath; dodatkowe warianty sprawdzano z oddzielnymi katalogami zasobów.
