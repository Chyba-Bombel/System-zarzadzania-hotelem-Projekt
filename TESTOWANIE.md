# Testowanie

Testy dodają i zmieniają rekordy. Uruchamiaj je wyłącznie na osobnej bazie z fikcyjnymi danymi, nigdy na danych hotelu.

## Przygotowanie bazy

Utwórz testowy schemat, np. hotel_manager_test. W roboczej kopii db/nowa_baza.sql zastąp nazwę schematu i wykonaj część przed pierwszym CREATE USER, bez końcowych kont/grantów. Obejmuje to także tabele i dane usług występujące po COMMIT. Nie kończ importu na samym COMMIT.

Przygotuj prywatny db-test.properties ze wskazaniem tego schematu. Konto fixture potrzebuje praw administracyjnych na testowym serwerze: testy tworzą triggery, a PrivilegeProbe tworzy i usuwa wyłącznie swoje tymczasowe konta. Nazwa bazy musi kończyć się _test albo _test_<liczba>, a uruchomienie zawierać -Dhotel.tests=true.

## Kompilacja i logika

```sh
mvn test-compile dependency:build-classpath -Dmdep.outputFile=target/test-classpath.txt -Dmdep.includeScope=test
```

Linux/macOS:

```sh
java -Dhotel.tests=true -Dhotel.config=db-test.properties -cp "target/classes:target/test-classes:$(cat target/test-classpath.txt)" RunAllProbes
```

Windows PowerShell:

```powershell
$testClasspath = Get-Content target/test-classpath.txt -Raw
java -Dhotel.tests=true -Dhotel.config=db-test.properties -cp "target/classes;target/test-classes;$($testClasspath.Trim())" RunAllProbes
```

Są to programy main, nie testy JUnit uruchamiane samoczynnie przez mvn test. Potrzebny jest zgodny JDK i zależności. Każdy wątek ma osobną sesję i musi się zalogować. TestDatabase jest fixture testowym, nie publicznym dostępem do połączenia aplikacji.

RunAllProbes wykonuje CalendarProbe, SecurityProbe, IntegrationProbe, RoomChangeProbe, RoomStateProbe, HistoryProbe, NotesProbe, ConfirmationProbe, ReportProbe, ServicesSearchProbe, GuestArchiveProbe i PrivilegeProbe. Testy obejmują uprawnienia, SQL/LIKE, hasła/sesje, terminy, rollback, współbieżność, usługi i rozliczenie, archiwum, historię i eksport.

PrivilegeProbe korzysta teraz z db/administracja/uprawnienia.sql. Dopasowuje konto, schemat i hosty do unikalnych tymczasowych użytkowników. Sprawdza brak DDL, DELETE nowych tabel oraz zmiany/usuwania płatności i historii. Nie używaj go do sprawdzania rzeczywistego konta produkcyjnego.

## GUI

GuiSmokeLauncher uruchom z tym samym classpath i konfiguracją na świeżej bazie testowej. Przed scenariuszem na tej bazie ustaw demonstracyjne rezerwacje pokoju 101 na ANULOWANA, aby umożliwić rezerwację z kalendarza. Nie rób tego na rzeczywistych pobytach.

Scenariusz obsługuje formularze, pokoje, kalendarz, notatki i ich konflikty, usługi, wyszukiwanie, kartę oraz archiwizację/przywracanie. Na końcu dezaktywuje demonstracyjne konto recepcji. ServicesGuiSteps i GuestArchiveGuiSteps są jego częściami. Przed kolejnym pełnym przebiegiem odtwórz fixture.

Poprzednie wydania sprawdzano w Monocle headless. Nie oznacza to uruchomienia natywnego skryptu Windows ani systemowego okna wyboru pliku. Ich wyniki pozostają w WYNIKI_TESTOW_ARCHIWUM_GOSCI.txt i wcześniejszych plikach.

## Sprawdzenie uporządkowania SQL

Bieżący zakres jest opisany w WYNIKI_TESTOW_BAZA.txt. Sprawdzono świeży import oraz jeden aktualizator na bazie starszej i obecnej, jego powtórzenie, zachowanie danych, odtworzenie płatności i prawa kont. Podstawowe warunki ręcznej próby:

1. Na kopii starszej bazy wykonaj cały db/aktualizuj_baze.sql jako administrator.
2. Porównaj stare wiersze i sprawdź nowe tabele/pola.
3. Zmień ofertę usługi i zarchiwizuj testowego gościa, a następnie wykonaj aktualizację ponownie.
4. Sprawdź zachowanie zmian i brak podwójnych zdarzeń płatności.
5. Sprawdź oba standardowe hosty, brak jednego z kont oraz pomocnik własnego konta.

Nowa instalacja i aktualizacja są alternatywami. db/administracja/ogranicz_uprawnienia.sql nie należy do zwykłej próby migracji: świadomie odbiera prawa wskazanym kontom.

## Testy poprawki 1.0.1 bez serwera bazy

Po `mvn test-compile` uruchom z folderu projektu:

Windows PowerShell:

```powershell
java -cp 'target/classes;target/test-classes' UpdateProbe
java -cp 'target/classes;target/test-classes' pl.hotel.baza.UpdateConnectionProbe
```

