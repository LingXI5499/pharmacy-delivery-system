
@echo off
chcp 65001 >nul
cd /d "%~dp0..\backend"
echo Starting Spring Boot backend on http://localhost:8089 ...
mvn spring-boot:run
pause
