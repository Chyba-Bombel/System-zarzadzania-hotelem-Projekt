# Notatki recepcji do rezerwacji

## 1. Zastosowanie

Notatka przechowuje krótkie uwagi organizacyjne, np. późny przyjazd, prośbę
odnoszącą się do pokoju lub ustalenie z gościem. Jedna rezerwacja ma jedną aktualną
notatkę. Tekst może mieć wiele wierszy, do 500 znaków Unicode.

Uwagi są widoczne dla zalogowanej recepcji i administratora. Nie są częścią
potwierdzenia dla gościa ani raportów HTML/CSV. Wpisane znaczniki HTML i tekst SQL
są zwykłą treścią, nie poleceniami do wykonania.

## 2. Obsługa w GUI

1. Otwórz **Rezerwacje**, zaznacz pobyt i kliknij **Notatka recepcji**.
2. Górne pole **Zapisana notatka** pokazuje ostatnio pobraną treść.
3. W polu **Twój tekst** wpisz zmiany i wybierz **Zapisz notatkę**.
4. Aby usunąć treść, kliknij **Wyczyść pole**, następnie zapisz.
5. **Zamknij** kończy podgląd bez zapisu niezapisanych zmian.

Edycja jest dostępna przy statusach POTWIERDZONA i ZAMELDOWANA. Notatkę pobytu
ANULOWANA albo ZAKONCZONA można odczytać, ale nie zmieniać; dotyczy to również administratora.

**Odśwież zapisaną** aktualizuje górne pole i status rezerwacji. Nie zastępuje szkicu
w dolnym polu. **Użyj zapisanej treści** świadomie zastępuje szkic górną treścią.
Jeżeli podczas edycji pobyt zostanie anulowany lub zakończony, zapis będzie odrzucony,
a odświeżenie przełączy okno do trybu tylko do odczytu.

## 3. Edycja na dwóch stanowiskach

Program porównuje aktualny tekst z treścią pobraną przez edytor. Gdy są różne,
nie nadpisuje nowszej notatki. Komunikat błędu pozostawia Twój szkic w oknie.
Odśwież zapisaną treść, porównaj oba pola, uzupełnij szkic i zapisz go ponownie.

Porównanie rozróżnia wielkie i małe litery oraz białe znaki. Nie polega na kolacji
tekstu SQL. Nie jest to licznik wersji: zmiana tekstu i późniejsze przywrócenie
identycznej wartości nie zostaną odróżnione od braku zmiany. Pole przechowuje tylko
bieżącą treść i nie ma osobnej historii, autora ani czasu edycji.

## 4. Obsługa w konsoli

Wybierz **27**, podaj ID. Program pokaże obecną notatkę, a dla aktywnej rezerwacji:

- **1** rozpoczyna wpisywanie nowej treści w wielu wierszach;
- **2** usuwa treść notatki;
- **0** wraca do menu.

Edycję zakończ linią `:zapisz`, a zrezygnuj linią `:anuluj`.
Aby zapisać linię zaczynającą się dwukropkiem, wpisz na początku `::`.
Zbyt długa linia zostaje odrzucona z komunikatem; możesz wpisać krótszą lub anulować.
Przy konflikcie program wyświetla niezapisany tekst, który można skopiować przed ponowieniem edycji.

## 5. Klasy i baza

| Element | Zadanie |
| --- | --- |
| `NotatkaRezerwacji` | Niezmienny rekord ID, statusu i treści; informacja, czy można edytować. |
| `BazaDanych` | Publiczny odczyt i zapis po sprawdzeniu zalogowanej sesji. |
| `NotatkiDAO` | Parametry SQL, walidacja, blokada rezerwacji i porównanie poprzedniej treści. |
| `NotatkiGUI` | Podgląd, szkic, odświeżanie i zapis w tle. |
| `NotatkiKonsola` | Wprowadzanie wielu wierszy w opcji 27. |
| `rezerwacje.uwagi` | Istniejące pole VARCHAR(500), NOT NULL, domyślnie puste. |

Zapis rozpoczyna transakcję READ_COMMITTED. Zapytanie `SELECT status, uwagi ... FOR UPDATE`
blokuje jedną rezerwację. DAO sprawdza bieżący status i dokładną zgodność starego tekstu
przez `String.equals`. Dopiero potem wykonuje parametryzowany UPDATE, zmieniając wyłącznie
`uwagi`. `commit()` zatwierdza zapis, a błąd powoduje `rollback()`.

Zapis identycznej treści nie wykonuje UPDATE. Nie zmienia terminu, ceny, pokoju,
uczestników, płatności ani pracownika przypisanego do utworzenia rezerwacji.
Nie dopisuje zdarzenia do dotychczasowego dziennika pobytu.

## 6. Walidacja i zakres danych

Limit jest liczony przez `codePointCount`, aby znaki spoza BMP, np. emoji, nie były
obcinane w połowie pary surogatów. Odpowiada to liczbie znaków VARCHAR w MySQL utf8mb4.
Końce linii CRLF i CR są zamieniane na LF. Nowe linie i tabulatory są dozwolone;
pozostałe znaki sterujące oraz niepoprawne surogaty są odrzucane.
Tekst składający się wyłącznie z białych znaków jest zapisywany jako pusty.

Pusta notatka oznacza usuniętą aktualną treść. W tym etapie nie ma osobnego archiwum
notatek ani mechanizmu przywracania wcześniejszych wersji. Istniejące kopie zapasowe
bazy pozostają poza zakresem tej funkcji. Notatka nie jest dodawana do modelu
potwierdzenia ani tabel eksportowanych raportów.

## 7. Aktualizacja i testy

Nie dodano migracji, tabel, uprawnień ani bibliotek. Działająca poprzednia wersja
ma już pole `uwagi`. Rozpakuj nową paczkę do osobnego folderu i przenieś własny
`db.properties`. Dla starszych baz nadal obowiązują wcześniejsze wymagane aktualizacje.
Nie importuj ponownie rekordów demonstracyjnych.

`NotesProbe` sprawdza walidację, emoji, konkurencyjne edycje, statusy i brak uwag
w dokumentach eksportowanych. Testy GUI obejmują zachowanie szkicu po konflikcie
i odczyt archiwum; konsola ma osobne scenariusze edycji. Wyniki bieżącego przebiegu
znajdują się w `WYNIKI_TESTOW_NOTATKI.txt`. Kod pozostaje bez komentarzy.
