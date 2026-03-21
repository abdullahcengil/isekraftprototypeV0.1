@echo off
cd /d "%~dp0"
title IseKraft Mod Builder
color 0A

echo.
echo  ============================================
echo   IseKraft RPG Mod Builder for Windows
echo  ============================================
echo.

REM ── Step 1: Java ─────────────────────────────────────────────────────────────
echo [1/4] Checking Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo  ERROR: Java 17 not found.
    echo  Install from: https://adoptium.net
    pause & exit /b 1
)
echo  Java found!

REM ── Step 2: Gradle wrapper ───────────────────────────────────────────────────
echo.
echo [2/4] Checking Gradle...
if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo  Downloading...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol='Tls12'; (New-Object Net.WebClient).DownloadFile('https://raw.githubusercontent.com/gradle/gradle/v8.6.0/gradle/wrapper/gradle-wrapper.jar','gradle\wrapper\gradle-wrapper.jar')" >nul 2>&1
    if not exist "gradle\wrapper\gradle-wrapper.jar" (
        curl -sL "https://raw.githubusercontent.com/gradle/gradle/v8.6.0/gradle/wrapper/gradle-wrapper.jar" -o "gradle\wrapper\gradle-wrapper.jar" >nul 2>&1
    )
)
if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo  ERROR: Could not download Gradle. Check internet connection.
    pause & exit /b 1
)
echo  Gradle ready!

REM ── Step 3: Build ────────────────────────────────────────────────────────────
echo.
echo [3/4] Building mod...
echo  (First run: downloads ~200MB. Takes 5-10 min.)
echo.

if exist ".gradle" rmdir /s /q ".gradle" >nul 2>&1
if exist "build"   rmdir /s /q "build"   >nul 2>&1

call gradlew.bat build --no-daemon --warning-mode none
set BUILD_RESULT=%errorlevel%

REM Gradle sometimes exits 0 even on warnings, check the jar exists
if not exist "build\libs\isekraft-1.1.0.jar" (
    if %BUILD_RESULT% neq 0 (
        echo.
        echo  BUILD FAILED. Paste the error above and send to Claude.
        pause & exit /b 1
    )
)

echo.
echo  Build complete!

REM ── Step 4: Install ──────────────────────────────────────────────────────────
echo.
echo [4/4] Installing to mods folder...

set MODS=%APPDATA%\.minecraft\mods
if not exist "%MODS%" mkdir "%MODS%"

del "%MODS%\isekraft-*.jar" >nul 2>&1
copy /Y "build\libs\isekraft-1.1.0.jar" "%MODS%\isekraft-1.1.0.jar" >nul 2>&1

if exist "%MODS%\isekraft-1.1.0.jar" (
    echo  Installed: %MODS%\isekraft-1.1.0.jar
) else (
    echo  Could not auto-install. Copy manually:
    echo    FROM: build\libs\isekraft-1.1.0.jar
    echo    TO:   %MODS%
)

echo.
echo  ============================================
echo   DONE! Launch Minecraft with Fabric 1.20.1
echo.
echo   Also need: Fabric API in mods folder
echo   https://modrinth.com/mod/fabric-api
echo  ============================================
echo.
pause
