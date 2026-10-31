@echo off
title CivSim Civilization Simulator
echo ========================================================
echo   Starting CivSim: Human Life ^& Civilization Simulator
echo ========================================================
echo.
echo Launching server on http://localhost:8081 ...
echo Opening your default browser...
start http://localhost:8081
echo.
if exist "C:\Program Files\Java\jdk-25.0.4.1\bin\java.exe" (
    "C:\Program Files\Java\jdk-25.0.4.1\bin\java.exe" -cp "bin;lib/*" civsim.web.CivSimWebApplication
) else (
    java -cp "bin;lib/*" civsim.web.CivSimWebApplication
)
pause

