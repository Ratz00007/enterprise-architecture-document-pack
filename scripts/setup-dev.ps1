<#
.SYNOPSIS
    Bootstrap a Windows dev machine for the Acme Claims project.

.DESCRIPTION
    Installs the toolchain listed in AGENTS.md: JDK 21, Node 20,
    pnpm, Maven Wrapper (project-local), Ansible, k6, Playwright.
    Idempotent — re-running is a no-op for already-installed tools.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$root = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $root

function Write-Step($msg) { Write-Host "`n=== $msg ===" -ForegroundColor Cyan }
function Test-Tool($name, [string]$versionCmd = "--version") {
    try { & $name $versionCmd 2>$null | Out-Null; return $true } catch { return $false }
}

# --- 1. JDK 21 (Temurin) ------------------------------------------------
Write-Step "Checking JDK 21"
if (-not (Test-Tool 'java' '-version')) {
    Write-Host "Installing Temurin JDK 21 via winget..."
    if (Get-Command winget -ErrorAction SilentlyContinue) {
        winget install --id EclipseAdoptium.Temurin.21.JDK -e --source winget --accept-source-agreements --accept-package-agreements
    } else {
        Write-Warning "winget not found. Install Temurin JDK 21 manually from https://adoptium.net/"
    }
} else {
    $v = (& java -version 2>&1 | Select-String 'version "(?<v>21[^"]*)' ).Matches[0].Groups['v'].Value
    Write-Host "Found Java $v"
}

# --- 2. Node 20 + pnpm --------------------------------------------------
Write-Step "Checking Node 20 + pnpm"
if (-not (Test-Tool 'node' '-v')) {
    Write-Host "Installing Node 20 LTS via winget..."
    if (Get-Command winget -ErrorAction SilentlyContinue) {
        winget install --id OpenJS.NodeJS.LTS -e --source winget --accept-source-agreements --accept-package-agreements
    } else {
        Write-Warning "winget not found. Install Node 20 LTS manually from https://nodejs.org/"
    }
}
if (-not (Test-Tool 'pnpm' '-v')) {
    Write-Host "Installing pnpm globally..."
    npm install -g pnpm@9
} else {
    Write-Host "pnpm $(pnpm -v) found"
}

# --- 3. Maven Wrapper (project-local) ----------------------------------
Write-Step "Ensuring Maven Wrapper"
if (-not (Test-Path (Join-Path $root 'apps/claims-api/mvnw.cmd'))) {
    Write-Host "Generating Maven Wrapper for apps/claims-api..."
    # Maven Wrapper is checked in. If missing, regenerate:
    Push-Location (Join-Path $root 'apps/claims-api')
    try {
        mvn -N wrapper:wrapper -Dmaven=3.9.6
    } finally { Pop-Location }
} else {
    Write-Host "Maven Wrapper present"
}

# --- 4. Playwright browser ----------------------------------------------
Write-Step "Installing Playwright browsers"
Push-Location (Join-Path $root 'apps/claims-web')
try {
    pnpm install --frozen-lockfile
    pnpm exec playwright install --with-deps chromium
} finally { Pop-Location }

# --- 5. k6 --------------------------------------------------------------
Write-Step "Checking k6"
if (-not (Test-Tool 'k6' 'version')) {
    Write-Host "Install k6 from https://k6.io/docs/getting-started/installation/"
} else {
    Write-Host "k6 $(k6 version) found"
}

# --- 6. Ansible (WSL only) ---------------------------------------------
Write-Step "Checking Ansible"
if (-not (Test-Tool 'ansible' '--version')) {
    Write-Host "Ansible runs on the Linux control host. If you're using WSL, install with: wsl sudo apt install -y ansible"
} else {
    Write-Host "Ansible $(ansible --version | Select-Object -First 1) found"
}

Write-Step "Done"
Write-Host "Next steps:"
Write-Host "  cd apps\claims-api; .\mvnw.cmd spring-boot:run"
Write-Host "  cd apps\claims-web; pnpm dev"
