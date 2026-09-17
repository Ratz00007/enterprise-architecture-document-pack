<#
.SYNOPSIS
    Run all integration tests (Testcontainers).
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $root

Write-Host "`n--- Integration tests (claims-api, Testcontainers) ---" -ForegroundColor Cyan
Push-Location (Join-Path $root 'apps/claims-api')
try { .\mvnw.cmd -B verify -Pintegration } finally { Pop-Location }
