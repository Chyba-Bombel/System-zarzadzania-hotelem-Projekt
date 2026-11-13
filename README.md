# System zarządzania hotelem — 1.0.1

Aplikacja JavaFX + JDBC + MySQL do obsługi gości, rezerwacji, pobytów, usług i rozliczeń. Projekt zachowuje również wersję konsolową. Kod jest podzielony na pakiety i mniejsze klasy, bez komentarzy w źródłach.

[Repozytorium projektu](https://github.com/Chyba-Bombel/System-zarzadzania-hotelem-Projekt)

## Wersja 1.0.1: instalator i konfiguracja

Instrukcja budowania, aktualizacji i migracji z 1.0.0: [AKTUALIZACJA_1.0.1.md](AKTUALIZACJA_1.0.1.md). Lista zmian: [CHANGELOG.md](CHANGELOG.md).

Konfiguracja Windows: `%LOCALAPPDATA%\HotelManagerData\config\db.properties`; logi: `%LOCALAPPDATA%\HotelManagerData\logs`. Są oddzielone od domyślnego folderu instalacji HotelManager. Przy niestandardowej instalacji nie wybieraj folderu HotelManagerData jako folderu programu.

Przed logowaniem kliknij **Ustawienia połączenia z bazą**. Pierwszy odczyt migruje znaleziony stary plik db.properties, a przy jego braku tworzy szablon z pustym hasłem. Istniejąca konfiguracja użytkownika nie jest nadpisywana; po migracji zmieniaj ją w ustawieniach, a nie w starym pliku projektu. `-Dhotel.config=...` nadal służy do jawnego wyboru pliku, zwłaszcza w testach. Samo 1.0.1 nie wymaga zmiany zgodnej bazy.

Zachowaj swoją ikonę hotel.ico obok pom.xml i uruchom na Windows:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\buduj_instalator.ps1
```

Wynik: `dist\1.0.1-exe`. To skrypt lokalny; obejrzyj go przed wykonaniem. Nie wykonuje testów ani migracji SQL. Gotowy instalator zawiera Javę, JavaFX i JDBC, ale nie serwer bazy. Kod i nowe testy sprawdzono na Linuxie; działanie instalatora oraz przejście między wersjami wymagają osobnej próby na Windows. Szczegóły: WYNIKI_TESTOW_1.0.1.txt.

## Baza danych — dwa główne pliki

| Sytuacja | Plik do wykonania |
| --- | --- |
| Masz już bazę z wcześniejszej wersji projektu | **db/aktualizuj_baze.sql** |
| Instalujesz projekt po raz pierwszy, bez jego bazy | **db/nowa_baza.sql** |

**Nie trzeba wybierać aktualizacji według etapów.** Jeden skrypt aktualizacji sprawdza istniejące elementy i obejmuje wszystkie dotychczasowe zmiany. Możesz wykonać go ponownie, również po częściowo zakończonej aktualizacji. Nie usuwa gości, rezerwacji ani płatności. Nie odtwarza nieznanej historii dawnych zmian.

Przed aktualizacją zamknij wszystkie stanowiska i zrób kopię bazy. Jako administrator otwórz cały plik w phpMyAdmin lub Workbench i wykonaj go. Dostosuj `USE hotel_manager`, jeśli Twój schemat ma inną nazwę. Zachowaj prywatny `db.properties`. Pełnego skryptu z rekordami demonstracyjnymi nie wykonuj na używanej bazie.

Aktualizacja dodaje prawa istniejącym standardowym kontom hotel_app dla localhost i 127.0.0.1. Nie tworzy kont i nie zmienia ich haseł. Własne konto techniczne wymaga jednorazowego dopasowania pomocnika w `db/administracja/uprawnienia.sql`. Folder administracja zawiera narzędzia dla administratora, nie kolejne etapy instalacji.

Dokładna instrukcja, warunki uruchomienia i opis praw: [db/README.md](db/README.md).

## Możliwości

| Ekran | Funkcje |
| --- | --- |
| Pulpit | Przyjazdy, dzisiejsze i zaległe wyjazdy, liczniki. |
| Goście | Dodawanie, edycja, wyszukiwanie, archiwizacja i przywracanie. |
| Pokoje | Dostępność, sprzątanie, dodawanie/edycja i administracyjne wyłączenie/przywrócenie. |
| Kalendarz | Miesięczny plan pokoi; podgląd pobytu i utworzenie rezerwacji od wolnej nocy. |
| Rezerwacje | Dodatkowi goście, zmiana dat i pokoju, historia, notatki, meldunek, wymeldowanie, anulowanie. |
| Wyszukiwanie zaawansowane | Nazwisko także dodatkowego gościa, pokój, status, daty i rozliczenie; strony po 100 wyników. |
| Usługi pobytu | Katalog administratora, dodawanie i anulowanie historycznych pozycji, rozliczenie razem z noclegami. |
| Płatności | Wpłaty i zaliczki; zwroty administratora, saldo i ewidencja transakcji. |
| Potwierdzenie | Podgląd danych, gości, usług i rozliczenia; HTML do wydruku/PDF przez przeglądarkę. |
| Raporty | Podsumowanie, pobyty, obłożenie, płatności i usługi; CSV/HTML. |
| Pracownicy i konto | Konta, dezaktywacja, role i zmiana własnego hasła. |

## Wymagania

- JDK 17 lub nowszy; projekt kompilowany do wersji 17.
- JavaFX 17.0.21, MySQL Connector/J 9.7.0.
- MySQL 8.0.16+; testy wydania wykonano na MySQL 8.0.46.
- Maven do uruchamiania na Linuxie/macOS lub z IntelliJ; skrypt Windows nie wymaga Maven.

Paczka zawiera biblioteki JavaFX dla Windows x64. Na innych systemach używaj Maven i właściwych bibliotek platformy. W skryptach uwzględniono różnice DROP CHECK/DROP CONSTRAINT między MySQL i MariaDB. Testy na MySQL nie stanowią pełnej weryfikacji działającego serwera MariaDB.

## Windows

1. Rozpakuj całą paczkę do nowego folderu, np. `C:\Projekty\Hotel_GUI`. Nie dokładaj nowych klas do starego src.
2. Zainstaluj JDK 17+ Windows x64 i sprawdź `java -version` oraz `javac -version`.
3. Uruchom serwer bazy, następnie wykonaj **jeden** plik zgodnie z tabelą na początku README.
4. Po uruchomieniu wybierz ustawienia połączenia i podaj adres, port i konto MySQL. Stary plik projektu jest importowany tylko przy braku konfiguracji użytkownika.
5. Uruchom `uruchom_windows.bat`.

Skrypt kompiluje źródła w podfolderach i kopiuje zasoby. Foldery src, resources, resources-version i lib oraz pom.xml muszą pozostać obok skryptu. Konfiguracja jest w folderze użytkownika. Nie dokładaj innych wersji JAR JavaFX: może to wywołać duplicate module.

## Linux

Na Ubuntu 24.04 LTS x86_64 z pulpitem:

```sh
sudo apt update
sudo apt install openjdk-17-jdk maven libgtk-3-0t64
```

Na Ubuntu 22.04 odpowiednik GTK to `libgtk-3-0`. W innych dystrybucjach zainstaluj właściwe pakiety JDK, Maven i GTK 3. Sprawdź narzędzia:

```sh
java -version
javac -version
mvn -version
```

Przejdź do folderu zawierającego pom.xml:

```sh
cd ~/Projekty/Hotel_GUI
```

Jeśli nie masz serwera i chcesz zainstalować lokalny MySQL:

```sh
sudo apt install mysql-server
sudo systemctl start mysql
```

Dla istniejącej bazy wykonaj tylko aktualizację:

```sh
mysql -u root -p < db/aktualizuj_baze.sql
```

Dla nowej instalacji, zamiast poprzedniego polecenia:

```sh
mysql -u root -p < db/nowa_baza.sql
```

Jeśli Twój lokalny administrator uwierzytelnia się przez sudo, zamiast `mysql -u root -p` użyj `sudo mysql`. Nie importuj pełnej bazy demonstracyjnej na swoje rekordy.

Uruchom GUI i ustaw połączenie przyciskiem przed logowaniem:

```sh
mvn javafx:run
```

Pierwsze pobranie zależności wymaga internetu. Uruchamiaj aplikację na zwykłym koncie użytkownika, w sesji graficznej. Nie używaj skryptu .bat i bibliotek javafx-win na Linuxie. Brak DISPLAY oznacza brak dostępu do pulpitu; samo SSH nie wystarcza. Na macOS również użyj Maven i bibliotek właściwych dla systemu.

## IntelliJ IDEA

Otwórz pom.xml jako projekt Maven. Ustaw Project SDK oraz Maven Runner JRE na zgodny JDK, poczekaj na zależności i uruchom Plugins → javafx → javafx:run. Konfiguracja znajduje się w folderze użytkownika; jawny hotel.config ma pierwszeństwo. `Launcher` uruchamia GUI, `Main` konsolę.

## Połączenie i konta

Przykładowa lokalna konfiguracja:

```properties
url=jdbc:mysql://localhost:3306/hotel_manager?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=10000
user=hotel_app
password=HotelDemo2026!
```

To konto techniczne MySQL, a nie pracownik z okna logowania. Dostosuj wartości i nie publikuj prywatnego db.properties. Zmienne HOTEL_DB_URL, HOTEL_DB_USER i HOTEL_DB_PASSWORD zastępują wartości pliku. Dane spoza loopback wymagają sslMode=VERIFY_IDENTITY oraz zaufanego certyfikatu, bez allowPublicKeyRetrieval=true.

| Rola | Login demo | Hasło demo |
| --- | --- | --- |
| Administrator | admin | Admin123! |
| Recepcja | recepcja | Recepcja123! |

Jeżeli hasła zmieniono wcześniej, użyj aktualnych. Nowa instalacja nie resetuje hasła istniejącego konta MySQL. Konta demonstracyjne służą nauce lokalnej. Hasła pracowników są skrótami PBKDF2 z solą i 600 000 iteracji. Po 5 błędnych logowaniach konto jest blokowane w bazie na 5 minut. Aktywność, rola i zmiana hasła są kontrolowane przy kolejnych operacjach.

Recepcja obsługuje gości, archiwum, pobyty, usługi, wpłaty i raporty. Administrator dodatkowo zarządza pokojami/kontami/cennikiem i przyjmuje zwroty. Konto aplikacji nie powinno być administratorem MySQL. Opcjonalne ograniczanie szerszych praw opisuje instrukcja w db.

## Zasady obsługi

1. Dodaj głównego gościa i wybierz pokój, daty oraz liczbę osób.
2. Dla dodatkowych osób wpisz imię i nazwisko. Nie potrzebują osobnych kartotek.
3. Zapis sprawdza dostępność, pojemność, stan pokoju i aktywność kartoteki.
4. Dodaj usługi i ewentualną zaliczkę; zameldowanie wymaga gotowego pokoju.
5. Rozlicz całą należność przed wymeldowaniem. Pokój przechodzi do sprzątania.

Pobyt trwa 1–30 nocy i nie zaczyna się w przeszłości. Dzień wyjazdu nie jest noclegiem. Cena pokoju nie jest mnożona przez liczbę osób. Zmiana pokoju zachowuje uzgodnioną stawkę i dotyczy całego pobytu, bez podziału noclegów między pokoje. Przy przeniesieniu zameldowanego pobytu poprzedni pokój wymaga sprzątania, a nowy musi być gotowy.

Usługi zachowują cenę i nazwę z chwili dodania. Ilość jest dla całego pobytu: 2 osoby × 3 śniadania = 6 porcji. Zmiana dat nie zmienia ilości, dostawka nie zwiększa pojemności. Obniżenie opłaconego rachunku może wymagać wcześniejszego zwrotu. Saldo = noclegi + aktywne usługi − wpłaty + zwroty. Anulowanie pobytu wymaga zwrócenia jego środków.

Archiwum ukrywa kartotekę z aktywnej listy i nowych rezerwacji, zachowując historię i płatności. Potwierdzony lub zameldowany pobyt blokuje archiwizację. Przywrócenie umożliwia ponowną edycję i rezerwowanie. Archiwizacja nie jest anonimizacją.

Historia ma 12 typów zdarzeń, w tym wpłaty, zwroty, usługi, notatki i osoby. Autor jest pobierany z sesji. Błąd historii wycofuje całą operację biznesową. Dawne płatności odtwarza aktualizacja z istniejącej ewidencji; innych nieznanych zmian program nie wymyśla.

Notatki są wewnętrzne, do 500 znaków; konflikt zapisu zachowuje szkic. Nie trafiają do dokumentu dla gościa. Potwierdzenie HTML nie jest fakturą ani bankowym dowodem płatności. PDF tworzy przeglądarka przez Ctrl+P.

Raport obejmuje oba dni graniczne. Obłożenie bazuje na aktualnej kartotece pokoi, także wyłączonych; nie ma historii ich liczby/stanu. Wartość noclegów jest wyceną nocy, płatności przepływami według daty transakcji. Usługi wybierane są według daty dodania i bieżącego statusu, bez podziału na noce i odtwarzania stanu na dawny dzień. CSV/HTML obejmują pięć tabel.

## Konsola i dokumentacja

Konsola ma opcje 25 — historia, 26 — potwierdzenie, 27 — notatka, 28 — stan pokoju, 29 — usługi, 30 — wyszukiwanie, 31 — cennik, 32 — archiwizacja, 33 — przywrócenie, 34 — archiwum.

| Dokument | Zawartość |
| --- | --- |
| db/README.md | Jeden wybór: nowa baza albo aktualizacja. |
| MAPA_KODU.md | Pakiety, klasy i miejsca wprowadzania zmian. |
| ARCHIWUM_GOSCI.md | Archiwizacja i przywracanie. |
| USLUGI_I_WYSZUKIWANIE.md | Usługi, saldo i filtry. |
| HISTORIA_REZERWACJI.md | Zdarzenia, autorzy, relacje i transakcje. |
| ZMIANA_POKOJU.md | Reguły przenoszenia. |
| POTWIERDZENIE_REZERWACJI.md | Podgląd i wydruk. |
| NOTATKI_REZERWACJI.md | Szkice i konflikty edycji. |
| STANY_POKOI.md | Wyłączanie i przywracanie pokoju. |
| TESTOWANIE.md | Testy na osobnym schemacie. |

## Budowa i weryfikacja

Main i Launcher są krótkimi wejściami. `pl.hotel.model` zawiera modele, `baza` fasadę i pakietowe DAO, `gui` ekrany i zadania JavaFX, `konsola` menu i obsługę terminala, `raporty` i `potwierdzenia` eksport. BazaDanych sprawdza sesję i role; surowe połączenie nie jest publicznym API. JDBC wykonuje się poza wątkiem JavaFX. Tabele używają parametrów i oddzielnych agregatów płatności/usług.

Wydanie 1.0.1 zmienia konfigurację, diagnostykę i pakowanie, bez zmian schematu bazy. Nowe wyniki: WYNIKI_TESTOW_1.0.1.txt. Starsze WYNIKI_TESTOW opisują wcześniejsze przebiegi i nie są potwierdzeniem ponownego wykonania całej integracji ani GUI dla 1.0.1. Testy zmieniają dane i wymagają izolowanej bazy; `mvn test` sam nie uruchamia programów Probe.

## Najczęstsze problemy

| Objaw | Co sprawdzić |
| --- | --- |
| Brak javac lub błędny release | JDK i wersja Javy używana przez Maven. |
| Duplicate module | Nie mieszaj wersji JavaFX i bibliotek Windows z zależnościami innego systemu. |
| Access denied | Dane konta MySQL i jego host w db.properties. |
| Brak tabeli lub kolumny | Wykonaj cały db/aktualizuj_baze.sql na odpowiedniej bazie. |
| Communications link failure | Serwer, adres, port i timeouty. |
| Błąd konfiguracji | Ustawienia połączenia, plik w folderze użytkownika albo jawnie ustawione hotel.config. |
| Unable to open DISPLAY | Uruchom w sesji graficznej. |
| Błąd zapisu po utracie sieci | Najpierw odśwież dane; serwer mógł już zatwierdzić operację. |

Rozpakowuj źródła nowego wydania do osobnego folderu. Konfiguracja użytkownika pozostaje wspólna; przejście ze starego instalatora opisuje AKTUALIZACJA_1.0.1.md. Nie kasuj bazy w celu wymiany plików programu.
