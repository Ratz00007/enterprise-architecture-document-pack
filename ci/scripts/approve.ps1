<#
.SYNOPSIS
    Approve a pending promotion. Requires a Keycloak token with
    the role appropriate for the target environment.

.DESCRIPTION
    This is the CLI counterpart of clicking "Approve" in the
    Jenkins UI. The Jenkins pipeline is the system of record; this
    script is a thin client.

.PARAMETER Env
    Target environment: dev | qa | uat | prod

.PARAMETER Build
    Jenkins build number to approve

.PARAMETER Comment
    Free-text comment recorded in the audit log
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidateSet('dev','qa','uat','prod')][string]$Env,
    [Parameter(Mandatory = $true)][int]$Build,
    [Parameter()][string]$Comment = ''
)

$ErrorActionPreference = 'Stop'
$jenkinsBase = $env:JENKINS_URL
if (-not $jenkinsBase) { $jenkinsBase = 'https://ci.internal.acme' }
$cred = $env:JENKINS_USER + ':' + $env:JENKINS_TOKEN

$uri = "$jenkinsBase/job/acme-claims/job/$Env/job/promote/$Build/input/Submit/proceedEmpty"
Write-Host "Approving $Env build $Build"
Invoke-RestMethod `
    -Method Post `
    -Uri $uri `
    -Headers @{
        Authorization = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($cred))
        'Jenkins-Crumb' = (Get-BasicCrumb)
    } `
    -Body @{ COMMENT = $Comment } `
    -ContentType 'application/x-www-form-urlencoded'
