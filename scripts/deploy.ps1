# Copies the built plugin jar into the SkyMinigames server's plugins folder.
# Run "mvn clean package" first (the VS Code "Deploy to server" task does both).

$ErrorActionPreference = 'Stop'

$pluginsDir = 'E:\.programming\Projects\.minecraft\.servers\SkyMinigames\plugins'
$projectDir = Split-Path -Parent $PSScriptRoot

$jar = Get-ChildItem (Join-Path $projectDir 'target') -Filter 'SkyLogin-*.jar' -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike 'original-*' } |
    Select-Object -First 1
if (-not $jar) {
    Write-Host 'No jar found in target\. Build the plugin first.' -ForegroundColor Red
    exit 1
}
if (-not (Test-Path $pluginsDir)) {
    Write-Host "Server plugins folder not found: $pluginsDir" -ForegroundColor Red
    exit 1
}

try {
    Remove-Item (Join-Path $pluginsDir 'SkyLogin*.jar')
    Copy-Item $jar.FullName $pluginsDir
} catch {
    Write-Host 'Could not replace the plugin jar. Stop the server first, then run this task again.' -ForegroundColor Red
    exit 1
}

Write-Host "Copied $($jar.Name) to $pluginsDir" -ForegroundColor Green
