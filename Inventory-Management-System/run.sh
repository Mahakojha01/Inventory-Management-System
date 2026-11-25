#!/usr/bin/env bash
set -e
mkdir -p bin
echo "Compiling Java Backend and DSA modules..."
javac -d bin src/model/*.java src/dsa/*.java src/db/*.java src/server/*.java src/Main.java
echo "Starting Inventory Management System Server..."
java -cp "bin:lib/*" Main
