# Testy programu

Scenariusze **zmieniają i dodają rekordy**. Używaj wyłącznie oddzielnej bazy z fikcyjnymi danymi.
Nie uruchamiaj testów na własnych danych hotelu.

## Przygotowanie

1. Przygotuj osobny schemat, np. `hotel_manager_test`, na swoim testowym MySQL.
2. Zrób roboczą kopię `db/HotelManager_baza.sql`. W tej kopii zmień nazwę
   `hotel_manager` na `hotel_manager_test` i wykonaj część do `COMMIT`, bez
   końcowej części tworzącej konta techniczne `hotel_app`.
3. Utwórz prywatny plik `db-test.properties` z URL do `hotel_manager_test`,
   kontem administracyjnym testowego serwera i hasłem. Nie publikuj go.
4. Ustaw `hotel.tests=true` i wskaż konfigurację. Programy odmawiają pracy,
   jeśli nazwa bazy nie kończy się `_test` lub `_test_<liczba>`.

Przykładowe uruchomienie logiki przez Maven, z głównego folderu projektu:

```sh
mvn test-compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass=RunAllProbes -Dexec.classpathScope=test -Dhotel.tests=true -Dhotel.config=db-test.properties
```

`RunAllProbes` wykonuje CalendarProbe, SecurityProbe, IntegrationProbe,
RoomChangeProbe, RoomStateProbe, HistoryProbe, NotesProbe, ConfirmationProbe, ReportProbe, ServicesSearchProbe, GuestArchiveProbe i PrivilegeProbe. Testy są programami z `main`, nie testami
uruchamianymi samoczynnie przez `mvn test`. Potrzebny jest JDK 17 lub nowszy.
Pierwsze pobranie pluginu i zależności wymaga internetu.

`SecurityProbe` uruchamia również osobny proces Javy i przekazuje mu classpath
testów, aplikacji oraz sterownika. Alternatywą dla pluginu jest zwykły classpath:

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

`PrivilegeProbe` tworzy i usuwa dwa tymczasowe konta MySQL z unikalną nazwą,
więc jego konto fixture potrzebuje uprawnień administracyjnych na testowym serwerze.
Testy nie mogą służyć do weryfikacji konta produkcyjnego.

## GUI

Na komputerze ze środowiskiem graficznym uruchom oddzielnie
`GuiSmokeLauncher` z tym samym classpath i parametrami testowymi.
Aby scenariusz miał wolny pokój 101, przed nim na **testowym schemacie** ustaw
rezerwacje tego pokoju na `ANULOWANA`. Są to dane demonstracyjne, nie rzeczywiste płatności.
GUI samo klika formularze, zapisuje fikcyjne dane, sprawdza zmianę pokoju, zachowanie ceny i uczestników, odświeżenie kalendarza oraz raport,
a na koniec dezaktywuje demonstracyjne konto recepcji. Odtwórz testowy schemat
przed następnym pełnym przebiegiem. W automatycznej weryfikacji paczki użyto
Monocle headless, a samego skryptu `.bat` nie uruchamiano na Windows.

Po zmianie hasła, roli lub użytkownika w testach należy zalogować właściwe
konto przed kolejną operacją. Każdy wątek ma swój kontekst uwierzytelnienia.
Testowa klasa `TestDatabase` korzysta z oddzielnego konta fixture i nie jest
częścią publicznego dostępu do bazy w aplikacji.

## Oczekiwane znaczniki

`CALENDAR_OK`, `SECURITY_OK`, `PARTICIPANTS_OK`, `INTEGRATION_OK`, `ROOM_CHANGE_OK`, `ROOM_STATE_OK`, `HISTORY_OK`, `CONFIRMATION_OK`, `NOTES_OK`, `REPORTS_OK`,
`PRIVILEGES_OK`, `ALL_PROBES_OK` oraz oddzielnie `GUI_ROOM_STATE_OK`, `GUI_NOTES_OK`, `GUI_CONFIRMATION_OK`, `GUI_HISTORY_OK`, `GUI_ROOM_CHANGE_OK` i `GUI_SMOKE_OK`.

Szczegółowy zakres i wynik weryfikacji dostarczonej paczki opisano w
`RAPORT_BEZPIECZENSTWA/README.md`.

## Historia

`HistoryProbe` sprawdza sześć rodzajów zdarzeń oraz tożsamość autora, różną od
pracownika tworzącego rezerwację. Trigger testowy wymusza błąd dopisania historii:
zmiana terminu, pokoju, anulowanie i wymeldowanie muszą zostać wycofane; sprzątanie
nie może pozostać zapisane. Sprawdzane jest również wycofanie utworzenia rezerwacji.
Ponad 100 zmian sprawdza limit i przechodzenie po stronach podczas nowego zapisu.

`PrivilegeProbe` wykonuje skrypt uprawnień historii na tymczasowych kontach
technicznych i sprawdza odmowę UPDATE/DELETE. GUI otwiera historię nowego pobytu,
sprawdza zdarzenia utworzenia i przeniesienia oraz obsługuje odświeżanie i zamknięcie.

Podczas weryfikacji paczki zaimportowano również schemat poprzedniej wersji,
wykonano migrację historii dwukrotnie i porównano wszystkie dotychczasowe rekordy.
Nowy dziennik pozostaje pusty dla zmian sprzed aktualizacji.

## Potwierdzenia rezerwacji

`ConfirmationProbe` sprawdza cenę całego pokoju, sumy wpłat i zwrotów, niezmienność
pobranego podglądu, aktualny pokój przy zachowaniu uzgodnionej ceny i anulowanie.
Próby równoczesnych zmian trzech tabel sprawdzają, czy odczyt dokumentu zachowuje
wspólny obraz danych. Testy eksportu obejmują znaki HTML, Unicode, apostrofy,
brak kontaktów w dokumencie, zastąpienie pliku, błąd zapisu i sprzątanie plików tymczasowych.

