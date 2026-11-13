@echo off
rem Double-click to stop all the services (see stop-all.ps1)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-all.ps1"
pause
