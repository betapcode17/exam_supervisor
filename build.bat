@echo off
REM Build script for Exam Supervisor Assignment System

echo =========================================
echo Building Exam Supervisor Assignment System
echo =========================================

REM Check Maven installation
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven is not installed or not in PATH
    exit /b 1
)

REM Check Java installation
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java is not installed or not in PATH
    exit /b 1
)

echo.
echo Java version:
java -version

echo.
echo Maven version:
mvn --version

REM Clean and build
echo.
echo Step 1: Cleaning...
call mvn clean

echo.
echo Step 2: Compiling...
call mvn compile

echo.
echo Step 3: Running tests...
call mvn test

echo.
echo Step 4: Packaging...
call mvn package

echo.
echo =========================================
echo Build completed successfully!
echo =========================================
echo.
echo To start server: java -cp target/classes;target/dependency/* MainServer 8888
echo To start client: java -cp target/classes;target/dependency/* MainClient
pause
