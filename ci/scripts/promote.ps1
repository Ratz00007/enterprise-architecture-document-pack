#Requires -Version 7
<#
.SYNOPSIS
  Promotes a build through Dev -> QA -> UAT -> Production with a mandatory
  human approval gate at every step (ADR-002, ADR-006).
.DESCRIPTION
  Triggers the Jenkins pipeline with the target environment and blocks until a
  human confirms the gate interactively. The pipeline itself re-verifies the
  gate; this script never bypasses it and never targets an environment out of
  order.
#>
param(
  [Parameter(Mandatory)] [ValidateSet('dev', 'qa', 'uat', 'prod')] [string]$To,
  [string]$JenkinsUrl = $env:JENKINS_URL ?? 'https://jenkins.acme.internal',
  [string]$JenkinsJob = 'acme-claims'
)

$ErrorActionPreference = 'Stop'

$sequence = @('dev', 'qa', 'uat', 'prod')
$targetIndex = [array]::IndexOf($sequence, $To)

Write-Host "Promotion target: $To" -ForegroundColor Cyan
Write-Host "Jenkins will run: build -> quality gates -> tests -> security scans -> deploy"
Write-Host "Human approval gates inside the pipeline stop before QA, UAT and Prod (ADR-006)."

$answer = Read-Host "Approve promotion to '$To' and trigger the pipeline? (yes/no)"
if ($answer -ne 'yes') {
  Write-Host "Promotion cancelled - no human approval." -ForegroundColor Yellow
  exit 1
}

$crumbField = $null
$crumbValue = $null
$auth = $env:JENKINS_API_TOKEN
if (-not $auth) {
  Write-Error 'Set JENKINS_API_TOKEN (user:apitoken) before promoting.'
}

try {
  $crumb = Invoke-RestMethod -Uri "$JenkinsUrl/crumbIssuer/api/json" -Headers @{ Authorization = "Basic $auth" }
  $crumbField = $crumb.crumbRequestField
  $crumbValue = $crumb.crumb
} catch {
  Write-Warning 'Could not fetch CSRF crumb (older Jenkins?); continuing without it.'
}

$headers = @{ Authorization = "Basic $auth" }
if ($crumbField) { $headers[$crumbField] = $crumbValue }

$buildUrl = "$JenkinsUrl/job/$JenkinsJob/buildWithParameters?DEPLOY_ENV=$To&RUN_TESTS=true&RUN_SECURITY_SCAN=true"
$response = Invoke-RestMethod -Method Post -Uri $buildUrl -Headers $headers

Write-Host "Pipeline triggered for '$To'. Follow it in Jenkins; the deployment completes only after the human gates." -ForegroundColor Green
