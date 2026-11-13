# Raport testów i przeglądu bezpieczeństwa

Data weryfikacji: **11 listopada 2026 r.**

Przegląd dotyczy dostarczonych źródeł aplikacji JavaFX/konsolowej i jej zależności.
Testy wykonywano na odizolowanej bazie z fikcyjnymi rekordami, nie na komputerze
użytkownika ani na jego rzeczywistej bazie hotelowej.

## Wynik

Poprawiono wykryte błędy kontroli dostępu i walidacji, dodano trwałą blokadę
logowania, ograniczono ekspozycję błędów i niebezpieczne ustawienia JDBC.
Zaktualizowano sterownik MySQL oraz JavaFX. Komentarze usunięto z plików kodu.
Scenariusz SecurityProbe zakończył się wynikiem **65 poprawnych asercji**. Dokładne znaczniki pomyślnych przebiegów są w `wyniki_testow.txt`.

**To przegląd kodu i scenariuszowe testy bezpieczeństwa, nie certyfikat ani gwarancja
odporności na wszystkie ataki.** Nie wykonywano internetowego skanowania cudzych
systemów, testów odporności systemu operacyjnego ani audytu serwera użytkownika.

## Znalezione problemy i zastosowane poprawki

| Obszar | Ustalenie przed zmianą | Zastosowana poprawka |
| --- | --- | --- |
| Zależności — wysoki wpływ | Dołączony Connector/J 9.2.0 należy do zakresu wersji objętych CVE-2025-30706. Nie przeprowadzano demonstracji przejęcia procesu przez tę podatność. | Sterownik 9.7.0, nowy JAR i konfiguracja Maven/skryptu. JavaFX z 17.0.18 do 17.0.21. |
| Autoryzacja — wysoki wpływ logiczny | Część metod bazy nie sprawdzała ról; ukrycie przycisku w GUI nie chroniło przed bezpośrednim wywołaniem metody. | Każda publiczna operacja danych wymaga uwierzytelnionego kontekstu; operacje administracyjne dodatkowo roli ADMIN. |
| Tożsamość wykonawcy | ID podane w argumentach lub sam obiekt `Pracownik` nie dowodziły zalogowania tej osoby. | Kontekst wątku tworzony dopiero po weryfikacji hasła; ID wykonującego porównywane z tym kontekstem. |
| Surowe połączenie | Publiczne `BazaDanych.polacz()` pozwalało omijać fasadę przez własne zapytania. | Usunięto je z publicznego API. Połączenia i DAO są wewnątrz pakietu bazy; raport korzysta z `RaportDAO`. |
| Zgadywanie haseł | Limit trzech prób był wyłącznie w uruchomionej aplikacji i znikał po restarcie. | Baza przechowuje licznik i czas blokady. Pięć błędnych prób blokuje nowe logowania na pięć minut, także z innego procesu. |
| Nieaktualne sesje | Zmiana hasła nie wymagała ponownego zalogowania już otwartej sesji; konsola opierała rolę na lokalnym obiekcie. | Przy chronionej operacji sprawdzane są aktualna aktywność, rola i hash hasła. Zmiana kończy możliwość dalszego użycia starego kontekstu. |
| Walidacja | W części DAO walidacja była niepełna albo zależała od formularza. NULL mógł wywołać wyjątek wykonania; puste lub zbyt długie dane mogły trafiać do SQL. | Walidacja wymaganych danych również w DAO, limity haseł/hashów i kosztu KDF oraz limity tekstu w kontrolkach. |
| Błędy | Surowe błędy sterownika mogły przekazywać szczegóły zapytań i konfiguracji. | Mapowanie błędów na komunikaty użytkowe; tryb `paranoid` sterownika. Nie pokazuje się SQL ani danych konfiguracyjnych w takim błędzie. |
| CSV | Wymagana była dodatkowa ostrożność dla formuł ukrytych za znakami formatującymi, np. BOM. | Wykrywanie niebezpiecznego początku także po takich znakach; apostrof dla tekstu-formuły, poprawne cytowanie i zachowanie kwot ujemnych. |

