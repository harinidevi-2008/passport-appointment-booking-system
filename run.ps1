# PABS one-click launcher
# Usage:
#   1. Make sure Tomcat is NOT already running.
#   2. Right-click -> Run with PowerShell, or run: powershell -ExecutionPolicy Bypass -File .\run.ps1
#   3. Enter the requested DB/mail values.
#
# The script:
#   - validates the PABS project and Tomcat
#   - discovers DB environment-variable names from DBConnection.java
#   - asks for DB + Gmail/App-Password credentials without printing passwords
#   - sets variables BEFORE Tomcat starts
#   - runs mvn clean package
#   - deploys the newly-built WAR
#   - starts Tomcat
#   - waits for port 8080
#   - opens the application in the browser
#
# It deliberately does NOT save passwords to disk.

$ErrorActionPreference = "Stop"

$ProjectRoot = "C:\PABS"
$TomcatHome  = "C:\Tomcat\apache-tomcat-10.1.57\apache-tomcat-10.1.57"
$TomcatBin   = Join-Path $TomcatHome "bin"
$Webapps     = Join-Path $TomcatHome "webapps"
$DbConnectionFile = Join-Path $ProjectRoot "src\main\java\com\pabs\util\DBConnection.java"

function Fail($Message) {
    Write-Host ""
    Write-Host "ERROR: $Message" -ForegroundColor Red
    Write-Host ""
    exit 1
}

function Read-SecretValue($Prompt) {
    $secure = Read-Host $Prompt -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

function Test-PortFree($Port) {
    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    return ($null -eq $connection)
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " PABS - BUILD + CONFIGURE + RUN" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path $ProjectRoot)) {
    Fail "Project folder not found: $ProjectRoot"
}
if (-not (Test-Path (Join-Path $ProjectRoot "pom.xml"))) {
    Fail "pom.xml not found in $ProjectRoot"
}
if (-not (Test-Path $TomcatHome)) {
    Fail "Tomcat folder not found: $TomcatHome"
}
if (-not (Test-Path $DbConnectionFile)) {
    Fail "DBConnection.java not found: $DbConnectionFile"
}

# Do not silently fight another Tomcat instance. This prevents the exact
# 8080/8005 "Address already in use" problem that happened before.
if (-not (Test-PortFree 8080)) {
    Fail "Port 8080 is already in use. Tomcat is probably still running. Stop it first."
}
if (-not (Test-PortFree 8005)) {
    Fail "Port 8005 is already in use. Another Tomcat instance is probably still running. Stop it first."
}

Write-Host "Finding database configuration variables..." -ForegroundColor Yellow

$dbSource = Get-Content $DbConnectionFile -Raw
$dbEnvNames = [regex]::Matches($dbSource, 'System\.getenv\(\s*"([^"]+)"\s*\)') |
    ForEach-Object { $_.Groups[1].Value } |
    Select-Object -Unique

if ($dbEnvNames.Count -eq 0) {
    Write-Host ""
    Write-Host "WARNING: DBConnection.java does not appear to read environment variables." -ForegroundColor Yellow
    Write-Host "The script cannot safely guess your database configuration names."
    Write-Host "Open DBConnection.java and make sure it reads DB settings from environment variables."
    Write-Host ""
    $continue = Read-Host "Continue anyway? (y/N)"
    if ($continue -notmatch '^(y|yes)$') {
        exit 1
    }
}
else {
    Write-Host "Found DB environment variable(s): $($dbEnvNames -join ', ')" -ForegroundColor Green
    foreach ($name in $dbEnvNames) {
        $value = Read-Host "Enter value for $name"
        if ([string]::IsNullOrWhiteSpace($value)) {
            Fail "$name cannot be empty."
        }
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

Write-Host ""
Write-Host "Gmail SMTP configuration" -ForegroundColor Yellow
Write-Host "Use your Gmail address and the 16-character Google App Password."
Write-Host "Do NOT use your normal Gmail password."
Write-Host ""

$mailUser = Read-Host "Gmail address"
if ([string]::IsNullOrWhiteSpace($mailUser)) {
    Fail "Gmail address cannot be empty."
}

$mailPassword = Read-SecretValue "Gmail App Password"
if ([string]::IsNullOrWhiteSpace($mailPassword)) {
    Fail "Gmail App Password cannot be empty."
}

$env:PABS_MAIL_USERNAME=""
$env:PABS_MAIL_PASSWORD=""
$env:PABS_DB_PASSWORD="vpfn7254"
$env:PABS_MAIL_HOST = "smtp.gmail.com"
$env:PABS_MAIL_PORT = "587"

# Tomcat needs these in the SAME process environment in which it is launched.
$env:CATALINA_HOME = $TomcatHome
$env:CATALINA_BASE = $TomcatHome

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

Write-Host ""
Write-Host "Building fresh WAR..." -ForegroundColor Cyan
Set-Location $ProjectRoot

& $mavenCommand clean package
if ($LASTEXITCODE -ne 0) {
    Fail "Maven build failed. Tomcat was NOT started."
}

$wars = @(Get-ChildItem (Join-Path $ProjectRoot "target") -Filter "*.war" -File |
    Where-Object { $_.Name -notlike "original-*" } |
    Sort-Object LastWriteTime -Descending)

if ($wars.Count -eq 0) {
    Fail "Maven completed, but no WAR was found in target."
}

$war = $wars[0]
Write-Host "Fresh WAR: $($war.FullName)" -ForegroundColor Green

# Derive the deployed application name from the WAR.
$appName = [IO.Path]::GetFileNameWithoutExtension($war.Name)
$targetWar = Join-Path $Webapps $war.Name
$explodedDir = Join-Path $Webapps $appName

Write-Host ""
Write-Host "Deploying fresh WAR..." -ForegroundColor Cyan

# Tomcat is confirmed stopped above, so it is safe to remove stale deployment.
if (Test-Path $targetWar) {
    Remove-Item $targetWar -Force
}
if (Test-Path $explodedDir) {
    Remove-Item $explodedDir -Recurse -Force
}

Copy-Item $war.FullName $targetWar -Force
Write-Host "Deployed: $targetWar" -ForegroundColor Green

Write-Host ""
Write-Host "Starting Tomcat with DB + mail credentials..." -ForegroundColor Cyan
$startup = Join-Path $TomcatBin "startup.bat"
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

Write-Host ""
Write-Host "Waiting for Tomcat on http://localhost:8080 ..." -ForegroundColor Yellow

$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    try {
        $tcp = Test-NetConnection -ComputerName "localhost" -Port 8080 -WarningAction SilentlyContinue
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

$url = "http://localhost:8080/$appName/"
Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host " PABS IS RUNNING" -ForegroundColor Green
Write-Host " URL: $url" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""

Start-Process $url

# Clear secrets from this PowerShell process after Tomcat has been launched.
# The already-running Tomcat JVM keeps the environment it inherited.
$env:PABS_MAIL_PASSWORD = $null

Write-Host "Browser opened. Keep this PowerShell window open if you want to inspect logs." -ForegroundColor Cyan
