# Usługi dodatkowe i wyszukiwanie rezerwacji

## 1. Aktualizacja istniejącej bazy

Istniejąca baza: zamknij stanowiska, wykonaj kopię i uruchom **`db/aktualizuj_baze.sql`** jako administrator MySQL/MariaDB. Skrypt obejmuje wszystkie dotychczasowe zmiany, więc nie wybierasz aktualizacji według etapów. Nowa, pusta instalacja: **`db/nowa_baza.sql`**. Zachowaj własny `db.properties`; pełnego importu demonstracyjnego nie wykonuj na swoich danych. Szczegóły: [db/README.md](db/README.md).

## 2. Katalog usług

Administrator: **Rezerwacje → Katalog usług**. Zaznacz usługę do edycji albo wybierz **Nowa**. Podaj nazwę, jednostkę, cenę i aktywność. Nie ma usuwania usług z historią pobytów. Wyłączenie ukrywa ofertę przy dodawaniu nowych pozycji.

| Przykładowa oferta | Cena | Jednostka |
| --- | ---: | --- |
| Śniadanie | 45,00 zł | porcja |
| Parking | 30,00 zł | doba |
| Dostawka | 60,00 zł | noc |

Każda pozycja pobytu kopiuje nazwę, jednostkę i cenę w chwili dodania. Późniejsza zmiana cennika nie przelicza starych rachunków. Zapis ze starego formularza po zmianie oferty jest odrzucany z prośbą o odświeżenie, zamiast pobierać nową cenę bez wiedzy pracownika. Analogicznie edycja katalogu porównuje poprzednie dane, aby nie nadpisać zmian innego administratora.

## 3. Usługi pobytu

1. Zaznacz rezerwację i wybierz **Usługi pobytu**.
2. Wybierz ofertę oraz ilość i kliknij **Dodaj usługę**.
3. Podsumowanie pokazuje noclegi, aktywne usługi, należność i saldo.
4. Zaznacz pozycję i wybierz **Anuluj pozycję**, aby ją wycofać. Pozycja pozostaje w tabeli z odpowiednim statusem.

Ilość podajesz dla **całego pobytu**: 2 osoby × 3 śniadania = 6 porcji. Program nie dopisuje pozycji codziennie. Zmiana terminu nie zmienia ilości usług. Korekta ilości polega na anulowaniu starej pozycji i dodaniu nowej. **Dostawka jest opłatą i nie zwiększa pojemności pokoju.**

Recepcja i administrator zmieniają usługi tylko w potwierdzonym lub zameldowanym pobycie. Zakończone i anulowane rezerwacje są tylko do odczytu. Ilość wynosi 1–1000, limit aktywnych pozycji to 100. Cena jest dodatnia, z najwyżej dwoma miejscami po przecinku; nowe dodanie nie może podnieść należności ponad 99 999 999,99 zł.

Konsola: **29** — usługi pobytu, **31** — katalog administratora. Przy anulowaniu podawaj ID pozycji pobytu, nie ID oferty z katalogu.

## 4. Rozliczenie, karta i raporty

Należność = liczba nocy × uzgodniona stawka pokoju + aktywne usługi. Saldo = należność − (wpłaty − zwroty). Anulowana rezerwacja ma zerową należność. Cena pokoju nie jest dodatkowo mnożona przez liczbę osób.

Dodanie usługi do opłaconego pobytu tworzy kwotę do dopłaty. Wpłata nie może przekroczyć aktualnej należności. Anulowanie opłaconej pozycji, które utworzyłoby nadpłatę, wymaga wcześniejszego zwrotu różnicy przez administratora. Zwrot nie zmniejsza ceny usług. Zmiana terminu sprawdza nadpłatę razem z usługami. Wymeldowanie wymaga rozliczenia noclegów i usług.

Anulowanie całej potwierdzonej rezerwacji wymaga wcześniejszego zwrotu jej środków i anuluje aktywne pozycje w tej samej transakcji. Nazwy i historyczne ceny pozostają w bazie.

Karta rezerwacji ma zakładkę **Usługi** oraz osobny subtotal w rozliczeniu. HTML zawiera pozycje i statusy; tylko aktywne zwiększają należność. PDF nadal tworzy przeglądarka przez Ctrl+P. Wewnętrzne notatki nie są eksportowane.

Raport ma piątą zakładkę **Usługi dodatkowe** i osobny wskaźnik sumy. Usługi są wybierane według **daty dodania**, z **bieżącym statusem**: anulowana pozycja ma wartość aktywną zero. Nie rozdziela się ich proporcjonalnie na noce i nie odtwarza stanu na dawny dzień. Obłożenie i wartość noclegów zachowują dotychczasowe definicje. Wpłaty i zwroty pozostają przepływami według daty transakcji. Eksport CSV/HTML obejmuje nową tabelę.

## 5. Wyszukiwanie zaawansowane

