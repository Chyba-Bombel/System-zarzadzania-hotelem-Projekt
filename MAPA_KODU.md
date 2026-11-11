# Mapa kodu — gdzie szukać poszczególnych funkcji

Projekt jest podzielony na pakiety według odpowiedzialności. Punkty wejścia pozostają krótkie: `Launcher` uruchamia GUI, a `Main` uruchamia menu konsoli. Uporządkowanie plików nie zmienia struktury bazy ani reguł działania hotelu.

## 1. Uruchamianie

| Klasa | Odpowiedzialność |
| --- | --- |
| `Launcher` | Wywołuje JavaFX `Application.launch(HotelGUI.class, args)`. |
| `Main` | Wywołuje `MenuKonsoli.main(args)`. |
| `pl.hotel.gui.HotelGUI` | Tworzy okno, łączy kontrolery ekranów i obsługuje nawigację. |
| `pl.hotel.konsola.MenuKonsoli` | Pętla menu, odczyt numeru opcji i wybór odpowiedniego modułu konsoli. |

Uruchamianie Maven pozostaje bez zmian: `mvn javafx:run`. Skrypt Windows wyszukuje teraz pliki `.java` także w podfolderach `src`, aby kompilować wszystkie pakiety.

## 2. Baza danych — pakiet `pl.hotel.baza`

`BazaDanych` jest **fasadą**, czyli wspólnym punktem dostępu do operacji bazy. Nie przechowuje już całego SQL. Przykład:

```java
public static ArrayList<Gosc> pobierzGosci() throws SQLException {
    KontrolaDostepu.wymagaj(false);
    return GoscieDAO.pobierzGosci();
}
```

GUI i konsola nadal wywołują `BazaDanych.pobierzGosci()`, ale właściwe zapytanie znajduje się w `GoscieDAO`. Skrót **DAO** oznacza klasę dostępu do danych. Implementacje DAO mają widoczność pakietową — pozostałe pakiety korzystają z publicznej fasady.

| Klasa | Przeniesione operacje |
| --- | --- |
| `PolaczenieBazy` | `polacz`: odczyt `db.properties` i utworzenie połączenia JDBC. |
| `GoscieDAO` | Pobieranie, dodawanie i edycja głównych gości. |
| `PokojeDAO` | Pobieranie, dodawanie i edycja pokoi; oznaczanie sprzątania. |
| `OdczytRezerwacjiDAO` | Odczyt rezerwacji, przypisanie obiektów gościa i pokoju, wczytanie dodatkowych osób. |
| `TworzenieRezerwacjiDAO` | Zapis rezerwacji: walidacja, blokada pokoju, dostępność, zapis i zatwierdzenie. |
| `AnulowanieRezerwacjiDAO` | Anulowanie i sprawdzenie pozostałych wpłat. |
| `TerminyRezerwacjiDAO` | Zmiana terminu wraz ze sprawdzeniem dostępności i rozliczenia. |
| `PotwierdzeniaDAO` | Odczyt danych pojedynczej rezerwacji, uczestników i sum wpłat/zwrotów w jednej transakcji tylko do odczytu. |
| `HistoriaDAO` | Odczyt stanu, dopisanie zdarzenia przez to samo Connection i stronicowany odczyt historii. |
| `ZmianaPokojuDAO` | Zmiana pokoju w jednej transakcji: blokady obu pokoi według rosnącego ID, kontrola nieaktualnego wyboru, pojemności i terminów, sprzątanie po przeniesieniu zameldowanych gości. |
| `PobytyDAO` | Zameldowanie i wymeldowanie; kontrola stanu pokoju i rozliczenia. |
| `UczestnicyDAO` | Zapis listy dodatkowych gości i zmiana uczestników istniejącego pobytu. |
| `RozliczeniaDAO` | Odczyt rozliczenia i pomocnicze obliczenie wpłat minus zwroty. |
| `WplatyDAO` | Rejestracja wpłat i kontrola maksymalnej kwoty. |
| `ZwrotyDAO` | Rejestracja zwrotów i kontrola dostępnych środków. |
| `PracownicyDAO` | Odczyt pracowników i kont, tworzenie oraz dezaktywacja kont. |
| `LogowanieDAO` | Logowanie oraz trwały licznik prób z blokadą czasową. |
| `KontrolaDostepu` | Zalogowany kontekst wątku, identyfikacja pracownika i sprawdzenie aktualnej roli, aktywności oraz hasha hasła. |
| `RaportDAO` | Zapytania raportu z zachowaniem transakcji odczytowej. |
| `HaslaDAO` | Zmiana hasła pracownika w bazie. |