GUI otwiera podgląd, sprawdza pokój i dodatkowego gościa, odświeża po nowej wpłacie
i zamyka okno. Oddzielny scenariusz konsolowy wykonuje zapis HTML oraz odmawia
zastąpienia istniejącego pliku. Nie uruchamiano systemowego okna wyboru pliku,
fizycznej drukarki ani skryptu Windows na Windows. PDF tworzy przeglądarka.
Bieżący wynik całego przebiegu znajduje się w `WYNIKI_TESTOW_POTWIERDZENIA.txt`.

## Notatki recepcji

`NotesProbe` sprawdza istniejące uwagi, pustą treść, nowe linie, 500 znaków Unicode
z emoji, odrzucenie znaków sterujących i niepoprawnych surogatów. Dwie równoczesne
edycje z tą samą poprzednią treścią mogą mieć tylko jednego zwycięzcę. Test porównuje
również wielkość liter mimo niewrażliwej na nią kolacji SQL. Wymuszony błąd UPDATE
nie może pozostawić częściowych zmian.

Sprawdzany jest brak uwag w potwierdzeniu, raporcie HTML i wszystkich tabelach CSV,
odczyt archiwalnych rezerwacji i odmowa ich edycji. `PrivilegeProbe` wykonuje odczyt
oraz zapis notatki na ograniczonym koncie technicznym.

GUI zachowuje szkic po konflikcie, pokazuje aktualną treść bez kasowania szkicu,
sprawdza limit pola i odczyt anulowanej rezerwacji. Konsola ma scenariusze wielu
wierszy, anulowania edycji i usunięcia treści. Bieżące wyniki są w `WYNIKI_TESTOW_NOTATKI.txt`.

## Wyłączanie i przywracanie pokoi

`RoomStateProbe` sprawdza uprawnienia, stany wejściowe, brak pokoju, przyszłą i
zameldowaną rezerwację, zachowanie archiwalnych pobytów, sprzątanie oraz rollback.
Wyścig nowej rezerwacji z wyłączeniem ma tylko jednego zwycięzcę. Sprawdzane jest
również równoczesne przeniesienie pobytu do pokoju, który administrator wyłącza.

GUI wyłącza pokój 208, sprawdza jego blokadę w kalendarzu, przywraca go do sprzątania,
a po zalogowaniu recepcji sprawdza brak przycisków administratora i oznacza sprzątanie.
Konsola ma scenariusze wyłączenia, przywrócenia, odmowy potwierdzenia oraz odmowy roli recepcji.
`PrivilegeProbe` weryfikuje działanie na dotychczasowych minimalnych grantach.
Wyniki bieżącego przebiegu są w `WYNIKI_TESTOW_STANY_POKOI.txt`.


## Nowe usługi, historia i wyszukiwanie

ServicesSearchProbe sprawdza ceny i nazwy historyczne, aktywność i rolę cennika, odrzucenie starej oferty, ilości, należność, zwroty przed anulowaniem, wymeldowanie, HTML, raport, dosłowne znaki LIKE/SQL, granice trzech trybów dat, stronicowanie i starsze daty historii. Wymusza błąd historii, badając rollback zwrotu, usługi, notatki i uczestników. Dwa wyścigi sprawdzają wpłatę kontra anulowanie usługi oraz usługę kontra wymeldowanie.

ServicesGuiSteps jest częścią GuiSmoke: katalog, edycja ceny, dodanie/anulowanie, karta, nazwisko dodatkowe, saldo, daty, otwarcie wyniku i role. PrivilegeProbe nadaje również granty usług i sprawdza brak DELETE nowych tabel. Konsola ma scenariusze 29–31.

Migrację sprawdzono dwukrotnie na poprzednim schemacie: stare rekordy i historia pozostały, płatności uzyskały pojedyncze wpisy, zmieniony cennik nie został nadpisany. Znaczniki: SERVICES_SEARCH_OK, GUI_SERVICES_SEARCH_OK i CONSOLE_SERVICES_SEARCH_OK. Pełny wynik wydania: WYNIKI_TESTOW_USLUGI_WYSZUKIWANIE.txt. Testy GUI używają Monocle; nie oznaczają uruchomienia natywnego skryptu Windows ani okna wyboru pliku.


## Archiwizacja gości

GuestArchiveProbe wchodzi w RunAllProbes. Testuje blokadę potwierdzonych i zameldowanych pobytów, zachowanie danych historycznych/finansów, role, metadane autora, przywrócenie, stare obiekty i formularze, rollback oraz wyścig z nową rezerwacją. GuestArchiveGuiSteps jest częścią GuiSmoke i testuje archiwizację, filtry, brak wyboru w nowym pobycie, anulowanie formularza i przywrócenie recepcji. PrivilegeProbe sprawdza oba zapisy na minimalnym koncie.

Migracja jest testowana dwa razy na poprzednim schemacie. Porównywane są stare dane oraz zachowanie już istniejącego stanu archiwalnego. Naprawiony skrypt usług/historii sprawdzono także dla obu postaci DROP na MySQL 8, co nie jest pełnym testem uruchomionej MariaDB. Wyniki: WYNIKI_TESTOW_ARCHIWUM_GOSCI.txt. Znaczniki: GUEST_ARCHIVE_OK, GUI_GUEST_ARCHIVE_ADMIN_OK, GUI_GUEST_ARCHIVE_RECEPTION_OK, CONSOLE_GUEST_ARCHIVE_OK.
