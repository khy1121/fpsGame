# FPS Game Integration Test
# Server 1 + Client 2

Write-Host "=== FPS Game Integration Test ===" -ForegroundColor Cyan
Write-Host ""

# Check build
if (-not (Test-Path "comfps-server\target\comfps-server-1.0-SNAPSHOT.jar")) {
    Write-Host "[ERROR] Server JAR not found. Run 'mvn clean package'" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path "comfps-client\target\comfps-client-1.0-SNAPSHOT.jar")) {
    Write-Host "[ERROR] Client JAR not found. Run 'mvn clean package'" -ForegroundColor Red
    exit 1
}

# Start server
Write-Host "[1/3] Starting server..." -ForegroundColor Yellow
$serverCP = "comfps-server\target\comfps-server-1.0-SNAPSHOT.jar;comfps-common\target\comfps-common-1.0-SNAPSHOT.jar"
$serverProcess = Start-Process java -ArgumentList "-cp",$serverCP,"com.fpsgame.server.ServerMain","--port","7777" -PassThru -WindowStyle Normal
Write-Host "  Server PID: $($serverProcess.Id)" -ForegroundColor Green

# Wait for server
Start-Sleep -Seconds 3

# Start client 1 (RED team)
Write-Host "[2/3] Starting client 1..." -ForegroundColor Yellow
$clientCP = "comfps-client\target\comfps-client-1.0-SNAPSHOT.jar;comfps-common\target\comfps-common-1.0-SNAPSHOT.jar"
$client1Process = Start-Process java -ArgumentList "-cp",$clientCP,"com.fpsgame.MainLauncher" -PassThru -WindowStyle Normal
Write-Host "  Client 1 PID: $($client1Process.Id)" -ForegroundColor Green

# Wait
Start-Sleep -Seconds 2

# Start client 2 (BLUE team)
Write-Host "[3/3] Starting client 2..." -ForegroundColor Yellow
$client2Process = Start-Process java -ArgumentList "-cp",$clientCP,"com.fpsgame.MainLauncher" -PassThru -WindowStyle Normal
Write-Host "  Client 2 PID: $($client2Process.Id)" -ForegroundColor Green

Write-Host ""
Write-Host "=== Test Running ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Test Points:" -ForegroundColor White
Write-Host "  1. RED spawn: (450, 1700)" -ForegroundColor Red
Write-Host "  2. BLUE spawn: (2550, 1700)" -ForegroundColor Blue
Write-Host "  3. Camera follows own character only" -ForegroundColor White
Write-Host "  4. WASD movement synced" -ForegroundColor White
Write-Host ""
Write-Host "Process Info:" -ForegroundColor Yellow
$serverPid = $serverProcess.Id
$client1Pid = $client1Process.Id
$client2Pid = $client2Process.Id
Write-Host "  Server PID: $serverPid"
Write-Host "  Client1 PID: $client1Pid"
Write-Host "  Client2 PID: $client2Pid"
Write-Host ""
