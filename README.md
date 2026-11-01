# System zarządzania hotelem

**Aplikacja okienkowa w JavaFX z bazą MySQL** do obsługi pracy recepcji: od dodania gościa i utworzenia rezerwacji po płatności, wymeldowanie i raport za wybrany okres.

Projekt rozwija wcześniejszą wersję konsolową. Wspólne klasy modelu i operacje JDBC obsługują dane hotelu, a JavaFX udostępnia formularze, tabele i menu pracownika.

[Repozytorium projektu](https://github.com/Chyba-Bombel/System-zarzadzania-hotelem-Projekt)

## Spis treści

- [Możliwości aplikacji](#możliwości-aplikacji)
- [Technologie i wymagania](#technologie-i-wymagania)
- [Uruchomienie na Windows](#uruchomienie-na-windows)
- [Baza danych: nowa instalacja lub aktualizacja](#baza-danych-nowa-instalacja-lub-aktualizacja)
- [Uruchomienie przez IntelliJ IDEA i Maven](#uruchomienie-przez-intellij-idea-i-maven)
- [Konta i uprawnienia](#konta-i-uprawnienia)
- [Przykładowa obsługa pobytu](#przykładowa-obsługa-pobytu)
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
| **Rezerwacje** | Tworzenie rezerwacji, dodatkowi goście, zmiana terminu, zameldowanie, wymeldowanie i anulowanie. |
| **Płatności** | Rozliczenie pobytu, wpłaty i zaliczki; dla administratora rejestrowanie zwrotów. |
| **Raporty** | Zestawienia za wybrany okres, eksport CSV i HTML oraz wydruk przez przeglądarkę. |
| **Pracownicy** | Tworzenie i dezaktywacja kont przez administratora. |
| **Moje konto** | Zmiana własnego hasła. |

Tabele umożliwiają sortowanie. Na ekranach obsługi gości, pokoi i rezerwacji dostępne są wyszukiwanie oraz filtry. Dane z bazy można pobrać ponownie przyciskiem **„Odśwież dane”**.

## Technologie i wymagania

| Element | Wersja / zastosowanie |
| --- | --- |
| Java | JDK 17 lub nowszy; kompilacja do wersji 17. |
| JavaFX | 17.0.18 — interfejs okienkowy i CSS. |
| MySQL | 8.0.16 lub nowszy — baza danych i transakcje InnoDB. |
| JDBC | MySQL Connector/J 9.2.0 — komunikacja aplikacji z bazą. |
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

Skrypt kompiluje pliki z `src`, kopiuje arkusz stylów i uruchamia klasę `Launcher`. Maven nie jest potrzebny w tym wariancie. Foldery `src`, `resources`, `lib` oraz plik `db.properties` muszą pozostać obok skryptu.

### Konfiguracja połączenia

Przykładowa konfiguracja dla lokalnej bazy demonstracyjnej:

```properties
url=jdbc:mysql://localhost:3306/hotel_manager?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=10000
user=hotel_app
password=HotelDemo2026!
```

Dostosuj adres serwera, port, nazwę bazy, użytkownika i hasło do własnej instalacji. To **konto techniczne MySQL**, a nie konto pracownika wpisywane w oknie logowania.

Konfiguracja demonstracyjna służy do nauki lokalnej. Przy udostępnianiu projektu nie publikuj własnych haseł do bazy.

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

Ten plik dodaje tabelę dodatkowych osób i zachowuje dotychczasowe rekordy. Dla innej nazwy bazy zmień zawarte w nim `USE hotel_manager;`. Jeśli aktualizacja została już wykonana, nie potrzebujesz kolejnej migracji. **Moduł raportów nie dodaje nowych tabel.**

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

## Konta i uprawnienia

Po zaimportowaniu niezmienionych danych demonstracyjnych:

| Rola | Login | Hasło demonstracyjne |
| --- | --- | --- |
| Administrator | `admin` | `Admin123!` |
| Recepcjonista | `recepcja` | `Recepcja123!` |

Jeżeli hasła zostały już zmienione, użyj aktualnych. Po trzech nieudanych próbach logowania uruchom aplikację ponownie; jest to limit danego uruchomienia programu.

Recepcjonista obsługuje gości, rezerwacje, pobyty, sprzątanie, wpłaty i raporty. Administrator ma również możliwość dodawania i edycji pokoi, zarządzania kontami oraz rejestrowania zwrotów.

Hasła pracowników są przechowywane jako skróty PBKDF2 z solą. Aktywność konta i jego rola są ponownie sprawdzane przed operacjami GUI korzystającymi z bazy. Dezaktywacja konta powoduje zakończenie jego sesji przy następnej takiej operacji.

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
| `src/Launcher.java` | Uruchomienie aplikacji JavaFX. |
| `src/HotelGUI.java` | Ekrany, formularze, tabele, obsługa działań użytkownika i zadania w tle. |
| `src/GuiRepository.java` | Pobieranie danych dla GUI i kontrola sesji pracownika. |
| `src/BazaDanych.java` | Połączenie JDBC, zapytania SQL i transakcje operacji hotelowych. |
| `src/Raporty.java` | Generowanie zestawień oraz eksport CSV i HTML. |
| `src/Osoba.java`, `src/Gosc.java`, `src/Pracownik.java` | Model osób; wspólne dane i dziedziczenie. |
| `src/Pokoj.java`, `src/Rezerwacja.java`, `src/Uczestnik.java` | Model pokoju, rezerwacji i dodatkowej osoby pobytu. |
| `src/Platnosc.java`, `src/Rozliczenie.java` | Dane płatności i obliczenia rozliczenia. |
| `src/KontoPracownika.java`, `src/Hasla.java` | Dane kont i obsługa haseł. |
| `src/Main.java` | Zachowana wersja konsolowa; nie zawiera wszystkich nowych funkcji GUI. |
| `resources/hotel.css` | Wygląd interfejsu. |
| `db/HotelManager_baza.sql` | Pełna baza z fikcyjnymi rekordami dla nowej instalacji. |
| `db/aktualizacja_uczestnicy.sql` | Aktualizacja wcześniejszej bazy o dodatkowych gości. |
| `lib/` | Biblioteki do uruchamiania skryptem Windows. |
| `tests/` | Scenariusze sprawdzające operacje bazy, GUI i raporty. |
| `db.properties`, `pom.xml` | Konfiguracja połączenia i projektu Maven. |
| `uruchom_windows.bat`, `START_TUTAJ.txt` | Skrypt uruchamiający i dodatkowa instrukcja. |

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
- Przy zmianach wykonanych na innym stanowisku należy odświeżyć listy. Sama zmiana hasła nie zamyka automatycznie już otwartych sesji.
- Role pracowników są kontrolowane przez aplikację; połączenia JDBC korzystają ze wspólnego konta technicznego MySQL.

## Weryfikacja

Sprawdzono kompilację na JDK 17 oraz scenariusze na rzeczywistym MySQL 8.0.46 w odizolowanej bazie testowej. Interfejs uruchomiono również w środowisku headless JavaFX.

| Scenariusz | Zakres |
| --- | --- |
| `IntegrationProbe` | Rezerwacje, konflikty terminów, płatności, rozliczanie pobytu, konta, role, równoczesne próby rezerwacji oraz zapis i edycja uczestników z wycofaniem błędnych zmian. |
| `GuiSmoke` / `GuiSmokeLauncher` | Logowanie, ekrany, formularze, zapis gościa i rezerwacji, edycja dodatkowych osób, rozliczenie, raporty i zakończenie sesji nieaktywnego konta. |
| `ReportProbe` | Granice okresu, obłożenie, wycena noclegów, płatności, dodatkowi goście oraz eksport CSV i HTML. |

**Scenariusze zmieniają dane — uruchamiaj je wyłącznie na osobnej bazie testowej.** Są to programy testowe z metodami `main`, a nie testy automatycznie wykonywane przez `mvn test`. Skrypt Windows przygotowano dla Windows x64; w środowisku weryfikacyjnym sprawdzono kompilację z dołączonymi bibliotekami, lecz nie uruchamiano samego `.bat` na Windows.

## Rozwiązywanie problemów

| Problem | Co sprawdzić |
| --- | --- |
| Brak polecenia `javac` | Zainstaluj JDK i dodaj katalog `bin` do `PATH`; samo JRE nie wystarczy. |
| `file not found: srcBazaDanych.java` | Użyj aktualnego skryptu `.bat`. W pliku listy źródeł ścieżki muszą mieć postać `src/BazaDanych.java`. |
| Nie znaleziono `db.properties` | Uruchamiaj program z głównego folderu projektu. |
| `Access denied` | Sprawdź użytkownika MySQL, jego hasło i uprawnienia w `db.properties`. |
| Brak połączenia z serwerem | Sprawdź działanie MySQL, adres i port. |
| Nie istnieje tabela `uczestnicy_rezerwacji` | Wykonaj `db/aktualizacja_uczestnicy.sql` na właściwej bazie. |
| Nie można zapisać rezerwacji | Sprawdź daty, liczbę osób, pojemność pokoju i konflikt z innymi rezerwacjami. |
| Raport jest nieaktualny | Wygeneruj go ponownie; eksport korzysta z danych widocznych w podglądzie. |
| Błąd sieci podczas zapisu | Najpierw odśwież dane. Serwer mógł zapisać zmianę przed utratą odpowiedzi; sprawdź wynik przed ponowieniem operacji, szczególnie wpłaty. |

Dodatkowe wskazówki uruchomienia i aktualizacji znajdują się w **[`START_TUTAJ.txt`](START_TUTAJ.txt)**.
