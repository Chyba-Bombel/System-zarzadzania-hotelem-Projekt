HOTEL MANAGER — LINUX

INSTALACJA
Paczka zawiera pakiet hotel-manager*.deb, tę instrukcję, folder db oraz
przykład konfiguracji. Jest przeznaczona dla zgodnej wersji Linux Mint,
Ubuntu lub innego systemu DEB o odpowiedniej architekturze i bibliotekach.
Nie oznacza to obsługi wszystkich dystrybucji ani wszystkich ich wersji.

Wypakuj paczkę i otwórz terminal w jej folderze. Powinien znajdować się
w nim jeden instalator wybranej wersji. Wykonaj:

sudo apt install ./hotel-manager*.deb

Program znajdziesz w menu jako HotelManager. Uruchamiaj bez sudo.
Java, JavaFX i JDBC są w pakiecie. JDK i Maven nie są potrzebne użytkownikowi.
Serwer MySQL/MariaDB i baza wymagają osobnego przygotowania.

BAZA
Przeczytaj db/README.md. nowa_baza.sql służy do nowej bazy;
aktualizuj_baze.sql do aktualizacji istniejącej, po wykonaniu kopii.
Poprawka 1.0.2 nie zmienia schematu bazy zgodnej z wersją 1.0.1.
Instalator nie wykonuje SQL i nie tworzy kopii bazy.

POŁĄCZENIE
Przed logowaniem wybierz Ustawienia połączenia z bazą.
Podaj adres JDBC, konto serwera i hasło. Konto MySQL to nie konto pracownika.
Testuj połączenie sprawdza bieżący formularz przez SELECT 1, bez zapisu
ustawień i bez sprawdzania całej struktury bazy. Zmienne HOTEL_DB_URL,
HOTEL_DB_USER i HOTEL_DB_PASSWORD mają pierwszeństwo również podczas testu.

Przykład adresu wyłącznie dla lokalnej bazy:
jdbc:mysql://localhost:3306/hotel_manager?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=10000

localhost oznacza komputer, na którym działa aplikacja. W maszynie wirtualnej
oznacza tę maszynę, a nie system gospodarza. Serwer zdalny wymaga TLS
VERIFY_IDENTITY i zaufanego certyfikatu; nie wyłączaj weryfikacji.

USTAWIENIA I KOPIE
Domyślnie: ~/.config/HotelManagerData/config/db.properties
Logi: ~/.config/HotelManagerData/logs
Kopie ustawień: ~/.config/HotelManagerData/config/backups
Jeśli ustawiono XDG_CONFIG_HOME, program używa go zamiast ~/.config.
Dokładne ścieżki pokazuje O programie; jawne hotel.config lub hotel.data
mogą je zmienić. Folder .config jest ukryty; zwykle pokażesz go Ctrl+H.

Przed każdym zapisem powstaje kopia poprzedniego pliku. Zachowywanych jest
do pięciu kopii. Przycisk Przywróć kopię pozwala wybrać wcześniejsze ustawienia;
przywrócenie zachowuje również aktualne ustawienia jako kolejną kopię.
Kopie zawierają hasło bazy — nie publikuj ich i nie dołączaj do zgłoszeń.
Nie zastępują kopii zapasowej danych hotelu.

DIAGNOSTYKA
W O programie możesz otworzyć folder logów lub skopiować wersję aplikacji,
wersję Javy, system i architekturę. Kopiowane informacje nie obejmują
konfiguracji bazy. Jeśli system nie pozwala otworzyć folderu, program pokaże
ścieżkę do ręcznego otwarcia. Schowek wymaga działającej sesji graficznej.
Log nie zapisuje treści wyjątków, zapytań SQL, haseł ani danych gości.

AKTUALIZACJE
Zamknij program, zachowaj konfigurację i wykonaj osobną kopię bazy.
Pobierz wyższą wersję dla tej samej architektury i otwórz jej folder:

sudo apt install ./hotel-manager*.deb

Pakiet musi zachowywać nazwę hotel-manager. Nie trzeba wcześniej usuwać
starej wersji. Konfiguracja użytkownika jest oddzielona od instalacji.
Jeśli wydanie wymaga SQL, postępuj według jego instrukcji. Automatyczne
pobieranie aktualizacji z GitHuba nie jest zaimplementowane.

PROBLEMY
Brak połączenia: sprawdź serwer, adres, port i sieć.
Odmowa dostępu: sprawdź hasło oraz uprawnienia konta z danego hosta.
Brak tabel/kolumn: sprawdź wersję struktury bazy przed importem SQL.
Po błędzie zapisu odśwież dane przed ponowieniem — zapis mógł się udać.
Niespełnione zależności DEB: użyj pakietu zgodnego z Twoim systemem.
Program wymaga pulpitu; sam terminal SSH bez dostępu do niego nie wystarczy.

ODINSTALOWANIE
sudo apt remove hotel-manager

Baza i pliki użytkownika są zarządzane osobno. Nie usuwaj ich, jeśli chcesz
zachować konfigurację i historię hotelu.

Repozytorium:
https://github.com/Chyba-Bombel/System-zarzadzania-hotelem-Projekt/
