# HotelManager 1.0.1

To paczka źródeł wersji 1.0.1. Gotowy instalator EXE budujesz na Windowsie. Ta poprawka nie zmienia struktury bazy ani rekordów, więc nie wymaga importu SQL, jeżeli baza działała już z poprzednią wersją aplikacji.

## Co zmieniono

- Konfiguracja jest przechowywana poza folderem instalacji: `%LOCALAPPDATA%\HotelManagerData\config\db.properties`.
- Na Linuxie domyślnie jest to `$XDG_CONFIG_HOME/HotelManagerData/config/db.properties`, a bez XDG: `~/.config/HotelManagerData/config/db.properties`.
- Istniejąca konfiguracja użytkownika ma pierwszeństwo i nie jest nadpisywana podczas migracji.
- Pierwszy odczyt może przenieść stary `db.properties` znajdujący się obok JAR-a, w katalogu roboczym lub pod ścieżką `hotel.legacy.config`. Stary plik pozostaje na miejscu. Jeśli go nie znaleziono, tworzony jest lokalny szablon z pustym hasłem.
- `-Dhotel.config=...` nadal jawnie wskazuje konkretny plik, zwłaszcza do testów. Nie dodawaj tej opcji do nowego instalatora: wymusiłaby starą lokalizację.
- Przycisk ustawień połączenia znajduje się przed logowaniem. Hasło jest maskowane, a zapis nie wykonuje połączenia. Dopiero logowanie sprawdza dostęp do serwera. Zmienne `HOTEL_DB_*` nadal mają pierwszeństwo.
- Numer wersji jest widoczny w tytule oraz w „O programie”. Maven pobiera go z wersji projektu w `pom.xml`; skrypt BAT robi to samo.
- Błędy połączenia rozróżniają brak serwera, odmowę dostępu, brak bazy, niezgodną strukturę i TLS. Przy utracie połączenia po zapisie trzeba sprawdzić dane przed ponowieniem.
- Dziennik zapisuje rodzaj wyjątku, kod SQL/SQLState i miejsca w kodzie. Nie zapisuje treści wyjątku, zapytań, parametrów, haseł ani kartotek gości. Logi: `%LOCALAPPDATA%\HotelManagerData\logs`, do trzech plików po około 1 MiB na instancję procesu.
- Wszystkie następne instalatory budowane nowym skryptem używają stałego identyfikatora z `installer/upgrade-uuid.txt`. Tego pliku nie zmieniaj między wersjami.

## Budowanie na Windows

Wymagane: Maven, JDK z jpackage (u autora JDK 27), WiX oraz własny prawidłowy plik `hotel.ico`. Umieść dotychczasową ikonę obok `pom.xml`; nie ma jej w tej paczce źródeł, ponieważ konwertowałeś ją na swoim komputerze.

Otwórz PowerShell w folderze projektu. Gdy lokalna polityka blokuje uruchamianie własnego skryptu, poniższe polecenie ustawia wyjątek tylko dla nowo uruchamianego procesu; przejrzyj skrypt przed jego użyciem.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\buduj_instalator.ps1 -Typ app-image
```

Podgląd powstaje w `dist\1.0.1-app-image\HotelManager`. Sprawdź ikonę, „O programie”, ustawienia połączenia, logowanie oraz odczyt/zapis danych. Konfiguracja użytkownika jest wspólna dla podglądu i instalacji na tym samym koncie Windows.

Następnie zbuduj instalator:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\buduj_instalator.ps1
```

Wynik: `dist\1.0.1-exe\HotelManager-1.0.1.exe`. Folder wyniku zawiera też README, skrypt aktualizacji, przykład konfiguracji i folder db. Możesz spakować zawartość tego folderu do ZIP/RAR.

Inna lokalizacja ikony:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\buduj_instalator.ps1 -Ikona 'C:\Moje ikony\hotel.ico'
```

Skrypt zatrzymuje się po błędzie Mavena/jpackage i nie pakuje prywatnego `db.properties`. Kompilacja pomija uruchamianie testów; sposób osobnej weryfikacji opisano w TESTOWANIE.md. Przy ponownej budowie zmień nazwę lub usuń wyłącznie poprzedni wygenerowany folder wyniku. Skrypt nie kasuje go automatycznie.

## Pierwsze przejście z instalatora 1.0.0

Poprzedni instalator był tworzony bez jawnie ustalonego identyfikatora aktualizacji. Nie zakładamy zgodności jego identyfikatora z nowym skryptem. Pierwsze przejście wykonaj jako kontrolowaną reinstalację, aby uniknąć dwóch wpisów aplikacji i utraty starego pliku ustawień.

1. Zamknij aplikację i wykonaj kopię bazy.
2. Zachowaj stary `app\db.properties` PRZED odinstalowaniem. Z folderu paczki instalacyjnej uruchom poniższe polecenie, podając prawdziwą ścieżkę:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\aktualizuj_windows.ps1 -StaraKonfiguracja 'C:\SCIEZKA_DO_STAREJ_INSTALACJI\app\db.properties' -TylkoKonfiguracja
```

W paczce źródłowej skrypt znajduje się pod `installer\aktualizuj_windows.ps1`. Jeśli konfiguracja użytkownika już istnieje, skrypt jej nie nadpisze. Sprawdź, czy zawiera właściwe dane; ewentualną zmianę wykonaj świadomie w ustawieniach aplikacji.

3. Odinstaluj starą aplikację przez ustawienia Windows. Nie usuwaj bazy ani folderu konfiguracji użytkownika.
4. Uruchom nowy `HotelManager-1.0.1.exe`.
5. Sprawdź wersję, połączenie oraz dotychczasowe dane.

## Aktualizacje od 1.0.1 wzwyż

Zwiększ wersję projektu w `pom.xml`, zachowaj nazwę HotelManager, instalację dla bieżącego użytkownika oraz ten sam UUID. Używaj `buduj_instalator.ps1` dla wszystkich kolejnych wersji.

Po wykonaniu kopii bazy, zamknięciu programu i wypakowaniu nowej paczki możesz uruchomić:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\aktualizuj_windows.ps1
```

Skrypt zachowuje lokalną kopię konfiguracji i uruchamia jedyny znajdujący się obok instalator `HotelManager-*.exe`. Nie pobiera aktualizacji z internetu, nie zmienia SQL i nie wykonuje kopii bazy. Instalację przeprowadza standardowy instalator Windows. Konfiguracja poza folderem programu pozostaje na miejscu.

## Co trzeba jeszcze sprawdzić na Windows

- Utworzenie podglądu i instalatora z Twoją ikoną, na Twoim JDK i WiX.
- Pierwsze przejście z faktycznie opublikowanego instalatora 1.0.0 według powyższej instrukcji.
- Przejście 1.0.1 → kolejna wyższa wersja, zbudowana tym samym skryptem i UUID: jeden wpis aplikacji, zachowane ustawienia, działający skrót i dane.
- Zamknięcie instalatora bez instalacji, zajęty plik programu, odinstalowanie oraz instalacja na czystym koncie Windows.

Tych prób nie oznaczono jako zaliczonych: środowisko przygotowania paczki jest linuksowe. Szczegóły wykonanych testów kodu są w WYNIKI_TESTOW_1.0.1.txt.
