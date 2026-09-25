# Start local infra for ecommerce-seckill
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot\..
Write-Host "Starting docker compose infra..."
docker compose up -d
docker compose ps
Write-Host ""
Write-Host "MySQL:  localhost:3307 (root/123456)"
Write-Host "Redis:  localhost:6379"
Write-Host "Rabbit: http://localhost:15672 (guest/guest)"
Write-Host "Nacos:  http://localhost:8848/nacos"
Write-Host "Sentinel: http://localhost:8858 (sentinel/sentinel)"
