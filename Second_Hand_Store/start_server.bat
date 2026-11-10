@echo off
title SecondHand MarketPlace Server
echo ============================================================
echo         Starting SecondHand MarketPlace...
echo ============================================================
echo Compiling SecondHandMarketplace.java...
javac SecondHandMarketplace.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Starting Application and Web Server at http://localhost:8080/ ...
echo.
java SecondHandMarketplace
pause
