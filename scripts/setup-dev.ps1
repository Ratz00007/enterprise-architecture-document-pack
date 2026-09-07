#Requires -Version 7
<#
.SYNOPSIS
  Bootstrap the Acme Claims development toolchain on Windows.
.DESCRIPTION
  Verifies (and prints install instructions for) everything the local stack
  needs. Installs are kept explicit rather than silent so the developer sees
  exactly what lands on the machine. After bootstrapping, `docker compose up
  --build` runs the whole platform locally.
#>
$ErrorActionPreference = 'Stop'

function Test-Command($name, $command, $installHint) {
  if (Get-Command $command -ErrorAction SilentlyContinue) {
    Write-Host "[OK]      $name" -ForegroundColor Green
    return $true
  }
  Write-Host "[MISSING] $name -> $installHint" -ForegroundColor Yellow
  return $false
}

Write-Host '=== Acme Claims dev bootstrap ===' -ForegroundColor Cyan

$allGood = $true

# JDK 21 (claims-api). Temurin recommended; any 21 works.
$jdk = $false
if (Test-Command 'JDK 21 (java)' 'java' 'winget install EclipseAdoptium.Temurin.21.JDK') {
  $version = (java -version 2>&1 | Select-Object -First 1) -join ''
  if ($version -match 'version "21') { Write-Host '[OK]      JDK major version 21' -ForegroundColor Green; $jdk = $true }
  else { Write-Host "[WARN]    java exists but is not 21: $version" -ForegroundColor Yellow }
} else { $allGood = $false }

# Maven or the wrapper (the wrapper is vendored in apps/claims-api)
if (Test-Command 'Maven (mvn) or use bundled ./mvnw' 'mvn' 'the repo ships ./mvnw, Maven is optional') { }
if (-not (Test-Command 'Node 20+' 'node' 'winget install OpenJS.NodeJS.LTS')) { $allGood = $false }

# Docker: Docker Desktop on Windows, or WSL2 Docker Engine reachable on localhost:2375
$dockerUp = $false
if ($env:DOCKER_HOST) {
  Write-Host "[INFO]    DOCKER_HOST is set to $env:DOCKER_HOST (e.g. WSL2 engine)" -ForegroundColor Cyan
}
if (Test-Command 'Docker (docker)' 'docker' 'Docker Desktop, or: wsl -d Ubuntu -u root apt-get install docker.io') {
  docker info *>$null
  if ($LASTEXITCODE -eq 0) { Write-Host '[OK]      Docker engine reachable' -ForegroundColor Green; $dockerUp = $true }
  else { Write-Host '[WARN]    docker CLI found but engine not reachable (start Docker Desktop)' -ForegroundColor Yellow }
} else { $allGood = $false }

Write-Host ''
if (-not $allGood) {
  Write-Host 'Install the missing tools above, then re-run this script.' -ForegroundColor Yellow
  exit 1
}

Write-Host @'
=== Next steps ===
1. Local stack (recommended, one origin on :8081):
     docker compose up --build
     Web:   http://localhost:8081   (login: adjuster1 / adjuster-dev-only)
     API:   http://localhost:8080/api/v1/actuator/health
     Grafana: http://localhost:3000 (admin / admin-dev-only)
2. Or run services individually:
     apps/claims-api:  ./mvnw spring-boot:run   (needs PostgreSQL on :5432)
     apps/claims-web:  npm install && npm run dev
3. Tests:
     API:  cd apps/claims-api; ./mvnw verify   (Testcontainers needs Docker)
     Web:  cd apps/claims-web; npm test
'@
