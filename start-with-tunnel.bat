@echo off
setlocal enabledelayedexpansion

echo ============================================
echo Powerlifting Assistant Server + cloudpub tunnel
echo ============================================
echo.

REM Path to clo.exe
set "CLO_PATH=d:\android projects\clo-3.0.2-stable-windows-x86_64\clo.exe"

REM Service GUID - выберите нужный:
REM Вариант 1: disloyally-limber-klipspringer (старый ПК - DESKTOP-SVF1BST)
REM set "SERVICE_GUID=243c975d-19e8-4adf-9a33-a52d01a78d14"
REM set "TUNNEL_URL=https://disloyally-limber-klipspringer.cloudpub.ru"

REM Вариант 2: sourly-benevolent-sanderling (новый аккаунт - текущий ПК)
set "SERVICE_GUID=1f3cd597-af45-4420-8d57-a6f3cf2fbc5b"
set "TUNNEL_URL=https://sourly-benevolent-sanderling.cloudpub.ru"

REM Server port
set SERVER_PORT=8080

cd /d "%~dp0"

echo [1/4] Stopping ALL existing tunnels...
"%CLO_PATH%" stop-all 2>nul
timeout /t 3 /nobreak >nul

echo [2/4] Starting server on port %SERVER_PORT%...
echo Server will open in a new window. Wait for "BUILD SUCCESSFUL" or server ready message.
echo.
start "Powerlifting Server" cmd /k "cd /d %~dp0 && gradlew.bat run"

echo [3/4] Starting server...
echo Server is starting in a new window. Wait ~60 seconds for it to be ready.
echo.

echo [4/4] Starting cloudpub tunnel in background...
echo Tunnel URL: %TUNNEL_URL%
echo.

REM Запускаем туннель в НОВОМ окне (не блокирует основной терминал)
start "Cloudpub Tunnel" cmd /k "cd /d "d:\android projects\clo-3.0.2-stable-windows-x86_64" && clo.exe start %SERVICE_GUID%"

timeout /t 5 /nobreak >nul

echo.
echo ============================================
echo SETUP COMPLETE
echo ============================================
echo Server:  Check the "Powerlifting Server" window
echo Tunnel:  %TUNNEL_URL%
echo.
echo IMPORTANT: 
echo 1. DO NOT run "clo.exe ls" - it may kill the tunnel!
echo 2. To check tunnel: open %TUNNEL_URL% in browser
echo 3. To stop tunnel:  Close "Cloudpub Tunnel" window
echo.
echo Build client with:
echo   gradlew assembleDebug -PPOWERLIFT_SERVER_BASE_URL="%TUNNEL_URL%/"
echo ============================================
echo.
pause