Otwórz **Rezerwacje → Wyszukiwanie zaawansowane**. Puste pola nie ograniczają wyniku, pozostałe filtry łączą się:

| Filtr | Znaczenie |
| --- | --- |
| Nazwisko | Fragment nazwiska głównego lub dodatkowego gościa; bez powielania rezerwacji, gdy pasuje kilka osób. |
| Pokój | Dokładny numer pokoju. |
| Status | Wybrany stan albo wszystkie. |
| Od / Do | Dwie granice, tylko jedna albo bez dat. |
| Tryb dat | Noce pobytu przecinają okres, przyjazd w okresie albo wyjazd w okresie. |
| Rozliczenie | Wszystkie, do zapłaty, bez wpłat netto, częściowo opłacone, saldo zero, nadpłata. |

Obie daty graniczne są włączne; rok 1900–9998, data końcowa nie wcześniejsza niż początkowa. W trybie pobytu badamy noce: rezerwacja kończąca się danego dnia nie nocuje tego dnia. W trybie wyjazdu ten dzień jest prawidłowym wynikiem.

**Szukaj** pobiera dane z bazy. Tabela pokazuje należność z usługami, netto i saldo. **Starsze wyniki** pobierają dalsze strony po 100 rezerwacji, bez cichego obcinania. **Wyczyść filtry** wraca do wszystkich. Po zmianie filtra poprzednie wyniki znikają do ponownego wyszukania. Zaznacz wynik i kliknij **Otwórz rezerwację**, aby przejść do obsługi pobytu.

„Bez wpłat netto” obejmuje też pełny zwrot wcześniejszych wpłat przy dodatniej należności. „Saldo zero” obejmuje rozliczone anulowania. „Nadpłata” pomaga znaleźć np. nieprawidłowe dane zaimportowane ze starszej wersji. Bieżąca aplikacja blokuje operacje tworzące nadpłatę. Wynik jest aktualny w chwili odczytu danej strony, nie zamraża zmian innych stanowisk. Nowy rekord z większym ID nie przesuwa granicy starszych stron.

Konsola: **30**. Enter pomija filtr. Statusy i stany rozliczenia wpisuj według nazw podanych w menu. Szybkie wyszukiwanie na głównej liście pozostaje dostępne.

## 6. Klasy i relacje

| Obszar | Klasy |
| --- | --- |
| Modele usług | UslugaHotelowa, PozycjaUslugi, UslugiRezerwacji. |
| Modele wyszukiwania | FiltrRezerwacji, WynikRezerwacji, StronaRezerwacji. |
| Baza | KatalogUslugDAO — cennik; UslugiDAO — spójny odczyt; ZapisUslugDAO — dodanie/anulowanie; KosztyDAO — sumy. |
| Wyszukiwanie | WyszukiwanieDAO — parametry, daty, saldo i stronicowanie. |
| Fasada | BazaDanych — sesja, role i zgodność autora z pracownikiem zalogowanym. |
| GUI | KatalogUslugGUI, UslugiGUI, WyszukiwanieGUI; wspólny szkielet OknoDanych. |
| Konsola | KatalogUslugKonsola, UslugiKonsola, WyszukiwanieKonsola. |

Jedna rezerwacja ma wiele pozycji usług, oferta katalogowa może należeć do wielu pozycji. Pozycja wskazuje autora dodania i ewentualnego anulowania. Klucze obce mają ON DELETE RESTRICT. Osobne agregaty usług i płatności zapobiegają mnożeniu kwot przy wielu pozycjach i wpłatach. Listy, karta, rozliczenie i raport używają spójnych odczytów; zapytania wykonują się poza wątkiem JavaFX.

## 7. Transakcje i zabezpieczenia

Wpłaty, zwroty, usługi i wymeldowanie blokują tę samą rezerwację przez FOR UPDATE, więc nie zatwierdzają równocześnie obliczeń na starym saldzie. Dodanie usługi blokuje także odczyt katalogu FOR SHARE. Historia zapisuje się przez to samo Connection; jej błąd wycofuje całą operację.

SQL używa PreparedStatement. Fragmenty zapytania zależą od zamkniętych list trybów, a tekst użytkownika jest parametrem. Znaki `%`, `_` i `=` w nazwisku są dosłowne. Ceny używają BigDecimal. Edycję cennika ogranicza do administratora także API, nie tylko widoczność przycisku. Konfiguracja i prawa MySQL nadal wymagają właściwego zabezpieczenia; testy nie oznaczają odporności na każdy możliwy atak.

Testy nowych funkcji: ServicesSearchProbe oraz ServicesGuiSteps w GuiSmoke; dodatkowo rozszerzono PrivilegeProbe. Polecenia: TESTOWANIE.md. Wyniki bieżącego wydania: WYNIKI_TESTOW_USLUGI_WYSZUKIWANIE.txt. Kod i SQL pozostają bez komentarzy.
