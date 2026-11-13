HOTEL MANAGER — INSTALACJA I AKTUALIZACJE

Paczka zawiera instalator Windows 64-bit, instrukcję, skrypt zachowania
konfiguracji, przykład konfiguracji oraz skrypty bazy danych.
Numer wersji odczytasz z nazwy instalatora i w aplikacji „O programie”.

INSTALACJA
1. Wypakuj całą paczkę.
2. Uruchom HotelManager-<wersja>.exe i dokończ instalację.
3. Przygotuj MySQL/MariaDB oraz zgodną bazę; instrukcja jest w db/README.md.
4. Uruchom aplikację i wybierz „Ustawienia połączenia z bazą”.
5. Podaj adres JDBC, konto i hasło MySQL, następnie zapisz i zaloguj się.

Instalator zawiera Javę, JavaFX i JDBC. Nie potrzebujesz osobnego JDK,
Mavena ani WiX. MySQL/MariaDB i baza nie są instalowane automatycznie.
Przygotowanie nowej bazy i aktualizacja istniejącej to różne operacje.
Nie importuj danych demonstracyjnych do używanej bazy.

KONFIGURACJA I LOGI
Konfiguracja: %LOCALAPPDATA%\HotelManagerData\config\db.properties
Logi błędów: %LOCALAPPDATA%\HotelManagerData\logs
Folder otworzysz przez Win+R, wklejając powyższą ścieżkę folderu.
Dokładną lokalizację pokazuje również „O programie”.

Istniejące ustawienia mają pierwszeństwo. Przy braku ustawień aplikacja
przenosi znaleziony stary db.properties albo tworzy szablon z pustym hasłem.
Zmienne HOTEL_DB_URL, HOTEL_DB_USER i HOTEL_DB_PASSWORD zastępują wartości
z pliku. Konto MySQL to nie to samo co konto pracownika w oknie logowania.
Przykładowe hasło demo nie jest hasłem Twojej rzeczywistej bazy.

localhost oznacza komputer, na którym uruchamiasz aplikację. Dla zdalnego
serwera wymagane jest połączenie TLS z weryfikacją nazwy i certyfikatu.
Nie przenoś ustawienia sslMode=DISABLED na połączenia zdalne.

PIERWSZA AKTUALIZACJA Z 1.0.0
Stary instalator nie miał jawnie ustalonego wspólnego UUID aktualizacji.
Dlatego pierwsze przejście wykonaj jako reinstalację:
1. Zamknij program i zrób kopię bazy.
2. Zachowaj stary app\db.properties PRZED odinstalowaniem aplikacji.
   W PowerShell otwartym w folderze tej paczki uruchom:

powershell -NoProfile -ExecutionPolicy Bypass -File .\aktualizuj_windows.ps1 -StaraKonfiguracja "C:\SCIEZKA_DO_STAREJ_INSTALACJI\app\db.properties" -TylkoKonfiguracja

   Podaj prawdziwą ścieżkę. Polecenie uruchamia przejrzany przez Ciebie
   lokalny skrypt z wyjątkiem polityki wykonania tylko dla tego procesu.
   Istniejący plik użytkownika nie jest nadpisywany. Sprawdź jego dane.
3. Odinstaluj starą aplikację w ustawieniach Windows. Nie usuwaj bazy.
4. Uruchom nowy instalator i sprawdź wersję, konfigurację oraz dane.

KOLEJNE AKTUALIZACJE
Instalatory od 1.0.1 budowane nowym skryptem używają wspólnego UUID.
Zamknij aplikację i wykonaj kopię bazy. Z folderu nowej paczki uruchom:

powershell -NoProfile -ExecutionPolicy Bypass -File .\aktualizuj_windows.ps1

Skrypt robi lokalną kopię konfiguracji i uruchamia instalator z tego folderu.
Nie pobiera wersji z internetu i nie robi kopii ani aktualizacji bazy.
Konfiguracja znajduje się poza folderem programu i pozostaje na miejscu.
Poprawka 1.0.1 sama nie wymaga zmiany bazy zgodnej z poprzednią aplikacją.

PROBLEMY
- Brak połączenia: sprawdź serwer, adres, port i dostęp do sieci.
- Odmowa dostępu: sprawdź konto MySQL, hasło i uprawnienia.
- Brak bazy/tabel/kolumn: sprawdź zgodność struktury z wersją aplikacji.
- Błąd TLS: sprawdź certyfikat i nazwę serwera; nie wyłączaj weryfikacji.
- Błąd po zapisie: odśwież dane przed ponowieniem; zapis mógł się udać.
- Zmiany pliku bez efektu: sprawdź właściwy plik oraz zmienne HOTEL_DB_*.

Nie przenoś samego HotelManager.exe poza folder instalacji.
Log nie zawiera treści wyjątków, zapytań SQL, haseł ani danych gości.
Zachowaj pełny komunikat z okna i odpowiedni log do diagnozy.
Nie udostępniaj pliku db.properties ani jego kopii z własnym hasłem.

Kopie bazy wykonuje się osobno — instalator nie jest kopią danych hotelu.
Odinstalowanie: Ustawienia Windows > Aplikacje > Zainstalowane aplikacje.
Folder danych użytkownika i baza są zarządzane osobno.

Repozytorium:
https://github.com/Chyba-Bombel/System-zarzadzania-hotelem-Projekt/
