@echo off
setlocal EnableExtensions
cd /d "%~dp0"

if not exist "%~dp0logs" mkdir "%~dp0logs"
for /f %%I in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd-HHmmss"') do set "STAMP=%%I"
set "LOG=%~dp0logs\build-all-%STAMP%.log"
set "LATEST=%~dp0logs\build-all-latest.log"

echo ============================================================
echo Windy - BUILD ALL
echo ============================================================
echo Live output is enabled.
echo Log: %LOG%
echo Finished jars: %~dp0build-output
echo ============================================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build.ps1" -All -LogPath "%LOG%" %*
set "EXITCODE=%ERRORLEVEL%"
copy /Y "%LOG%" "%LATEST%" >nul 2>nul

echo.
echo ============================================================
if "%EXITCODE%"=="0" (
    echo BUILD ALL FINISHED SUCCESSFULLY.
    echo Finished jars are in: %~dp0build-output
) else (
    echo BUILD ALL FAILED. Exit code: %EXITCODE%
    echo Please send me this log file:
    echo   %LATEST%
)
echo ============================================================
echo.
pause
exit /b %EXITCODE%
