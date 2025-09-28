@echo off
REM Nile Dot Com E-Store Application Launcher for Windows
REM Author: Eduardo Canelas
REM Description: Automated launcher script for the E-Store application

title Nile Dot Com E-Store Launcher

echo.
echo 🛒 Welcome to Nile Dot Com E-Store!
echo ====================================
echo.

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Error: Java is not installed or not in PATH
    echo Please install Java 8 or higher and try again
    echo Download from: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

REM Display Java version
echo ☕ Java version:
java -version
echo.

REM Check if NileDotCom.java exists
if not exist "NileDotCom.java" (
    echo ❌ Error: NileDotCom.java not found in current directory
    echo Please make sure you're in the correct directory
    pause
    exit /b 1
)

REM Check if inventory.csv exists
if not exist "inventory.csv" (
    echo ❌ Error: inventory.csv not found
    echo The inventory file is required for the application to run
    pause
    exit /b 1
)

echo 🔧 Compiling application...
javac NileDotCom.java

if %errorlevel% equ 0 (
    echo ✅ Compilation successful!
    echo.
    echo 🚀 Launching Nile Dot Com E-Store...
    echo    - Modern GUI with professional styling
    echo    - Smart inventory management system
    echo    - Quantity-based discount pricing
    echo    - Complete transaction processing
    echo.
    java NileDotCom
) else (
    echo ❌ Compilation failed!
    echo Please check the source code for errors
    pause
    exit /b 1
)