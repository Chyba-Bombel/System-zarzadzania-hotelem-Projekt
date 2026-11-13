# HotelManager 1.0.2

Poprawka konfiguracji, diagnostyki i pakowania. Wygląd całego GUI pozostaje zakresem planowanej wersji 1.1.0. Wydanie 1.0.2 nie zmienia struktury bazy zgodnej z 1.0.1 i nie wymaga ponownego importu SQL.

## Nowe funkcje

### Test połączenia przed logowaniem

Otwórz „Ustawienia połączenia z bazą”, uzupełnij formularz i kliknij „Testuj połączenie”. Test działa w tle, wykonuje wyłącznie `SELECT 1` i zamyka połączenie. Nie zapisuje pól formularza, nie loguje pracownika i nie sprawdza całej struktury bazy ani praw do wszystkich operacji hotelowych. Dopiero „Zapisz” zmienia plik konfiguracji. Podczas testu pola, zapis i przywracanie są zablokowane; można zamknąć dialog. Obowiązują timeouty JDBC.

Zmienne `HOTEL_DB_URL`, `HOTEL_DB_USER` i `HOTEL_DB_PASSWORD` mają pierwszeństwo nad formularzem, tak samo jak przy normalnym połączeniu. Jeśli zmienna nadpisuje pole, zmiana samego formularza go nie zastąpi.

### Kopie ustawień i przywracanie

Przed nadpisaniem pliku powstaje kopia jego poprzedniej treści. Jeśli nie uda się utworzyć kopii, zapis zostaje przerwany i aktualny plik pozostaje nietknięty. Nowy plik zapisywany jest przez plik tymczasowy i podmianę, z atomowym przeniesieniem tam, gdzie system je obsługuje. Zachowywanych jest do pięciu kopii; gdy porządkowanie starszych plików się nie powiedzie, błąd jest logowany i liczba może tymczasowo być większa.

Kopie znajdują się w podfolderze `backups` obok bieżącego `db.properties`. Przycisk „Przywróć kopię” pozwala wybrać plik. Przywrócenie również zachowuje poprzedni stan jako kopię. Dowolny plik spoza zarządzanego folderu i dowiązania do innych plików nie są przyjmowane. Uszkodzony format bieżącego pliku nie blokuje otwarcia formularza przywracania.

Kopie zawierają hasło konta MySQL. Nie publikuj ich ani nie dołączaj do logów lub Releases. Są lokalną pomocą przy zmianie ustawień, nie kopią danych hotelu. Nadal wykonuj niezależne kopie bazy.

### Diagnostyka

W „O programie” są przyciski „Otwórz folder logów” i „Kopiuj informacje o wersji”. Do schowka trafiają numer aplikacji, Java, nazwa systemu i architektura — bez adresu bazy, loginu, hasła i danych gości. Gdy otwarcie menedżera plików nie jest obsługiwane, wyświetlana jest ścieżka do ręcznego otwarcia.

Domyślne pliki użytkownika:

| System | Konfiguracja | Logi |
| --- | --- | --- |
| Windows | `%LOCALAPPDATA%\HotelManagerData\config\db.properties` | `%LOCALAPPDATA%\HotelManagerData\logs` |
| Linux | `~/.config/HotelManagerData/config/db.properties` | `~/.config/HotelManagerData/logs` |

Na Linuksie `XDG_CONFIG_HOME` zastępuje `~/.config`. Nadal działają jawne opcje `hotel.config` i `hotel.data`. Folderu danych nie wybieraj jako katalogu instalacji programu.

## Budowanie instalatora Windows

Zachowaj własny `hotel.ico` obok `pom.xml`. Używaj tego samego konta Windows i instalacji dla bieżącego użytkownika. Z folderu projektu:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\buduj_instalator.ps1 -Typ app-image
```

Po sprawdzeniu podglądu zbuduj instalator:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\buduj_instalator.ps1
```

Polecenie uruchamia przejrzany lokalny skrypt z wyjątkiem polityki tylko dla tego procesu. Wynik: `dist\1.0.2-exe`. Zawiera instalator, README, skrypt aktualizacji, przykład konfiguracji i folder db. Możesz spakować zawartość do ZIP/RAR.

Nie zmieniaj `installer/upgrade-uuid.txt`, nazwy HotelManager ani zakresu instalacji między wersjami. UUID jest taki sam jak w dostarczonym skrypcie 1.0.1. Jeśli wcześniejszy instalator powstał inną metodą, zgodność wymaga sprawdzenia; nie jest gwarantowana samym numerem wersji.

