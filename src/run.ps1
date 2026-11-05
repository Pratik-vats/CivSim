Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  Starting CivSim: Human Life & Civilization Simulator" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Launching server on http://localhost:8085 ..." -ForegroundColor Green
Start-Process "http://localhost:8085"
Write-Host "Opening browser..." -ForegroundColor Green
Write-Host ""

$javaExe = "C:\Program Files\Java\jdk-25.0.4.1\bin\java.exe"
if (Test-Path $javaExe) {
    & $javaExe -cp "bin;lib/*" civsim.web.CivSimWebApplication
} else {
    java -cp "bin;lib/*" civsim.web.CivSimWebApplication
}

