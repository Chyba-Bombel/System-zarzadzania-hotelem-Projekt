HOTEL MANAGER
Instrukcja instalacji i pierwszego uruchomienia
Wersja instalatora: 1.0.0 | Windows 64-bit

1. CO ZNAJDUJE SIĘ W PACZCE?

HotelManager-1.0.0.exe to instalator systemu zarządzania hotelem.
Aplikacja służy do obsługi gości, pokoi i rezerwacji.

Program zawiera środowisko Java, biblioteki JavaFX oraz sterownik JDBC.
Do uruchomienia zainstalowanej aplikacji nie potrzebujesz JDK, Mavena ani WiX.
Instalator nie instaluje serwera MySQL/MariaDB i nie tworzy automatycznie bazy.

2. WYMAGANIA

- Windows 64-bit.
- Dostęp do działającego serwera MySQL lub MariaDB.
- Baza hotel_manager ze strukturą zgodną z wersją programu.
- Konto bazy danych z uprawnieniami wymaganymi przez aplikację.

Serwer bazy może działać na tym samym komputerze lub na osobnym serwerze.
Internet nie jest wymagany przy korzystaniu z lokalnej bazy.

3. INSTALACJA PROGRAMU

1. Wypakuj całą paczkę ZIP do wybranego folderu.
2. Uruchom HotelManager-1.0.0.exe.
3. Wybierz folder instalacji i przejdź przez kolejne kroki instalatora.
4. Zakończ instalację.
5. Przed pierwszym uruchomieniem skonfiguruj połączenie z bazą (punkt 5).

Instalator przygotowano do instalacji dla bieżącego użytkownika.
Program uruchamiaj ze skrótu utworzonego podczas instalacji lub z pliku
HotelManager.exe znajdującego się w wybranym folderze instalacji.

4. PRZYGOTOWANIE BAZY DANYCH

Jeśli baza jest już przygotowana i zgodna z aplikacją, nie twórz jej ponownie.

Jeśli uruchamiasz program na nowym komputerze:

1. Zainstaluj i uruchom MySQL albo MariaDB, np. odpowiednią usługę w XAMPP.
2. Otwórz narzędzie do zarządzania bazą, np. phpMyAdmin.
3. Zaimportuj skrypt tworzący bazę, dostarczony z tą samą wersją projektu.
4. Przygotuj konto bazy danych dla aplikacji i nadaj mu wymagane uprawnienia.
5. Zapisz nazwę bazy, adres serwera, port, nazwę użytkownika i hasło.

Jeżeli do paczki dołączono folder db z plikami nowa_baza.sql oraz
aktualizuj_baze.sql, pierwszy służy do przygotowania nowej bazy, a drugi
do aktualizacji istniejącej. Przed importem przeczytaj dołączoną instrukcję
db/README.md. Nie importuj skryptu nowej bazy do bazy z własnymi danymi
bez wcześniejszego sprawdzenia jego działania i wykonania kopii zapasowej.

Sam instalator EXE nie zastępuje skryptów SQL. Jeżeli ich nie otrzymałeś,
poproś autora paczki o pliki bazy zgodne z wersją aplikacji.

5. KONFIGURACJA POŁĄCZENIA

Zamknij aplikację, jeśli jest uruchomiona.
Otwórz folder wybrany podczas instalacji, następnie podfolder app.
Otwórz plik db.properties w Notatniku.

Przykład konfiguracji dla bazy na tym samym komputerze:

url=jdbc:mysql://localhost:3306/hotel_manager?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=10000
user=hotel_app
password=TWOJE_HASLO

Zastąp TWOJE_HASLO rzeczywistym hasłem użytkownika bazy.
Nie dodawaj cudzysłowów wokół wartości.
Zapisz plik jako db.properties, a nie db.properties.txt, w kodowaniu UTF-8.

Znaczenie ustawień:

