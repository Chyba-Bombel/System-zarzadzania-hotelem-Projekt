# Zmiana pokoju istniejącej rezerwacji

## 1. Jak korzystać

1. Zaloguj się jako recepcja lub administrator.
2. Otwórz **Rezerwacje**, zaznacz potwierdzoną albo zameldowaną rezerwację.
3. Kliknij **Zmień pokój**. Przed otwarciem formularza program odświeża dane.
4. Wybierz jeden z pokoi dostępnych przez cały termin pobytu i mieszczących wszystkie osoby.
5. Sprawdź informację o cenie i zakresie zmiany, następnie zapisz.
6. Tabela rezerwacji, pokoje oraz kalendarz są odświeżane po zatwierdzeniu.

W konsoli tę samą operację uruchamia opcja **24**; wybór nowego numeru pokoju
wymaga potwierdzenia słowem `TAK`.

## 2. Zasady

| Warunek | Zachowanie |
| --- | --- |
| Nowy pokój jest za mały, wyłączony lub zajęty | Odmowa bez zmiany danych. |
| Rezerwacja jest anulowana albo zakończona | Odmowa. |
| Wyjazd przypada dzisiaj lub minął | Odmowa; najpierw obsłuż zakończenie pobytu lub popraw termin zgodnie z dostępnymi regułami. |
| Rezerwacja potwierdzona | Możliwy również pokój do sprzątania; przed zameldowaniem trzeba go przygotować. |
| Rezerwacja zameldowana | Nowy pokój musi być gotowy; stary otrzyma stan do sprzątania, chyba że jest wyłączony. |
| Nowy pokój ma inny cennik | Pozostaje cena uzgodniona przy utworzeniu rezerwacji. |
| Rezerwacja ma wpłaty i dodatkowych gości | Płatności i uczestnicy pozostają przy tym samym ID rezerwacji. |
| Inny pracownik już zmienił pokój | Odmowa nieaktualnego żądania; odśwież dane i wybierz ponownie. |
| Wyjazd innego pobytu równa się przyjazdowi tego pobytu | Terminy nie kolidują; data wyjazdu jest granicą wyłączną. |

## 3. Jak działa kod

`BazaDanych.zmienPokojRezerwacji(id, poprzedniPokojId, nowyPokojId)` sprawdza
zalogowaną sesję, następnie deleguje operację do pakietowej klasy `ZmianaPokojuDAO`.
Poprzednie ID pochodzi z odczytanej rezerwacji i chroni przed nadpisaniem zmiany innego pracownika.

DAO rozpoczyna transakcję `READ_COMMITTED` z wyłączonym automatycznym zatwierdzaniem.
Blokuje oba wiersze pokoi przez `FOR UPDATE`, zawsze od mniejszego ID do większego.
Potem blokuje rezerwację i odczytuje jej aktualny pokój, status, termin i liczbę osób.
Nie ufa danym formularza przy ocenie tych warunków.

Konflikt aktywnego pobytu sprawdza warunek `data_od < koniec AND data_do > poczatek`,
z pominięciem bieżącej rezerwacji. Blokada nowego pokoju serializuje tę operację
z tworzeniem innej rezerwacji. Zmiana `pokoj_id` i ewentualne oznaczenie starego
pokoju do sprzątania korzystają z jednego połączenia. `commit()` zatwierdza całość;
błąd powoduje `rollback()` obu zmian. Wszystkie wartości trafiają do SQL przez parametry.

`ZmianaPokojuGUI` zawiera formularz i wstępnie filtruje pokoje, ale ostateczną decyzję
podejmuje DAO. Zapis działa w tle przez `ZadaniaGUI`. Osobny `ZmianaPokojuKonsola`
wywołuje tę samą metodę, więc reguły obu interfejsów są wspólne.

## 4. Ograniczenie historii

Tabela `rezerwacje` ma pojedynczy `pokoj_id`. Program zmienia go dla całej rezerwacji,
a nie od daty przeniesienia. Dlatego kalendarz i raport obłożenia pokazują cały pobyt
w nowym pokoju, również noce sprzed przeniesienia. Zmiany przypisania pokoju są teraz zapisywane w dzienniku zdarzeń opisanym w `HISTORIA_REZERWACJI.md`.
Formularz uprzedza o tym przed zatwierdzeniem. Dokładna historia noclegów w kilku
pokojach wymaga osobnej tabeli odcinków pobytu oraz zmian w kalendarzu i raportach.

## 5. Aktualizacja i testy

Sama funkcja zmiany pokoju nie dodaje tabel. Obecna paczka zawiera jednak także historię
zdarzeń, dlatego wykonaj aktualizację i nadaj uprawnienia według `HISTORIA_REZERWACJI.md`.
Zachowaj własny `db.properties`. Nie importuj ponownie danych demonstracyjnych.

`RoomChangeProbe` sprawdza odmowy, zachowanie ceny i wpłat, uczestników, styki dat,
przeniesienie zameldowanej rezerwacji oraz sprzątanie. Błąd sprzątania wymuszony
przez testowy trigger sprawdza wycofanie już zmienionej rezerwacji. Równoległe
przeniesienie i nowa rezerwacja tego samego terminu mogą mieć tylko jednego zwycięzcę.
Sprawdzane są również ruchy w przeciwnych kierunkach bez zakleszczenia.

`GuiSmoke` obsługuje formularz zmiany pokoju i sprawdza nowy pokój w kalendarzu.
`PrivilegeProbe` weryfikuje operację na minimalnych uprawnieniach konta MySQL.
Scenariusze uruchamiaj wyłącznie na oddzielnej bazie testowej według `TESTOWANIE.md`.