Klasyfikacja wpływu dotyczy logiki tego programu. Program nie udostępnia
publicznego serwera HTTP; nie oznacza to, że każda wymieniona luka była
zdalnie wykorzystywalna przez niezalogowanego użytkownika internetu.

## Dodatkowe zabezpieczenia

- Jawnie wyłączono deserializację obiektów sterownika, lokalny import plików,
  URL w lokalnym imporcie i wielokrotne zapytania w jednym wywołaniu.
  Te funkcje nie były domyślnie włączone w starej konfiguracji — teraz program
  odrzuca również ich niebezpieczne włączenie w URL.
- Duplikaty parametrów URL są odrzucane. Timeout połączenia/odczytu musi
  wynosić od 1 do 30000 ms; ustawione są również bezpieczne wartości domyślne.
- Zdalny serwer wymaga `VERIFY_IDENTITY` oraz poprawnego, zaufanego certyfikatu;
  nie można użyć tam dynamicznego pobierania klucza RSA zamiast TLS.
  W tym przeglądzie sprawdzano odrzucanie konfiguracji bez weryfikacji TLS,
  nie pełne wdrożenie certyfikatów na serwerze użytkownika.
- Nowy profil kont technicznych nie otrzymuje DDL ani UPDATE/DELETE dla płatności.
  Wycofanie płatności jest osobnym wpisem zwrotu, a nie usunięciem historii.
- Hasła są solone, przetwarzane przez PBKDF2-HMAC-SHA256 z 600000 iteracji;
  wynik porównywany przez `MessageDigest.isEqual`. Dane KDF mają limity rozmiaru
  i kosztu. Stare prawidłowe demonstracyjne hashe pozostają zgodne.
- Testy wymagają jawnego `hotel.tests=true` i nazwy osobnej bazy testowej.
  Fixture z połączeniem administracyjnym nie jest częścią aplikacji produkcyjnej.

## Wykonane testy

Środowisko: JDK 17, kod kompilowany z `--release 17`, MySQL 8.0.46 z InnoDB,
JavaFX 17.0.21, Connector/J 9.7.0. GUI sprawdzano automatycznie przez Monocle
headless. Kompilację sprawdzono także z dołączonymi bibliotekami Windows;
samego `.bat` nie uruchamiano na Windows.

| Scenariusz | Sprawdzane przypadki |
| --- | --- |
| `SecurityProbe` | Brak dostępu bez logowania; niedozwolone role; fałszywy obiekt administratora i obce ID wykonawcy; SQL injection jako dane; NULL i limity; blokada logowania między procesami i jej wygaśnięcie; zmiana roli, aktywności i hasła w otwartej sesji; równoczesne wpłaty i zwroty; niebezpieczne ustawienia JDBC i ukrycie szczegółów SQL. |
| `HistoryProbe` | 34 asercje: autorzy, stany przed i po, odmowy, brak pozornych zmian, rollback przy błędzie historii oraz stronicowanie. |
| `RoomChangeProbe` | 26 asercji zmiany pokoju: dostępność, cena i płatności, goście, statusy, sprzątanie, rollback po wymuszonym błędzie oraz równoczesne zapisy. |
| `IntegrationProbe` | Dodawanie/edycja, rezerwacje, terminy, płatności, rozliczenie, zameldowanie/wymeldowanie, anulowanie, rollback, dodatkowi goście oraz dwie próby rezerwacji tego samego pokoju. |
| `ReportProbe` | Granice dat, poprawna wycena, brak mnożenia kwot przez liczbę gości, CSV: separator, cudzysłów, nowa linia, formuły/BOM/tabulator; HTML: kodowanie danych jako tekst. |
| `PrivilegeProbe` | Odmowa DDL i UPDATE/DELETE płatności na ograniczonym koncie MySQL; poprawne logowanie i obsługa gościa, pokoju, rezerwacji, wpłaty, zwrotu i raportu. |
| `CalendarProbe` | Przyjazd/wyjazd, miesiące i rok przestępny, anulowane/zakończone pobyty, wyłączenia i konflikty. |
| Konsola | Logowanie i listy gości, pokoi oraz rezerwacji dla ADMIN i RECEPCJA, prawidłowe zakończenie programu; zmiana pokoju przez opcję 24. |
| `GuiSmoke` | Sceny, role, walidacja i zapis gościa, rezerwacja, uczestnicy, zmiana pokoju i aktualizacja kalendarza, podgląd historii, rozliczenie, raporty, kalendarz, rezerwacja z wolnej nocy i zakończenie sesji dezaktywowanego konta. |
| Migracja | Ponowne wykonanie aktualizacji bezpieczeństwa bez błędu i bez usuwania istniejących rekordów. |

