@echo off

rem When launched by Explorer, Windows commonly runs BAT files through cmd /c.
rem The interactive multi-select launcher can then disappear as soon as it
rem finishes. Re-launch it once under cmd /k so the console stays open.
if not defined WINDY_BUILD_ONE_KEEP_OPEN (
    echo(%CMDCMDLINE% | findstr /I /C:" /c " >nul
    if not errorlevel 1 (
        set "WINDY_BUILD_ONE_KEEP_OPEN=1"
        start "" "%ComSpec%" /k ""%~f0" %*"
        exit /b
    )
)

setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

if not exist "%~dp0logs" mkdir "%~dp0logs"

echo ============================================================
echo Windy - BUILD SELECTED
echo ============================================================
echo.
echo   A) Forge    1.20.1
echo   B) Fabric   1.20.1
echo   C) Fabric   1.21.1
echo   D) NeoForge 1.21.1
echo   E) Fabric   26.1.x
echo   F) NeoForge 26.1.x
echo   G) Fabric   26.2.x
echo   H) NeoForge 26.2.x
echo   I) Fabric   26.3.x
echo   J) NeoForge 26.3.x
echo.
echo Enter one letter for a single build, or multiple letters separated
echo by commas for several builds.
echo Example: J
echo Example: E,F,G,H,I,J
echo.

if "%~1"=="" (
    set /p "SELECTION=Build target(s): "
) else (
    set "SELECTION=%~1"
)

if not defined SELECTION (
    echo.
    echo No target entered. Nothing was built.
    echo.
    pause
    exit /b 1
)

set "CLEANARG="
if /I "%~2"=="-Clean" set "CLEANARG=-Clean"

for /f %%I in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd-HHmmss"') do set "STAMP=%%I"

set /a BUILT=0
set /a FAILED=0
set "FAILURES="

rem Allow both comma-separated and space-separated input.
set "ITEMS=%SELECTION:,= %"

echo.
echo ============================================================
echo Selected: %SELECTION%
if defined CLEANARG echo Clean build: YES
echo ============================================================
echo.

for %%S in (%ITEMS%) do (
    call :ResolveTarget "%%~S"
    if defined RESOLVED (
        call :BuildTarget "!RESOLVED!"
    ) else (
        echo [WARNING] Unknown selection: %%~S
        set /a FAILED+=1
        set "FAILURES=!FAILURES! %%~S"
    )
)

echo.
echo ============================================================
if "!FAILED!"=="0" (
    echo ALL SELECTED BUILDS FINISHED SUCCESSFULLY.
    echo Built !BUILT! target(s).
    echo Finished jars are in: %~dp0build-output
) else (
    echo SELECTED BUILDS FINISHED WITH !FAILED! FAILURE(S).
    echo Failed:!FAILURES!
)
echo ============================================================
echo.
pause

if "!FAILED!"=="0" (
    exit /b 0
) else (
    exit /b 1
)

:ResolveTarget
set "RESOLVED="
set "CHOICE=%~1"

if /I "%CHOICE%"=="A" set "RESOLVED=forge-1.20.1"
if /I "%CHOICE%"=="B" set "RESOLVED=fabric-1.20.1"
if /I "%CHOICE%"=="C" set "RESOLVED=fabric-1.21.1"
if /I "%CHOICE%"=="D" set "RESOLVED=neoforge-1.21.1"
if /I "%CHOICE%"=="E" set "RESOLVED=fabric-26.1.x"
if /I "%CHOICE%"=="F" set "RESOLVED=neoforge-26.1.x"
if /I "%CHOICE%"=="G" set "RESOLVED=fabric-26.2.x"
if /I "%CHOICE%"=="H" set "RESOLVED=neoforge-26.2.x"
if /I "%CHOICE%"=="I" set "RESOLVED=fabric-26.3.x"
if /I "%CHOICE%"=="J" set "RESOLVED=neoforge-26.3.x"

rem Backwards compatibility: full target names still work too.
if not defined RESOLVED (
    if exist "%~dp0targets\%CHOICE%\" set "RESOLVED=%CHOICE%"
)

exit /b 0

:BuildTarget
set "TARGET=%~1"
set "LOG=%~dp0logs\build-%TARGET%-%STAMP%.log"
set "LATEST=%~dp0logs\build-one-latest.log"

echo.
echo ============================================================
echo BUILDING: %TARGET%
echo ============================================================
echo Log: %LOG%
echo.

if defined CLEANARG (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build.ps1" "%TARGET%" -LogPath "%LOG%" -Clean
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build.ps1" "%TARGET%" -LogPath "%LOG%"
)

set "EXITCODE=%ERRORLEVEL%"
copy /Y "%LOG%" "%LATEST%" >nul 2>nul

if "%EXITCODE%"=="0" (
    echo.
    echo === %TARGET% SUCCESS ===
    set /a BUILT+=1
) else (
    echo.
    echo === %TARGET% FAILED ^(exit code %EXITCODE%^) ===
    echo Log: %LOG%
    set /a FAILED+=1
    set "FAILURES=!FAILURES! %TARGET%"
)

exit /b 0
