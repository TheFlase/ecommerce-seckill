param(
  [Parameter(Mandatory = $true)][string]$Module,
  [Parameter(Mandatory = $true)][string]$Name
)

$ErrorActionPreference = "Stop"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;D:\soft_dir\apache-maven-3.9.0\bin;" + [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$logDir = Join-Path $root "logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

Write-Host "Starting $Name ($Module) with JAVA_HOME=$env:JAVA_HOME"
mvn -f (Join-Path $root "$Module\pom.xml") -q spring-boot:run
