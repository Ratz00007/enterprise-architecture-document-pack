<#
.SYNOPSIS
    Scaffold a new ADR (Architecture Decision Record) file.

.PARAMETER Title
    Short title of the decision, e.g. "Add HAProxy to the prod edge".

.PARAMETER Number
    Optional explicit ADR number. If omitted, the next free number
    in docs/architecture/adr/ is used.

.EXAMPLE
    pwsh -File scripts\gen-adr.ps1 -Title "Add HAProxy to the prod edge"
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Title,
    [Parameter()][int]$Number
)

$ErrorActionPreference = 'Stop'

$root = Resolve-Path (Join-Path $PSScriptRoot '..')
$adrDir = Join-Path $root 'docs/architecture/adr'
$indexFile = Join-Path $adrDir 'INDEX.md'

if (-not (Test-Path $adrDir)) { New-Item -ItemType Directory -Path $adrDir -Force | Out-Null }

if (-not $Number) {
    $existing = Get-ChildItem $adrDir -Filter 'ADR-*.md' |
        ForEach-Object { if ($_.Name -match 'ADR-(\d{3})') { [int]$Matches[1] } } |
        Sort-Object -Descending
    $Number = if ($existing.Count -gt 0) { $existing[0] + 1 } else { 1 }
}

$slug = ($Title -replace '[^A-Za-z0-9]+', '-' -replace '^-|-$', '').ToLower()
$filename = "ADR-{0:D3}-{1}.md" -f $Number, $slug
$path = Join-Path $adrDir $filename

$template = @"
# ADR-$('{0:D3}' -f $Number): $Title

- **Status:** Proposed
- **Date:** $(Get-Date -Format 'yyyy-MM-dd')
- **Deciders:** —
- **Supersedes:** —
- **Superseded by:** —

## Context

What is the issue we're seeing that motivates this decision?

## Decision

What did we choose?

## Consequences

**Positive**

- …

**Negative**

- …

**Neutral**

- …

## Alternatives considered

- …

## References

- …

---

> When this ADR is accepted, edit `INDEX.md` to add it to the
> register and link the page above.
"@

Set-Content -Path $path -Value $template -Encoding utf8
Write-Host "Created $path"
Write-Host "Next steps:"
Write-Host "  1. Edit the file to fill in Context, Decision, Consequences."
Write-Host "  2. Open a PR. Reviewers merge when Accepted."
Write-Host "  3. Update $indexFile to add the new ADR to the register."
