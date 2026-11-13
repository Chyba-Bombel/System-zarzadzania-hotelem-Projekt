param(
    [ValidateSet('exe', 'app-image')][string]$Typ = 'exe',
    [string]$Ikona = 'hotel.ico'
)

$ErrorActionPreference = 'Stop'
if ($env:OS -ne 'Windows_NT') { throw 'Instalator Windows trzeba budowac na Windows.' }
Push-Location $PSScriptRoot
try {
    foreach ($narzedzie in @('java', 'javac', 'mvn', 'jpackage')) {
        if (-not (Get-Command $narzedzie -ErrorAction SilentlyContinue)) { throw "Brak polecenia: $narzedzie" }
    }
    if ($Typ -eq 'exe') {
        $wixBin = Join-Path ${env:ProgramFiles(x86)} 'WiX Toolset v3.14\bin'
        if (Test-Path -LiteralPath $wixBin) { $env:Path = "$wixBin;$env:Path" }
        $wix3 = (Get-Command candle -ErrorAction SilentlyContinue) -and (Get-Command light -ErrorAction SilentlyContinue)
        if (-not $wix3 -and -not (Get-Command wix -ErrorAction SilentlyContinue)) { throw 'Brak WiX w PATH.' }
    }
    $ikonaPlik = (Resolve-Path -LiteralPath $Ikona).Path
    $naglowek = [System.IO.File]::ReadAllBytes($ikonaPlik)
    if ($naglowek.Length -lt 6 -or $naglowek[0] -ne 0 -or $naglowek[1] -ne 0 -or $naglowek[2] -ne 1 -or $naglowek[3] -ne 0) {
        throw 'Ikona musi byc prawdziwym plikiem ICO.'
    }
    [xml]$pom = Get-Content -Raw -LiteralPath 'pom.xml'
    $wersja = [string]$pom.project.version
    $artefakt = [string]$pom.project.artifactId
    $uuid = [guid]::Parse((Get-Content -Raw 'installer\upgrade-uuid.txt').Trim()).ToString()
    foreach ($modul in @('base', 'graphics', 'controls')) {
        if (-not (Test-Path "lib\javafx-win\javafx-$modul-17.0.21-win.jar")) { throw "Brak JavaFX Windows: $modul" }
    }
    if (-not (Test-Path 'lib\mysql-connector-j-9.7.0.jar')) { throw 'Brak sterownika JDBC.' }
    & mvn clean package '-Dmaven.test.skip=true'
    if ($LASTEXITCODE -ne 0) { throw 'Kompilacja Maven nie powiodla sie.' }
    $wejscie = New-Item -ItemType Directory -Force 'target\installer-input'
    $jar = "$artefakt-$wersja.jar"
    Copy-Item "target\$jar" $wejscie.FullName
    foreach ($modul in @('base', 'graphics', 'controls')) { Copy-Item "lib\javafx-win\javafx-$modul-17.0.21-win.jar" $wejscie.FullName }
    Copy-Item 'lib\mysql-connector-j-9.7.0.jar' $wejscie.FullName
    Copy-Item 'db.properties.example' $wejscie.FullName
    $wynik = Join-Path 'dist' "$wersja-$Typ"
    if (Test-Path -LiteralPath $wynik) { throw "Folder wyniku juz istnieje: $wynik. Zmien jego nazwe lub usun poprzedni wynik." }
    $argumenty = @(
        '--type', $Typ,
        '--name', 'HotelManager',
        '--app-version', $wersja,
        '--input', $wejscie.FullName,
        '--main-jar', $jar,
        '--main-class', 'Launcher',
        '--icon', $ikonaPlik,
        '--add-modules', 'java.se,jdk.unsupported,jdk.charsets',
        '--java-options', '--enable-native-access=ALL-UNNAMED',
        '--dest', $wynik
    )
    if ($Typ -eq 'exe') {
        $argumenty += @('--win-upgrade-uuid', $uuid, '--win-per-user-install', '--win-dir-chooser', '--win-menu', '--win-menu-group', 'HotelManager', '--win-shortcut')
    }
    & jpackage @argumenty
    if ($LASTEXITCODE -ne 0) { throw 'jpackage nie utworzyl pakietu.' }
    if ($Typ -eq 'exe') {
        Copy-Item 'installer\aktualizuj_windows.ps1' $wynik
        Copy-Item 'installer\README.txt' $wynik
        Copy-Item 'db' (Join-Path $wynik 'db') -Recurse
        Copy-Item 'db.properties.example' $wynik
    }
    Write-Host "Gotowe: $wynik"
} finally {
    Pop-Location
}
