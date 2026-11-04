# CampusMart 一键启动脚本（Windows / PowerShell）
# 用法: powershell -ExecutionPolicy Bypass -File campus-mart\scripts\start-all.ps1
# 说明: 自行管理 6 个 Java 进程, 日志写入 campus-mart\logs\<service>.log

$ErrorActionPreference = 'Stop'

$Root     = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)   # campus-mart
$Java     = 'D:\project\jdk17\bin\java.exe'
$LogDir   = Join-Path $Root 'logs'
$UploadDir = Join-Path $Root 'upload'

if (-not (Test-Path $Java))      { throw "找不到 JDK: $Java" }
if (-not (Test-Path $LogDir))    { New-Item -ItemType Directory -Path $LogDir | Out-Null }
if (-not (Test-Path $UploadDir)) { New-Item -ItemType Directory -Path $UploadDir | Out-Null }

# ---------- 加载 AI 密钥（不提交仓库，路径在 workspace/_env/ai-key.env） ----------
$AiEnvFile = Join-Path (Split-Path -Parent $Root) '_env\ai-key.env'
if (Test-Path $AiEnvFile) {
    Get-Content $AiEnvFile | ForEach-Object {
        if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') {
            [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
        }
    }
    Write-Host "已加载 AI 配置: $AiEnvFile" -ForegroundColor Green
} else {
    Write-Host "未找到 $AiEnvFile，AI 服务将走离线兜底" -ForegroundColor Yellow
}

# ---------- 基础设施检查 ----------
# 用 TcpClient 探测（比 Test-NetConnection 快几十倍，且不依赖 WinHTTP）
function Test-TcpPort([int]$Port) {
    $c = New-Object System.Net.Sockets.TcpClient
    try {
        $t = $c.ConnectAsync('127.0.0.1', $Port)
        return ($t.Wait(500) -and $c.Connected)
    } catch { return $false } finally { $c.Close() }
}

function Test-Port([int]$Port, [string]$Name) {
    if (-not (Test-TcpPort $Port)) { throw "$Name 未监听 127.0.0.1:$Port，请先启动它（见 docs\06-build-and-run.md 第 2 节）" }
    Write-Host "  [OK] $Name :$Port" -ForegroundColor Green
}

Write-Host "检查基础设施..." -ForegroundColor Cyan
Test-Port 3306 'MySQL'
Test-Port 6379 'Redis'
Test-Port 8848 'Nacos'

# ---------- 服务清单（启动顺序：业务服务 -> auth -> gateway） ----------
$Services = @(
    @{ Name = 'campus-system';   Port = 9201; Jar = 'campus-modules\campus-system\target\campus-system-1.0.0.jar' },
    @{ Name = 'campus-item';     Port = 9202; Jar = 'campus-modules\campus-item\target\campus-item-1.0.0.jar' },
    @{ Name = 'campus-analysis'; Port = 9203; Jar = 'campus-modules\campus-analysis\target\campus-analysis-1.0.0.jar' },
    @{ Name = 'campus-ai';       Port = 9204; Jar = 'campus-modules\campus-ai\target\campus-ai-1.0.0.jar' },
    @{ Name = 'campus-auth';     Port = 9200; Jar = 'campus-auth\target\campus-auth-1.0.0.jar' },
    @{ Name = 'campus-gateway';  Port = 8081; Jar = 'campus-gateway\target\campus-gateway-1.0.0.jar' }
)

# ---------- 检查 jar 是否已构建 ----------
foreach ($s in $Services) {
    $jarPath = Join-Path $Root $s.Jar
    if (-not (Test-Path $jarPath)) {
        throw "缺少 $($s.Name) 的 jar：$jarPath`n请先执行: mvn -s `"D:/project/deepseek/person project/_env/maven-settings.xml`" -DskipTests package"
    }
}

# ---------- 启动 ----------
foreach ($s in $Services) {
    $jarPath = Join-Path $Root $s.Jar
    $logOut  = Join-Path $LogDir "$($s.Name).log"
    $logErr  = Join-Path $LogDir "$($s.Name).err.log"

    $running = Test-TcpPort $s.Port
    if ($running) {
        Write-Host "  [SKIP] $($s.Name) 已在 :$($s.Port) 运行" -ForegroundColor Yellow
        continue
    }

    Write-Host "启动 $($s.Name) :$($s.Port) ..." -ForegroundColor Cyan
    # 注意：路径里有空格（person project），传给 java 的参数必须显式加双引号，
    # 否则 Start-Process 会把路径按空格拆开，报 "Unable to access jarfile D:\project\deepseek\person"
    $args = @(
        '-Dfile.encoding=UTF-8',
        '-Duser.timezone=Asia/Shanghai',
        '-Xms256m', '-Xmx768m',
        '-jar', "`"$jarPath`"",
        "--campus.upload.path=`"$UploadDir`""
    )
    Start-Process -FilePath $Java -ArgumentList $args -WindowStyle Hidden `
        -RedirectStandardOutput $logOut -RedirectStandardError $logErr | Out-Null
    Start-Sleep -Seconds 3
}

# ---------- 等待端口就绪 ----------
Write-Host "`n等待服务就绪（最多 120 秒）..." -ForegroundColor Cyan
$deadline = (Get-Date).AddSeconds(120)
foreach ($s in $Services) {
    $ready = $false
    while ((Get-Date) -lt $deadline) {
        if (Test-TcpPort $s.Port) {
            $ready = $true; break
        }
        Start-Sleep -Seconds 2
    }
    if ($ready) { Write-Host "  [UP]   $($s.Name) :$($s.Port)" -ForegroundColor Green }
    else        { Write-Host "  [DOWN] $($s.Name) :$($s.Port)  <-- 查看 logs\$($s.Name).log" -ForegroundColor Red }
}

Write-Host "`n完成。" -ForegroundColor Green
Write-Host "  网关      : http://localhost:8081/api"
Write-Host "  接口文档  : http://localhost:8081/doc.html"
Write-Host "  Nacos     : http://127.0.0.1:8848/nacos"
Write-Host "  日志目录  : $LogDir"
