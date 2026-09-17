<#
.SYNOPSIS
    Run all unit tests.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $root

# Backend
Write-Host "`n--- JUnit (claims-api) ---" -ForegroundColor Cyan
Push-Location (Join-Path $root 'apps/claims-api')
try { .\mvnw.cmd -B test } finally { Pop-Location }

# Frontend
Write-Host "`n--- Vitest (claims-web) ---" -ForegroundColor Cyan
Push-Location (Join-Path $root 'apps/claims-web')
try { pnpm test } finally { Pop-Location }
