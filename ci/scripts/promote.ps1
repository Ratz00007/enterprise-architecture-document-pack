<#
.SYNOPSIS
    Promote an artifact to a target environment through the Jenkins pipeline.

.DESCRIPTION
    The Jenkins pipeline is the only path that can promote to a new
    environment — this script is a thin wrapper that triggers the
    job. It does NOT bypass the approval gate; the human approver
    still has to say yes in the Jenkins UI.

.PARAMETER Env
    Target environment: dev | qa | uat | prod

.PARAMETER Build
    Jenkins build number to promote

.EXAMPLE
    pwsh -File ci\scripts\promote.ps1 -Env qa -Build 142
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidateSet('dev','qa','uat','prod')][string]$Env,
    [Parameter(Mandatory = $true)][int]$Build
)

$ErrorActionPreference = 'Stop'

$jobName = "acme-claims/$Env/promote"
$jenkinsBase = $env:JENKINS_URL
if (-not $jenkinsBase) { $jenkinsBase = 'https://ci.internal.acme' }
$cred = $env:JENKINS_USER + ':' + $env:JENKINS_TOKEN

$uri = "$jenkinsBase/job/acme-claims/job/$Env/job/promote/buildWithParameters"
$body = @{
    BUILD_NUMBER = $Build
    PROMOTED_BY  = $env:USERNAME
} | ConvertTo-Json

Write-Host "Promoting build $Build to $Env via $uri"
$response = Invoke-RestMethod `
    -Method Post `
    -Uri $uri `
    -Headers @{ Authorization = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($cred)) } `
    -Body $body `
    -ContentType 'application/json' `
    -MaximumRedirection 0 `
    -ErrorAction SilentlyContinue

if ($response -is [System.Management.Automation.HttpResponseException]) {
    Write-Error "Promotion request failed: $($_.Exception.Message)"
    exit 1
}

Write-Host "Queued. Watch the pipeline at $jenkinsBase/job/acme-claims/job/$Env/job/promote"
