@echo off
echo ===================================================
echo Building SDDE Monorepo Services (Windows Host)
echo ===================================================
cd backend
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Backend compilation failed!
    exit /b %ERRORLEVEL%
)
cd ../frontend
call npm install
call npm run build
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Frontend build compilation failed!
    exit /b %ERRORLEVEL%
)
echo ===================================================
echo [SUCCESS] SDDE Monorepo compiled successfully!
echo ===================================================
