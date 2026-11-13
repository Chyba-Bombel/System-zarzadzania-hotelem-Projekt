# Wyłączanie i przywracanie pokoi

## 1. Obsługa w GUI

1. Zaloguj się jako administrator i otwórz **Pokoje**.
2. Zaznacz pokój. **Wyłącz pokój** ustawi WYLACZONY, **Przywróć pokój** ustawi DO_SPRZATANIA.
3. Program odświeża dane i sprawdza aktywne rezerwacje. Jeżeli są, pokaże ich numery i zatrzyma operację.
4. Sprawdź opis w oknie i zatwierdź przyciskiem operacji. Anulowanie zamyka okno bez zmian.
5. Po przywróceniu przygotuj pokój, zaznacz go i wybierz **Oznacz jako posprzątany**.

Operacja sprzątania jest dostępna również recepcji, ale wyłączanie i przywracanie
wymagają administratora. Ukrycie przycisków uzupełnia sprawdzenie roli w fasadzie bazy.

## 2. Reguły stanów

| Stan początkowy | Operacja | Wynik |
| --- | --- | --- |
| GOTOWY lub DO_SPRZATANIA | Wyłączenie przez administratora | WYLACZONY, jeśli brak aktywnych rezerwacji. |
| WYLACZONY | Przywrócenie przez administratora | DO_SPRZATANIA, jeśli brak aktywnych rezerwacji. |
| DO_SPRZATANIA | Oznaczenie sprzątania przez recepcję lub administratora | GOTOWY, jeśli nie ma zameldowanego pobytu. |
| WYLACZONY | Próba oznaczenia sprzątania | Odmowa; najpierw potrzebne jest przywrócenie. |

POTWIERDZONA oraz ZAMELDOWANA oznaczają aktywną rezerwację, niezależnie od dat.
Także rezerwacja na odległy termin blokuje wyłączenie, bo nie jest to wyłączenie
czasowe. Nierozliczony lub zaległy pobyt nadal trzeba obsłużyć zgodnie z istniejącymi
zasadami. Nowa funkcja nie anuluje, nie przenosi ani nie rozlicza rezerwacji samodzielnie.

Przywrócenie nie oznacza, że pokój został przygotowany. DO_SPRZATANIA pozwala
rezerwować dostępny termin zgodnie z wcześniejszymi regułami, ale zameldowanie
wymaga GOTOWY. Cena, numer, typ i pojemność pokoju pozostają bez zmian.
Anulowane oraz zakończone pobyty nie blokują operacji i nie są usuwane.

## 3. Obsługa w konsoli

Administrator wybiera **28**, wpisuje numer pokoju, następnie:

- **1** — wyłączenie;
- **2** — przywrócenie do sprzątania;
- **0** — powrót.

Zmiana wymaga potwierdzenia `TAK`. Inna odpowiedź pozostawia stan bez zmian.
Recepcja nie widzi pozycji menu, a ręczne wpisanie 28 również nie daje uprawnień.
Sprzątanie pozostaje pod opcją 16.

## 4. Klasy i zapis

| Klasa | Odpowiedzialność |
| --- | --- |
| `BazaDanych` | Sprawdza rolę ADMIN i deleguje operację. |
| `DostepnoscPokoiDAO` | Transakcja READ_COMMITTED, blokada wiersza pokoju, weryfikacja stanu i aktywnych rezerwacji, UPDATE. |
| `StanPokojuGUI` | Odświeżenie danych i opis zmiany przed zatwierdzeniem; praca w tle. |
| `StanPokojuKonsola` | Obsługa opcji 28 i odświeżenie list po zapisie. |
| `PokojeDAO` | Dotychczasowe oznaczanie sprzątania. |
| `Pokoj` | Dotychczasowy model; zwracany również po zmianie stanu. |

DAO najpierw blokuje pokój przez `SELECT ... FOR UPDATE`. Porównuje stan z tym,
który administrator widział przy przygotowaniu operacji. Różnica oznacza odmowę
i konieczność odświeżenia. Nie jest to pełny licznik wersji; kontrolowany jest stan pokoju.

Następnie sprawdza aktywne rezerwacje i zmienia wyłącznie kolumnę `stan`.
`commit()` zatwierdza całość, błąd uruchamia `rollback()`. Wszystkie wartości
przekazywane są do zapytań przez parametry.

Tworzenie rezerwacji i przenoszenie pobytu również blokują pokój przed zapisem.
Dlatego sprawdzenie dostępności nie zostaje oddzielone od samej zmiany:
przy równoczesnym wyłączeniu i nowej rezerwacji tylko jedna operacja może się udać.
Ostateczną decyzję podejmuje DAO, nie sam formularz.

## 5. Zakres i ograniczenia

To zmiana bieżącego stanu, bez dat początku i końca remontu. Nie rezerwuje się
okresu wyłączenia i nie ma osobnego dziennika stanów pokoju ani powodu wyłączenia.
Dziennik rezerwacji nadal dotyczy zdarzeń pobytów. Kalendarz pokazuje bieżący stan
pokoju i nie odtwarza historycznych okresów wyłączenia.

Zasady raportów nie zostały zmienione: obłożenie odnosi się do wszystkich pokoi
z aktualnej kartoteki, także wyłączonych. Nie odejmuje się historycznej liczby
niedostępnych pokoi. Wyłączenie nie usuwa rezerwacji ani zapisanych płatności.

## 6. Aktualizacja i sprawdzenie

Istniejąca baza: zamknij stanowiska, wykonaj kopię i uruchom **`db/aktualizuj_baze.sql`** jako administrator MySQL/MariaDB. Skrypt obejmuje wszystkie dotychczasowe zmiany, więc nie wybierasz aktualizacji według etapów. Nowa, pusta instalacja: **`db/nowa_baza.sql`**. Zachowaj własny `db.properties`; pełnego importu demonstracyjnego nie wykonuj na swoich danych. Szczegóły: [db/README.md](db/README.md).

Weryfikację funkcji opisuje TESTOWANIE.md oraz odpowiedni plik WYNIKI_TESTOW.

