@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo ============================================================
echo Windy - REBUILD BUILD-OUTPUT FROM EXISTING BUILDS
echo ============================================================
echo This does NOT compile anything.
echo It only copies already-built release jars from targets\*\build\libs.
echo ============================================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build.ps1" -CollectOnly
set "EXITCODE=%ERRORLEVEL%"

echo.
if "%EXITCODE%"=="0" (
    echo Finished. Jars are in: %~dp0build-output
) else (
    echo Collection failed. Exit code: %EXITCODE%
)
echo.
pause
exit /b %EXITCODE%
