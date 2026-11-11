# Potwierdzenie danych rezerwacji

## 1. Obsługa w GUI

1. Zaloguj się jako recepcja lub administrator.
2. Otwórz **Rezerwacje**, zaznacz pobyt i kliknij **Potwierdzenie**.
3. Sprawdź zakładki **Pobyt**, **Goście** i **Rozliczenie**.
4. Po zmianie danych na innym stanowisku lub dopisaniu wpłaty kliknij **Odśwież dane**.
5. Kliknij **Zapisz HTML**, wybierz folder i nazwę. Zastąpienie istniejącego pliku wymaga potwierdzenia.
6. Otwórz plik w przeglądarce, naciśnij **Ctrl+P** i wybierz drukarkę lub zapis jako PDF.

Program zapisuje HTML, nie plik PDF. Nie potrzebuje nowej biblioteki PDF ani
modułu JavaFX WebView. Wygląd drukowania zależy od przeglądarki i jej ustawień;
dokument ma style dla formatu A4. Dane nie są wysyłane pocztą ani do internetu.

## 2. Obsługa w konsoli

Wybierz **26**, podaj ID rezerwacji i sprawdź wyświetlone dane. Wpisz ścieżkę,
np. `rezerwacja_42.html`. Brakujące rozszerzenie `.html` zostanie dopisane.
Jeśli plik istnieje, wpisz `TAK`, aby go zastąpić. Inna odpowiedź pozostawia plik.
Folder docelowy musi istnieć; program nie tworzy całego drzewa katalogów.
Przed zapisem ponownie sprawdza aktywność sesji.

## 3. Dane i zasady

Dokument pokazuje numer rezerwacji, status, głównego gościa, pokój i typ,
termin, liczbę nocy i osób, listę uczestników oraz kwoty rozliczenia.
Telefon i e-mail gościa oraz dane kont pracowników nie są eksportowane.
Brakujące nazwiska w starszych pobytach są oznaczane komunikatem o brakujących danych.

Cena noclegów = liczba nocy × zapisana cena pokoju za noc. Liczba gości nie mnoży ceny.
Zmiana cennika ani przeniesienie pokoju nie zmienia uzgodnionej stawki rezerwacji.
Wpłaty i zwroty są sumowane osobno; saldo = należność − (wpłaty − zwroty).
Dodatnie saldo oznacza kwotę do zapłaty, ujemne kwotę do zwrotu.

Anulowana rezerwacja ma należność równą zero, widoczne kwoty z ewidencji
oraz wyraźny komunikat archiwalny. Zakończony pobyt także jest oznaczony jako
archiwalny. Taki dokument nie potwierdza aktywnego przyszłego pobytu.
Dokument nie jest fakturą ani bankowym dowodem płatności.

## 4. Podgląd a nowe zmiany

Przy otwarciu i odświeżeniu dane są pobierane z MySQL, a nie z pamięci tabeli GUI.
Podgląd jest niezmienną kopią. Eksport zapisuje dokładnie tę kopię, z widocznym
czasem serwera z chwili pobrania danych. Zapis sam nie podmienia treści dokumentu
na dane, których użytkownik nie widział. Po późniejszej zmianie lub wpłacie odśwież
podgląd i przygotuj nowy plik. Dawniej zapisany HTML nie aktualizuje się automatycznie.

## 5. Klasy i transakcja odczytu

| Klasa | Zadanie |
| --- | --- |
| `BazaDanych` | Chroniona metoda `pobierzPotwierdzenieRezerwacji(id)`. |
| `PotwierdzeniaDAO` | Rezerwacja, dane pokoju i gościa, uczestnicy oraz sumy płatności. |
| `PotwierdzenieRezerwacji` | Niezmienny rekord modelu, własna kopia listy i obliczanie kwot. |
| `Potwierdzenia` | Podpisy, formatowanie, kodowanie tekstu HTML i generowanie dokumentu. |
| `ZapisPliku` | Wspólny zapis plików UTF-8 dla raportów i potwierdzeń. |
| `PotwierdzenieGUI` | Podgląd w trzech zakładkach i wybór ścieżki eksportu. |
| `PotwierdzenieKonsola` | Odpowiednik obsługi w opcji 26. |

DAO używa jednego połączenia, transakcji tylko do odczytu i izolacji REPEATABLE_READ.
Pierwsze zapytanie ustala obraz danych dla kolejnych odczytów uczestników i płatności.
Zmiany zatwierdzone równocześnie przez inne połączenie nie mieszają starych i nowych
wartości wewnątrz jednego dokumentu. Nie ma blokad FOR UPDATE ani modyfikacji bazy.

Odczyt nie dodaje wpisu do historii. Tożsamość i aktywność pracownika sprawdza fasada.
Zapytania używają parametrów; minimalne konto techniczne ma już wymagany SELECT.

## 6. HTML i zapis pliku

Dane tekstowe są kodowane: znaki `<`, `>`, `&`, cudzysłowy i apostrofy nie tworzą
znaczników ani atrybutów. Dokument nie korzysta ze skryptów, zewnętrznych obrazów,
czcionek czy arkuszy. Polityka CSP dodatkowo ogranicza zasoby do lokalnego stylu.
Nazwa sugerowanego pliku zawiera wyłącznie ID oraz datę, bez nazwisk.

Zapis najpierw tworzy plik tymczasowy w folderze docelowym, następnie przenosi go
na właściwą nazwę. Używa ATOMIC_MOVE, jeśli system to obsługuje, a w przeciwnym
razie zwykłego przeniesienia z zastąpieniem. Plik tymczasowy jest sprzątany po błędzie.
Pełna atomowość nie jest gwarantowana w systemach plików bez obsługi ATOMIC_MOVE.

## 7. Aktualizacja i testy

Nie ma nowej migracji, zależności ani konfiguracji. Jeśli działa poprzednia wersja
z historią, rozpakuj paczkę do osobnego folderu i przenieś własny `db.properties`.
Dla starszych baz nadal obowiązują wcześniejsze migracje. Nie importuj ponownie danych demo.

`ConfirmationProbe` sprawdza kwoty, role, uczestników, anulowanie, kodowanie HTML,
błędy zapisu i spójność równoczesnego odczytu. `GuiSmoke` sprawdza podgląd i jego
odświeżenie po wpłacie. Konsola ma scenariusz zapisu i odmowy zastąpienia pliku.
Wynik bieżącej regresji jest w `WYNIKI_TESTOW_POTWIERDZENIA.txt`.
Źródła pozostają bez komentarzy; opis działania znajduje się w dokumentacji.
