$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $scriptDir
Write-Host "Starting CivSim on http://localhost:8085 ..." -ForegroundColor Green
node .\start.js
$civSimExitCode = $LASTEXITCODE
Pop-Location
exit $civSimExitCode

