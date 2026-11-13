#!/usr/bin/env bash
set -euo pipefail
trap 'printf "Budowanie przerwane w wierszu %s. Nie publikuj niekompletnej paczki.\n" "$LINENO" >&2' ERR

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
if [[ "$(uname -s)" != Linux ]]; then printf 'Ten skrypt wymaga Linuksa.\n' >&2; exit 1; fi
typ="${1:-deb}"
ikona="${2:-hotel.png}"
if [[ "$typ" != deb && "$typ" != app-image ]] || (( $# > 2 )); then
    printf 'Użycie: bash buduj_instalator_linux.sh [deb|app-image] [ścieżka-do-ikony.png]\n' >&2
    exit 1
fi
for narzedzie in java javac mvn jpackage python3; do
    command -v "$narzedzie" >/dev/null || { printf 'Brak polecenia: %s\n' "$narzedzie" >&2; exit 1; }
done
if [[ "$typ" == deb ]]; then
    for narzedzie in fakeroot dpkg-deb dpkg-shlibdeps dpkg-query; do
        command -v "$narzedzie" >/dev/null || { printf 'Brak polecenia: %s\n' "$narzedzie" >&2; exit 1; }
    done
fi
wersja="$(python3 - <<'PY'
import re
import xml.etree.ElementTree as E
v = E.parse('pom.xml').getroot().findtext('{http://maven.apache.org/POM/4.0.0}version')
if not v or not re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+', v):
    raise SystemExit('Wersja projektu musi mieć postać X.Y.Z.')
print(v)
PY
)"
arch="$(uname -m)"
wynik="$PWD/dist/$wersja-linux-$arch-$typ"
archiwum="$wynik.zip"
if [[ -e "$wynik" || -e "$archiwum" ]]; then
    printf 'Wynik już istnieje: %s. Przenieś go lub zmień nazwę przed kolejną budową.\n' "$wynik" >&2
    exit 1
fi
opcje_ikony=()
if [[ -f "$ikona" ]]; then
    python3 - "$ikona" <<'PY'
import sys
with open(sys.argv[1], 'rb') as f:
    if f.read(8) != b'\x89PNG\r\n\x1a\n':
        raise SystemExit('Ikona musi być prawdziwym plikiem PNG.')
PY
    opcje_ikony=(--icon "$ikona")
elif (( $# >= 2 )); then
    printf 'Nie znaleziono wskazanej ikony: %s\n' "$ikona" >&2
    exit 1
else
    printf 'Brak hotel.png — pakiet otrzyma domyślną ikonę.\n'
fi
mvn -ntp clean package -Dmaven.test.skip=true
wejscie="$PWD/target/installer-input-linux"
mkdir -p "$wejscie"
mvn -ntp org.apache.maven.plugins:maven-dependency-plugin:3.11.0:copy-dependencies \
    -DincludeScope=runtime "-DoutputDirectory=$wejscie"
cp -- "target/hotel-gui-$wersja.jar" "$wejscie/"
if compgen -G "$wejscie/*-win.jar" >/dev/null; then
    printf 'Wykryto biblioteki JavaFX Windows. Sprawdź zależności Maven.\n' >&2
    exit 1
fi
argumenty=(--type "$typ" --name HotelManager --app-version "$wersja"
    --input "$wejscie" --main-jar "hotel-gui-$wersja.jar" --main-class Launcher
    --add-modules java.se,jdk.unsupported,jdk.charsets
    --java-options --enable-native-access=ALL-UNNAMED
    --dest "$wynik")
if [[ "$typ" == deb ]]; then
    gtk="${HOTEL_GTK_PACKAGE:-}"
    if [[ -n "$gtk" && "$gtk" != libgtk-3-0 && "$gtk" != libgtk-3-0t64 ]]; then
        printf 'HOTEL_GTK_PACKAGE musi mieć wartość libgtk-3-0 lub libgtk-3-0t64.\n' >&2
        exit 1
    fi
    for kandydat in libgtk-3-0t64 libgtk-3-0; do
        if [[ -n "$gtk" ]]; then break; fi
        if [[ "$(dpkg-query -W -f='${Status}' "$kandydat" 2>/dev/null || true)" == 'install ok installed' ]]; then gtk="$kandydat"; break; fi
    done
    if [[ -z "$gtk" ]]; then printf 'Nie ustalono pakietu GTK 3. Zainstaluj GTK albo ustaw HOTEL_GTK_PACKAGE na nazwę pakietu docelowego systemu: libgtk-3-0 lub libgtk-3-0t64.\n' >&2; exit 1; fi
    argumenty+=(--linux-package-name hotel-manager --linux-shortcut
        --linux-package-deps "$gtk, libc6, libstdc++6, libfreetype6, libfontconfig1, libx11-6, libxi6, libxrender1, libxtst6")
fi
jpackage "${argumenty[@]}" "${opcje_ikony[@]}"
if [[ "$typ" == deb ]]; then
    cp -- installer/README-Linux.txt "$wynik/README-Linux.txt"
    cp -- db.properties.example "$wynik/"
    cp -R -- db "$wynik/db"
    python3 - "$wynik" "$archiwum" <<'PY'
from pathlib import Path
import sys
import zipfile
root, target = map(Path, sys.argv[1:])
if len(list(root.glob('*.deb'))) != 1:
    raise SystemExit('Brak jednego gotowego pakietu DEB.')
tmp = target.with_suffix('.zip.tmp')
try:
    with zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as z:
        for p in sorted(root.rglob('*')):
            if p.is_symlink():
                raise SystemExit('Paczka nie może zawierać dowiązań do innych plików.')
            if p.is_file():
                z.write(p, p.relative_to(root).as_posix())
    tmp.rename(target)
finally:
    tmp.unlink(missing_ok=True)
print('Gotowe archiwum:', target)
PY
else
    printf 'Gotowy podgląd: %s/HotelManager/bin/HotelManager\n' "$wynik"
fi
