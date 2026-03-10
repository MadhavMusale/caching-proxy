#!/bin/bash
set -e

echo "⚡ CacheProxy — Production Stack"
echo ""

# Check Docker
if ! command -v docker &> /dev/null; then
  echo "❌ Docker not found. Install from https://docker.com"
  exit 1
fi

# Check .env file
if [ ! -f .env ]; then
  echo "⚠️  No .env file found. Creating from template..."
  cp .env.example .env
  echo "📝 Edit .env and set your API_KEYS before going to production!"
  echo ""
fi

echo "📦 Building and starting containers..."
docker compose up --build -d

echo ""
echo "✅ CacheProxy is running!"
echo ""
echo "  🌐 Frontend Dashboard  → http://localhost:3000"
echo "  ⚙️  Backend API         → http://localhost:8080"
echo "  🗄️  Redis               → localhost:6379"
echo "  📊 Cache Stats         → http://localhost:8080/cache/stats"
echo "  ❤️  Health Check        → http://localhost:8080/cache/health"
echo ""
echo "  🔑 Default API key:    dev-key-change-me-in-production"
echo "     (change this in .env before deploying!)"
echo ""
echo "  📡 Test a proxy request:"
echo "  curl -H 'X-API-Key: dev-key-change-me-in-production' \\"
echo "       http://localhost:8080/proxy/products/1?origin=http://dummyjson.com"
echo ""
echo "  🛑 Stop: docker compose down"
