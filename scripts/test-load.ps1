<#
.SYNOPSIS
    Run the k6 load test against UAT.

.PARAMETER BaseUrl
    The target base URL. Defaults to UAT.

.PARAMETER Token
    A bearer token with at least the qa_lead role.
#>
[CmdletBinding()]
param(
    [Parameter()][string]$BaseUrl = 'https://uat-app.internal.acme',
    [Parameter(Mandatory = $true)][string]$Token
)

$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
$script = Join-Path $root 'tests/load/claims-api.js'

if (-not (Get-Command k6 -ErrorAction SilentlyContinue)) {
    throw "k6 not found. Install from https://k6.io/docs/getting-started/installation/"
}

$env:BASE_URL = $BaseUrl
$env:TOKEN = $Token

Write-Host "k6 -> $BaseUrl"
k6 run $script
