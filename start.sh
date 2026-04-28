#!/bin/bash
# Quick start script for Unix/Linux/MacOS

echo "🚀 Starting Multi-tenant Nextcloud Storage Monitor..."

# Create .env from example if it doesn't exist
if [ ! -f .env ]; then
    echo "📝 Creating .env file from .env.example..."
    cp .env.example .env
    echo "✅ .env file created. You can edit it if needed."
fi

# Start docker compose
echo "🐳 Starting Docker Compose..."
docker-compose up -d

echo ""
echo "✅ Services are starting..."
echo ""
echo "📊 Check status:"
echo "   docker-compose ps"
echo ""
echo "📝 View logs:"
echo "   docker-compose logs -f"
echo ""
echo "🌐 Access points:"
echo "   Frontend:  http://localhost:5173/admin/storage"
echo "   Backend:   http://localhost:8080/api/tenants"
echo "   Nextcloud: http://localhost:8081"
echo "              (admin / adminpass)"
echo ""
echo "⏳ First startup may take 2-3 minutes for Nextcloud initialization."
echo ""
echo "🛑 To stop: docker-compose down"
