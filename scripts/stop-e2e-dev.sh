#!/bin/bash

# Script to stop the E2E development environment

echo "🛑 Stopping E2E development environment..."
docker-compose -f docker-compose.e2e.yml down

echo "✅ E2E development environment stopped"
