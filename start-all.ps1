# Starts every backend service in its own window, in the right order, waiting for each one to listen
# on its port before starting the next. Usage (from the project root):
#   .\start-all.ps1                 backend only
#   .\start-all.ps1 -WithFrontend   backend + Angular app (npm install is run the first time)
# Needs Maven (mvn) in the PATH. Close a window to stop its service, or run .\stop-all.ps1 to stop all.
param([switch]$WithFrontend)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backend = Join-Path $root 'backend'

# Order matters: the registry first, the gateway last (it needs the others to be registered to route to them).
$services = @(
    @{ Name = 'eureka-server';              Port = 8761 },
    @{ Name = 'auth-service';               Port = 8081 },
    @{ Name = 'patient-service';            Port = 8082 },
    @{ Name = 'consultation-service';       Port = 8083 },
    @{ Name = 'results-service';            Port = 8084 },
    @{ Name = 'ai-service';                 Port = 8085 },
    @{ Name = 'glucose-monitoring-service'; Port = 8086 },
    @{ Name = 'notification-service';       Port = 8087 },
    @{ Name = 'api-gateway';                Port = 8080 }
)

function Test-Port([int]$port) {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $client.Connect('localhost', $port)
        return $true
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

function Wait-Port([int]$port, [int]$timeoutSec) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (Test-Port $port) { return $true }
        Start-Sleep -Seconds 2
    }
    return $false
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host 'Maven (mvn) was not found in the PATH.' -ForegroundColor Red
    Write-Host 'Install Maven (https://maven.apache.org/download.cgi) and add its bin folder to the PATH,'
    Write-Host 'or start the services from IntelliJ instead (see the README).'
    exit 1
}

foreach ($s in $services) {
    $dir = Join-Path $backend $s.Name
    if (Test-Port $s.Port) {
        Write-Host ("{0,-28} already running on port {1}, skipped" -f $s.Name, $s.Port) -ForegroundColor Yellow
        continue
    }
    Write-Host ("{0,-28} starting (the first start compiles the code, it can take a few minutes)..." -f $s.Name)
    # The window closes when the service stops normally and stays open (pause) if it fails, so the error can be read.
    Start-Process -FilePath 'cmd.exe' -WorkingDirectory $dir `
        -ArgumentList '/c', "title $($s.Name) && mvn spring-boot:run || pause"
    if (Wait-Port $s.Port 300) {
        Write-Host ("{0,-28} up on port {1}" -f $s.Name, $s.Port) -ForegroundColor Green
        if ($s.Name -eq 'eureka-server') { Start-Sleep -Seconds 3 }
    } else {
        Write-Host ("{0,-28} did not open port {1} within 5 minutes: look at its window for the error." -f $s.Name, $s.Port) -ForegroundColor Red
        exit 1
    }
}

if ($WithFrontend) {
    $front = Join-Path $root 'frontend'
    if (Test-Port 4200) {
        Write-Host 'frontend                     already running on port 4200, skipped' -ForegroundColor Yellow
    } else {
        $cmd = 'title frontend && '
        if (-not (Test-Path (Join-Path $front 'node_modules'))) { $cmd += 'npm install && ' }
        $cmd += 'npm start || pause'
        Write-Host 'frontend                     starting...'
        Start-Process -FilePath 'cmd.exe' -WorkingDirectory $front -ArgumentList '/c', $cmd
        if (Wait-Port 4200 600) {
            Write-Host 'frontend                     up on port 4200' -ForegroundColor Green
        } else {
            Write-Host 'frontend did not open port 4200 within 10 minutes: look at its window.' -ForegroundColor Red
            exit 1
        }
    }
}

Write-Host ''
Write-Host 'Everything is running.' -ForegroundColor Green
Write-Host 'Registry:  http://localhost:8761   (wait ~30 s until the 8 services are listed)'
if ($WithFrontend) { Write-Host 'App:       http://localhost:4200' }
