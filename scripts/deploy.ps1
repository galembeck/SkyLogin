# Copies the built plugin jar into every SkyMinigames network server that uses it.
# Run "mvn clean package" first (the VS Code "Deploy to server" task does both).

$ErrorActionPreference = 'Stop'

$network = 'E:\.programming\Projects\.minecraft\.servers\SkyMinigames'
$plugin  = 'SkyLogin'
$servers = @('login')

$projectDir = Split-Path -Parent $PSScriptRoot
$jar = Get-ChildItem (Join-Path $projectDir 'target') -Filter "$plugin-*.jar" -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike 'original-*' } |
    Select-Object -First 1
if (-not $jar) {
    Write-Host 'No jar found in target\. Build the plugin first.' -ForegroundColor Red
    exit 1
}

$failed = @()
foreach ($server in $servers) {
    $serverDir = Join-Path $network $server
    if (-not (Test-Path $serverDir)) {
        Write-Host "Skipped $server (folder not found)" -ForegroundColor Yellow
        continue
    }
    $pluginsDir = Join-Path $serverDir 'plugins'
    New-Item -ItemType Directory -Force -Path $pluginsDir | Out-Null
    try {
        Remove-Item (Join-Path $pluginsDir "$plugin*.jar")
        Copy-Item $jar.FullName $pluginsDir
        Write-Host "Copied $($jar.Name) to $server" -ForegroundColor Green
    } catch {
        $failed += $server
    }
}

if ($failed.Count -gt 0) {
    Write-Host "Could not replace the jar on: $($failed -join ', '). Stop those servers first, then run this task again." -ForegroundColor Red
    exit 1
}