Testy są przykładami regresji, a nie dowodem pokrycia każdej możliwej ścieżki
czy pełnym fuzzingiem, testem obciążeniowym lub analizą całego łańcucha dostaw.

## Zależności i źródła

Wyniki zapytań do OSV znajdują się w `zaleznosci.json`, a SHA-256 dołączonych JAR
w `biblioteki_sha256.json`. Lista obejmuje również zależności testowe z classpath
Maven; samo wyszukiwanie po współrzędnych Maven ma ograniczone pokrycie.

OSV zwróciło pusty wynik dla starego 9.2.0, mimo że komunikat Oracle wskazuje
CVE-2025-30706. Dlatego weryfikacja producenta była konieczna. Brak zgłoszenia
w tej bazie **nie oznacza braku podatności**.

Wersję JDBC 9.7.0 wybrano dla zgodności z MySQL 8.0. Nowsza linia 26.10.0
wymaga MySQL 8.4 lub nowszego. To wybór zgodności z dotychczasowym projektem,
nie deklaracja, że 9.7.0 jest najnowszą wersją sterownika. Aktualizowanie serwera
oraz JDK/systemu operacyjnego pozostaje osobnym obowiązkiem administratora.
Testowy MySQL 8.0.46 nie jest deklaracją bezpiecznej konfiguracji produkcyjnej.

Źródła pierwotne sprawdzone podczas przeglądu:

