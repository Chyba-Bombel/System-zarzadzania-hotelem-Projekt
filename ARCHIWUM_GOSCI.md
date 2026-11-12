# Archiwizowanie gości

## 1. Działanie

Archiwizacja ukrywa nieużywaną kartotekę z aktywnej listy i wyboru gościa przy nowej rezerwacji. Nie usuwa danych kontaktowych, rezerwacji, uczestników, płatności ani historii. Archiwalną kartotekę można przywrócić. Nie jest to anonimizacja ani trwałe usunięcie danych.

Recepcja i administrator mogą archiwizować i przywracać gości. Potwierdzona lub zameldowana rezerwacja głównego gościa blokuje archiwizację, niezależnie od dat. Najpierw zakończ lub anuluj taki pobyt zgodnie z zasadami płatności. Zakończone i anulowane rezerwacje nie blokują archiwizacji.

Dodatkowi goście rezerwacji są zapisani jako imię i nazwisko, bez relacji do kartoteki `goscie`. Archiwizowanie kartoteki nie zmienia tych tekstów i nie stanowi zakazu dodania osoby o takim samym nazwisku do listy uczestników.

## 2. Aktualizacja bazy

1. Zamknij program na wszystkich stanowiskach i wykonaj kopię bazy.
2. Rozpakuj paczkę do nowego folderu, zachowaj własny `db.properties`.
3. Jako administrator MySQL/MariaDB wykonaj `db/aktualizacja_archiwum_gosci.sql`.
4. Jeśli schemat ma inną nazwę, dostosuj `USE hotel_manager`.
5. Uruchom aplikację i sprawdź zakładkę Goście.

W phpMyAdmin wybierz bazę i zaimportuj plik lub wykonaj jego całą zawartość w SQL. W Workbench otwórz plik i wykonaj cały skrypt. Na Linuxie, z głównego folderu projektu:

```sh
mysql -u root -p < db/aktualizacja_archiwum_gosci.sql
```

Migracja dodaje trzy pola i indeks, nie zmienia starych kartotek w archiwalne. Można uruchomić ją ponownie: istniejące kolumny i metadane pozostają. DDL w MySQL wykonuje niejawne zatwierdzenia; przerwaną migrację należy dokończyć przed uruchomieniem programu. Standardowe konto aplikacji ma już SELECT/UPDATE do gości, więc nie potrzebuje nowych grantów.

Jeżeli poprzednia aktualizacja usług i historii została przerwana, najpierw dokończ ją poprawionym `db/aktualizacja_uslugi_historia.sql`, a następnie wykonaj brakujące `db/uprawnienia_uslugi.sql`. W paczce poprawiono rozróżnienie DROP CHECK w MySQL i DROP CONSTRAINT w MariaDB. Wspólną blokadę odczytu katalogu zapisano jako LOCK IN SHARE MODE. Nie wykonuj pełnego importu demonstracyjnego na swoich danych.

Dla nowej instalacji `db/HotelManager_baza.sql` zawiera już wszystkie pola. Starsze wymagane migracje nadal obowiązują, jeśli ich nie wykonano.

## 3. Obsługa GUI

W zakładce **Goście** wybierz filtr:

| Filtr | Zawartość |
| --- | --- |
| Aktywni | Domyślny widok bieżących kartotek. |
| Archiwalni | Kartoteki ukryte, z czasem i autorem archiwizacji. |
| Wszyscy | Obie grupy, z oznaczeniem stanu. |

Filtr łączy się z dotychczasowym wyszukiwaniem nazwiska, telefonu, e-maila i ID. Kolumny Kartoteka, Archiwizowano i Archiwizował pokazują stan; przy węższym oknie przewiń tabelę poziomo.

Zaznacz aktywnego gościa i kliknij **Archiwizuj**. Formularz wyjaśnia skutek operacji; dopiero przycisk zatwierdzenia zapisuje zmianę. Po sukcesie gość znika z widoku Aktywni. Przełącz na Archiwalni lub Wszyscy, aby go odszukać.

Zaznacz archiwalnego gościa i kliknij **Przywróć**. Po zatwierdzeniu wróci do aktywnych kartotek i wyboru w nowej rezerwacji. Zamknięcie formularza bez zatwierdzenia nie zapisuje operacji.

Edycja kontaktów archiwalnej kartoteki jest zablokowana. Najpierw ją przywróć. Baza również sprawdza tę regułę, więc nieaktualny formularz nie zapisze edycji po archiwizacji z innego stanowiska. Nowa rezerwacja ponownie sprawdza stan gościa w transakcji; stary formularz lub obiekt Java z dawnym stanem nie omija blokady.

Po zmianie na innym stanowisku użyj **Odśwież dane**. Po błędzie sieci sprawdź stan kartoteki przed ponowieniem zapisu.

