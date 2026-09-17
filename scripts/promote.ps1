<#
.SYNOPSIS
    Helper for the human-friendly promotion script. Delegates to
    ci/scripts/promote.ps1 so there's one source of truth.

.DESCRIPTION
    Keep this script thin. The real work is in
    ci/scripts/promote.ps1. This file exists for the
    "scripts\promote.ps1" mental model.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidateSet('dev','qa','uat','prod')][string]$Env,
    [Parameter(Mandatory = $true)][int]$Build
)

$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
& (Join-Path $root 'ci/scripts/promote.ps1') -Env $Env -Build $Build