- [Oracle: CVE-2025-30706, wersje Connector/J 9.0.0–9.2.0](https://www.oracle.com/security-alerts/cpuapr2025verbose.html).
- [Oracle/MySQL: Connector/J 9.7.0 i zgodność z MySQL 8.0](https://dev.mysql.com/doc/relnotes/connector-j/en/news-9-7-0.html).
- [Oracle/MySQL: wymagania Connector/J 26.10.0](https://dev.mysql.com/doc/relnotes/connector-j/en/news-26-10-0.html).
- [Gluon: aktualizacje JavaFX 17](https://gluonhq.com/products/javafx/openjfx-17-release-notes/).
- [OWASP: przechowywanie haseł](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html).
- [OWASP: logowanie i jego ograniczanie](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html).
- [MySQL: właściwości bezpieczeństwa JDBC](https://dev.mysql.com/doc/connectors/en/connector-j-connp-props-security.html).

## Istotne ograniczenia — czego poprawki nie rozwiązują

**Najważniejsza granica: klient desktopowy i jego konto MySQL muszą być zaufane.**
Program nadal łączy się bezpośrednio z bazą przez wspólne konto techniczne.
Osoba mająca to hasło oraz dostęp do serwera może użyć własnego klienta SQL,
zmienić dane kont i obejść role aplikacji. Ukrycie połączenia w pakiecie Java,
ThreadLocal ani zaciemnienie JAR nie stanowią ochrony przed osobą kontrolującą
proces, pliki i środowisko uruchomieniowe. Pełne odseparowanie niezaufanej recepcji
od uprawnień administracyjnych wymaga serwera/API, który przechowuje dane JDBC
po stronie zaufanej i autoryzuje każde żądanie.

Ponadto:

- Zmień publicznie znane hasła demonstracyjne pracowników i konta MySQL.
  Nie używaj `root` jako konta technicznego aplikacji.
- Chroń konfigurację uprawnieniami systemu plików; zmienne środowiskowe pomagają
  nie publikować haseł w repozytorium, ale nie są sejfem przeciw administratorowi hosta.
- Blokada logowania jest czasowa. Ktoś znający login może wywołać krótką blokadę
  przez błędne próby. Nie dodano MFA ani mechanizmu odzyskiwania kont przez e-mail.
- Nie dodano automatycznej blokady ekranu po bezczynności. Odejdź od stanowiska
  dopiero po wylogowaniu lub zablokowaniu systemu.
- Kontrola sesji odbywa się przed operacjami. Zmiana uprawnień nie cofa już
  zatwierdzonych zmian ani nie usuwa informacji wcześniej odczytanych do pamięci.
- Działanie po utracie odpowiedzi sieciowej może być niepewne: przed powtórzeniem
  wpłaty odśwież dane. Nie dodano kluczy idempotencji płatności.
- Kopie zapasowe, szyfrowanie dysku, aktualizacje i dostępność serwera wymagają
  zabezpieczeń poza tym programem. Aplikacja nie przyjmuje prawdziwych danych kart
  ani nie łączy się z bankiem.
- Ataki właściwe serwerom WWW, takie jak CSRF przez HTTP, nie mają tu takiej samej
  powierzchni ataku. Sprawdzono kodowanie eksportu HTML; nie deklaruje się audytu strony WWW.

## Wdrożenie aktualizacji

1. Zrób kopię swojej bazy.
2. Wykonaj jako administrator `db/aktualizuj_baze.sql` na istniejącym
   schemacie. Nie importuj ponownie pełnej bazy z rekordami demo.
3. Rozpakuj aplikację do nowego folderu i przenieś własny `db.properties`.
4. Użyj nowego skryptu Windows lub Maven; nie kopiuj starych bibliotek JAR.
5. Dostosuj TLS dla serwera poza loopback i zmień hasła demonstracyjne.
6. Opcjonalnie zastosuj profil `db/administracja/ogranicz_uprawnienia.sql` wyłącznie do kont
   technicznych i schematu wymienionych w tym skrypcie. Własne granty wymagają
   sprawdzenia przez administratora.

Szczegóły powtórzenia testów: `../TESTOWANIE.md`.

## Rozszerzenie: dziennik rezerwacji

Dopisanie zdarzenia jest częścią transakcji operacji biznesowej. Potwierdzono
wycofanie utworzenia, terminu, pokoju, anulowania i wymeldowania po wymuszonym
błędzie historii; przy wymeldowaniu wycofuje się również stan sprzątania.
Tożsamość pochodzi z uwierzytelnionego kontekstu, nie z formularza.

Nowa tabela wymaga `db/aktualizuj_baze.sql` i odpowiednich uprawnień
SELECT/INSERT; standardowe konta obsługuje `db/administracja/uprawnienia.sql`.
Test migracji porównał wszystkie stare rekordy po dwukrotnym wykonaniu skryptu.
Test ograniczonego konta potwierdził odczyt/dopisanie oraz odmowę UPDATE/DELETE.

GUI nie udostępnia edycji historii, ale wspólne konto MySQL, stare szerokie granty
lub dostęp administracyjny nadal pozwalają ingerować w bazę. Dziennik nie ma
zabezpieczenia kryptograficznego i nie rejestruje operacji wykonanych poza nowym
programem. Historia zdarzeń nie zastępuje ewidencji odcinków pobytu w kilku pokojach.