### Transakcje pozostały wspólne

Podział na pliki nie oznacza nowych połączeń dla każdego pomocniczego kroku. Jeśli operacja wymaga kilku zapytań, nadal wykonuje je w tej samej transakcji. Na przykład `TworzenieRezerwacjiDAO` przekazuje istniejące `Connection` do `UczestnicyDAO.zapiszUczestnikow(...)`. Rezerwacja i dodatkowi goście są zatwierdzani razem; błąd powoduje wycofanie całej operacji.

Podobnie sprawdzenie salda w operacjach zapisu korzysta z już otwartego połączenia przekazanego do `RozliczeniaDAO.pobierzNetto(...)`. Zachowano kolejność blokad oraz dotychczasowe `commit`, `rollback` i `try-with-resources`.

## 3. Konsola — pakiet `pl.hotel.konsola`

| Klasa | Odpowiedzialność |
| --- | --- |
| `MenuKonsoli` | Menu, przełączanie opcji, rozpoczęcie i zakończenie sesji. |
| `StanKonsoli` | Listy odczytanych danych, skaner i zalogowany pracownik. |
| `WejscieKonsoli` | Wczytywanie tekstu, liczb, dat, kwot i sprawdzanie e-maila. |
| `GoscieKonsola` | Dodawanie, edycja, wyszukiwanie i wyświetlanie gości. |
| `PokojeKonsola` | Wolne pokoje, dodawanie, edycja i sprzątanie. |
| `RezerwacjeKonsola` | Tworzenie, anulowanie i zmiana terminu rezerwacji. |
| `PotwierdzenieKonsola` | Opcja 26: dane dokumentu, wybór ścieżki i potwierdzenie zastąpienia pliku. |
| `HistoriaKonsola` | Opcja 25: przeglądanie historii rezerwacji w konsoli. |
| `ZmianaPokojuKonsola` | Opcja 24: wybór rezerwacji i nowego pokoju, potwierdzenie i odświeżenie danych. |
| `PobytyKonsola` | Zameldowanie i wymeldowanie. |
| `PlatnosciKonsola` | Rozliczenie, wybór metody płatności, wpłata i zwrot. |
| `KontaKonsola` | Logowanie, role, pracownicy, dezaktywacja i zmiana hasła. |

Dawna konsola nadal pracuje w jednym wątku i zachowuje swój statyczny stan sesji. Nie przeniesiono go do GUI. Nowe funkcje okienkowe, takie jak formularze uczestników i raporty, pozostają dostępne w GUI zgodnie z wcześniejszą wersją.

## 4. GUI — pakiet `pl.hotel.gui`

Każda klasa `Ekran...` otrzymuje w konstruktorze ten sam obiekt koordynatora `HotelGUI`. Dzięki temu ekrany korzystają z jednej sesji i tych samych list, bez tworzenia własnych kopii połączenia czy aplikacji.

