$ErrorActionPreference = "Stop"

# Project
$PROJECT_DIR = "C:\PABS"

# Tomcat
$TOMCAT_HOME = "C:\Tomcat\apache-tomcat-10.1.57\apache-tomcat-10.1.57"

# WAR
$WAR_NAME = "passport-booking-system.war"
$WAR_SOURCE = "$PROJECT_DIR\target\$WAR_NAME"
$WAR_DEST = "$TOMCAT_HOME\webapps\$WAR_NAME"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "       PABS - BUILD & RUN" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Set-Location $PROJECT_DIR

# Build
Write-Host "[1/3] Building PABS..." -ForegroundColor Yellow
mvn clean package

if (-not (Test-Path $WAR_SOURCE)) {
    Write-Host "ERROR: WAR file was not created." -ForegroundColor Red
    exit 1
}

# Deploy
Write-Host ""
Write-Host "[2/3] Deploying WAR to Tomcat..." -ForegroundColor Yellow
Copy-Item $WAR_SOURCE $WAR_DEST -Force

# Start Tomcat
Write-Host ""
Write-Host "[3/3] Starting Tomcat..." -ForegroundColor Yellow
& "$TOMCAT_HOME\bin\startup.bat"

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "PABS is starting!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Open:" -ForegroundColor Cyan
Write-Host "http://localhost:8080/passport-booking-system/" -ForegroundColor White
Write-Host ""