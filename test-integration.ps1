<#
 FPS Game integration test script (ASCII-only for PS 5.1 compatibility)
 - Starts server (1) and clients (2)
 - Requires built jars under comfps-*/target
#>

Write-Host "=== FPS Game Integration Test ===" -ForegroundColor Cyan
Write-Host ""

# Build check
if (-not (Test-Path "comfps-server\target\comfps-server-1.0-SNAPSHOT.jar")) {
    Write-Host "[ERROR] Missing server JAR. Run 'mvn clean package' first." -ForegroundColor Red
    exit 1
}

if (-not (Test-Path "comfps-client\target\comfps-client-1.0-SNAPSHOT.jar")) {
    Write-Host "[ERROR] Missing client JAR. Run 'mvn clean package' first." -ForegroundColor Red
    exit 1
}

# Start server
Write-Host "[1/3] Starting server..." -ForegroundColor Yellow
$serverCP = "comfps-server\target\comfps-server-1.0-SNAPSHOT.jar;comfps-common\target\comfps-common-1.0-SNAPSHOT.jar"
$serverProcess = Start-Process java -ArgumentList "-cp", $serverCP, "com.fpsgame.server.ServerMain", "--port", "7777" -PassThru -WindowStyle Normal
Write-Host "  Server PID:" $serverProcess.Id -ForegroundColor Green

# Wait server boot
Start-Sleep -Seconds 3

# Start client 1 (RED)
Write-Host "[2/3] Starting client 1..." -ForegroundColor Yellow
$clientCP = "comfps-client\target\comfps-client-1.0-SNAPSHOT.jar;comfps-common\target\comfps-common-1.0-SNAPSHOT.jar"
$client1Process = Start-Process java -ArgumentList "-cp", $clientCP, "com.fpsgame.MainLauncher" -PassThru -WindowStyle Normal
Write-Host "  Client1 PID:" $client1Process.Id -ForegroundColor Green

# Wait a bit
Start-Sleep -Seconds 2

# Start client 2 (BLUE)
Write-Host "[3/3] Starting client 2..." -ForegroundColor Yellow
$client2Process = Start-Process java -ArgumentList "-cp", $clientCP, "com.fpsgame.MainLauncher" -PassThru -WindowStyle Normal
Write-Host "  Client2 PID:" $client2Process.Id -ForegroundColor Green

Write-Host ""
Write-Host "=== Integration test launched ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Manual checks:" -ForegroundColor White
Write-Host "  1) RED spawn at ~ (450, 1700)" -ForegroundColor Red
Write-Host "  2) BLUE spawn at ~ (2550, 1700)" -ForegroundColor Blue
Write-Host "  3) Camera follows local player" -ForegroundColor White
Write-Host "  4) WASD movement syncs" -ForegroundColor White
Write-Host ""
Write-Host "Process info:" -ForegroundColor Yellow
Write-Host "  Server PID:" $serverProcess.Id -ForegroundColor Gray
Write-Host "  Client1 PID:" $client1Process.Id -ForegroundColor Gray
Write-Host "  Client2 PID:" $client2Process.Id -ForegroundColor Gray
Write-Host ""
