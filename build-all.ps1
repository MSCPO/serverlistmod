# Build MSCPO-serverlist for all supported Minecraft versions on both loaders.
# Uses the local Gradle under D:\java (offline-friendly, no wrapper download needed).
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File build-all.ps1          # build all versions
#   powershell -ExecutionPolicy Bypass -File build-all.ps1 1.21.11  # build a specific version
#   powershell -ExecutionPolicy Bypass -File build-all.ps1 -Copy    # also copy jars to .\dist
#
# Retries each version a few times to work around flaky network downloads.

param(
    [string]$Version = "",
    [switch]$Copy
)

$ErrorActionPreference = 'Continue'

$Gradle = 'D:\java\gradle-9.5.1\bin\gradle.bat'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path

# Supported Minecraft versions (26.1 maps to 26.1.2, the toolchain-supported line).
$Supported = @('26.2', '26.1.2', '1.21.11', '1.21.8', '1.21.4', '1.21.1')

if ($Version) {
    if ($Supported -notcontains $Version) {
        Write-Host "Unknown version '$Version'. Supported: $($Supported -join ', ')" -ForegroundColor Red
        exit 1
    }
    $Targets = @($Version)
} else {
    $Targets = $Supported
}

foreach ($mc in $Targets) {
    Write-Host "`n========== Building $mc (Fabric + NeoForge) ==========" -ForegroundColor Cyan
    $ok = $false
    for ($attempt = 1; $attempt -le 5 -and -not $ok; $attempt++) {
        Write-Host "--- attempt $attempt ---"
        & $Gradle ":fabric:build" ":neoforge:build" "-Pmc=$mc" --console=plain
        if ($LASTEXITCODE -eq 0) {
            $ok = $true
        } else {
            Start-Sleep -Seconds 3
        }
    }
    if (-not $ok) {
        Write-Host "FAILED to build $mc" -ForegroundColor Red
        exit 1
    }
}

if ($Copy) {
    $dist = Join-Path $Root 'dist'
    New-Item -ItemType Directory -Force -Path $dist | Out-Null
    foreach ($mc in $Targets) {
        foreach ($loader in @('fabric', 'neoforge')) {
            Get-ChildItem (Join-Path $Root "$loader\build\libs") -Filter "*-$mc.jar" -ErrorAction SilentlyContinue |
                Copy-Item -Destination $dist -Force
        }
    }
    Write-Host "`nJars copied to $dist" -ForegroundColor Green
}

Write-Host "`nAll builds finished." -ForegroundColor Green