## 4. Obsługa konsoli

| Opcja | Działanie |
| --- | --- |
| 32 | Archiwizacja aktywnego gościa; ID i potwierdzenie TAK. |
| 33 | Przywrócenie archiwalnego gościa; ID i potwierdzenie TAK. |
| 34 | Aktualna lista archiwalnych kartotek, dat i autorów. |

Opcje 1 i 3 domyślnie pokazują i wyszukują aktywnych gości. Przy tworzeniu rezerwacji program pobiera aktualną listę i odrzuca archiwalny wybór. Wspólna lista danych nadal zawiera kartoteki archiwalne, aby można było odczytać stare rezerwacje.

## 5. Dane i relacje

| Pole `goscie` | Znaczenie |
| --- | --- |
| archiwizowany | BOOLEAN, domyślnie FALSE. |
| archiwizowano | Czas ostatniego przejścia do bieżącego archiwum, DATETIME(6). |
| archiwizowal_pracownik_id | Klucz obcy do zalogowanego autora, ON DELETE RESTRICT. |

Przywrócenie ustawia FALSE i czyści bieżący czas oraz autora. Nie tworzy się osobnego trwałego dziennika archiwizowania/przywracania. Login wyświetlany w tabeli pochodzi z kartoteki wskazanego pracownika, nie z kopii historycznego loginu. Historia rezerwacji pozostaje niezmieniona i nadal obejmuje zdarzenia pobytów.

`goscie` nadal ma relację 1:N do `rezerwacje`. Indeks `(gosc_id,status)` przyspiesza sprawdzanie aktywnych pobytów. Żadne zapytanie odczytu historii, karty, raportu i rezerwacji nie odrzuca archiwalnego głównego gościa. Agregaty finansowe i obłożenie nie zmieniają się przez samą archiwizację.

## 6. Klasy i transakcje

- `Gosc`: stan, czas i login autora; dotychczasowy konstruktor tworzy aktywny model.
- `GoscieDAO`: odczytuje także metadane; edycja ma warunek archiwizowany=FALSE.
- `ArchiwumGosciDAO`: blokada kartoteki, weryfikacja aktywnych rezerwacji, archiwizacja i przywrócenie.
- `BazaDanych`: publiczne archiwizujGoscia i przywrocGoscia, po sprawdzeniu sesji.
- `TworzenieRezerwacjiDAO`: pod blokadą pokoju blokuje i sprawdza aktualną kartotekę gościa przed zapisem.
- `EkranGosci`, `ArchiwumGosciGUI`, `TabeleGUI`: filtry, przyciski, potwierdzenie i kolumny.
- `ArchiwumGosciKonsola`: opcje 32–34.

Archiwizacja używa READ_COMMITTED i SELECT gościa FOR UPDATE. Pod tą blokadą sprawdza aktywne pobyty i zapisuje stan, czas oraz rzeczywistego autora z sesji. Nowa rezerwacja blokuje najpierw pokój, następnie tę samą kartotekę. Jeśli archiwizacja wygra, nowy zapis odczyta stan archiwalny i zostanie odrzucony. Jeśli rezerwacja wygra, archiwizacja zobaczy aktywny pobyt i zostanie odrzucona. Sprawdzenie aktywnych rezerwacji w archiwizacji jest odczytem bez blokowania ich wierszy, co nie odwraca kolejności blokad.

Nie ma usuwania rekordów ani możliwości wskazania cudzego autora archiwizacji w API. Konto i rola są sprawdzane tak jak przy pozostałych operacjach kartoteki. Źródła pozostają bez komentarzy.

## 7. Weryfikacja

`GuestArchiveProbe` sprawdza autoryzację, metadane autora, blokady aktywnych pobytów, przywrócenie, stare formularze, zachowanie historii i finansów, rollback błędu oraz równoczesne tworzenie rezerwacji. `PrivilegeProbe` sprawdza oba zapisy na minimalnych grantach.

`GuestArchiveGuiSteps` w `GuiSmoke` weryfikuje filtry, formularze, anulowanie bez zapisu, brak archiwalnego gościa w nowej rezerwacji i przywracanie przez recepcję. Konsola ma scenariusze 32–34. Migracja jest wykonywana dwukrotnie na poprzednim schemacie z porównaniem rekordów.

Wynik bieżącego przebiegu: `WYNIKI_TESTOW_ARCHIWUM_GOSCI.txt`. Polecenia testowania: `TESTOWANIE.md`. Testy bazodanowe wykonano na MySQL 8; nie zastępują pełnej weryfikacji na działającym serwerze MariaDB. Testy zmieniają dane i wymagają osobnego schematu z fikcyjnymi rekordami.
