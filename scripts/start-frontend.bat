
@echo off
chcp 65001 >nul
cd /d "%~dp0..\frontend"
echo Installing dependencies if needed and starting Vite on http://localhost:5173 ...
if not exist node_modules npm install
npm run dev
pause
