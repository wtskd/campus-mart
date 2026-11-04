# CampusMart 一键停止脚本（Windows / PowerShell）
# 用法: powershell -ExecutionPolicy Bypass -File campus-mart\scripts\stop-all.ps1
# 只杀本项目 campus-* 的 java 进程，不动 MySQL / Redis / Nacos

$names = @('campus-gateway','campus-auth','campus-system','campus-item','campus-analysis','campus-ai')

$targets = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
    Where-Object { $cmd = $_.CommandLine; $cmd -and ($names | Where-Object { $cmd -like "*$_*" }) }

if (-not $targets) {
    Write-Host '没有正在运行的 CampusMart 服务。' -ForegroundColor Yellow
    return
}

foreach ($p in $targets) {
    $label = ($names | Where-Object { $p.CommandLine -like "*$_*" } | Select-Object -First 1)
    Write-Host "停止 $label (PID $($p.ProcessId)) ..." -ForegroundColor Cyan
    Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
}

Start-Sleep -Seconds 2
Write-Host '已停止全部 CampusMart 服务。' -ForegroundColor Green
