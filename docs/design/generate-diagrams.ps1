#Requires -Version 5.1
<#
.SYNOPSIS
    Renders Mermaid and PlantUML design diagrams to PNG (and PDF where supported).

.DESCRIPTION
    Exports Phase 0 design artifacts from docs/design/ to docs/exports/.
    Uses @mermaid-js/mermaid-cli (mmdc) for .mmd files and PlantUML for .puml files.

.EXAMPLE
    .\generate-diagrams.ps1
    .\generate-diagrams.ps1 -Format pdf
#>

param(
    [ValidateSet("png", "pdf", "both")]
    [string]$Format = "png"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ExportDir = Join-Path (Split-Path -Parent $ScriptDir) "exports"

if (-not (Test-Path $ExportDir)) {
    New-Item -ItemType Directory -Path $ExportDir -Force | Out-Null
}

Write-Host "=== Utility Billing System — Diagram Export ===" -ForegroundColor Cyan
Write-Host "Source : $ScriptDir"
Write-Host "Output : $ExportDir"
Write-Host ""

# ---------------------------------------------------------------------------
# Mermaid diagrams (.mmd → .png / .pdf)
# ---------------------------------------------------------------------------

$mermaidFiles = @(
    "erd.mmd",
    "system-flow.mmd",
    "bill-lifecycle.mmd",
    "reading-capture-flow.mmd"
)

function Invoke-MermaidExport {
    param([string]$InputFile, [string]$OutputExt)

    $baseName = [System.IO.Path]::GetFileNameWithoutExtension($InputFile)
    $outputFile = Join-Path $ExportDir "$baseName.$OutputExt"
    $inputPath = Join-Path $ScriptDir $InputFile

    $outName = "$baseName.$OutputExt"
    Write-Host "  Mermaid: $InputFile -> $outName" -ForegroundColor Green

    if ($OutputExt -eq "pdf") {
        npx --yes @mermaid-js/mermaid-cli `
            -i $inputPath `
            -o $outputFile `
            -b transparent `
            -e pdf
    } else {
        npx --yes @mermaid-js/mermaid-cli `
            -i $inputPath `
            -o $outputFile `
            -b transparent `
            -w 1920 `
            -H 1080
    }
}

# Check Node.js for mmdc
$nodeAvailable = $null -ne (Get-Command node -ErrorAction SilentlyContinue)

if ($nodeAvailable) {
    Write-Host "[Mermaid CLI]" -ForegroundColor Yellow
    foreach ($file in $mermaidFiles) {
        if (-not (Test-Path (Join-Path $ScriptDir $file))) {
            Write-Warning "Missing: $file"
            continue
        }
        if ($Format -eq "png" -or $Format -eq "both") {
            Invoke-MermaidExport -InputFile $file -OutputExt "png"
        }
        if ($Format -eq "pdf" -or $Format -eq "both") {
            Invoke-MermaidExport -InputFile $file -OutputExt "pdf"
        }
    }
} else {
    Write-Warning "Node.js not found. Install Node.js or use https://mermaid.live to export .mmd files manually."
}

Write-Host ""

# ---------------------------------------------------------------------------
# PlantUML diagrams (.puml → .png / .pdf)
# ---------------------------------------------------------------------------

$plantUmlFiles = @(
    "erd.puml",
    "system-flow.puml"
)

function Invoke-PlantUmlExport {
    param([string]$InputFile, [string]$OutputExt)

    $inputPath = Join-Path $ScriptDir $InputFile
    Write-Host "  PlantUML: $InputFile (output .$OutputExt)" -ForegroundColor Green

    if ($script:PlantUmlJar) {
        java -jar $script:PlantUmlJar "-t$OutputExt" -o $ExportDir $inputPath
    } else {
        # Try plantuml on PATH
        $plantumlCmd = Get-Command plantuml -ErrorAction SilentlyContinue
        if ($plantumlCmd) {
            & plantuml "-t$OutputExt" -o $ExportDir $inputPath
        } else {
            throw "PlantUML not available"
        }
    }
}

# Locate plantuml.jar (local or download hint)
$jarCandidates = @(
    (Join-Path $ScriptDir "plantuml.jar"),
    (Join-Path (Split-Path -Parent $ScriptDir) "tools" "plantuml.jar")
)
$script:PlantUmlJar = $jarCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1

$javaAvailable = $null -ne (Get-Command java -ErrorAction SilentlyContinue)
$plantUmlOnPath = $null -ne (Get-Command plantuml -ErrorAction SilentlyContinue)

if ($javaAvailable -and ($script:PlantUmlJar -or $plantUmlOnPath)) {
    Write-Host "[PlantUML]" -ForegroundColor Yellow
    foreach ($file in $plantUmlFiles) {
        if (-not (Test-Path (Join-Path $ScriptDir $file))) {
            Write-Warning "Missing: $file"
            continue
        }
        try {
            if ($Format -eq "png" -or $Format -eq "both") {
                Invoke-PlantUmlExport -InputFile $file -OutputExt "png"
            }
            if ($Format -eq "pdf" -or $Format -eq "both") {
                Invoke-PlantUmlExport -InputFile $file -OutputExt "pdf"
            }
        } catch {
            Write-Warning "PlantUML export failed for ${file}: $_"
        }
    }
} else {
    Write-Warning "PlantUML not configured. Download plantuml.jar to docs/design/ or use https://www.plantuml.com/plantuml"
}

Write-Host ""
Write-Host "Done. Check: $ExportDir" -ForegroundColor Cyan

# List exported files
Get-ChildItem $ExportDir -File | ForEach-Object {
    $sizeKb = [math]::Round($_.Length / 1024, 1)
    Write-Host "  -> $($_.Name) ($sizeKb kilobytes)"
}
