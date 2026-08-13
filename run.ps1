# PABS one-click launcher
#
# Usage:
#   1. Make sure Tomcat is NOT already running.
#   2. Run:
#        powershell -ExecutionPolicy Bypass -File .\run.ps1
#
# The script:
#   - validates the PABS project and Tomcat
#   - uses the pre-configured DB + Gmail credentials below
#   - runs mvn clean package
#   - deploys the newly-built WAR
#   - starts Tomcat
#   - waits for port 8080
#   - opens the application in the browser
#
# IMPORTANT:
#   Credentials are stored directly in this file.
#   Do NOT commit this file to a public repository.

$ErrorActionPreference = "Stop"

# ==========================================
# PABS CONFIGURATION
# ==========================================

$ProjectRoot = "C:\PABS"

$TomcatHome = "C:\Tomcat\apache-tomcat-10.1.57\apache-tomcat-10.1.57"
$TomcatBin  = Join-Path $TomcatHome "bin"
$Webapps    = Join-Path $TomcatHome "webapps"

$DbConnectionFile = Join-Path `
    $ProjectRoot `
    "src\main\java\com\pabs\util\DBConnection.java"


# ==========================================
# YOUR CREDENTIALS
# ==========================================
#
# CHANGE ONLY THESE VALUES
#

$mailUser = "couldnotcomeupwithaname7254@gmail.com"

$mailPassword = "qkwa txdy xonx potp"

$env:PABS_DB_PASSWORD = "vpfn7254"


# ==========================================
# MAIL CONFIGURATION
# ==========================================

$env:PABS_MAIL_USERNAME = $mailUser
$env:PABS_MAIL_PASSWORD = $mailPassword
$env:PABS_MAIL_HOST     = "smtp.gmail.com"
$env:PABS_MAIL_PORT     = "587"


# ==========================================
# HELPER FUNCTIONS
# ==========================================

function Fail($Message) {

    Write-Host ""
    Write-Host "ERROR: $Message" -ForegroundColor Red
    Write-Host ""

    exit 1
}


function Test-PortFree($Port) {

    $connection = Get-NetTCPConnection `
        -LocalPort $Port `
        -State Listen `
        -ErrorAction SilentlyContinue

    return ($null -eq $connection)
}


# ==========================================
# STARTUP MESSAGE
# ==========================================

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " PABS - BUILD + CONFIGURE + RUN" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""


# ==========================================
# VALIDATE PROJECT
# ==========================================

if (-not (Test-Path $ProjectRoot)) {

    Fail "Project folder not found: $ProjectRoot"
}


if (-not (Test-Path (Join-Path $ProjectRoot "pom.xml"))) {

    Fail "pom.xml not found in $ProjectRoot"
}


# ==========================================
# VALIDATE TOMCAT
# ==========================================

if (-not (Test-Path $TomcatHome)) {

    Fail "Tomcat folder not found: $TomcatHome"
}


if (-not (Test-Path $DbConnectionFile)) {

    Fail "DBConnection.java not found: $DbConnectionFile"
}


# ==========================================
# MAKE SURE TOMCAT IS STOPPED
# ==========================================

if (-not (Test-PortFree 8080)) {

    Fail "Port 8080 is already in use. Tomcat is probably still running. Stop it first."
}


if (-not (Test-PortFree 8005)) {

    Fail "Port 8005 is already in use. Another Tomcat instance is probably still running. Stop it first."
}


# ==========================================
# CHECK DATABASE CONFIGURATION
# ==========================================

Write-Host "Checking database configuration..." -ForegroundColor Yellow

$dbSource = Get-Content $DbConnectionFile -Raw

$dbEnvNames = [regex]::Matches(
    $dbSource,
    'System\.getenv\(\s*"([^"]+)"\s*\)'
) |
    ForEach-Object {
        $_.Groups[1].Value
    } |
    Select-Object -Unique


if ($dbEnvNames.Count -eq 0) {

    Write-Host ""
    Write-Host "WARNING: DBConnection.java does not appear to read environment variables." -ForegroundColor Yellow
    Write-Host "The script will continue using the configured PABS_DB_PASSWORD."
    Write-Host ""
}
else {

    Write-Host "Found DB environment variable(s): $($dbEnvNames -join ', ')" -ForegroundColor Green

    # The password is already configured above.
    # Do not ask for input.
    #
    # If DBConnection.java uses additional environment variables,
    # configure them here if required by your project.

    Write-Host "Database credentials loaded from run.ps1." -ForegroundColor Green
}


# ==========================================
# DISPLAY CONFIGURATION STATUS
# ==========================================

Write-Host ""
Write-Host "Loading Gmail configuration..." -ForegroundColor Yellow

if ([string]::IsNullOrWhiteSpace($env:PABS_MAIL_USERNAME)) {

    Fail "Gmail username is empty."
}


if ([string]::IsNullOrWhiteSpace($env:PABS_MAIL_PASSWORD)) {

    Fail "Gmail App Password is empty."
}


if ([string]::IsNullOrWhiteSpace($env:PABS_DB_PASSWORD)) {

    Fail "Database password is empty."
}


Write-Host "Gmail username configured." -ForegroundColor Green
Write-Host "Gmail App Password configured." -ForegroundColor Green
Write-Host "Database password configured." -ForegroundColor Green


# ==========================================
# TOMCAT ENVIRONMENT
# ==========================================

$env:CATALINA_HOME = $TomcatHome
$env:CATALINA_BASE = $TomcatHome


# ==========================================
# CHECK JAVA
# ==========================================

Write-Host ""
Write-Host "Checking Java..." -ForegroundColor Yellow

if (-not $env:JAVA_HOME) {

    $possibleJava = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"

    if (Test-Path $possibleJava) {

        $env:JAVA_HOME = $possibleJava
    }
}


if (-not $env:JAVA_HOME) {

    Fail "JAVA_HOME is not set and the expected JDK 17 folder was not found."
}


Write-Host "JAVA_HOME = $env:JAVA_HOME" -ForegroundColor Green


# ==========================================
# CHECK MAVEN
# ==========================================

Write-Host ""
Write-Host "Checking Maven..." -ForegroundColor Yellow

$mvn = Get-Command mvn.cmd -ErrorAction SilentlyContinue


if (-not $mvn) {

    $mvnw = Join-Path $ProjectRoot "mvnw.cmd"

    if (Test-Path $mvnw) {

        $mavenCommand = $mvnw
    }
    else {

        Fail "Maven (mvn.cmd) was not found and mvnw.cmd is not present in the project."
    }
}
else {

    $mavenCommand = $mvn.Source
}


Write-Host "Maven = $mavenCommand" -ForegroundColor Green


# ==========================================
# BUILD FRESH WAR
# ==========================================

Write-Host ""
Write-Host "Building fresh WAR..." -ForegroundColor Cyan

Set-Location $ProjectRoot

& $mavenCommand clean package


if ($LASTEXITCODE -ne 0) {

    Fail "Maven build failed. Tomcat was NOT started."
}


# ==========================================
# FIND GENERATED WAR
# ==========================================

$wars = @(
    Get-ChildItem `
        (Join-Path $ProjectRoot "target") `
        -Filter "*.war" `
        -File |
    Where-Object {
        $_.Name -notlike "original-*"
    } |
    Sort-Object LastWriteTime -Descending
)