| Klasa | Odpowiedzialność |
| --- | --- |
| `HotelGUI` | Okno, nawigacja i wspólny układ strony. |
| `StanGUI` | Stan konkretnej instancji: konto, listy, tabele, filtry, aktywny dialog i wykonawca zadań. |
| `EkranLogowania` | Formularz logowania i licznik prób. |
| `EkranPulpitu` | Liczniki, dzisiejsze przyjazdy i wyjazdy. |
| `EkranGosci` | Tabela oraz formularz dodawania i edycji gościa. |
| `EkranPokoi` | Tabela pokoi, sprawdzanie wolnego terminu, formularz pokoju i sprzątanie. |
| `EkranKalendarza` | Miesięczny plan pokoi, filtrowanie, szczegóły i otwieranie formularza z datą. |
| `KomorkaKalendarza` | Wygląd oraz obsługa kliknięcia pojedynczej nocy. |
| `EkranRezerwacji` | Rezerwacje, formularze terminu i uczestników, zameldowanie, wymeldowanie i anulowanie. |
| `PotwierdzenieGUI` | Podgląd dokumentu, odświeżanie oraz wybór ścieżki eksportu HTML. |
| `HistoriaRezerwacjiGUI` | Okno historii, szczegóły oraz strony po 100 wpisów. |
| `ZmianaPokojuGUI` | Osobny formularz zmiany pokoju, wstępny filtr dostępności i zapis przez fasadę. |
| `EkranPlatnosci` | Podgląd rozliczenia, wpłaty i zwroty. |
| `EkranPracownikow` | Lista kont, formularz nowego konta i dezaktywacja. |
| `EkranKonta` | Widok własnego konta i formularz zmiany hasła. |
| `EkranRaportow` | Wybór dat, podgląd raportu, wybór miejsca eksportu. |
| `Formularz` | Wspólne okno formularza: etykiety, kontrolki, zapis, anulowanie i błąd walidacji. |
| `TabeleGUI` | Konfiguracja kolumn, sortowanie i filtrowanie. |
| `NarzedziaGUI` | Formatowanie cen, nazw i czasu; tworzenie kontrolek oraz walidacja dat i haseł. |
| `ZadaniaGUI` | Wykonywanie pracy przez `Task`, kontrola sesji, odświeżanie oraz obsługa zakończenia zadania. |
| `KomunikatyGUI` | Komunikaty błędów i okna potwierdzenia. |
| `SqlAkcja` | Mały interfejs dla operacji, które mogą zgłosić `SQLException`. |

Stan GUI jest **instancyjny**. Nie zmieniono go na globalne pola statyczne. Operacje JDBC i eksport nadal odbywają się poza wątkiem interfejsu.

## 5. Modele, hasła i raporty

- `pl.hotel.model`: `Osoba`, `Gosc`, `Pracownik`, `Pokoj`, `Rezerwacja`, `Uczestnik`, `Platnosc`, `Rozliczenie` i `KontoPracownika`.
- `pl.hotel.bezpieczenstwo.Hasla`: algorytm tworzenia i sprawdzania skrótów; nie wykonuje operacji JDBC.
- `pl.hotel.serwis.GuiRepository`: wczytanie zestawu danych na potrzeby ekranów i kontrola aktywnego konta.
- `pl.hotel.raporty.Raporty`: model raportu i eksport CSV/HTML; odczyt danych i agregacja są w `baza/RaportDAO`; wybór pliku w oknie należy do `EkranRaportow`.

### Plan miesięczny — `pl.hotel.kalendarz`

`PlanKalendarza` buduje indeks pobytów po ID pokoju i dacie nocy. Nie korzysta
z JavaFX ani JDBC. Wyjazd jest granicą wyłączną, anulowane są pomijane,
a zakończone pozostają historią i nie blokują rezerwacji. `PoleDnia` udostępnia
listę pobytów, wykrywa konflikt aktywnych rezerwacji i sprawdza, czy można
rozpocząć nowy pobyt danego dnia. Ostateczna dostępność nadal jest kontrolowana
przez `TworzenieRezerwacjiDAO` podczas zapisu. `CalendarProbe` sprawdza reguły
planu bez uruchamiania GUI ani MySQL.

## 6. Gdzie wprowadzać zmianę?