Linux/macOS:

```sh
java -cp 'target/classes:target/test-classes' UpdateProbe
java -cp 'target/classes:target/test-classes' pl.hotel.baza.UpdateConnectionProbe
```

Uruchamiaj bez zmiennych HOTEL_DB_*, które celowo nadpisują konfigurację testową. UpdateProbe sprawdza migrację, brak nadpisywania, UTF-8 i znaki specjalne hasła, jawny plik, pierwszeństwo zmiennych, pierwsze uruchomienie, błędy i log bez danych poufnych. UpdateConnectionProbe używa atrapy Driver JDBC i nie łączy się z rzeczywistą bazą; potwierdza przekazanie ustawień i zachowanie blokad niebezpiecznych opcji. Nie dodawaj sterownika MySQL do classpath tej próby, aby nie użyć rzeczywistego serwera.

Testy są programami main i nie uruchamiają się samoczynnie przez mvn test. Tworzą tylko pliki tymczasowe, nie zmieniają istniejącej bazy. Checklistę Windows i aktualizacji instalatorem zawiera AKTUALIZACJA_1.0.1.md.

## Poprawka 1.0.2

Po kompilacji testów wykonaj bez sterownika MySQL w classpath (Windows używa średnika zamiast dwukropka):

```bash
java -cp 'target/classes:target/test-classes' Patch102Probe
```

Patch102Probe nie wymaga serwera. Sprawdza poprzednią treść kopii, Unicode, przywracanie, odtwarzanie uszkodzonego pliku, limit pięciu kopii, odmowę obcego pliku i dowiązania, zachowanie konfiguracji po błędzie kopii, sprzątanie plików tymczasowych, prawa POSIX i diagnostykę bez danych bazy.

Z pełnym classpath zależności i osobną bazą testową, przygotowaną jak powyżej, uruchom `ConnectionTestProbe`. Sprawdza prawdziwy SELECT 1, brak zapisu pliku i rekordów, złe hasło, brak bazy, niedostępny serwer oraz zabezpieczenia JDBC. Nigdy nie używaj danych produkcyjnych.

`SettingsGuiLauncher` z tą samą konfiguracją testową sprawdza formularz, dobry i błędny test bez zapisu, zapis, przywrócenie, schowek diagnostyczny, folder logów lub ścieżkę awaryjną oraz otwarcie przywracania przy uszkodzonym pliku. Wymaga świeżej konfiguracji konta testowego root z pustym hasłem lub dostosowania fixture; to narzędzie testowe, nie instrukcja dla konta aplikacji.

Do prób bez pulpitu z Monocle ustaw `-Dglass.platform=Monocle -Dmonocle.platform=Headless -Dheadless.geometry=1920x1080-32 -Dprism.order=sw -Djava.awt.headless=true`. Domyślny mały ekran Monocle może powodować błędy bufora przy większym oknie. Dołącz testowe zależności z pom.xml. Przebieg headless nie potwierdza integracji z menedżerem plików ani instalacji na pulpicie Windows.

Testy biznesowe nadal uruchamia `RunAllProbes`. Wyniki konkretnego przebiegu są w WYNIKI_TESTOW_1.0.2.txt.

## Motywy i pulpit — 1.1.0

`AppearanceProbe` nie wymaga bazy. Sprawdza zapis i odczyt preferencji w oddzielnym katalogu tymczasowym, ponowne uruchomienie JVM, niepoprawny plik i skalę oraz brak zmiany konfiguracji JDBC. Sprawdza też liczniki pulpitu: rozłączne kategorie, puste dane, unikalne pokoje i zaległe wyjazdy.

`ThemeGuiLauncher` wymaga świeżej testowej bazy demonstracyjnej i osobnego katalogu `hotel.data`, początkowo bez pliku `wyglad.properties`. Sprawdza wszystkie strony w dwóch motywach, istniejący dialog po zmianie motywu, popupy, zapis skali, małe okno, kontrast tytułu, brak nakładania kart i zachowanie ról po wylogowaniu. Przygotowanie classpath pozostaje takie jak wyżej. Przykład na Linuxie:

```sh
java -Dhotel.data=/tmp/hotel-theme-probe -cp "target/classes:target/test-classes:$(cat target/test-classpath.txt)" AppearanceProbe
java -Dhotel.tests=true -Dhotel.config=db-test.properties -Dhotel.data=/tmp/hotel-theme-probe -cp "target/classes:target/test-classes:$(cat target/test-classpath.txt)" ThemeGuiLauncher
```

Do uruchomienia bez pulpitu dodaj opcje `-Dglass.platform=Monocle -Dmonocle.platform=Headless -Dheadless.geometry=1920x1200-32 -Dprism.order=sw -Djava.awt.headless=true`. Monocle jest zależnością testową; nie trafia do instalatora.

Pełny `GuiSmokeLauncher` sprawdzaj oddzielnie po odtworzeniu danych dla obu motywów. W katalogu testowego profilu przygotuj `wyglad.properties` z `motyw=JASNY` albo `motyw=CIEMNY` i `skala=100`. Samo `mvn test` nie uruchamia tych scenariuszy. Wyniki tego wydania: `WYNIKI_TESTOW_1.1.0.txt`.
