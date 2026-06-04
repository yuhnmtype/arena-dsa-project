@echo off
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
if %errorlevel% == 0 (
    echo Compile successful!
) else (
    echo Compile failed!
)
del sources.txt