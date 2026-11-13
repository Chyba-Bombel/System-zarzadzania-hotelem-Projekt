param(
    [string]$StaraKonfiguracja,
    [string]$Instalator,
    [switch]$TylkoKonfiguracja
)

$ErrorActionPreference = 'Stop'
if ($env:OS -ne 'Windows_NT') { throw 'Ten skrypt wymaga Windows.' }
if (Get-Process -Name 'HotelManager' -ErrorAction SilentlyContinue) { throw 'Zamknij HotelManager przed aktualizacja.' }
$folder = Join-Path $env:LOCALAPPDATA 'HotelManagerData\config'
$cel = Join-Path $folder 'db.properties'
if (-not [string]::IsNullOrWhiteSpace($StaraKonfiguracja)) {
    $stary = (Resolve-Path -LiteralPath $StaraKonfiguracja).Path
    $tekst = Get-Content -Raw -LiteralPath $stary -Encoding UTF8
    foreach ($pole in @('url', 'user', 'password')) {
        if ($tekst -notmatch "(?m)^\s*$pole\s*[:=]") { throw "W starym pliku brakuje pola: $pole" }
    }
    New-Item -ItemType Directory -Force $folder | Out-Null
    if (-not (Test-Path -LiteralPath $cel)) {
        [System.IO.File]::Copy($stary, $cel, $false)
        Write-Host 'Przeniesiono konfiguracje do folderu uzytkownika.'
    } else {
        Write-Host 'Konfiguracja uzytkownika juz istnieje. Nie zostala nadpisana.'
    }
}
if (Test-Path -LiteralPath $cel) {
    $kopia = Join-Path $folder ('db.properties.backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N'))
    Copy-Item -LiteralPath $cel -Destination $kopia
    Write-Host 'Utworzono lokalna kopie konfiguracji. Nie udostepniaj jej innym osobom.'
}
if ($TylkoKonfiguracja) {
    if (-not (Test-Path -LiteralPath $cel)) { throw 'Brak konfiguracji. Wskaz -StaraKonfiguracja.' }
    Write-Host 'Zachowano konfiguracje. Instalator nie zostal uruchomiony.'
    return
}
if ([string]::IsNullOrWhiteSpace($Instalator)) {
    $pliki = @(Get-ChildItem -LiteralPath $PSScriptRoot -Filter 'HotelManager-*.exe' -File)
    if ($pliki.Count -ne 1) { throw 'Wskaz parametr -Instalator albo pozostaw obok skryptu jeden instalator HotelManager-*.exe.' }
    $Instalator = $pliki[0].FullName
} else {
    $Instalator = (Resolve-Path -LiteralPath $Instalator).Path
}
Write-Host 'Przed zmiana wersji wykonaj osobna kopie bazy danych. Skrypt nie wykonuje kopii ani migracji SQL.'
$proces = Start-Process -FilePath $Instalator -Wait -PassThru
if ($proces.ExitCode -notin @(0, 1641, 3010)) { throw "Instalator zakonczyl sie kodem: $($proces.ExitCode). Zachowano kopie konfiguracji." }
Write-Host 'Instalator zakonczyl prace. Sprawdz wersje i polaczenie w aplikacji.'
