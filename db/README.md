# Baza danych — wybierz jeden scenariusz

| Sytuacja | Co uruchomić |
| --- | --- |
| Masz bazę z wcześniejszej wersji projektu | **aktualizuj_baze.sql** |
| Instalujesz projekt po raz pierwszy i nie masz jego bazy | **nowa_baza.sql** |

Nie wykonuj obu plików kolejno na używanej bazie. `nowa_baza.sql` tworzy bazę z rekordami demonstracyjnymi; nie służy do aktualizacji istniejącego hotelu.

## Mam już bazę

1. Zamknij aplikację na wszystkich stanowiskach i wykonaj kopię bazy.
2. W phpMyAdmin, Workbench albo kliencie MySQL połącz się jako administrator.
3. Otwórz i wykonaj **cały aktualizuj_baze.sql**.
4. Zachowaj własny db.properties i uruchom nową wersję aplikacji.

Na Linuxie, z głównego folderu projektu:

```sh
mysql -u root -p < db/aktualizuj_baze.sql
```

Skrypt obejmuje bezpieczeństwo logowania, dodatkowych gości, historię, usługi oraz archiwizację. Sprawdza istnienie kolumn, indeksów i ograniczeń. Zachowuje stare rekordy i obecny stan kartotek. Stare płatności dopisuje do historii tylko wtedy, gdy nie mają jeszcze powiązanego wpisu. Przykładowe oferty usług dodaje bez nadpisywania istniejących.

Można uruchomić go ponownie także po częściowo wykonanej wcześniejszej aktualizacji. DDL nie jest jedną transakcją: po błędzie nie uruchamiaj programu, tylko popraw przyczynę i dokończ cały skrypt. Kopia bazy pozostaje konieczna. Aktualizacja zakłada istniejące podstawowe tabele projektu: pracownicy, goscie, pokoje, rezerwacje i platnosci. Nie jest importerem dowolnego obcego schematu.

Dla innej nazwy bazy zmień jedyne `USE hotel_manager` na początku aktualizacji. Skrypt sam używa aktualnego schematu przy nadawaniu praw.

## Instaluję od zera

Wykonaj **nowa_baza.sql** jednorazowo, jako administrator, na serwerze bez bazy tego projektu:

```sh
mysql -u root -p < db/nowa_baza.sql
```

Powstaną tabele, przykładowi goście/pokoje/rezerwacje, trzy usługi oraz konta demonstracyjne. Użyj konfiguracji z db.properties.example. Dla własnej nazwy schematu zastąp `hotel_manager` w całym pliku przed wykonaniem.

Nowa instalacja zawiera już wszystkie aktualizacje. Nie musisz dodatkowo wykonywać aktualizuj_baze.sql. Hasła istniejących kont technicznych nie są resetowane: jeśli hotel_app istnieje już na serwerze, konfiguracja musi używać jego aktualnego hasła. Skrypt nie jest przeznaczony do ponownego importu na istniejące tabele.

## Uprawnienia

Zwykła aktualizacja sama dodaje potrzebne prawa **istniejącym** kontom hotel_app@localhost i hotel_app@127.0.0.1. Nie tworzy brakującego konta i nie zmienia hasła. Brak jednego z hostów nie przerywa aktualizacji. Wymagane jest konto administracyjne mające również prawo GRANT i odczyt mysql.user.

Jeśli używasz własnego konta technicznego, jednorazowo dostosuj `administracja/uprawnienia.sql`: nazwę bazy oraz zmienne @konto i @host. Wykonaj po aktualizacji, osobno dla każdego używanego hosta. Skrypt nie tworzy użytkownika ani nie ustala hasła; pominięcie nieistniejącego konta jest sygnalizowane wynikiem SQL.

`administracja/ogranicz_uprawnienia.sql` to opcjonalne narzędzie dla administratora: odbiera i odtwarza prawa standardowych kont projektu. Nie należy do zwykłej instalacji ani aktualizacji. Nie uruchamiaj go dla wspólnego użytkownika lub innego schematu bez świadomego dostosowania.

Samo dodawanie GRANT nie odbiera wcześniejszych szerokich uprawnień. Aplikacji nie uruchamiaj na koncie administratora MySQL.

## Co się zmieniło w folderze

Poszczególne pliki aktualizacji z etapów zostały scalone do jednego aktualizuj_baze.sql. Nie trzeba szukać ich kolejności ani pamiętać, co już wykonano. Rozpakuj nową paczkę do osobnego folderu, żeby nie pozostały w nim pliki z poprzedniej paczki. Nie usuwaj przy tym swojej bazy ani prywatnej konfiguracji.

Skrypty nie zawierają poleceń SOURCE ani DELIMITER; można wkleić całą zawartość w phpMyAdmin. Dobór DROP CHECK/DROP CONSTRAINT jest zależny od serwera. Testy wydania opisano w WYNIKI_TESTOW_BAZA.txt w głównym folderze projektu.
