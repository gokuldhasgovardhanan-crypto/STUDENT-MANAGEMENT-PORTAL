@echo off
title Student Attendance Management System
echo ===================================================================
echo Starting Student Attendance Management System...
echo ===================================================================

if exist "target\StudentAttendanceSystem.jar" (
    java -jar target\StudentAttendanceSystem.jar
) else (
    echo Fat JAR not found. Building with Maven...
    mvn clean package -DskipTests
    if exist "target\StudentAttendanceSystem.jar" (
        java -jar target\StudentAttendanceSystem.jar
    ) else (
        echo Build failed. Please ensure Java 17 and Maven are installed.
        pause
    )
)
