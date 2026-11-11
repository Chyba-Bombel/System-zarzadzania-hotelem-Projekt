# Historia zmian rezerwacji

## 1. Co dodaje ten etap

Dziennik pokazuje, kto i kiedy utworzył rezerwację, zmienił jej termin lub pokój,
zameldował, wymeldował albo anulował pobyt. Każdy wpis przechowuje stan przed
zmianą i po niej: numer i ID pokoju, daty, status, liczbę osób oraz stawkę za noc.
Nie zapisuje haseł, telefonów ani adresów e-mail. Edycja kontaktu gościa, listy
uczestników i operacje płatnicze nie są zdarzeniami tego dziennika; wpłaty i zwroty
nadal mają własną ewidencję w tabeli `platnosci`.

## 2. Uruchomienie na istniejącej bazie

1. Wykonaj kopię swojej bazy.
2. W MySQL Workbench, jako administrator, otwórz i wykonaj
   `db/aktualizacja_historia.sql` na swoim schemacie. Dla innej nazwy zmień `USE`.
3. Nadaj kontu technicznemu odczyt i dopisywanie tabeli. Dla standardowych dwóch
   kont projektowych wykonaj `db/uprawnienia_historia.sql`. Jeśli używasz innego
   loginu, hosta lub schematu, dostosuj polecenia. Nie twórz nieużywanych kont.
   Jeżeli masz tylko jedno z wymienionych kont, wykonaj tylko pasujący `GRANT`.
4. Rozpakuj nowy program do osobnego folderu, przenieś własny `db.properties`.
5. Uruchom program i wykonaj zmianę testowej rezerwacji, następnie otwórz historię.

Nowa instalacja z `HotelManager_baza.sql` zawiera tabelę i uprawnienia.
Nie wykonuj pełnego importu na dotychczasowych rekordach. Migracja historii nie
zastępuje wymaganych wcześniej migracji bezpieczeństwa ani dodatkowych gości.

## 3. Ekran i konsola

W GUI zaznacz rezerwację i kliknij **Historia**. Tabela pokazuje najnowsze wpisy.
Po zaznaczeniu wiersza poniżej odczytasz oba stany. **Starsze wpisy** pobiera
kolejną stronę, **Najnowsze / odśwież** wraca do początku. **Zamknij** kończy podgląd.
W konsoli wybierz **25**, podaj ID rezerwacji; starsze strony potwierdzaj słowem `TAK`.
Odczyt jest dostępny dla recepcji i administratora po sprawdzeniu bieżącej sesji.

## 4. Baza i relacje

| Pole | Znaczenie |
| --- | --- |
| `id` | Rosnący identyfikator wpisu, porządek wyświetlania. |
| `rezerwacja_id` | Klucz obcy do jednej rezerwacji; rezerwacja ma wiele zdarzeń. |
| `pracownik_id` | Klucz obcy do autora operacji, pobrany z uwierzytelnionej sesji. |
| `login_pracownika` | Login zapisany w momencie zdarzenia; późniejsza zmiana konta nie przepisuje tej wartości. |
| `akcja` | Jeden z sześciu dozwolonych rodzajów zdarzeń. |
| `przed`, `po` | Tekstowe kopie poprzednich i nowych danych; `przed` jest NULL przy utworzeniu. |
| `utworzono` | Data i czas serwera MySQL z dokładnością do mikrosekund. |

Klucze obce mają `ON DELETE RESTRICT`. Program dezaktywuje konta, zamiast usuwać
pracowników powiązanych z historią. Indeks `(rezerwacja_id, id)` wspiera strony.

## 5. Przebieg transakcji i klasy

DAO wykonujące operację blokuje odpowiednie wiersze zgodnie z dotychczasowymi
regułami. `HistoriaDAO.stan(connection, id)` odczytuje stan przed zmianą.
Po wykonaniu modyfikacji `HistoriaDAO.zapisz(connection, id, akcja, przed)` odczytuje
nowy stan, pobiera tożsamość z kontekstu i dopisuje wpis przez to samo połączenie.
Dopiero potem wykonywany jest `commit()`. Błąd historii uruchamia dotychczasowy
`rollback()`, obejmujący rezerwację, ewentualne sprzątanie i zdarzenie.

Dla utworzenia poprzedni stan jest NULL. Powtórzenie identycznego terminu nie
tworzy pozornej zmiany, bo oba stany są równe. Odrzucone operacje nie są wpisami
historii — to dziennik zatwierdzonych zmian, a nie rejestr prób i błędów logowania.

Publiczna fasada `BazaDanych` udostępnia `pobierzHistorieRezerwacji(id, przedId)`.
Nie udostępnia dopisywania wpisów przez formularz ani parametrów autora zdarzenia.
`ZmianaRezerwacji` i `StronaHistorii` są niezmiennymi rekordami modelu.
`HistoriaRezerwacjiGUI` odpowiada za okno, a `HistoriaKonsola` za opcję 25.

Strona zawiera do 100 wpisów. Zapytanie pobiera 101. wiersz, aby rozpoznać, czy
są starsze. Następna strona używa `id < ostatnieId`, więc nowy wpis na początku
nie przesuwa granicy starszych wyników. Wartości trafiają do SQL przez parametry.

## 6. Ograniczenia

Historia zaczyna działać po aktualizacji programu i bazy. Nie odtwarza dawnych
zmian i nie dopisuje sztucznych autorów do rekordów demonstracyjnych.
Starsze rezerwacje mogą mieć pustą historię aż do pierwszej nowej operacji.
Bezpośrednie zmiany przez Workbench lub starszą wersję programu nie są logowane.

To dziennik zmian, a nie dokładna historia noclegów w kolejnych pokojach.
Zmiana `pokoj_id` nadal dotyczy całej rezerwacji w kalendarzu i raportach.
Nie zmieniono sposobu liczenia obłożenia i należności.

Interfejs nie umożliwia edycji ani usuwania wpisów. Nowy profil minimalnych
uprawnień przyznaje tylko SELECT i INSERT do historii. Dodatkowy skrypt GRANT
nie odbiera wcześniejszych szerokich uprawnień; sprawdź je osobno, jeżeli konto
miało pełny dostęp do schematu. Administrator MySQL albo osoba ze wspólnym
hasłem JDBC i odpowiednimi grantami może zmienić dziennik. Nie jest to rejestr
kryptograficznie zabezpieczony przed ingerencją osoby kontrolującej serwer.

## 7. Sprawdzenie

`HistoryProbe` weryfikuje autorów, sześć zdarzeń, obie kopie danych, pomijanie
pozornych zmian i odmów, rollback przy wymuszonym błędzie oraz ponad 100 wpisów.
`PrivilegeProbe` wykonuje skrypt grantów na tymczasowych kontach i sprawdza zakaz
UPDATE/DELETE historii. GUI i konsola mają scenariusze odczytu historii.
Migracja została sprawdzona dwukrotnie na poprzednim schemacie z porównaniem
wszystkich istniejących rekordów. Polecenia uruchomienia są w `TESTOWANIE.md`.
Źródła pozostają bez komentarzy; wyjaśnienia znajdują się w dokumentacji.