- localhost: komputer, na którym uruchamiasz aplikację.
- 3306: port serwera bazy; zmień go, jeśli serwer używa innego portu.
- hotel_manager: nazwa bazy danych.
- user: konto serwera MySQL/MariaDB, a nie login pracownika w aplikacji.
- password: hasło tego konta bazy danych. (UWAGA: W db.properties, jest podane hasło demonstracyjne!)

Powyższy adres jest przykładem wyłącznie dla lokalnego serwera.
Dla serwera zdalnego uzyskaj od administratora adres połączenia z poprawnie
skonfigurowanym TLS i weryfikacją tożsamości serwera. Nie przenoś ustawienia
sslMode=DISABLED z przykładu lokalnego na połączenie zdalne.

Jeżeli system ma ustawione zmienne HOTEL_DB_URL, HOTEL_DB_USER lub
HOTEL_DB_PASSWORD, mogą one zastępować odpowiadające im wartości z pliku.

6. PIERWSZE URUCHOMIENIE

1. Sprawdź, czy serwer MySQL/MariaDB działa.
2. Uruchom HotelManager ze skrótu lub z folderu instalacji.
3. Zaloguj się kontem pracownika istniejącym w przygotowanej bazie.
4. Sprawdź, czy wczytują się goście, pokoje i rezerwacje.

Dane logowania do aplikacji otrzymasz od osoby przygotowującej bazę.
Login pracownika i konto połączenia z bazą to dwa różne konta.
Jeśli otrzymałeś konto demonstracyjne, zmień jego hasło przed użyciem
programu do pracy z rzeczywistymi danymi.

7. NAJCZĘSTSZE PROBLEMY

Nie można połączyć się z bazą / Communications link failure:
Sprawdź działanie serwera, adres, port oraz dostępność sieci.

Access denied for user:
Sprawdź user i password w konfiguracji oraz uprawnienia konta bazy.

Unknown database:
Sprawdź nazwę bazy w adresie url i upewnij się, że baza została utworzona.

Brak tabeli lub kolumny:
Struktura bazy może nie odpowiadać wersji aplikacji. Sprawdź właściwe
skrypty instalacyjne lub aktualizacyjne. Najpierw wykonaj kopię bazy.

Zmiany konfiguracji nie działają:
Edytuj plik app/db.properties w folderze ZAINSTALOWANEGO programu.
Zamknij i uruchom aplikację ponownie. Sprawdź też zmienne HOTEL_DB_*.

Nie można się zalogować, ale baza jest dostępna:
Sprawdź konto pracownika, jego hasło i aktywność w aplikacji.
Nie wpisuj w oknie logowania danych konta MySQL/MariaDB.

Program nie uruchamia się:
Nie przenoś samego HotelManager.exe poza folder instalacji.
Aplikacja potrzebuje również podfolderów app i runtime.
Zapisz dokładny komunikat błędu i przekaż go autorowi programu.

Stara ikona na skrócie:
Sprawdź nowy skrót utworzony przez instalator. Windows może przechowywać
poprzednią ikonę w pamięci podręcznej.

8. AKTUALIZACJA I KOPIE ZAPASOWE

Przed aktualizacją zamknij program, wykonaj kopię bazy danych i zachowaj
własny plik app/db.properties poza folderem instalacji.
Po instalacji nowej wersji sprawdź konfigurację przed uruchomieniem.
Aktualizację bazy wykonuj zgodnie z instrukcją danej wersji projektu.

Dane gości i rezerwacji znajdują się w bazie, a nie w samym pliku EXE.
Skopiowanie instalatora nie jest kopią zapasową danych hotelu.
Plik db.properties może zawierać hasło: nie publikuj go i nie przesyłaj
razem z własnymi danymi dostępowymi innym osobom.

9. ODINSTALOWANIE

Otwórz Ustawienia Windows > Aplikacje > Zainstalowane aplikacje,
odszukaj HotelManager i wybierz Odinstaluj.
Serwer bazy i przechowywane w nim dane są zarządzane osobno.

Repozytorium projektu:
https://github.com/Chyba-Bombel/System-zarzadzania-hotelem-Projekt/
