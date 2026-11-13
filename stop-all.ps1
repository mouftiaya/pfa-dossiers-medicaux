# Stops every service started by start-all.ps1 (and the Angular dev server) by closing whatever listens on their ports.
$ports = 8080, 8081, 8082, 8083, 8084, 8085, 8086, 8087, 8761, 4200

foreach ($port in $ports) {
    $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($c in $connections) {
        try {
            Stop-Process -Id $c.OwningProcess -Force -ErrorAction Stop
            Write-Host "Stopped the process on port $port (pid $($c.OwningProcess))" -ForegroundColor Green
        } catch {
            Write-Host "Could not stop the process on port $port : $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}
Write-Host 'Done.'
