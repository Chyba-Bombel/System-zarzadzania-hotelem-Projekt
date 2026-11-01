# System zarządzania hotelem — JavaFX

Kompletna aplikacja okienkowa oparta na konsolowym projekcie użytkownika:
https://github.com/Chyba-Bombel/System-zarzadzania-hotelem-Projekt

## Uruchomienie

Na **Windows x64**: JDK 17+, działający MySQL, poprawny `db.properties`, następnie
`uruchom_windows.bat`. Biblioteki JavaFX dla Windows są dołączone.

W **IntelliJ** otwórz `pom.xml` jako projekt Maven i uruchom cel `javafx:run`.
Z terminalu z zainstalowanym Maven:

```sh
mvn javafx:run
```

Szczegółowa instrukcja: **START_TUTAJ.txt**.

## Ekrany

Pulpit, goście, pokoje, rezerwacje, płatności, pracownicy i moje konto.
Tabele mają wyszukiwanie, sortowanie, odświeżanie i operacje dla zaznaczonego wiersza.
Formularze zawierają walidację, kalendarze i pola haseł.

Administrator zarządza pokojami i kontami oraz rejestruje zwroty.
Recepcjonista obsługuje gości, rezerwacje, pobyty, sprzątanie i wpłaty.

## Zgodność z bazą

Dotychczasowy schemat `hotel_manager` pozostaje bez zmian. Istniejącej bazy
nie trzeba ponownie importować. Plik SQL jest przeznaczony wyłącznie do nowej instalacji.
Nie ma migracji ani automatycznego kasowania rekordów przy uruchomieniu.

## Kod

- `Launcher` — punkt wejścia GUI.
- `HotelGUI` — sceny, tabele, formularze, walidacja UI i zadania asynchroniczne.
- `GuiRepository` — odświeżanie zestawu danych, typy pokoi i weryfikacja sesji.
- `BazaDanych` — dotychczasowe operacje JDBC i transakcje; połączenie z pliku właściwości.
- `resources/hotel.css` — wygląd okien.
- `Main` — zachowana wersja konsolowa.
- Pozostałe klasy modelu i haseł pochodzą z projektu; `Rezerwacja` otrzymała gettery potrzebne tabelom.

JDBC wykonuje się na jednym wątku roboczym przez JavaFX `Task`. Wyniki wracają
na wątek interfejsu; na czas operacji widoczny jest wskaźnik pracy i blokada kolejnych zapisów.
Formularz pozostaje otwarty po błędzie zapisu, umożliwiając poprawienie danych.
Operacje zapisu ponownie sprawdzają aktualną aktywność i rolę konta.

## Zakres i ograniczenia

- Kwoty dotyczą całego pokoju za noc, pobyt od 1 do 30 nocy.
- Przy zmianie ceny pokoju wcześniejsze rezerwacje zachowują swoją stawkę.
- Zmiana numeru, typu i ceny pokoju jest dostępna; pojemności istniejącego pokoju nie zmieniamy.
- Zameldowanie wymaga gotowego pokoju oraz daty należącej do pobytu.
- Wymeldowanie wymaga zerowego salda i ustawia pokój do sprzątania.
- Przed anulowaniem należy zwrócić wpłacone środki.
- Wpłaty i zwroty są ewidencją w MySQL — aplikacja nie obsługuje banku ani terminala.
- Pulpit to bieżący przegląd, bez raportów okresowych i eksportu CSV.
- Operacje zewnętrzne są widoczne po odświeżeniu; blokady transakcyjne nadal chronią zapis.
- Role są sprawdzane w aplikacji; oba stanowiska korzystają ze wspólnego konta technicznego MySQL.
- Zmiana hasła nie unieważnia automatycznie już otwartych sesji.
- Dane demo są fikcyjne. Parametry TLS w dołączonej konfiguracji są przeznaczone do nauki lokalnej.

## Weryfikacja

Sprawdzono kompilację na JDK 17, uruchomienie JavaFX w środowisku headless
oraz działanie na rzeczywistym MySQL 8.0.46 w izolowanej bazie testowej.
Próby obejmowały logowanie, role, sceny, walidację formularza gościa, zapis gościa
i rezerwacji, odczyt rozliczenia, wykrycie dezaktywowanej sesji, a także scenariusz
pobytu z płatnościami, wycofanie nieudanych zmian i dwie konkurencyjne rezerwacje.
Skrypt `.bat` przygotowano dla Windows; nie był uruchamiany na Windows w tym środowisku.
Źródła prób znajdują się w `tests`. Nie uruchamiaj ich na własnej bazie — zmieniają fikcyjne dane testowe.

## Biblioteki

JavaFX 17.0.18 i MySQL Connector/J 9.2.0. Maven dobiera platformę JavaFX automatycznie.
Dołączone oryginalne pliki JAR zachowują metadane i informacje licencyjne dostawców.
