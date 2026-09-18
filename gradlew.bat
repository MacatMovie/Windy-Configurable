@echo off
setlocal EnableExtensions

rem Windy multi-target build launcher.
rem Dispatches to each target's compatible Gradle/Java launcher because
rem the supported Minecraft/loaders span several Gradle and Java generations.
rem
rem Typical usage:
rem   .\gradlew build
rem   .\gradlew clean build
rem   .\gradlew clean

set "ROOT=%~dp0"
set "TASKS=%*"
if "%~1"=="" set "TASKS=build"

echo %TASKS% | findstr /I /C:"clean" >nul
if not errorlevel 1 if exist "%ROOT%build-output" rmdir /S /Q "%ROOT%build-output"

if not exist "%ROOT%targets\forge-1.20.1\gradlew.bat" (
    echo ERROR: Target projects are missing. Run this from the Windy monorepo root.
    exit /b 1
)

call :runTarget "Forge 1.20.1" "forge-1.20.1"
if errorlevel 1 exit /b %ERRORLEVEL%
call :runTarget "Fabric 1.20.1" "fabric-1.20.1"
if errorlevel 1 exit /b %ERRORLEVEL%
call :runTarget "Fabric 1.21.1" "fabric-1.21.1"
if errorlevel 1 exit /b %ERRORLEVEL%
call :runTarget "NeoForge 1.21.1" "neoforge-1.21.1"
if errorlevel 1 exit /b %ERRORLEVEL%
call :runTarget "Fabric 26.1.x" "fabric-26.1.x"
if errorlevel 1 exit /b %ERRORLEVEL%
call :runTarget "NeoForge 26.1.x" "neoforge-26.1.x"
if errorlevel 1 exit /b %ERRORLEVEL%
call :runTarget "Fabric 26.2.x" "fabric-26.2.x"
if errorlevel 1 exit /b %ERRORLEVEL%
call :runTarget "NeoForge 26.2.x" "neoforge-26.2.x"
if errorlevel 1 exit /b %ERRORLEVEL%
call :runTarget "Fabric 26.3.x" "fabric-26.3.x"
if errorlevel 1 exit /b %ERRORLEVEL%
call :runTarget "NeoForge 26.3.x" "neoforge-26.3.x"
if errorlevel 1 exit /b %ERRORLEVEL%

call :collectJars

echo.
echo ============================================================
echo All 10 Windy targets completed successfully.
echo If the build task was run, release JARs are collected in:
echo   %ROOT%build-output
echo ============================================================
exit /b 0

:runTarget
set "LABEL=%~1"
set "DIR=%~2"
echo.
echo ============================================================
echo Building %LABEL%
echo ============================================================
pushd "%ROOT%targets\%DIR%"
call gradlew.bat %TASKS%
set "RESULT=%ERRORLEVEL%"
popd
if not "%RESULT%"=="0" (
    echo.
    echo ERROR: %LABEL% failed with exit code %RESULT%.
    exit /b %RESULT%
)
exit /b 0

:collectJars
if not exist "%ROOT%build-output" mkdir "%ROOT%build-output" >nul 2>nul
for %%T in (forge-1.20.1 fabric-1.20.1 fabric-1.21.1 neoforge-1.21.1 fabric-26.1.x neoforge-26.1.x fabric-26.2.x neoforge-26.2.x fabric-26.3.x neoforge-26.3.x) do (
    for %%J in ("%ROOT%targets\%%T\build\libs\Windy_*.jar") do (
        if exist "%%~fJ" copy /Y "%%~fJ" "%ROOT%build-output\" >nul
    )
)
exit /b 0
