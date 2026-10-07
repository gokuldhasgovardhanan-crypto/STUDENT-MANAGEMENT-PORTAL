@echo off
title Student Attendance Management System
echo ===================================================================
echo Starting Student Attendance Management System...
echo ===================================================================

if exist "target\StudentAttendanceSystem.jar" (
    java -jar target\StudentAttendanceSystem.jar
) else (
    echo Fat JAR not found. Building with Maven...
    where mvn >nul 2>&1
    if %errorlevel% equ 0 (
        mvn clean package -DskipTests
    ) else if exist "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" (
        "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" clean package -DskipTests
    ) else (
        echo Maven executable not found in PATH or standard locations.
    )

    if exist "target\StudentAttendanceSystem.jar" (
        java -jar target\StudentAttendanceSystem.jar
    ) else (
        echo Build failed. Please ensure Java 17 and Maven are installed.
        pause
    )
)
