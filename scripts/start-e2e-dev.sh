#!/bin/bash

# Script to start the E2E development environment
# This starts the Docker containers but doesn't run the tests

# Set environment variable to prevent teardown
export PLAYWRIGHT_TEARDOWN=false

# Check if containers are already running
BACKEND_RUNNING=$(docker ps --filter "name=eazy-recycling-backend-e2e-1" --format "{{.Names}}")
FRONTEND_RUNNING=$(docker ps --filter "name=eazy-recycling-frontend-e2e-1" --format "{{.Names}}")

if [ -n "$BACKEND_RUNNING" ] && [ -n "$FRONTEND_RUNNING" ]; then
  echo "✅ E2E environment is already running"
else
  echo "🚀 Starting E2E development environment..."
  docker-compose -f docker-compose.e2e.yml up -d --build
  
  echo "⏳ Waiting for services to be ready..."
  
  # Wait for backend health check to pass
  echo "  Waiting for backend to be healthy..."
  attempt=0
  max_attempts=30
  while [ $attempt -lt $max_attempts ]; do
    if curl -s http://localhost:8081/actuator/health > /dev/null; then
      echo "  ✅ Backend is healthy"
      break
    fi
    attempt=$((attempt+1))
    echo "  ⏳ Waiting for backend... ($attempt/$max_attempts)"
    sleep 2
  done
  
  if [ $attempt -eq $max_attempts ]; then
    echo "❌ Backend health check timed out"
  fi
  
  # Check if frontend is accessible
  echo "  Checking frontend..."
  if curl -s http://localhost:5174 > /dev/null; then
    echo "  ✅ Frontend is accessible"
  else
    echo "  ⚠️ Frontend may not be ready yet"
  fi
fi

echo "✅ E2E development environment is running"
echo "Run tests with: npm run e2e:dev"
echo "To stop the environment: ./scripts/stop-e2e-dev.sh"
