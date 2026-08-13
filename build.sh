#!/bin/bash
echo "Building SDDE Services..."
cd backend && mvn clean package -DskipTests
cd ../frontend && npm run build
