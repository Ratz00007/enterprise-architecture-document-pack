<#
.SYNOPSIS
    Roll back the production environment to a previous artifact digest.

.PARAMETER Env
    Target environment (typically prod).

.PARAMETER ToDigest
    The target digest in the form 'sha256:<64 hex chars>'.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidateSet('uat','prod')][string]$Env,
    [Parameter(Mandatory = $true)][string]$ToDigest
)

$ErrorActionPreference = 'Stop'
if ($ToDigest -notmatch '^sha256:[a-f0-9]{64}$') {
    throw "ToDigest must look like 'sha256:<64 hex chars>'."
}

$jenkinsBase = $env:JENKINS_URL
if (-not $jenkinsBase) { $jenkinsBase = 'https://ci.internal.acme' }
$cred = $env:JENKINS_USER + ':' + $env:JENKINS_TOKEN

$uri = "$jenkinsBase/job/acme-claims/job/$Env/job/rollback/buildWithParameters"
$body = @{
    TARGET_DIGEST = $ToDigest
    ROLLBACK_BY   = $env:USERNAME
} | ConvertTo-Json

Invoke-RestMethod `
    -Method Post `
    -Uri $uri `
    -Headers @{ Authorization = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($cred)) } `
    -Body $body `
    -ContentType 'application/json'
