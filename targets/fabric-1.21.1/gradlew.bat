@echo off
setlocal EnableExtensions

rem Project-specific Gradle/Java launcher for this source project.
rem Usage: .\gradlew build   or   .\gradlew clean build

set "REQUIRED_GRADLE=9.5.1"
set "REQUIRED_JAVA=21"
set "GRADLE_EXE=C:\Gradle\gradle-%REQUIRED_GRADLE%\bin\gradle.bat"

if not exist "%GRADLE_EXE%" (
    echo ERROR: Required Gradle %REQUIRED_GRADLE% was not found at:
    echo   %GRADLE_EXE%
    echo.
    echo Install/copy Gradle %REQUIRED_GRADLE% to C:\Gradle\gradle-%REQUIRED_GRADLE% or update GRADLE_EXE in gradlew.bat.
    exit /b 1
)

call :selectJava
if errorlevel 1 exit /b 1

set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Using Gradle %REQUIRED_GRADLE%
echo Using JAVA_HOME=%JAVA_HOME%
echo.
call "%GRADLE_EXE%" %*
exit /b %ERRORLEVEL%

:selectJava
if "%REQUIRED_JAVA%"=="17" goto java17
if "%REQUIRED_JAVA%"=="21" goto java21
if "%REQUIRED_JAVA%"=="25" goto java25
goto javaFallback

:java17
if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-17"
    exit /b 0
)
for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do (
    if exist "%%~fD\bin\java.exe" (
        set "JAVA_HOME=%%~fD"
        exit /b 0
    )
)
for /d %%D in ("C:\Program Files\Microsoft\jdk-17*") do (
    if exist "%%~fD\bin\java.exe" (
        set "JAVA_HOME=%%~fD"
        exit /b 0
    )
)
goto javaFallback

:java21
if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    exit /b 0
)
for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do (
    if exist "%%~fD\bin\java.exe" (
        set "JAVA_HOME=%%~fD"
        exit /b 0
    )
)
for /d %%D in ("C:\Program Files\Microsoft\jdk-21*") do (
    if exist "%%~fD\bin\java.exe" (
        set "JAVA_HOME=%%~fD"
        exit /b 0
    )
)
if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
    exit /b 0
)
goto javaFallback

:java25
if exist "C:\Program Files\Java\jdk-25\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-25"
    exit /b 0
)
for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-25*") do (
    if exist "%%~fD\bin\java.exe" (
        set "JAVA_HOME=%%~fD"
        exit /b 0
    )
)
for /d %%D in ("C:\Program Files\Microsoft\jdk-25*") do (
    if exist "%%~fD\bin\java.exe" (
        set "JAVA_HOME=%%~fD"
        exit /b 0
    )
)
goto javaFallback

:javaFallback
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
    echo WARNING: Could not auto-find the preferred Java %REQUIRED_JAVA% install.
    echo Falling back to existing JAVA_HOME=%JAVA_HOME%
    exit /b 0
)

echo ERROR: Java %REQUIRED_JAVA% could not be found automatically.
echo Expected a JDK under C:\Program Files\Java, Eclipse Adoptium, or Microsoft.
echo Set JAVA_HOME once, then run .\gradlew again.
exit /b 1
