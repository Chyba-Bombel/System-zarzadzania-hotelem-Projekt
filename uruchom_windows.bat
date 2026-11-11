@echo off
setlocal DisableDelayedExpansion
chcp 65001 >nul
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
  echo Brak JDK w PATH. Zainstaluj JDK 17 lub nowszy albo uruchom projekt przez Maven w IntelliJ.
  pause
  exit /b 1
)
if not exist "lib\javafx-win\javafx-controls-17.0.18-win.jar" (
  echo Brak bibliotek JavaFX dla Windows. Rozpakuj cala paczke ZIP.
  pause
  exit /b 1
)
if not exist "out-gui" mkdir "out-gui"
(for /R "src" %%F in (*.java) do (
  set "plikZrodlowy=%%~fF"
  setlocal EnableDelayedExpansion
  echo "!plikZrodlowy:\=/!"
  endlocal
)) > ".sources-gui.txt"
echo Kompilowanie programu...
javac -encoding UTF-8 --release 17 --module-path "lib\javafx-win" --add-modules javafx.controls -cp "lib\mysql-connector-j-9.2.0.jar" -d "out-gui" @.sources-gui.txt
if errorlevel 1 (
  echo Nie udalo sie skompilowac programu. Sprawdz komunikaty powyzej.
  del ".sources-gui.txt" >nul 2>nul
  pause
  exit /b 1
)
del ".sources-gui.txt" >nul 2>nul
copy /Y "resources\hotel.css" "out-gui\hotel.css" >nul
java --module-path "lib\javafx-win" --add-modules javafx.controls -cp "out-gui;lib\mysql-connector-j-9.2.0.jar" Launcher
if errorlevel 1 pause
endlocal
