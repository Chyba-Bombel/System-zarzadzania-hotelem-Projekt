# Historia zmian rezerwacji

## 1. Zakres

Dziennik jednej rezerwacji pokazuje czas serwera MySQL, autora i dane przed/po. Ma 12 typów: UTWORZENIE, ZMIANA_TERMINU, ZMIANA_POKOJU, ZAMELDOWANIE, WYMELDOWANIE, ANULOWANIE, WPLATA, ZWROT, DODANIE_USLUGI, ANULOWANIE_USLUGI, ZMIANA_NOTATKI i ZMIANA_UCZESTNIKOW.

Zmiany pobytu pokazują pokój, daty, status, liczbę osób i stawkę. Płatności: ID, kwotę, metodę oraz wpłaty netto przed/po. Usługi: historyczną pozycję i wartość. Notatki i uczestnicy: poprzednią oraz nową treść/listę. Identyczne dane nie tworzą pozornych zmian.

Historia nie obejmuje edycji kartoteki gościa, cennika, kont ani stanu pokoju. Nie jest rejestrem błędnych logowań i odrzuconych operacji. Nie zapisuje haseł, telefonów ani e-maili; nie wpisuj ich do notatek.

## 2. Aktualizacja

Istniejąca baza: zamknij stanowiska, wykonaj kopię i uruchom **`db/aktualizuj_baze.sql`** jako administrator MySQL/MariaDB. Skrypt obejmuje wszystkie dotychczasowe zmiany, więc nie wybierasz aktualizacji według etapów. Nowa, pusta instalacja: **`db/nowa_baza.sql`**. Zachowaj własny `db.properties`; pełnego importu demonstracyjnego nie wykonuj na swoich danych. Szczegóły: [db/README.md](db/README.md).

## 3. Stare płatności

Migracja dopisuje wpłaty i zwroty z tabeli platnosci: czas transakcji, pracownika, kwotę, metodę i ID. Login kopiuje z aktualnej kartoteki pracownika przy migracji, ponieważ ledger nie ma dawnego brzmienia loginu. Wpis jest wyraźnie oznaczony jako odtworzony; nie wymyśla poprzedniego salda. Unikalny platnosc_id zapobiega duplikatom po ponownej migracji.

Dawnych zmian terminu, pokoju, notatek i osób bez ewidencji nie da się odtworzyć. Nowe płatności zapisują pełne stany netto i login z chwili operacji. Zmiany wykonane bezpośrednio przez Workbench lub dawny program nie są automatycznie logowane.

## 4. Obsługa

Rezerwacje → zaznacz pobyt → Historia. Tabela ma do 100 wpisów; wybierz wiersz, by odczytać oba stany. Starsze wpisy pobiera następną stronę, Najnowsze / odśwież wraca do początku. Konsola: 25, ID pobytu, starsze strony potwierdzaj TAK.

Podgląd jest dostępny recepcji i administratorowi. Historia zawiera wewnętrzne notatki i nazwiska, dlatego nie trafia do raportu ani potwierdzenia dla gościa.

## 5. Tabela i relacje

| Pole | Znaczenie |
| --- | --- |
| id | Unikalne ID; rozstrzyga kolejność przy tym samym czasie. |
| rezerwacja_id | Klucz obcy do pobytu. |
| pracownik_id | Klucz obcy do autora. |
| login_pracownika | Kopia loginu. Oznaczone stare płatności mają login z chwili migracji. |
| platnosc_id | Opcjonalny unikalny klucz obcy do płatności, NULL dla innych akcji. |
| akcja | Jeden z 12 typów kontrolowanych CHECK. |
| przed, po | Tekstowe migawki; przed jest NULL przy utworzeniu i odtworzeniu starej płatności. |
| utworzono | DATETIME(6), czas serwera lub czas starej transakcji. |

Klucze obce mają ON DELETE RESTRICT. Indeks (rezerwacja_id,utworzono,id) wspiera kolejność czasu i ID malejąco. Nie sortujemy wyłącznie po ID: stara płatność zaimportowana teraz ma większe ID, lecz starszą datę.

## 6. Kod

HistoriaDAO.stan odczytuje stan pobytu; zapisz dopisuje jego zmianę, a opis przyjmuje migawki właściwe dla płatności, usług, notatek lub osób. DAO operacji używa tego samego Connection, autora z kontekstu sesji i dopiero potem wykonuje commit. Błąd historii uruchamia rollback całej operacji.

Fasada BazaDanych udostępnia chroniony odczyt pobierzHistorieRezerwacji(id,przedId), bez publicznego dopisywania dowolnego zdarzenia i autora. Model: ZmianaRezerwacji, StronaHistorii. Widoki: HistoriaRezerwacjiGUI i HistoriaKonsola.

Kursor 0 pobiera najnowsze. Dalsza strona przekazuje ostatnie ID; DAO ustala jego czas i wybiera starszą datę albo tę samą z mniejszym ID. Pobranie 101. wiersza rozpoznaje dalsze strony. Kursor innego pobytu jest odrzucany. Nowe wpisy na początku nie przesuwają granicy starszych wyników.

## 7. Granice i testy

Interfejs nie edytuje i nie usuwa historii. Minimalne konto MySQL ma SELECT i INSERT; GRANT nie odbiera wcześniejszych szerokich uprawnień. Osoba kontrolująca serwer lub wspólne hasło techniczne z szerszymi prawami może ingerować w dane. To nie jest dziennik kryptograficznie odporny na administratora bazy.

Zmiana pokoju nadal dotyczy całego pobytu, bez odcinków noclegów. HistoryProbe sprawdza dotychczasowe zdarzenia i rollback. ServicesSearchProbe rozszerza weryfikację o finanse, usługi, notatki, uczestników, błędy historii i daty importu. PrivilegeProbe weryfikuje granty, GUI i konsola mają osobne scenariusze. Wyniki: WYNIKI_TESTOW_USLUGI_WYSZUKIWANIE.txt.
