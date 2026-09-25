# Smoke test via Gateway
$ErrorActionPreference = "Stop"
$base = "http://localhost:8080"

Write-Host "1) login testuser1"
Set-Content -Path "$env:TEMP\login.json" -Value '{"username":"testuser1","password":"password"}' -Encoding ascii
$login = curl.exe -s -X POST "$base/user/login" -H "Content-Type: application/json" --data-binary "@$env:TEMP\login.json"
Write-Host $login
$token = ($login | ConvertFrom-Json).data
if (-not $token) { throw "login failed" }

Write-Host "2) login admin for warm-up"
Set-Content -Path "$env:TEMP\admin.json" -Value '{"username":"admin","password":"password"}' -Encoding ascii
$adminLogin = curl.exe -s -X POST "$base/user/login" -H "Content-Type: application/json" --data-binary "@$env:TEMP\admin.json"
$adminToken = ($adminLogin | ConvertFrom-Json).data

Write-Host "3) warm-up activity 1"
curl.exe -s -X POST "$base/seckill/warm-up/1" -H "Authorization: $adminToken"
Write-Host ""

Write-Host "4) do seckill"
$r = curl.exe -s -X POST "$base/seckill/do-seckill?activityId=1&quantity=1" -H "Authorization: $token"
Write-Host $r
Write-Host "DONE"
