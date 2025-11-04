# 서버 시작 스크립트
Write-Host "=== Starting Server with Debug Logs ===" -ForegroundColor Cyan
$serverCP = "comfps-server\target\comfps-server-1.0-SNAPSHOT.jar;comfps-common\target\comfps-common-1.0-SNAPSHOT.jar"
java -cp $serverCP com.fpsgame.server.ServerMain --port 7777
