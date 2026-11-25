@echo off
if not exist bin mkdir bin
echo Compiling Java Backend and DSA modules...
javac -d bin src\model\*.java src\dsa\*.java src\db\*.java src\server\*.java src\Main.java
if %errorlevel% neq 0 (
    echo Compilation failed. Make sure JDK is installed.
    pause
    exit /b %errorlevel%
)
echo Starting Inventory Management System Server...
java -cp "bin;lib\*" Main
pause
