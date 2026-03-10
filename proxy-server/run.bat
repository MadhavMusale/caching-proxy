@echo off
echo Starting CacheProxy Full Stack...
echo.

where docker >nul 2>nul
if %errorlevel% neq 0 (
  echo Docker not found. Install from https://docker.com
  exit /b 1
)

echo Building and starting containers...
docker compose up --build -d

echo.
echo CacheProxy is running!
echo.
echo   Frontend Dashboard  --^> http://localhost:3000
echo   Backend API         --^> http://localhost:8080
echo   Cache Stats         --^> http://localhost:8080/cache/stats
echo   Health Check        --^> http://localhost:8080/cache/health
echo.
echo   Stop: docker compose down
