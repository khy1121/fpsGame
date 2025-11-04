# 클라이언트 시작 스크립트
param(
    [int]$ClientNum = 1
)

Write-Host "=== Starting Client $ClientNum with Debug Logs ===" -ForegroundColor Cyan
$clientCP = "comfps-client\target\comfps-client-1.0-SNAPSHOT.jar;comfps-common\target\comfps-common-1.0-SNAPSHOT.jar"
java -cp $clientCP com.fpsgame.MainLauncher
