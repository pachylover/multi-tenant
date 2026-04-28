# Quick start script for Windows (PowerShell)
# Usage: .\start.ps1

Write-Host "🚀 Starting Multi-tenant Nextcloud Storage Monitor..." -ForegroundColor Green

# Create .env from example if it doesn't exist
if (-not (Test-Path .env)) {
    Write-Host "📝 Creating .env file from .env.example..." -ForegroundColor Yellow
    Copy-Item .env.example .env
    Write-Host "✅ .env file created. You can edit it if needed." -ForegroundColor Green
}

# Start docker compose
Write-Host "🐳 Starting Docker Compose..." -ForegroundColor Cyan
docker-compose up -d

Write-Host ""
Write-Host "✅ Services are starting..." -ForegroundColor Green
Write-Host ""
Write-Host "📊 Check status:" -ForegroundColor Yellow
Write-Host "   docker-compose ps"
Write-Host ""
Write-Host "📝 View logs:" -ForegroundColor Yellow
Write-Host "   docker-compose logs -f"
Write-Host ""
Write-Host "🌐 Access points:" -ForegroundColor Cyan
Write-Host "   Frontend:  http://localhost:5173/admin/storage"
Write-Host "   Backend:   http://localhost:8080/api/tenants"
Write-Host "   Nextcloud: http://localhost:8081"
Write-Host "              (admin / adminpass)"
Write-Host ""
Write-Host "⏳ First startup may take 2-3 minutes for Nextcloud initialization." -ForegroundColor Yellow
Write-Host ""
Write-Host "🛑 To stop: docker-compose down" -ForegroundColor Red
