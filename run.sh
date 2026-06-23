#!/bin/bash
# Nile dot com E-Store launcher (Gradle).
# Builds the project (compile + tests + coverage) then runs the desktop app.
set -e

echo "Nile dot com E-Store"
echo "===================="

if [ ! -x "./gradlew" ]; then
  echo "Error: ./gradlew not found. Run from the project root."
  exit 1
fi

echo "Building (compile + test + coverage)..."
./gradlew build --console=plain

echo "Launching..."
./gradlew run --console=plain