## Budowanie instalatora Linux

Skrypt uruchamiaj na Linuksie ze zgodnym JDK, Mavenem i Pythonem 3. Dla DEB potrzebne są `fakeroot` oraz narzędzia `dpkg-dev`. Biblioteki JavaFX pobiera Maven dla systemu budującego; nie są kopiowane pliki z `javafx-win`.

Na Mint 22 / Ubuntu 24.04 narzędzia można przygotować tak:

```bash
sudo apt update
sudo apt install openjdk-21-jdk maven python3 fakeroot dpkg-dev libgtk-3-0t64
```

Najpierw podgląd:

```bash
bash buduj_instalator_linux.sh app-image
```

Gotowy podgląd jest w `dist/1.0.2-linux-<architektura>-app-image/HotelManager/bin/HotelManager`. Uruchamiaj go ze zwykłego konta i sprawdź działanie z testową bazą.

Następnie pełna paczka:

```bash
bash buduj_instalator_linux.sh
```

Skrypt kompiluje aplikację, tworzy DEB i ZIP zawierający DEB, README-Linux.txt, folder db i db.properties.example. Nie pakuje prywatnej konfiguracji ani kopii użytkownika. Wersję odczytuje z pom.xml. Folder i archiwum powstają w `dist/1.0.2-linux-<architektura>-deb` oraz pod tą samą nazwą z końcówką `.zip`.

Jeśli obok pom.xml jest prawidłowy `hotel.png`, zostanie użyty. Bez niego skrypt używa domyślnej ikony i informuje o tym. Możesz wskazać inną ikonę:

```bash
bash buduj_instalator_linux.sh deb '/home/uzytkownik/Obrazy/hotel.png'
```

Pakiet deklaruje zależności systemowe GTK/X11 i czcionek. Nazwę pakietu GTK (`libgtk-3-0t64` lub starsze `libgtk-3-0`) skrypt odczytuje z zainstalowanych pakietów systemu. W minimalnym kontenerze bez takiej informacji można jawnie wskazać nazwę dla docelowej dystrybucji, np. `HOTEL_GTK_PACKAGE=libgtk-3-0t64 bash buduj_instalator_linux.sh`. Nie służy to do omijania zgodności systemów.

Skrypt zatrzymuje się po błędzie. Nie nadpisuje wcześniejszego folderu wyniku ani ZIP-a — przenieś lub zmień nazwę starych wyników przed ponowną budową. DEB nie jest uniwersalnym pakietem na wszystkie dystrybucje: zależy od architektury i bibliotek systemu budowania.

## Aktualizacja 1.0.1 → 1.0.2

1. Zamknij aplikację, wykonaj kopię bazy i zachowaj prywatną konfigurację poza paczką instalacyjną.
2. Windows: użyj nowego instalatora zbudowanego tym samym skryptem, UUID i zakresem użytkownika. Pomocniczy `aktualizuj_windows.ps1` robi lokalną kopię konfiguracji i uruchamia instalator. Sprawdź, czy po aktualizacji istnieje jeden wpis HotelManager w ustawieniach systemu.
3. Linux: z folderu nowej paczki z jednym DEB wykonaj `sudo apt install ./hotel-manager*.deb`. Nazwa pakietu nadal jest `hotel-manager`.
4. W „O programie” sprawdź 1.0.2, konfigurację i dostęp do starych danych. Przywracanie kopii ustawień nie cofa wersji programu ani danych bazy.
5. Dla pierwszego przejścia z 1.0.0 nadal obowiązuje procedura z AKTUALIZACJA_1.0.1.md — zachowanie starego pliku PRZED odinstalowaniem.

## Sprawdzenie przed publikacją

Wyniki wykonanych testów są w WYNIKI_TESTOW_1.0.2.txt. Na własnym Windowsie i Mincie sprawdź:

- czystą instalację i przejście z faktycznie wydanej 1.0.1;
- poprawną wersję, ikonę i jeden wpis aplikacji w systemie;
- zachowanie ustawień, test dobrego i złego połączenia, kopię i przywrócenie;
- logowanie obu ról, rezerwację, wpłatę, rozliczenie, raport i ponowne uruchomienie;
- otwieranie logów oraz kopiowanie informacji w prawdziwym pulpicie;
- zamknięcie instalatora bez instalacji i zachowanie danych po odinstalowaniu.

Nie uznawaj aktualizacji Windows za przetestowaną wyłącznie na podstawie kompilacji Java lub niezmienionego UUID. Publikację wydania na GitHubie wykonaj po próbie instalatora na docelowym systemie.
