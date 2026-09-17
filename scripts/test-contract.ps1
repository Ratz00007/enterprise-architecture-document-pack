<#
.SYNOPSIS
    Run all contract tests (Pact provider).
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $root

Write-Host "`n--- Pact provider verification (claims-api) ---" -ForegroundColor Cyan
Push-Location (Join-Path $root 'apps/claims-api')
try { .\mvnw.cmd -B pact:verify } finally { Pop-Location }
