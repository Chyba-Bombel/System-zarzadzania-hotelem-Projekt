#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
if [[ "${1:-}" == --help ]]; then
    printf 'Użycie: bash pakuj_flatpak.sh [repozytorium Flatpak] [gałąź]\nDomyślnie: flatpak-repo oraz master. Skrypt wymaga wcześniej zbudowanej aplikacji.\n'
    exit 0
fi
if (( $# > 2 )); then
    printf 'Użycie: bash pakuj_flatpak.sh [repozytorium Flatpak] [gałąź]\n' >&2
    exit 1
fi
for narzedzie in flatpak python3; do
    command -v "$narzedzie" >/dev/null || { printf 'Brak narzędzia: %s\n' "$narzedzie" >&2; exit 1; }
done
repo="${1:-flatpak-repo}"
galaz="${2:-master}"
app_id="io.github.ChybaBombel.HotelManager"
for plik in pom.xml db.properties.example db/README.md; do
    [[ -f "$plik" ]] || { printf 'Brak %s. Umieść skrypt w głównym folderze projektu.\n' "$plik" >&2; exit 1; }
done
[[ -d "$repo" ]] || { printf 'Brak repozytorium %s. Najpierw wykonaj flatpak-builder z --repo=flatpak-repo.\n' "$repo" >&2; exit 1; }
wersja="$(python3 - <<'PY'
import re
import xml.etree.ElementTree as E
v = E.parse('pom.xml').getroot().findtext('{http://maven.apache.org/POM/4.0.0}version')
if not v or not re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+', v):
    raise SystemExit('Wersja w pom.xml musi mieć postać X.Y.Z.')
print(v)
PY
)"
arch="$(flatpak --default-arch)"
if [[ ! "$arch" =~ ^[A-Za-z0-9_]+$ ]]; then
    printf 'Nie udało się ustalić architektury Flatpaka.\n' >&2
    exit 1
fi
nazwa="HotelManager-$wersja-linux-$arch-flatpak"
mkdir -p dist
wynik="$PWD/dist/$nazwa"
zip="$wynik.zip"
if [[ -e "$wynik" || -e "$zip" ]]; then
    printf 'Wynik już istnieje. Przenieś go przed ponownym pakowaniem:\n%s\n%s\n' "$wynik" "$zip" >&2
    exit 1
fi
roboczy="$(mktemp -d "$PWD/dist/.flatpak-paczka-XXXXXXXX")"
sprzataj() { rm -rf -- "$roboczy"; }
trap sprzataj EXIT
trap 'printf "Pakowanie przerwane w wierszu %s. Sprawdź błąd powyżej.\n" "$LINENO" >&2' ERR
paczka="$roboczy/$nazwa"
mkdir -p "$paczka"
plik_flatpak="HotelManager-$wersja-$arch.flatpak"
flatpak build-bundle \
    --arch="$arch" \
    "$repo" \
    "$paczka/$plik_flatpak" \
    "$app_id" \
    "$galaz" \
    --runtime-repo=https://dl.flathub.org/repo/flathub.flatpakrepo
[[ -s "$paczka/$plik_flatpak" ]] || { printf 'Nie powstał niepusty plik .flatpak.\n' >&2; exit 1; }
python3 - "$paczka" "$wersja" "$arch" "$app_id" "$plik_flatpak" "$roboczy/$nazwa.zip" <<'PY'
from pathlib import Path
import shutil
import sys
import zipfile

cel, wersja, arch, app_id, instalator, archiwum = sys.argv[1:]
cel = Path(cel)
for zrodlo in [Path('db'), Path('db.properties.example')]:
    if zrodlo.is_symlink() or (zrodlo.is_dir() and any(p.is_symlink() for p in zrodlo.rglob('*'))):
        raise SystemExit('Nie pakuję dowiązań symbolicznych z db ani db.properties.example.')
shutil.copytree('db', cel / 'db')
shutil.copyfile('db.properties.example', cel / 'db.properties.example')
instrukcja = f'''HOTEL MANAGER {wersja} — LINUX FLATPAK
Architektura: {arch}

ZAWARTOŚĆ
{instalator} — aplikacja z Javą, JavaFX i JDBC.
db/ — skrypty i instrukcja przygotowania bazy.
db.properties.example — przykładowa konfiguracja, nie prywatne hasło autora.

INSTALACJA
Zainstaluj Flatpak. Na Linux Mint jest zazwyczaj dostępny; sprawdź:
flatpak --version

Na Mint/Ubuntu, jeżeli go brakuje:
sudo apt update
sudo apt install flatpak

Dodaj Flathub dla swojego użytkownika:
flatpak remote-add --user --if-not-exists flathub https://flathub.org/repo/flathub.flatpakrepo

Rozpakuj cały ZIP. Otwórz terminal w folderze zawierającym plik .flatpak:
flatpak install --user ./{instalator}

Pierwsza instalacja może pobrać środowisko uruchomieniowe z Flathuba.
Zapewnij dostęp do internetu i wolne miejsce na aplikację oraz środowisko.
Nie trzeba osobno instalować JDK, JavaFX, JDBC ani Mavena.
Wymagany jest Linux z obsługą Flatpaka i zgodna architektura {arch}.

URUCHOMIENIE
W menu wybierz HotelManager albo wykonaj:
flatpak run --user {app_id}

BAZA DANYCH
MySQL nie jest częścią paczki. Może działać lokalnie albo na osobnym serwerze.
Najpierw przeczytaj db/README.md i wybierz właściwy plik SQL:
- nowa baza: db/nowa_baza.sql;
- starsza baza wymagająca zmiany struktury: db/aktualizuj_baze.sql.
Nie importuj nowej bazy demonstracyjnej na istniejące dane hotelu.
Przed zmianami struktury wykonaj kopię bazy.

Przed logowaniem wybierz Ustawienia połączenia z bazą.
Podaj adres JDBC i dane konta MySQL, nie login pracownika.
localhost oznacza komputer uruchamiający aplikację; w maszynie wirtualnej
jest to maszyna wirtualna, a nie jej system gospodarz.
Zdalny serwer wymaga bezpiecznej konfiguracji TLS zgodnie z db/README.md.
Plik db.properties.example jest wzorem; ustawienia zapisuj z formularza.

KONFIGURACJA I RAPORTY
Flatpak ma oddzielny katalog konfiguracji:
~/.var/app/{app_id}/config/HotelManagerData/
Stara konfiguracja zwykłej instalacji nie jest automatycznie importowana.
Raporty zapisuj w Dokumentach. Manifest opisany w instrukcji projektu
udostępnia aplikacji ten katalog, a nie cały dysk.

AKTUALIZACJE
Zamknij aplikację, pobierz nowe wydanie i zainstaluj nowy plik .flatpak.
Paczki muszą zachowywać identyfikator aplikacji, architekturę i gałąź.
Samo pobranie paczki z GitHub Releases nie włącza automatycznych aktualizacji.
Zmiany bazy wykonuj tylko wtedy, gdy wymaga ich instrukcja danego wydania.

ODINSTALOWANIE
flatpak uninstall --user {app_id}
Baza MySQL jest zarządzana osobno.

PROBLEMY
Brak połączenia: sprawdź adres, serwer, port, uprawnienia i sieć.
Brak miejsca: zwolnij miejsce lub powiększ dysk maszyny wirtualnej.
Brak dostępu do raportu: sprawdź zapis w folderze Dokumenty.
Przed wydaniem przetestuj instalację na czystym systemie oraz eksport HTML/CSV.

Repozytorium projektu:
https://github.com/Chyba-Bombel/System-zarzadzania-hotelem-Projekt/
'''
(cel / 'README-Linux-Flatpak.txt').write_text(instrukcja, encoding='utf-8')
with zipfile.ZipFile(archiwum, 'x', zipfile.ZIP_DEFLATED) as z:
    for plik in sorted(cel.rglob('*')):
        if plik.is_symlink():
            raise SystemExit('Wykryto dowiązanie w paczce.')
        if plik.is_file():
            z.write(plik, (Path(cel.name) / plik.relative_to(cel)).as_posix())
with zipfile.ZipFile(archiwum) as z:
    if z.testzip() is not None:
        raise SystemExit('Kontrola ZIP zakończona błędem.')
PY
mv -T -- "$paczka" "$wynik"
mv -T -- "$roboczy/$nazwa.zip" "$zip"
printf '\nGotowy folder: %s\nGotowy ZIP: %s\n' "$wynik" "$zip"
