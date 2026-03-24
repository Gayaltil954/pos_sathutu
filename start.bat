@echo off
REM POS System Quick Start Script for Windows

echo.
echo ========================================
echo   POS SYSTEM - MongoDB Atlas Edition
echo ========================================
echo.

REM Check if Java is installed
echo Checking Java installation...
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java 17+ not found!
    echo Please install Java from https://www.oracle.com/java/technologies/
    pause
    exit /b 1
)

REM Check if Maven is installed
echo Checking Maven installation...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven not found!
    echo Please install Maven from https://maven.apache.org/
    pause
    exit /b 1
)

echo Java and Maven are installed!
echo.
echo MongoDB Atlas Configuration: ACTIVE
echo Connection: Cluster0 (POS_Sathutu database)
echo.

REM Build and start backend
echo.
echo ========================================
echo   Building Backend...
echo ========================================
echo.
cd /d "%~dp0"
echo Current directory: %CD%
cd pos-backend

echo Building with Maven...
call mvn clean install -q

if errorlevel 1 (
    echo ERROR: Backend build failed!
    echo Please check Maven configuration and try again.
    pause
    exit /b 1
)

echo Build successful!
echo.
echo ========================================
echo   Starting Backend Server...
echo ========================================
echo Backend will start on: http://localhost:8080
echo.

REM Start backend in a new window
start "POS Backend Server" cmd /k mvn spring-boot:run

REM Wait for backend to start
echo.
echo Waiting for backend to initialize (35 seconds)...
timeout /t 35 /nobreak

REM Initialize categories
echo.
echo Initializing default categories...
curl -X POST http://localhost:8080/api/categories/initialize >nul 2>&1
echo Categories initialized!
echo.

REM Build frontend
echo ========================================
echo   Building Frontend...
echo ========================================
echo.
cd /d "%~dp0"
cd pos-frontend

echo Building Frontend with Maven...
call mvn clean install -q

if errorlevel 1 (
    echo ERROR: Frontend build failed!
    pause
    exit /b 1
)

echo Build successful!
echo.

echo ========================================
echo   Launching POS Application...
echo ========================================
echo.
echo Starting JavaFX Application...
call mvn javafx:run

echo.
echo ========================================
echo   Application Closed
echo ========================================
echo.
pause
