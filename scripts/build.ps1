param(
    [Parameter(Position=0)][string]$Target,
    [switch]$Clean,
    [switch]$All,
    [switch]$CollectOnly,
    [string]$LogPath
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
$TargetsDir = Join-Path $Root 'targets'
$OutputDir = Join-Path $Root 'build-output'
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$transcriptStarted = $false
if ($LogPath) {
    $logDir = Split-Path -Parent $LogPath
    if ($logDir) { New-Item -ItemType Directory -Force -Path $logDir | Out-Null }
    try {
        Start-Transcript -Path $LogPath -Force | Out-Null
        $transcriptStarted = $true
    } catch {
        Write-Warning "Could not start transcript logging: $($_.Exception.Message)"
    }
}

function Show-MatrixProgress([int]$Current, [int]$Total, [string]$Name) {
    if ($Total -le 0) { return }
    $width = 30
    $filled = [Math]::Floor(($Current / $Total) * $width)
    if ($filled -gt $width) { $filled = $width }
    $bar = ('#' * $filled) + ('-' * ($width - $filled))
    $pct = [Math]::Floor(($Current / $Total) * 100)
    Write-Host ""
    Write-Host ("[{0}] {1,3}%  ({2}/{3})  {4}" -f $bar, $pct, $Current, $Total, $Name) -ForegroundColor Cyan
}

function Collect-TargetJars([string]$Name) {
    $dir = Join-Path $TargetsDir $Name
    $libs = Join-Path $dir 'build\libs'
    if (-not (Test-Path $libs)) {
        Write-Host "No existing build output for $Name" -ForegroundColor Yellow
        return 0
    }

    $count = 0
    Get-ChildItem $libs -Filter 'Windy_*.jar' | Where-Object {
        $_.Name -notmatch 'sources|dev|shadow'
    } | ForEach-Object {
        Copy-Item $_.FullName (Join-Path $OutputDir $_.Name) -Force
        Write-Host "Collected: $($_.Name)" -ForegroundColor DarkGray
        $count++
    }
    return $count
}

function Build-Target([string]$Name) {
    $dir = Join-Path $TargetsDir $Name
    $launcher = Join-Path $dir 'gradlew.bat'
    if (-not (Test-Path $launcher)) {
        throw "Unknown target '$Name' or missing target launcher. Run Build-One.bat without arguments to see targets."
    }

    Write-Host "`n=== $Name ===" -ForegroundColor Cyan
    Push-Location $dir
    try {
        $gradleArgs = @('--console=plain', '--no-daemon', '--no-watch-fs')
        if ($Clean) { $gradleArgs += 'clean' }
        $gradleArgs += 'build'

        Write-Host "Running target launcher: gradlew.bat $($gradleArgs -join ' ')" -ForegroundColor DarkGray

        # Start-Transcript in Windows PowerShell 5.1 does not reliably capture native
        # child-process stdout/stderr. Run through cmd.exe and mirror merged output.
        $escapedArgs = ($gradleArgs | ForEach-Object {
            if ($_ -match '[\s&|<>^]') { '"' + ($_ -replace '"','\"') + '"' } else { $_ }
        }) -join ' '
        $cmdLine = '"' + $launcher + '" ' + $escapedArgs + ' 2>&1'
        & cmd.exe /d /s /c $cmdLine | ForEach-Object { Write-Host $_ }
        $exitCode = $LASTEXITCODE
        if ($exitCode -ne 0) { throw "$Name build failed with exit code $exitCode" }

        [void](Collect-TargetJars $Name)
    }
    finally {
        Pop-Location
    }
}

try {
    $allTargets = @(
        'forge-1.20.1',
        'fabric-1.20.1',
        'fabric-1.21.1',
        'neoforge-1.21.1',
        'fabric-26.1.x',
        'neoforge-26.1.x',
        'fabric-26.2.x',
        'neoforge-26.2.x',
        'fabric-26.3.x',
        'neoforge-26.3.x'
    )

    if ($CollectOnly) {
        Write-Host "Rebuilding build-output from existing target outputs (no compilation)..." -ForegroundColor Cyan
        $totalCollected = 0
        foreach ($t in $allTargets) { $totalCollected += (Collect-TargetJars $t) }
        Write-Host "Collected $totalCollected jar(s) into: $OutputDir" -ForegroundColor Green
        exit 0
    }

    if ($All) {
        if ($Clean -and (Test-Path $OutputDir)) {
            Remove-Item $OutputDir -Recurse -Force
            New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
        }

        $failures = @()
        $total = $allTargets.Count
        $i = 0
        foreach ($t in $allTargets) {
            $i++
            Show-MatrixProgress $i $total $t
            try {
                Build-Target $t
                Write-Host "=== $t SUCCESS ===" -ForegroundColor Green
            } catch {
                $failures += $t
                Write-Host "=== $t FAILED ===" -ForegroundColor Red
                Write-Host $_.Exception.Message -ForegroundColor Red
            }
        }

        Write-Host "`n============================================================"
        if ($failures.Count -eq 0) {
            Write-Host "[##############################] 100%  ALL TARGETS BUILT SUCCESSFULLY" -ForegroundColor Green
            Write-Host "Output: $OutputDir"
            exit 0
        }

        Write-Host ("BUILD MATRIX FINISHED WITH {0} FAILURE(S):" -f $failures.Count) -ForegroundColor Red
        $failures | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
        throw ("Build failed for: " + ($failures -join ', '))
    }

    if (-not $Target) {
        Write-Host 'Available targets:'
        $allTargets | ForEach-Object { Write-Host "  $_" }
        Write-Host "`nUsage: .\scripts\build.ps1 fabric-26.3.x [-Clean]"
        Write-Host "       .\scripts\build.ps1 -All [-Clean]"
        Write-Host "       .\scripts\build.ps1 -CollectOnly"
        exit 0
    }

    Show-MatrixProgress 1 1 $Target
    Build-Target $Target
    Write-Host "[##############################] 100%  $Target BUILT SUCCESSFULLY" -ForegroundColor Green
    exit 0
}
finally {
    if ($transcriptStarted) {
        try { Stop-Transcript | Out-Null } catch {}
    }
}
