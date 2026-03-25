@echo off
setlocal enabledelayedexpansion

set SHOULD_PAUSE=1
if /i "%NON_INTERACTIVE%"=="1" set SHOULD_PAUSE=0

echo.
echo ========================================
echo   POS System - Windows Installer Build
echo ========================================
echo.

cd /d "%~dp0"

where jpackage >nul 2>&1
if errorlevel 1 (
    echo ERROR: jpackage not found in PATH.
    echo Install JDK 17+ and make sure jpackage is available.
    if "%SHOULD_PAUSE%"=="1" pause
    exit /b 1
)

echo [1/7] Building backend jar...
call mvn -f pos-backend\pom.xml clean package -DskipTests
if errorlevel 1 (
    echo ERROR: Backend build failed.
    if "%SHOULD_PAUSE%"=="1" pause
    exit /b 1
)

echo [2/7] Building frontend jar...
call mvn -f pos-frontend\pom.xml clean package -DskipTests
if errorlevel 1 (
    echo ERROR: Frontend build failed.
    if "%SHOULD_PAUSE%"=="1" pause
    exit /b 1
)

echo [3/8] Copying frontend runtime dependencies...
call mvn -f pos-frontend\pom.xml dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target\dependencies
if errorlevel 1 (
    echo ERROR: Failed to copy frontend dependencies.
    if "%SHOULD_PAUSE%"=="1" pause
    exit /b 1
)

echo [4/8] Copying backend runtime dependencies...
call mvn -f pos-backend\pom.xml dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target\dependencies
if errorlevel 1 (
    echo ERROR: Failed to copy backend dependencies.
    if "%SHOULD_PAUSE%"=="1" pause
    exit /b 1
)

echo [5/8] Preparing jpackage input folder...
if exist dist rmdir /s /q dist
mkdir dist\input
mkdir dist\installer

set FRONTEND_JAR=
for %%F in (pos-frontend\target\pos-frontend-*.jar.original) do (
    set FRONTEND_JAR=%%~fF
)

if "%FRONTEND_JAR%"=="" (
    for %%F in (pos-frontend\target\pos-frontend-*.jar) do (
        echo %%~nxF | findstr /i /v ".jar.original" >nul
        if not errorlevel 1 set FRONTEND_JAR=%%~fF
    )
)

if "%FRONTEND_JAR%"=="" (
    echo ERROR: Could not find frontend runnable jar.
    if "%SHOULD_PAUSE%"=="1" pause
    exit /b 1
)

set BACKEND_JAR=
for %%F in (pos-backend\target\pos-backend-*.jar.original) do (
    set BACKEND_JAR=%%~fF
)

if "%BACKEND_JAR%"=="" (
    for %%F in (pos-backend\target\pos-backend-*.jar) do (
        echo %%~nxF | findstr /i /v ".jar.original" >nul
        if not errorlevel 1 set BACKEND_JAR=%%~fF
    )
)

if "%BACKEND_JAR%"=="" (
    echo ERROR: Could not find backend runnable jar.
    if "%SHOULD_PAUSE%"=="1" pause
    exit /b 1
)

echo [6/8] Copying app artifacts...
copy /y "%FRONTEND_JAR%" dist\input\pos-frontend.jar >nul
copy /y "%BACKEND_JAR%" dist\input\pos-backend.jar >nul
xcopy /e /i /y pos-frontend\target\dependencies dist\input >nul
xcopy /e /i /y pos-backend\target\dependencies dist\input >nul

set ICON_ARG=
if exist assets\app-icon.ico (
    set ICON_ARG=--icon assets\app-icon.ico
    echo Using custom icon: assets\app-icon.ico
) else if exist app-icon.ico (
    set ICON_ARG=--icon app-icon.ico
    echo Using custom icon: app-icon.ico
) else (
    echo No custom icon found. Default app icon will be used.
)

echo [7/8] Running jpackage...
where candle.exe >nul 2>&1
if errorlevel 1 goto APP_IMAGE_FALLBACK
where light.exe >nul 2>&1
if errorlevel 1 goto APP_IMAGE_FALLBACK

jpackage ^
    --type exe ^
    --name "POS System" ^
    --input dist\input ^
    --main-jar pos-frontend.jar ^
    --main-class com.posystem.fx.PosFxApplication ^
    --java-options "--module-path" ^
    --java-options "$APPDIR" ^
    --java-options "--add-modules=javafx.controls,javafx.fxml" ^
    --dest dist\installer ^
    --win-shortcut ^
    --win-menu ^
    %ICON_ARG% ^
    --vendor "POS System" ^
    --description "Point of Sale Desktop Application"

if errorlevel 1 (
    echo WARNING: EXE packaging failed. Falling back to portable app-image...
    goto APP_IMAGE_FALLBACK
)
goto PACKAGE_DONE

:APP_IMAGE_FALLBACK
echo WiX tools not found. Creating portable app-image instead...
jpackage ^
    --type app-image ^
    --name "POS System" ^
    --input dist\input ^
    --main-jar pos-frontend.jar ^
    --main-class com.posystem.fx.PosFxApplication ^
    --java-options "--module-path" ^
    --java-options "$APPDIR" ^
    --java-options "--add-modules=javafx.controls,javafx.fxml" ^
    --dest dist\installer ^
    %ICON_ARG% ^
    --vendor "POS System" ^
    --description "Point of Sale Desktop Application"

if errorlevel 1 (
        echo ERROR: app-image packaging failed.
    if "%SHOULD_PAUSE%"=="1" pause
        exit /b 1
)

:PACKAGE_DONE

echo [8/8] Done.
echo Installer created in: dist\installer
echo.
if "%SHOULD_PAUSE%"=="1" pause
