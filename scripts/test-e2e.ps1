<#
.SYNOPSIS
    Run the end-to-end Playwright + Cucumber suite against UAT.

.PARAMETER Env
    Target environment (default: uat).
#>
[CmdletBinding()]
param(
    [Parameter()][ValidateSet('dev','qa','uat')][string]$Env = 'uat'
)

$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $root

$env:PLAYWRIGHT_BASE_URL = switch ($Env) {
    'dev' { 'http://localhost:5173' }
    'qa'  { 'https://qa-app.internal.acme' }
    'uat' { 'https://uat-app.internal.acme' }
}

Write-Host "`n--- E2E (Playwright + Cucumber) against $Env at $env:PLAYWRIGHT_BASE_URL ---" -ForegroundColor Cyan
Push-Location (Join-Path $root 'apps/claims-web')
try { pnpm test:e2e } finally { Pop-Location }
