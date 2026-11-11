# System zarządzania hotelem

**Aplikacja okienkowa w JavaFX z bazą MySQL** do obsługi pracy recepcji: od dodania gościa i utworzenia rezerwacji po płatności, wymeldowanie i raport za wybrany okres.

Projekt rozwija wcześniejszą wersję konsolową. Wspólne klasy modelu i operacje JDBC obsługują dane hotelu, a JavaFX udostępnia formularze, tabele i menu pracownika.

[Repozytorium projektu](https://github.com/Chyba-Bombel/System-zarzadzania-hotelem-Projekt)

## Spis treści

- [Możliwości aplikacji](#możliwości-aplikacji)
- [Technologie i wymagania](#technologie-i-wymagania)
- [Uruchomienie na Windows](#uruchomienie-na-windows)
- [Uruchomienie na Linuxie](#uruchomienie-na-linuxie)
- [Baza danych: nowa instalacja lub aktualizacja](#baza-danych-nowa-instalacja-lub-aktualizacja)
- [Uruchomienie przez IntelliJ IDEA i Maven](#uruchomienie-przez-intellij-idea-i-maven)
- [Konta i uprawnienia](#konta-i-uprawnienia)
- [Przykładowa obsługa pobytu](#przykładowa-obsługa-pobytu)
- [Kalendarz rezerwacji](#kalendarz-rezerwacji)
- [Zmiana pokoju](#zmiana-pokoju)
- [Raporty i eksport](#raporty-i-eksport)
- [Budowa projektu](#budowa-projektu)
- [Relacje w bazie danych](#relacje-w-bazie-danych)
- [Zasady działania](#zasady-działania)
- [Weryfikacja](#weryfikacja)
- [Rozwiązywanie problemów](#rozwiązywanie-problemów)

## Możliwości aplikacji

| Ekran | Dostępne funkcje |
| --- | --- |
| **Pulpit** | Liczniki, przyjazdy na dziś oraz dzisiejsze i zaległe wyjazdy. |
| **Goście** | Dodawanie, edycja i wyszukiwanie danych gości. |
| **Pokoje** | Lista pokoi, sprawdzanie dostępności, oznaczanie sprzątania; dla administratora dodawanie i edycja pokoju. |
| **Kalendarz** | Miesięczny plan pokoi, szczegóły pobytu i rozpoczęcie rezerwacji od klikniętej wolnej nocy. |
| **Rezerwacje** | Tworzenie rezerwacji, dodatkowi goście, zmiana terminu i pokoju, zameldowanie, wymeldowanie i anulowanie. |
| **Płatności** | Rozliczenie pobytu, wpłaty i zaliczki; dla administratora rejestrowanie zwrotów. |
| **Raporty** | Zestawienia za wybrany okres, eksport CSV i HTML oraz wydruk przez przeglądarkę. |
| **Pracownicy** | Tworzenie i dezaktywacja kont przez administratora. |
| **Moje konto** | Zmiana własnego hasła. |

Tabele umożliwiają sortowanie. Na ekranach obsługi gości, pokoi i rezerwacji dostępne są wyszukiwanie oraz filtry. Dane z bazy można pobrać ponownie przyciskiem **„Odśwież dane”**.

## Technologie i wymagania

| Element | Wersja / zastosowanie |
| --- | --- |
| Java | JDK 17 lub nowszy; kompilacja do wersji 17. |
| JavaFX | 17.0.21 — interfejs okienkowy i CSS. |
| MySQL | 8.0.16 lub nowszy — baza danych i transakcje InnoDB. |
| JDBC | MySQL Connector/J 9.7.0 — komunikacja aplikacji z bazą. |
| Maven | Opcjonalny przy uruchamianiu skryptem Windows; potrzebny do uruchomienia przez `mvn javafx:run`. |

Paczka zawiera biblioteki **JavaFX dla Windows x64** i sterownik JDBC. Na Linux/macOS użyj Maven, który dobiera biblioteki dla systemu. Potrzebne jest środowisko graficzne oraz systemowe biblioteki wymagane przez JavaFX.

## Uruchomienie na Windows

1. Rozpakuj **całą paczkę** do wybranego folderu, np. `C:\Projekty\Hotel_GUI`.
2. Zainstaluj JDK 17 lub nowszy dla Windows x64. W terminalu sprawdź:

   ```bat
   java -version
   javac -version
   ```

3. Uruchom serwer MySQL i przygotuj bazę zgodnie z [instrukcją poniżej](#baza-danych-nowa-instalacja-lub-aktualizacja).
4. Otwórz `db.properties` i wpisz dane połączenia ze swoją bazą.
5. Uruchom **`uruchom_windows.bat`**.

Skrypt kompiluje pliki z `src` oraz jego podfolderów, kopiuje arkusz stylów i uruchamia klasę `Launcher`. Maven nie jest potrzebny w tym wariancie. Foldery `src`, `resources`, `lib` oraz plik `db.properties` muszą pozostać obok skryptu.

### Konfiguracja połączenia

Przykładowa konfiguracja dla lokalnej bazy demonstracyjnej:

```properties
url=jdbc:mysql://localhost:3306/hotel_manager?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=10000
user=hotel_app
password=HotelDemo2026!
```

Dostosuj adres serwera, port, nazwę bazy, użytkownika i hasło do własnej instalacji. To **konto techniczne MySQL**, a nie konto pracownika wpisywane w oknie logowania.

Konfiguracja demonstracyjna służy do nauki lokalnej. Przy udostępnianiu projektu nie publikuj własnych haseł do bazy.

## Uruchomienie na Linuxie

Na Linuxie uruchamiaj projekt przez **Maven**. Nie uruchamiaj `uruchom_windows.bat` i nie używaj bibliotek z `lib/javafx-win` — są przeznaczone dla Windows. Maven pobierze zależności JavaFX odpowiednie dla platformy.

Poniższe polecenia instalacji dotyczą **Ubuntu 24.04 LTS, x86_64, z pulpitem graficznym**. W innych dystrybucjach zainstaluj odpowiedniki: JDK 17, Maven i biblioteki GTK 3. Sama aplikacja korzysta z tego samego `pom.xml` i tych samych źródeł.

### 1. Zainstaluj JDK, Maven i GTK

W terminalu wykonaj:

```bash
sudo apt update
sudo apt install openjdk-17-jdk maven libgtk-3-0t64
```

Sprawdź instalację:

```bash
java -version
javac -version
mvn -version
```

Java i kompilator powinny mieć wersję co najmniej 17. Sprawdź także wersję Javy wskazaną przez Maven. W Ubuntu 22.04 odpowiednikiem pakietu GTK jest `libgtk-3-0`; nie kopiuj nazwy pakietu z innej wersji systemu bez sprawdzenia jej dostępności.

### 2. Rozpakuj projekt i przejdź do jego folderu

Przykładowo, jeśli rozpakowałeś projekt do katalogu `Projekty`:

```bash
cd ~/Projekty/Hotel_GUI
ls pom.xml db.properties
```

Oba pliki powinny być widoczne. Jeśli polecenie zgłasza ich brak, przejdź do właściwego folderu. Linux rozróżnia wielkie i małe litery w nazwach plików.

### 3. Przygotuj MySQL

**Jeśli korzystasz z działającego serwera i istniejącej bazy**, nie instaluj drugiego serwera i nie importuj pełnych danych demonstracyjnych. Przejdź do konfiguracji połączenia. Starsza baza może wymagać tylko pliku `db/aktualizacja_uczestnicy.sql`, zgodnie z rozdziałem o aktualizacji.

**Dla nowej, lokalnej instalacji na Ubuntu**:

```bash
sudo apt install mysql-server
sudo systemctl start mysql
systemctl status mysql
```

Wykonaj pełny skrypt bazy **jednorazowo**, z głównego katalogu projektu:

```bash
sudo mysql < db/HotelManager_baza.sql
```

Polecenie zakłada lokalny dostęp administratora MySQL przez `sudo`. Jeśli serwer wymaga hasła administratora, zamiast niego użyj:

```bash
mysql -u root -p < db/HotelManager_baza.sql
```

Dla wcześniejszej bazy bez tabeli dodatkowych gości wykonaj **wyłącznie migrację**:

```bash
sudo mysql < db/aktualizacja_uczestnicy.sql
```

Na serwerze z logowaniem hasłem użyj analogicznie `mysql -u root -p` zamiast `sudo mysql`. Pełny import i migracja to dwa różne warianty — nowa instalacja zawiera już tabelę uczestników. Nie wykonuj pełnego importu na swojej dotychczasowej bazie.

### 4. Ustaw połączenie

Otwórz plik w dowolnym edytorze, np.:

```bash
nano db.properties
```

Dla lokalnej bazy demonstracyjnej ustaw:

```properties
url=jdbc:mysql://localhost:3306/hotel_manager?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=10000
user=hotel_app
password=HotelDemo2026!
```

W `nano` zapisz zmiany skrótem **Ctrl+O**, potwierdź Enterem i zamknij edytor przez **Ctrl+X**. Jeśli używasz własnego konta, portu lub hasła, zachowaj swoje wartości.

Możesz sprawdzić lokalne połączenie niezależnie od aplikacji:

```bash
mysql -h 127.0.0.1 -P 3306 -u hotel_app -p hotel_manager
```

Po zalogowaniu wpisz `SHOW TABLES;`, a następnie `EXIT;`. Hasło podajesz dopiero po wyświetleniu pytania przez klienta MySQL.

### 5. Uruchom okno aplikacji

Z folderu zawierającego `pom.xml` i `db.properties` wykonaj:

```bash
mvn clean javafx:run
```

Pierwsze uruchomienie wymaga internetu do pobrania zależności. Maven kompiluje projekt, kopiuje zasoby i uruchamia GUI. Kolejne uruchomienia mogą korzystać z:

```bash
mvn javafx:run
```

W oknie aplikacji użyj konta pracownika, np. demonstracyjnego `admin` / `Admin123!`. Aplikację uruchamiaj na zwykłym koncie użytkownika; `sudo` jest potrzebne przy instalacji pakietów i ewentualnie administracji MySQL.

### Problemy typowe dla Linuxa

| Komunikat / objaw | Rozwiązanie |
| --- | --- |
| `mvn: command not found` | Zainstaluj Maven i otwórz ponownie terminal. |
| `release version 17 not supported` | Sprawdź `mvn -version`. Maven korzysta ze zbyt starego JDK. |
| Maven używa innej Javy niż `java -version` | Sprawdź `JAVA_HOME`. Dla JDK 17 z pakietu Ubuntu x86_64 możesz w bieżącym terminalu ustawić `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`, a następnie sprawdzić `mvn -version`. |
| Brak GTK / `Unable to load glass GTK library` | Zainstaluj biblioteki GTK 3 właściwe dla swojej dystrybucji; dla Ubuntu 24.04 polecenie znajduje się w kroku 1. |
| `Unable to open DISPLAY` | Uruchom program z terminala otwartego w sesji graficznej. Zwykła sesja SSH lub serwer bez pulpitu nie udostępnia okna aplikacji. |
| `Communications link failure` / odmowa połączenia | Sprawdź `systemctl status mysql` oraz adres i port w `db.properties`. |
| `Access denied for user 'hotel_app'` | Sprawdź hasło i istnienie konta na wybranym serwerze. Nie zastępuj danych pracownika danymi MySQL. |
| Brak `db.properties` lub `pom.xml` | Przejdź do głównego folderu projektu przed uruchomieniem Maven. |

Oficjalne materiały: [JavaFX z Maven](https://openjfx.io/openjfx-docs/maven), [instalacja MySQL na Ubuntu](https://documentation.ubuntu.com/server/how-to/databases/install-mysql/) oraz [pakiet GTK dla Ubuntu 24.04](https://packages.ubuntu.com/noble/libgtk-3-0t64).

## Baza danych: nowa instalacja lub aktualizacja

### Nie masz jeszcze bazy

1. Otwórz MySQL Workbench i połącz się jako administrator MySQL.
2. Otwórz plik **`db/HotelManager_baza.sql`**.
3. Wykonaj skrypt jednorazowo. Utworzy bazę `hotel_manager`, tabele, fikcyjne rekordy i konta techniczne `hotel_app`.
4. Sprawdź ustawienia `db.properties` i uruchom aplikację.

Pełny skrypt zawiera już tabelę dodatkowych gości. Jeżeli konto `hotel_app` istnieje na serwerze, dostosuj końcową część skryptu tworzącą konta zamiast wykonywać ją ponownie bez zmian.

### Masz bazę z wcześniejszej wersji

**Nie importuj ponownie pełnego skryptu z danymi demonstracyjnymi.**

Jeśli Twoja baza nie ma tabeli `uczestnicy_rezerwacji`, wykonaj jako administrator:

```sql
-- Otwórz i wykonaj plik:
-- db/aktualizacja_uczestnicy.sql
```

Ten plik dodaje tabelę dodatkowych osób i zachowuje dotychczasowe rekordy. Dla innej nazwy bazy zmień zawarte w nim `USE hotel_manager;`. Jeśli aktualizacja została już wykonana, nie potrzebujesz kolejnej migracji. **Moduł raportów nie dodaje nowych tabel.** Aktualizacja bezpieczeństwa wymaga jednak dwóch pól logowania opisanych poniżej.

Przy aktualizacji programu zachowaj własny plik `db.properties`. Starsze rezerwacje mogą pokazywać komunikat o brakujących danych dodatkowych osób — uzupełnisz je przez **„Goście pobytu”** w potwierdzonej lub zameldowanej rezerwacji.

## Uruchomienie przez IntelliJ IDEA i Maven

1. Otwórz `pom.xml` jako projekt Maven.
2. Ustaw Project SDK oraz Maven Runner JRE na JDK 17 lub nowszy.
3. Poczekaj na pobranie zależności. Pierwsze uruchomienie wymaga połączenia z internetem.
4. Skonfiguruj bazę i `db.properties`.
5. W panelu Maven wybierz **Plugins → javafx → javafx:run** albo wykonaj w głównym folderze projektu:

   ```sh
   mvn javafx:run
   ```

Katalog roboczy musi zawierać `db.properties`. **`Launcher` uruchamia GUI**, natomiast `Main` pozostaje punktem wejścia wcześniejszej wersji konsolowej.

## Aktualizacja bezpieczeństwa

Przed uruchomieniem tej wersji na starej bazie wykonaj jako administrator MySQL
**`db/aktualizacja_bezpieczenstwo.sql`**. Skrypt dodaje licznik prób i czas blokady
do `pracownicy`, bez usuwania danych; można wykonać go ponownie. Jeżeli nazwa
Twojej bazy jest inna, dostosuj `USE hotel_manager;`. Nowa instalacja z pełnego
skryptu zawiera już te pola. Nie importuj ponownie rekordów demonstracyjnych.

Rozpakuj program do nowego folderu, przenieś własny `db.properties` i użyj nowego
skryptu Windows — zawiera nowe nazwy plików JAR. Zachowano zgodność z MySQL 8.0;
sterownik JDBC zaktualizowano do 9.7.0, a JavaFX do 17.0.21.

Opcjonalny **`db/ogranicz_uprawnienia.sql`** ogranicza uprawnienia demonstracyjnych
kont technicznych `hotel_app@localhost` i `hotel_app@127.0.0.1`. Wykonaj go tylko,
jeśli używasz tych kont i schematu. Nie jest przeznaczony dla konta `root` ani
innego wspólnego użytkownika; własne konta wymagają dopasowania grantów.
Nowe instalacje otrzymują już taki ograniczony zestaw uprawnień.

Połączenie z serwerem innym niż loopback wymaga `sslMode=VERIFY_IDENTITY` oraz
zaufanego certyfikatu serwera; wyłącz `allowPublicKeyRetrieval` w takim URL.
Konfiguracja demonstracyjna z wyłączonym TLS jest dopuszczana tylko dla loopback.
Dane można podać także przez `HOTEL_DB_URL`, `HOTEL_DB_USER` i `HOTEL_DB_PASSWORD`,
które zastępują wartości z pliku. Nie publikuj własnego `db.properties`.
`.gitignore` nie usuwa plików już śledzonych przez Git ani wcześniejszych haseł
z historii. Ujawnione hasło trzeba zmienić.

Publiczne metody bazy wymagają wcześniejszego `BazaDanych.zaloguj(...)` w tym samym
wątku. GUI wykonuje logowanie i dalsze operacje na swoim jednym wątku roboczym,
a konsola na wątku menu. Nie można zastąpić logowania konstruktorem `Pracownik`.
`BazaDanych.polacz()` usunięto z publicznego API. Testy używają osobnej, jawnie
zabezpieczonej klasy fixture zamiast otwierać połączenia przez aplikację.

Komentarze usunięto ze źródeł Java, testów, CSS, SQL, XML i konfiguracji;
wyjaśnienia pozostają w dokumentacji Markdown i `START_TUTAJ.txt`.

## Konta i uprawnienia

Po zaimportowaniu niezmienionych danych demonstracyjnych:

| Rola | Login | Hasło demonstracyjne |
| --- | --- | --- |
| Administrator | `admin` | `Admin123!` |
| Recepcjonista | `recepcja` | `Recepcja123!` |

Jeżeli hasła zostały już zmienione, użyj aktualnych. Po trzech nieudanych próbach logowania uruchom aplikację ponownie; jest to limit danego uruchomienia programu.

Recepcjonista obsługuje gości, rezerwacje, pobyty, sprzątanie, wpłaty i raporty. Administrator ma również możliwość dodawania i edycji pokoi, zarządzania kontami oraz rejestrowania zwrotów.

Po pięciu nieudanych logowaniach konto jest blokowane na pięć minut. Licznik jest w bazie, więc restart aplikacji nie usuwa blokady. Hasła pracowników są przechowywane jako skróty PBKDF2 z losową solą i 600 000 iteracji. Aktywność konta i jego rola są ponownie sprawdzane przed operacjami GUI korzystającymi z bazy. Dezaktywacja konta powoduje zakończenie jego sesji przy następnej takiej operacji.

## Przykładowa obsługa pobytu

1. **Dodaj głównego gościa** na ekranie „Goście”.
2. Na ekranie **„Rezerwacje”** wybierz „Nowa rezerwacja”, gościa, pokój i termin.
3. Ustaw liczbę osób. Dla każdej dodatkowej osoby wpisz imię i nazwisko.
4. Zapisz rezerwację. Program sprawdzi pojemność pokoju, termin i dostępność.
5. W razie potrzeby zarejestruj zaliczkę w **„Płatnościach”**.
6. W dniu pobytu zaznacz rezerwację i wybierz **„Zamelduj”**. Pokój musi być gotowy.
7. Po rozliczeniu należności wybierz **„Wymelduj”**. Pokój otrzyma stan `DO_SPRZATANIA`.
8. Po sprzątaniu oznacz pokój jako posprzątany.

### Kilku gości w jednym pokoju

Przykład: **Błażej Wencel** jest głównym gościem, a rezerwacja obejmuje trzy osoby. W dodatkowych polach wpisujesz **Annę Nowak** i **Jana Kowalskiego**. Wszystkie osoby mają ten sam pokój i termin pobytu.

Główny gość pochodzi z kartoteki `goscie`. Dodatkowym osobom wystarczą imię i nazwisko — nie trzeba tworzyć osobnych kartotek z telefonem i e-mailem. Ich dane możesz uzupełnić lub poprawić przyciskiem **„Goście pobytu”**. W zakończonych i anulowanych pobytach lista jest dostępna do podglądu.

Liczba osób obejmuje głównego gościa. Dodanie nazwisk nie zwiększa liczby miejsc w pokoju ani nie zmienia ceny rezerwacji.

## Kalendarz rezerwacji

Wybierz **„Kalendarz”** w menu. Wiersze odpowiadają pokojom, a kolumny nocom
zaczynającym się danego dnia. Strzałki przełączają miesiące, „Dzisiaj” wraca
do bieżącego miesiąca. Możesz też podać datę i kliknąć „Przejdź”. Filtr szuka
numeru lub typu pokoju. Dalsze dni zobaczysz po przewinięciu tabeli poziomo.

- **+** — wolna noc: kliknięcie odświeża dane i otwiera formularz z pokojem,
  datą przyjazdu i domyślnym wyjazdem następnego dnia. Możesz zmienić termin
  w formularzu; ostateczna dostępność jest sprawdzana w transakcji zapisu.
- **Niebieskie #ID** — potwierdzona rezerwacja; **zielone #ID** — zameldowany pobyt.
- **Szare #ID** — zakończony pobyt pokazany jako historia; nie blokuje nowego zapisu.
- **—** — brak możliwości utworzenia nowego pobytu: termin w przeszłości lub
  pokój obecnie wyłączony. Wyłączenie nie usuwa historycznych rezerwacji z podglądu.
- **!** — co najmniej dwie aktywne rezerwacje tej samej nocy; trzeba sprawdzić dane.

Kliknięcie #ID pokazuje gości, termin, status i cenę pobytu. Przycisk
„Otwórz na liście rezerwacji” przełącza do dotychczasowego ekranu obsługi.
Jeśli tego dnia zapisano kilka pobytów, można wybrać je z listy pod szczegółami.
Podpowiedź po najechaniu myszą podaje także numer pokoju i datę nocy.

**Dzień wyjazdu nie jest zajętą nocą.** Anulowane rezerwacje są pomijane.
Kalendarz korzysta z ostatnio pobranych danych — użyj „Odśwież dane”, aby
zobaczyć zapisy z innych stanowisk. Numer pokoju, jego pojemność i stan są
odczytywane z aktualnej kartoteki, bez historii zmian stanu pokoju.

Nie dodano nowych tabel ani migracji. Kalendarz działa na tej samej bazie
co wcześniejsza wersja. Ten etap nie obejmuje przenoszenia rezerwacji myszą
ani zmiany pokoju istniejącej rezerwacji.

## Zmiana pokoju

W zakładce **Rezerwacje** zaznacz pobyt i kliknij **„Zmień pokój”**. Formularz
pokaże inne pokoje z wystarczającą liczbą miejsc, wolne na cały termin pobytu.
Dostępność jest ponownie sprawdzana w transakcji przy zapisie.

- Funkcja jest dostępna dla recepcji i administratora.
- Cena uzgodniona przy rezerwacji, daty, płatności oraz dodatkowi goście pozostają bez zmian.
- Przeniesienie zameldowanych gości wymaga pokoju `GOTOWY` i oznacza poprzedni pokój jako `DO_SPRZATANIA`; pokój już wyłączony pozostaje wyłączony.
- Anulowane, zakończone oraz pobyty z wyjazdem dzisiaj lub wcześniej nie mogą być przenoszone.
- W konsoli wybierz **24. Zmień pokój rezerwacji** i potwierdź wpisując `TAK`.

**Ograniczenie modelu:** rezerwacja ma jeden `pokoj_id`. Zmiana przypisuje nowy
pokój całemu pobytowi, również przeszłym nocom w kalendarzu i raportach.
Nie jest to podział pobytu na noclegi w kilku pokojach; historia kolejnych przeniesień
nie jest rejestrowana. Formularz informuje o tym przed zapisem.

Ten etap **nie zmienia struktury bazy**. Jeżeli masz już aktualizację bezpieczeństwa,
nie wykonuj ponownie importu ani migracji. Rozpakuj program do nowego folderu
i przenieś własny `db.properties`. Szczegóły: [`ZMIANA_POKOJU.md`](ZMIANA_POKOJU.md).

## Raporty i eksport

Na ekranie **„Raporty”** wybierz okres od 1 do 366 dni i kliknij **„Generuj raport”**. Obie daty należą do wybranego okresu.

| Zakładka | Zawartość |
| --- | --- |
| **Podsumowanie** | Liczba pobytów, pokojonoce, osobonoce, obłożenie, wartość noclegów, wpłaty i zwroty. |
| **Rezerwacje i goście** | Pobyty przecinające wybrany okres, dane głównego i dodatkowych gości oraz wycena części pobytu w okresie. |
| **Obłożenie pokoi** | Liczba zarezerwowanych nocy i procent obłożenia dla poszczególnych pokoi. |
| **Płatności** | Transakcje wykonane w wybranym okresie, niezależnie od terminu rezerwacji. |

- **„Zapisz tabelę CSV”** eksportuje wybraną zakładkę do pliku dla Excela lub LibreOffice. Kodowanie: UTF-8 z BOM; separator: średnik.
- **„Zapisz cały raport HTML / PDF”** zapisuje wszystkie zakładki jako HTML. Otwórz plik w przeglądarce, kliknij „Drukuj / zapisz jako PDF” i wybierz zapis do PDF w oknie drukowania.

Program nie tworzy pliku PDF bezpośrednio. Zmiana okresu unieważnia podgląd; po nowych zapisach w bazie wygeneruj raport ponownie.

### Jak liczone są wyniki

**Pokojonoc** to jedna noc rezerwacji jednego pokoju. **Osobonoce** to liczba tych nocy pomnożona przez liczbę osób. Dzień wyjazdu nie jest noclegiem. Do obłożenia wliczane są rezerwacje potwierdzone, zameldowane i zakończone; anulowane są pomijane.

Obłożenie odnosi się do wszystkich pokoi zapisanych obecnie w bazie, także wyłączonych. Projekt nie przechowuje historii zmian liczby i dostępności pokoi, dlatego jest to **obłożenie rezerwacyjne według aktualnej bazy pokoi**.

Wartość noclegów korzysta ze stawki zapisanej przy rezerwacji i obejmuje tylko noce przypadające na wybrany okres. Wpłaty i zwroty liczone są osobno, według daty transakcji. Wycena pobytów nie oznacza otrzymanych pieniędzy i nie jest raportem księgowym.

## Budowa projektu

| Plik / katalog | Rola |
| --- | --- |
| `src/Launcher.java` | Krótki punkt wejścia GUI; nadal uruchamiany przez Maven i skrypt Windows. |
| `src/Main.java` | Krótki punkt wejścia zachowanej wersji konsolowej. |
| `src/pl/hotel/model/` | Modele: gość, pokój, rezerwacja, uczestnik, pracownik i płatność. |
| `src/pl/hotel/baza/BazaDanych.java` | Publiczne API bazy: przekazuje wywołania do odpowiednich klas DAO. |
| `src/pl/hotel/baza/PolaczenieBazy.java` | Konfiguracja JDBC, ograniczenia funkcji sterownika, timeouty i TLS dla zdalnej bazy. |
| `src/pl/hotel/baza/KontrolaDostepu.java` | Uwierzytelniony kontekst wątku i sprawdzanie aktualnych uprawnień konta. |
| `src/pl/hotel/baza/RaportDAO.java` | Odczyt danych raportu bez udostępniania surowego połączenia innym pakietom. |
| `src/pl/hotel/baza/*DAO.java` | Zapytania i transakcje podzielone według obszaru: goście, pokoje, rezerwacje, pobyty, rozliczenia, konta. |
| `src/pl/hotel/konsola/` | Osobno menu, odczyt danych od użytkownika, stan konsoli oraz obsługa poszczególnych obszarów. |
| `src/pl/hotel/gui/HotelGUI.java` | Cykl życia okna i nawigacja między ekranami. |
| `src/pl/hotel/kalendarz/PlanKalendarza.java` | Indeks rezerwacji według pokoju i nocy; zasady dostępności widoku, bez JavaFX i SQL. |
| `src/pl/hotel/gui/KomorkaKalendarza.java` | Kolor, podpowiedź i kliknięcie komórki kalendarza. |
| `src/pl/hotel/gui/Ekran*.java` | Ekrany i formularze: goście, pokoje, rezerwacje, płatności, konta, raporty i pulpit. |
| `src/pl/hotel/gui/StanGUI.java` | Dane jednej instancji GUI, w tym sesja, tabele i filtry. |
| `src/pl/hotel/gui/Formularz.java`, `TabeleGUI.java`, `NarzedziaGUI.java` | Wspólna obsługa formularzy, konfiguracja tabel, kontrolki, formatowanie i walidacja. |
| `src/pl/hotel/gui/ZadaniaGUI.java`, `KomunikatyGUI.java` | Zadania w tle, odświeżanie i obsługa błędów. |
| `src/pl/hotel/serwis/GuiRepository.java` | Odczyt zestawu danych dla GUI i weryfikacja sesji. |
| `src/pl/hotel/raporty/Raporty.java` | Generowanie raportu i eksport CSV oraz HTML. |
| `src/pl/hotel/bezpieczenstwo/` | Skróty haseł, błąd wygasłej sesji oraz bezpieczne komunikaty błędów. |
| `src/pl/hotel/walidacja/Dane.java` | Walidacja wymaganych danych także przy wywołaniu bez GUI. |
| `resources/hotel.css` | Wygląd interfejsu. |
| `db/HotelManager_baza.sql` | Pełna baza z fikcyjnymi rekordami dla nowej instalacji. |
| `db/aktualizacja_uczestnicy.sql` | Aktualizacja wcześniejszej bazy o dodatkowych gości. |
| `lib/` | Biblioteki do uruchamiania skryptem Windows. |
| `tests/` | Scenariusze sprawdzające bazę, konsolę, GUI i raporty. |
| `db.properties`, `pom.xml` | Konfiguracja połączenia i projektu Maven. |
| `uruchom_windows.bat`, `START_TUTAJ.txt` | Skrypt uruchamiający i dodatkowa instrukcja. |
| [`MAPA_KODU.md`](MAPA_KODU.md) | Opis podziału klas i wskazówki, gdzie wprowadzać kolejne zmiany. |

### Aktualizacja do uporządkowanej struktury

Rozpakuj tę wersję **do nowego folderu** i przenieś do niego własny `db.properties`.
Nie mieszaj starych plików z płaskiego `src` z nowymi pakietami.
`Launcher` i `Main` zachowują nazwy punktów wejścia, a pozostałe klasy znajdują się
w `pl.hotel.*`. Użyj również nowego `uruchom_windows.bat`, który szuka źródeł
rekurencyjnie w podfolderach i zapisuje ścieżki z ukośnikiem `/` dla `javac`.

**Sam podział kodu nie wymaga żadnej zmiany tabel ani ponownego importowania bazy.**
Przy własnych rozszerzeniach zaktualizuj importy, np. `import pl.hotel.baza.BazaDanych;`.
Dane, reguły pobytów, granice transakcji i dostępne funkcje pozostają bez zmian.

Zapytania JDBC oraz zapis raportów wykonują się w tle przez JavaFX `Task`. Interfejs pokazuje stan operacji i blokuje kolejne działania do jej zakończenia. Formularze pozostają otwarte po błędzie zapisu, aby można było poprawić dane.

## Relacje w bazie danych

| Tabela | Przechowywane informacje i powiązania |
| --- | --- |
| `goscie` | Dane kontaktowe głównych gości; jeden gość może mieć wiele rezerwacji. |
| `pokoje` | Numer, typ, pojemność, cena i stan; jeden pokój może mieć wiele rezerwacji w różnych terminach. |
| `pracownicy` | Konta pracowników, role i aktywność; pracownik jest przypisywany do rezerwacji i płatności. |
| `rezerwacje` | Powiązanie głównego gościa, pokoju i pracownika z terminem, liczbą osób, stawką i statusem. |
| `uczestnicy_rezerwacji` | Dodatkowe osoby przypisane do jednej rezerwacji — relacja 1:N. |
| `platnosci` | Wpłaty i zwroty przypisane do rezerwacji oraz pracownika — rezerwacja może mieć wiele transakcji. |

Dodatkowi goście są przypisani do **konkretnego pobytu**, nie na stałe do pokoju. Dane rezerwacji i jej nowych uczestników są zapisywane w jednej transakcji.

## Zasady działania

- Nowy pobyt trwa od **1 do 30 nocy** i nie może zaczynać się w przeszłości.
- Liczba osób nie może przekroczyć pojemności wybranego pokoju.
- Nakładające się aktywne rezerwacje tego samego pokoju są odrzucane. Wyjazd jednego gościa i przyjazd kolejnego mogą przypadać na ten sam dzień.
- Cena dotyczy **całego pokoju za noc**. Zmiana cennika pokoju nie zmienia stawek wcześniejszych rezerwacji.
- Formularz edycji pokoju nie zmienia jego pojemności, aby zachować zgodność wcześniejszych rezerwacji.
- Zameldowanie wymaga pokoju `GOTOWY`; pokój `WYLACZONY` nie przyjmuje nowych rezerwacji.
- Wymeldowanie wymaga zerowego salda. Przed anulowaniem należy zwrócić pozostałe wpłacone środki.
- Wpłaty i zwroty są ewidencją w bazie — aplikacja nie jest połączona z bankiem ani terminalem płatniczym.
- Transakcje i blokady `FOR UPDATE` chronią operacje wymagające spójnych danych, w tym sprawdzanie dostępności przy zapisie rezerwacji.
- Przy zmianach wykonanych na innym stanowisku należy odświeżyć listy. Zmiana hasła, roli lub dezaktywacja konta wymaga ponownego logowania przy kolejnej operacji chronionej.
- Role i tożsamość pracownika są sprawdzane w publicznej warstwie operacji, niezależnie od widoczności przycisków. Wewnętrzne DAO i połączenia JDBC nie są publicznym API. Połączenia korzystają jednak ze wspólnego konta technicznego MySQL, co ogranicza izolację między rolami na poziomie samego serwera.

## Weryfikacja

Sprawdzono kompilację na JDK 17 oraz scenariusze na rzeczywistym MySQL 8.0.46 w odizolowanej bazie testowej. Interfejs uruchomiono również w środowisku headless JavaFX.

| Scenariusz | Zakres |
| --- | --- |
| `RoomChangeProbe` | Zmiana pokoju: dostępność, pojemność, statusy, cena i wpłaty, dodatkowi goście, sprzątanie, pełny rollback oraz równoczesne przeniesienia i rezerwacje. |
| `IntegrationProbe` | Rezerwacje, konflikty terminów, płatności, rozliczanie pobytu, konta, role, równoczesne próby rezerwacji oraz zapis i edycja uczestników z wycofaniem błędnych zmian. |
| `GuiSmoke` / `GuiSmokeLauncher` | Logowanie, ekrany, formularze, zapis gościa i rezerwacji, edycja dodatkowych osób, zmiana pokoju i odświeżenie kalendarza, rozliczenie, raporty i zakończenie sesji nieaktywnego konta. |
| `SecurityProbe` | Odmowa nieuprawnionych operacji, SQL injection, blokada logowania między procesami, wygaszenie sesji, równoległe wpłaty i zwroty, limity danych oraz ustawienia JDBC. |
| `PrivilegeProbe` | Działanie programu na ograniczonym koncie technicznym i odmowa DDL oraz modyfikacji/usuwania płatności. |
| `CalendarProbe` | Noc przyjazdu i wyjazdu, zmiana miesiąca, luty przestępny, anulowania, zakończone pobyty, wyłączenie pokoju i konflikty. |
| `ReportProbe` | Granice okresu, obłożenie, wycena noclegów, płatności, dodatkowi goście oraz eksport CSV i HTML. |

**Scenariusze zmieniają dane — uruchamiaj je wyłącznie na osobnej bazie testowej.** Są to programy testowe z metodami `main`, a nie testy automatycznie wykonywane przez `mvn test`. Skrypt Windows przygotowano dla Windows x64; w środowisku weryfikacyjnym sprawdzono kompilację z dołączonymi bibliotekami, lecz nie uruchamiano samego `.bat` na Windows.

## Rozwiązywanie problemów

| Problem | Co sprawdzić |
| --- | --- |
| `duplicate module on application module path` | Nie mieszaj wersji bibliotek. Aktualny skrypt wskazuje dokładnie trzy JAR-y JavaFX 17.0.21; rozpakuj paczkę do nowego folderu i przenieś własny `db.properties`. |
| Brak polecenia `javac` | Zainstaluj JDK i dodaj katalog `bin` do `PATH`; samo JRE nie wystarczy. |
| `file not found: srcBazaDanych.java` | Użyj aktualnego skryptu `.bat`. Skrypt musi wyszukać źródła w podfolderach i zapisać ścieżki z ukośnikiem `/`, np. `src/pl/hotel/baza/BazaDanych.java`. |
| Nie znaleziono `db.properties` | Uruchamiaj program z głównego folderu projektu. |
| `Access denied` | Sprawdź użytkownika MySQL, jego hasło i uprawnienia w `db.properties`. |
| Brak połączenia z serwerem | Sprawdź działanie MySQL, adres i port. |
| Nie istnieje tabela `uczestnicy_rezerwacji` | Wykonaj `db/aktualizacja_uczestnicy.sql` na właściwej bazie. |
| Nie można zapisać rezerwacji | Sprawdź daty, liczbę osób, pojemność pokoju i konflikt z innymi rezerwacjami. |
| Raport jest nieaktualny | Wygeneruj go ponownie; eksport korzysta z danych widocznych w podglądzie. |
| Błąd sieci podczas zapisu | Najpierw odśwież dane. Serwer mógł zapisać zmianę przed utratą odpowiedzi; sprawdź wynik przed ponowieniem operacji, szczególnie wpłaty. |

Opis audytu, wyników oraz ograniczeń: **[`RAPORT_BEZPIECZENSTWA/README.md`](RAPORT_BEZPIECZENSTWA/README.md)**. Instrukcja uruchomienia scenariuszy: **[`TESTOWANIE.md`](TESTOWANIE.md)**.

Dodatkowe wskazówki uruchomienia i aktualizacji znajdują się w **[`START_TUTAJ.txt`](START_TUTAJ.txt)**.
