<#
.SYNOPSIS
    Run all linters across the repo. Idempotent.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $root

$failed = 0

function Run-Step($name, [scriptblock]$cmd) {
    Write-Host "`n--- $name ---" -ForegroundColor Cyan
    try {
        & $cmd
        Write-Host "OK" -ForegroundColor Green
    } catch {
        Write-Host "FAILED: $_" -ForegroundColor Red
        $script:failed++
    }
}

# Backend
Run-Step "Checkstyle (claims-api)" {
    Push-Location (Join-Path $root 'apps/claims-api')
    try { .\mvnw.cmd -B -q checkstyle:check } finally { Pop-Location }
}
Run-Step "SpotBugs (claims-api)" {
    Push-Location (Join-Path $root 'apps/claims-api')
    try { .\mvnw.cmd -B -q spotbugs:check } finally { Pop-Location }
}
Run-Step "PMD (claims-api)" {
    Push-Location (Join-Path $root 'apps/claims-api')
    try { .\mvnw.cmd -B -q pmd:check } finally { Pop-Location }
}

# Frontend
Run-Step "ESLint (claims-web)" {
    Push-Location (Join-Path $root 'apps/claims-web')
    try { pnpm lint } finally { Pop-Location }
}
Run-Step "Prettier (claims-web)" {
    Push-Location (Join-Path $root 'apps/claims-web')
    try { pnpm format } finally { Pop-Location }
}
Run-Step "TypeScript (claims-web)" {
    Push-Location (Join-Path $root 'apps/claims-web')
    try { pnpm typecheck } finally { Pop-Location }
}

# Docs
Run-Step "Markdownlint (docs)" {
    if (Get-Command markdownlint -ErrorAction SilentlyContinue) {
        markdownlint "**/*.md" --ignore node_modules --ignore dist --ignore target
    } else {
        Write-Host "markdownlint not installed; skipping"
    }
}

if ($failed -gt 0) {
    Write-Host "`n$failed step(s) failed" -ForegroundColor Red
    exit 1
} else {
    Write-Host "`nAll linters clean" -ForegroundColor Green
}
