# Zmiany

## 1.1.0

- Nowy pulpit i boczne menu z ikonami wektorowymi.
- Jasny i ciemny motyw dla ekranów, formularzy i komunikatów.
- Przełącznik przed logowaniem i w menu, zapamiętywanie preferencji.
- Rozmiar tekstu 100%, 110% i 120%.
- Wykres stanów pokoi, zakładki przyjazdów i wyjazdów oraz tygodniowy podgląd rezerwacji.
- Karta wybranego pobytu w widoku rezerwacji i czytelniejsze oznaczenia statusów.
- Układ dopasowujący się do szerokości okna oraz przewijanie.
- Osobne, małe klasy wyglądu i pulpitu; bez nowych zależności.

Brak zmian schematu bazy. Preferencje wyglądu zapisują się osobno od konfiguracji połączenia. Tłumaczenia pozostają na kolejny etap. Szczegóły: AKTUALIZACJA_1.1.0.md.

## 1.0.2

- Test połączenia w tle z bieżącego formularza, bez zapisywania ustawień.
- Kopia poprzedniej konfiguracji przed zmianą i wybór kopii do przywrócenia.
- Zachowanie konfiguracji przy błędzie kopii oraz obsługa uszkodzonego pliku.
- Otwieranie folderu logów i kopiowanie informacji diagnostycznych bez danych bazy.
- Skrypt Linuksa: podgląd, DEB i kompletne archiwum ZIP.
- Polska etykieta anulowania i czytelniejsze komunikaty formularza połączenia.
- Zachowany UUID Windows i nazwa pakietu Linux do kolejnych aktualizacji.
- Ponowna weryfikacja logiki na osobnym MySQL oraz GUI na Linuxie.

Brak zmian struktury bazy. Przejście z faktycznie wydanego instalatora Windows wymaga próby na Windows; nie jest potwierdzone samym testem kodu. Szczegóły w WYNIKI_TESTOW_1.0.2.txt.

## 1.0.1

- Konfiguracja bazy w folderze użytkownika, poza plikami instalatora.
- Migracja starych ustawień z zachowaniem istniejącej konfiguracji.
- Edycja połączenia przed logowaniem, hasło w polu maskowanym.
- Wersja z pom.xml w oknie i w „O programie”.
- Czytelniejsze błędy połączenia, schematu i TLS.
- Dziennik błędów z rotacją i bez treści wyjątków, SQL oraz danych gości.
- Skrypt budowania instalatora Windows ze stałym UUID kolejnych aktualizacji.
- Skrypt kopii/migracji konfiguracji i instrukcja pierwszego przejścia z 1.0.0.
- Testy konfiguracji, migracji, maskowania logów, błędów i ustawień JDBC.

Brak zmian schematu bazy w tej poprawce. Zbudowanie instalatora i sprawdzenie aktualizacji na Windows pozostaje krokiem przed publikacją wydania.
