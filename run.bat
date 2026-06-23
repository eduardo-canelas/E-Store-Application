@echo off
REM Nile dot com E-Store launcher (Gradle) for Windows.
title Nile dot com E-Store

echo Nile dot com E-Store
echo ====================

if not exist "gradlew.bat" (
    echo Error: gradlew.bat not found. Run from the project root.
    exit /b 1
)

echo Building (compile + test + coverage)...
call gradlew.bat build --console=plain
if %errorlevel% neq 0 ( echo Build failed. & exit /b 1 )

echo Launching...
call gradlew.bat run --console=plain
