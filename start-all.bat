@echo off
rem Double-click to start the backend and the Angular app (see start-all.ps1)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-all.ps1" -WithFrontend
pause
