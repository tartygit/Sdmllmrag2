@echo off
echo ===================================================
echo Launching SDDE Test Runner (Windows Host)
echo ===================================================
cd backend
call mvn test
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Backend tests failed!
    exit /b %ERRORLEVEL%
)
echo ===================================================
echo [SUCCESS] SDDE backend test suite passed flawlessly!
echo ===================================================
