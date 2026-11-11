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
RoomChangeProbe, HistoryProbe, ReportProbe i PrivilegeProbe. Testy są programami z `main`, nie testami
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

`CALENDAR_OK`, `SECURITY_OK`, `PARTICIPANTS_OK`, `INTEGRATION_OK`, `ROOM_CHANGE_OK`, `HISTORY_OK`, `REPORTS_OK`,
`PRIVILEGES_OK`, `ALL_PROBES_OK` oraz oddzielnie `GUI_HISTORY_OK`, `GUI_ROOM_CHANGE_OK` i `GUI_SMOKE_OK`.

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