if ($wars.Count -eq 0) {

    Fail "Maven completed, but no WAR was found in target."
}


$war = $wars[0]

Write-Host "Fresh WAR: $($war.FullName)" -ForegroundColor Green


# ==========================================
# DETERMINE DEPLOYMENT NAME
# ==========================================

$appName = [IO.Path]::GetFileNameWithoutExtension($war.Name)

$targetWar = Join-Path `
    $Webapps `
    $war.Name

$explodedDir = Join-Path `
    $Webapps `
    $appName


# ==========================================
# DEPLOY FRESH WAR
# ==========================================

Write-Host ""
Write-Host "Deploying fresh WAR..." -ForegroundColor Cyan


# Tomcat is confirmed stopped above.
# Remove old deployment if present.

if (Test-Path $targetWar) {

    Remove-Item $targetWar -Force
}


if (Test-Path $explodedDir) {

    Remove-Item $explodedDir -Recurse -Force
}


Copy-Item `
    $war.FullName `
    $targetWar `
    -Force


Write-Host "Deployed: $targetWar" -ForegroundColor Green


# ==========================================
# START TOMCAT
# ==========================================

Write-Host ""
Write-Host "Starting Tomcat with DB + mail credentials..." -ForegroundColor Cyan

$startup = Join-Path `
    $TomcatBin `
    "startup.bat"


if (-not (Test-Path $startup)) {

    Fail "startup.bat not found: $startup"
}


Push-Location $TomcatBin

try {

    & $startup
}
finally {

    Pop-Location
}


if ($LASTEXITCODE -ne 0) {

    Fail "Tomcat startup command failed."
}


# ==========================================
# WAIT FOR TOMCAT
# ==========================================

Write-Host ""
Write-Host "Waiting for Tomcat on http://localhost:8080 ..." -ForegroundColor Yellow


$ready = $false


for ($i = 0; $i -lt 30; $i++) {

    Start-Sleep -Seconds 1

    try {

        $tcp = Test-NetConnection `
            -ComputerName "localhost" `
            -Port 8080 `
            -WarningAction SilentlyContinue

        if ($tcp.TcpTestSucceeded) {

            $ready = $true

            break
        }
    }
    catch {

        # Keep waiting.
    }
}


if (-not $ready) {

    Write-Host ""
    Write-Host "Tomcat did not become reachable on port 8080." -ForegroundColor Red

    Write-Host "Check the newest logs in:"
    Write-Host "  $TomcatHome\logs"

    exit 1
}


# ==========================================
# OPEN APPLICATION
# ==========================================

$url = "http://localhost:8080/$appName/"


Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host " PABS IS RUNNING" -ForegroundColor Green
Write-Host " URL: $url" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""


Start-Process $url


Write-Host "Browser opened." -ForegroundColor Cyan
Write-Host "Keep this PowerShell window open if you want to inspect logs." -ForegroundColor Cyan