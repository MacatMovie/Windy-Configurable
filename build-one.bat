@echo off
setlocal EnableExtensions
if "%~1"=="" goto usage
set "ROOT=%~dp0"
set "TARGET=%~1"
shift

rem Build the remaining command-line arguments into the Gradle task/options list.
rem %%* is not changed by SHIFT in cmd.exe, so collect %%1 manually instead.
set "TASKS="
:collectTasks
if "%~1"=="" goto tasksReady
if defined TASKS (
    set "TASKS=%TASKS% %1"
) else (
    set "TASKS=%1"
)
shift
goto collectTasks

:tasksReady
if not defined TASKS set "TASKS=build"
if not exist "%ROOT%targets\%TARGET%\gradlew.bat" goto usage

pushd "%ROOT%targets\%TARGET%"
call gradlew.bat %TASKS%
set "RESULT=%ERRORLEVEL%"
popd

if not "%RESULT%"=="0" exit /b %RESULT%

echo %TASKS% | findstr /I /C:"build" >nul
if errorlevel 1 exit /b 0

if not exist "%ROOT%build-output" mkdir "%ROOT%build-output" >nul 2>nul
for %%J in ("%ROOT%targets\%TARGET%\build\libs\Windy_*.jar") do (
    if exist "%%~fJ" copy /Y "%%~fJ" "%ROOT%build-output\" >nul
)

echo.
echo Single-target build completed successfully.
echo Release JAR copied to:
echo   %ROOT%build-output
exit /b 0

:usage
echo Usage: .\build-one.bat TARGET [gradle tasks/options]
echo.
echo Targets:
echo   forge-1.20.1
echo   fabric-1.20.1
echo   fabric-1.21.1
echo   neoforge-1.21.1
echo   fabric-26.1.x
echo   neoforge-26.1.x
echo   fabric-26.2.x
echo   neoforge-26.2.x
echo   fabric-26.3.x
echo   neoforge-26.3.x
echo.
echo Example: .\build-one.bat neoforge-26.3.x clean build
exit /b 1
