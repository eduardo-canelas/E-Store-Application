#!/bin/bash

# Nile Dot Com E-Store Application Launcher
# Author: Eduardo Canelas
# Description: Automated launcher script for the E-Store application

echo "🛒 Welcome to Nile Dot Com E-Store!"
echo "===================================="
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java is not installed or not in PATH"
    echo "Please install Java 8 or higher and try again"
    echo "Download from: https://www.oracle.com/java/technologies/downloads/"
    exit 1
fi

# Display Java version
echo "☕ Java version:"
java -version
echo ""

# Check if NileDotCom.java exists
if [ ! -f "NileDotCom.java" ]; then
    echo "❌ Error: NileDotCom.java not found in current directory"
    echo "Please make sure you're in the correct directory"
    exit 1
fi

# Check if inventory.csv exists
if [ ! -f "inventory.csv" ]; then
    echo "❌ Error: inventory.csv not found"
    echo "The inventory file is required for the application to run"
    exit 1
fi

echo "🔧 Compiling application..."
javac NileDotCom.java

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo ""
    echo "🚀 Launching Nile Dot Com E-Store..."
    echo "   - Modern GUI with professional styling"
    echo "   - Smart inventory management system"
    echo "   - Quantity-based discount pricing"
    echo "   - Complete transaction processing"
    echo ""
    java NileDotCom
else
    echo "❌ Compilation failed!"
    echo "Please check the source code for errors"
    exit 1
fi