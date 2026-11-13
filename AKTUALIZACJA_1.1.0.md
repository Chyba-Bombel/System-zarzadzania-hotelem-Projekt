# HotelManager 1.1.0 — nowy wygląd

Wydanie wprowadza nowy interfejs JavaFX, jasny i ciemny motyw oraz wybór wielkości tekstu. Reguły rezerwacji, płatności i uprawnienia pracowników pozostają wspólne dla GUI i konsoli.

## Aktualizacja

1. Zamknij uruchomioną aplikację. Zachowaj prywatną konfigurację i własną ikonę `hotel.ico` / `hotel.png`.
2. Rozpakuj źródła do nowego folderu. W swoim repozytorium zastąp stare pliki projektu nowymi, zachowując `.git` i pliki prywatne. Nie łącz skompilowanych klas dwóch wersji.
3. Baza zgodna z 1.0.2 nie wymaga żadnego skryptu SQL. Dla starszej struktury użyj jednego aktualizatora zgodnie z `db/README.md`; nie importuj danych demonstracyjnych na istniejącą bazę.
4. Uruchom `uruchom_windows.bat` na Windows albo `mvn javafx:run` na Linuxie. Zachowano odczyt konfiguracji z folderu użytkownika.
5. Do wydania instalatora uruchom `buduj_instalator.ps1` albo `bash buduj_instalator_linux.sh`. Wersja jest pobierana z `pom.xml`: 1.1.0. Zachowano UUID aktualizacji Windows i nazwę pakietu Linux.

Archiwum zawiera źródła i skrypty, nie gotowy instalator dla Windows. Przed publikacją instalatora sprawdź jego uruchomienie oraz aktualizację na docelowym systemie.

## Motyw i tekst

Przed zalogowaniem oraz w dolnej części lewego menu znajduje się przycisk **Tryb ciemny / Tryb jasny**. Napis oznacza tryb, na który przełączysz aplikację.

Wybierz **Wygląd**, aby ustawić motyw i tekst 100%, 110% albo 120%. Zmiany obowiązują od razu w ekranach, formularzach, komunikatach i listach rozwijanych. Domyślnie aplikacja uruchamia się w jasnym motywie z tekstem 100%.

Preferencje są wspólne dla pracowników używających tego samego konta systemowego. Pozostają po wylogowaniu, restarcie i aktualizacji aplikacji. Plik `wyglad.properties` znajduje się w katalogu danych aplikacji:

- Windows: `%LOCALAPPDATA%\HotelManagerData\wyglad.properties`.
- Linux: `~/.config/HotelManagerData/wyglad.properties`; pierwszeństwo ma `XDG_CONFIG_HOME`.
- Jawne `-Dhotel.data=...` wskazuje inny katalog danych.

Wygląd zapisuje się osobno od `config/db.properties`. Usunięcie pliku preferencji przy zamkniętej aplikacji przywróci domyślny wygląd. Uszkodzony plik nie blokuje uruchomienia. Jeśli zapis się nie powiedzie, zmiana działa w bieżącej sesji, a aplikacja wyświetla informację.

Pasek tytułu okna i natywne okna wyboru pliku są rysowane przez system operacyjny. Ich wygląd zależy od ustawień systemu. Tłumaczenia interfejsu są przewidziane do kolejnego etapu; ta wersja jest po polsku.

## Nowy pulpit

- Zajętość: liczba różnych pokoi z zameldowanym pobytem, podzielona przez liczbę pokoi niewyłączonych z użytkowania. To stan fizycznego zajęcia, nie procent wszystkich przyszłych rezerwacji.
- Przyjazdy: potwierdzone rezerwacje zaczynające się dzisiaj.
- Wyjazdy: zameldowane pobyty kończące się dzisiaj lub wcześniej, nadal wymagające wymeldowania.
- Do sprzątania: niezajęte, niewyłączone pokoje wymagające przygotowania.
- Wykres stanów pokoi: rozłączne kategorie zajęte, gotowe, do sprzątania i wyłączone. Ich suma odpowiada liczbie pokoi.
- Przyjazdy i wyjazdy mają osobne zakładki z przyciskiem otwarcia rezerwacji.
- Podgląd tygodnia pokazuje pierwsze cztery pokoje i siedem najbliższych nocy. Kliknięcie pobytu otwiera rezerwację, a wolnej nocy — formularz. Pełną listę pokoi pokazuje ekran Kalendarz.

Dane pochodzą z ostatniego odczytu. Przycisk **Odśwież dane** pobiera aktualny stan. Stopka pokazuje czas tego odczytu i wynik ostatniej operacji; napis „Sesja aktywna” nie jest ciągłym testem dostępności serwera.

## Pozostałe ekrany

Nowe kolory, ikony, tabele i oznaczenia stanów obejmują wszystkie ekrany. Widok rezerwacji ma dodatkowo kartę zaznaczonego pobytu z gośćmi, terminem, pokojem, należnością i saldem. Należność uwzględnia aktywne usługi.

Mniejsze okna korzystają z przewijania. Na węższym pulpicie karty przechodzą do dwóch kolumn, a główne sekcje układają się pionowo. Wysoki tekst może wymagać przewinięcia. Minimalne okno ma 1040 × 680 pikseli.

## Organizacja kodu

`PreferencjeWygladu` odpowiada za odczyt i atomowy zapis ustawień. `MotywyGUI` nakłada paletę i rozmiar tekstu na wszystkie okna JavaFX, także otwierane później. `UstawieniaWygladuGUI` udostępnia przełącznik i formularz. `NawigacjaGUI` buduje menu i ramę aplikacji. `hotel.css` zawiera palety i style kontrolek. `IkonyGUI` rysuje ikony wektorowe bez dodatkowej biblioteki.

Pulpit podzielono na osobne komponenty kart, tabeli pobytów, wykresu i kalendarza. `PodsumowaniePulpitu` wykonuje obliczenia bez połączenia z bazą. Nie dodano komentarzy do kodu źródłowego.