| Chcę zmienić… | Otwieram… |
| --- | --- |
| Układ miesięcznego kalendarza | `gui/EkranKalendarza.java` i `gui/KomorkaKalendarza.java`. |
| Zasady oznaczania nocy w kalendarzu | `kalendarz/PlanKalendarza.java`. |
| Zapytanie SQL pobierające gości | `baza/GoscieDAO.java`. |
| Zasady tworzenia rezerwacji | `baza/TworzenieRezerwacjiDAO.java`; sprawdzam też spójność zmiany terminu w `TerminyRezerwacjiDAO`. |
| Formularz nowej rezerwacji | `gui/EkranRezerwacji.java`. |
| Wygląd wspólnych formularzy | `gui/Formularz.java` i `resources/hotel.css`. |
| Kolumny lub filtr tabeli | `gui/TabeleGUI.java`. |
| Tekst i pozycje menu konsoli | `konsola/MenuKonsoli.java`. |
| Walidację danych wpisywanych w konsoli | `konsola/WejscieKonsoli.java`. |
| Treść raportu lub sposób liczenia obłożenia | `raporty/Raporty.java`. |
| Formularz logowania | `gui/EkranLogowania.java`. |
| Adres serwera i hasło techniczne MySQL | `db.properties`, bez edycji źródeł. |
| Tytuł lub ikonę głównego okna | `gui/HotelGUI.java`, metoda `start`. |

Ścieżki z tabeli odnoszą się do `src/pl/hotel`, oprócz wskazanych plików konfiguracyjnych i zasobów.

## 7. Aktualizacja własnych rozszerzeń

Nie mieszaj plików nowej wersji ze starym płaskim katalogiem `src`. Rozpakuj projekt do osobnego folderu, zachowaj swój `db.properties` i przenieś własne zasoby. Nie trzeba importować bazy ponownie.

Jeżeli dopisywałeś własne klasy, użyj nowych importów, np.:

```java
import pl.hotel.baza.BazaDanych;
import pl.hotel.model.Gosc;
import pl.hotel.model.Rezerwacja;
```

Publiczne operacje `BazaDanych` zachowują dotychczasowe argumenty i wyniki, ale wymagają rzeczywistego logowania w tym samym wątku. Surowe `polacz()` nie jest już publiczne; raporty czytają bazę przez `RaportDAO`. Podział nie dodaje nowych funkcji i nie zastępuje wymaganej wcześniej migracji uczestników, jeśli Twoja stara baza jeszcze jej nie zawiera.

## Aktualizacja bezpieczeństwa

Fasada wymaga logowania, a metody z ID wykonującego pracownika porównują je
z tożsamością rzeczywistej sesji. Nie przekazuje się sesji jako dowolnego obiektu
`Pracownik`: powstaje wewnętrznie dopiero po sprawdzeniu hasła.
Własny wątek wykonujący operacje musi sam się zalogować.
Zmiana roli, aktywności lub hasła powoduje błąd `SesjaWygasla` przy kolejnym użyciu.
Dwa pola blokady logowania dodaje `db/aktualizacja_bezpieczenstwo.sql`.
Komentarze przeniesiono do dokumentacji — źródła nie mają komentarzy.

## Historia zmian

`model/ZmianaRezerwacji` jest niezmiennym rekordem jednego zdarzenia;
`model/StronaHistorii` przechowuje niezmienną listę i informację o starszych wpisach.
Fasada udostępnia wyłącznie chroniony odczyt historii. Dopisywanie jest wewnętrzną
operacją DAO wywoływaną przed `commit()` operacji biznesowej.
Szczegóły i migracja: `HISTORIA_REZERWACJI.md`.

## Potwierdzenia i wspólny zapis plików

`model/PotwierdzenieRezerwacji` jest niezmienną kopią danych pobytu i kwot.
Przechowuje własną listę uczestników i oblicza noce, należność oraz saldo.
`potwierdzenia/Potwierdzenia` przygotowuje podpisy pól, formatowanie i samodzielny
HTML. Koduje wszystkie dane tekstowe przed umieszczeniem w dokumencie.

`pliki/ZapisPliku` zapisuje UTF-8 do pliku tymczasowego i przenosi go na miejsce
docelowe. Korzystają z niego potwierdzenia i dotychczasowe raporty CSV/HTML.
`PotwierdzeniaDAO` używa jednego połączenia i REPEATABLE_READ, aby nazwy, goście
oraz płatności pochodziły z jednego obrazu danych. Odczyt nie dopisuje historii.
