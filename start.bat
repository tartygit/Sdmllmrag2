@echo off
echo ===================================================
echo Launching SDDE Docker Infrastructure (Windows Host)
echo ===================================================
docker compose -f docker/docker-compose.yml up -d
echo [SUCCESS] Docker services launched in background!
