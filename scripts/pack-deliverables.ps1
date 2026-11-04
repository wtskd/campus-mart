# CampusMart 成果物打包脚本
# 用法: powershell -ExecutionPolicy Bypass -File campus-mart\scripts\pack-deliverables.ps1
# 产物: <workspace>\deliverables\CampusMart-结课成果物-<日期>.zip
#       内部结构: campus-mart\（源码+文档+SQL+脚本，剔除 node_modules/target/dist/logs/.git）

$ErrorActionPreference = 'Stop'

$Root    = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)   # campus-mart
$Ws      = Split-Path -Parent $Root
$EnvDir  = Join-Path $Ws '_env'
$Stage   = Join-Path $EnvDir 'stage'
$OutDir  = Join-Path $Ws 'deliverables'
$Date    = Get-Date -Format 'yyyyMMdd'
$ZipPath = Join-Path $OutDir "CampusMart-结课成果物-$Date.zip"

Write-Host "== CampusMart 成果物打包 ==" -ForegroundColor Cyan
Write-Host "源目录: $Root"

if (Test-Path $Stage) { Remove-Item $Stage -Recurse -Force }
New-Item -ItemType Directory -Path $Stage -Force | Out-Null
if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Path $OutDir -Force | Out-Null }

$Target = Join-Path $Stage 'campus-mart'
Write-Host "复制源码（排除 node_modules / target / dist / logs / .git）..." -ForegroundColor Cyan
$rc = robocopy $Root $Target /E /NFL /NDL /NJH /NJS /XD node_modules target dist logs .git .idea /XF *.log /R:1 /W:1
if ($LASTEXITCODE -ge 8) { throw "robocopy 失败，退出码 $LASTEXITCODE" }

# 精简上传目录：只保留演示图，去掉运行期上传
$Up = Join-Path $Target 'upload'
if (Test-Path $Up) {
    Get-ChildItem $Up -Directory | Where-Object { $_.Name -ne 'demo' } | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
    Get-ChildItem $Up -File -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
}

# 剔除开发期临时文件（不是成果物的一部分）
$JunkPatterns = @('_probe*.mjs', '_t1.mjs', '*.tmp', '*.bak', 'nohup.out')
foreach ($pat in $JunkPatterns) {
    Get-ChildItem $Target -Recurse -File -Filter $pat -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
}
# 周志不属于「项目电子资料」，单独交校友邦平台
$Jd = Join-Path $Target 'docs\journals'
if (Test-Path $Jd) { Remove-Item $Jd -Recurse -Force }

# 交付说明
$Readme = Join-Path $Stage '交付说明.md'
$lines = @(
    '# CampusMart 校园易站 · 结课成果物',
    '',
    '本项目为「企业实习」课程结课作业，个人独立完成的前后台多端微服务项目。',
    '',
    '## 一、成果物清单',
    '',
    '| 目录/文件 | 说明 |',
    '|---|---|',
    '| campus-mart/README.md | 项目总说明、任务要求对照表、快速开始 |',
    '| campus-mart/docs/ | 7 份文档：总体设计、数据库、接口契约、接口清单、编码约定、构建运行、验证报告 |',
    '| campus-mart/sql/ | 4 个 SQL：建表+种子、业务演示数据、权限补齐脚本 |',
    '| campus-mart/campus-common/** | 公共模块（统一响应、异常、JWT、Redis、权限注解、操作日志） |',
    '| campus-mart/campus-gateway/** | 网关（路由、JWT 校验、白名单、CORS） |',
    '| campus-mart/campus-auth/** | 认证服务（验证码、登录、退出、动态菜单） |',
    '| campus-mart/campus-modules/campus-system/** | 系统管理（用户/角色/菜单/学院/字典/参数/日志/文件） |',
    '| campus-mart/campus-modules/campus-item/** | 二手交易（商品/分类/收藏/评论/订单/举报/消息/公告/轮播） |',
    '| campus-mart/campus-modules/campus-analysis/** | 数据分析（概览/趋势/分类/漏斗/RFM/排行/大屏/CSV 导出） |',
    '| campus-mart/campus-modules/campus-ai/** | AI 能力（文案生成/RAG 客服/估价/内容审核/推荐） |',
    '| campus-mart/campus-admin-web/** | 管理后台（Vue3 + Element Plus + ECharts） |',
    '| campus-mart/campus-app/** | 用户端（uniapp Vue3，H5 + 微信小程序） |',
    '| campus-mart/scripts/ | 一键启动/停止/打包脚本 |',
    '| campus-mart/docker/ + docker-compose.yml | 容器化部署方案 |',
    '',
    '## 二、运行环境',
    '',
    '- JDK 17、Maven 3.9+、Node 18+、MySQL 8.0、Redis 6+、Nacos 2.3.x',
    '- 详细步骤见 campus-mart/docs/06-build-and-run.md',
    '',
    '## 三、三步跑起来',
    '',
    '```bash',
    '# 1. 建库（MySQL 8.0，ngram 全文索引需要）',
    'mysql -uroot -p < campus-mart/sql/01-schema.sql',
    'mysql -uroot -p campus_mart < campus-mart/sql/02-seed-business.sql',
    'mysql -uroot -p campus_mart < campus-mart/sql/03-perms-extra.sql',
    '',
    '# 2. 构建',
    'cd campus-mart && mvn -DskipTests package',
    '',
    '# 3. 启动（先起 Redis 与 Nacos）',
    'powershell -ExecutionPolicy Bypass -File campus-mart/scripts/start-all.ps1',
    '```',
    '',
    '启动后：网关 http://localhost:8081/api ，接口文档 http://localhost:8081/doc.html ，',
    '演示账号 admin / admin123（另有 operator / auditor / student01~03，密码同 admin123）。',
    '',
    '## 四、验证情况',
    '',
    '- 后端接口冒烟：33 项全通过',
    '- 前后端接口契约：前端声明的 64 个 GET 接口逐个真实探测，63 通过（1 条为正确业务规则）',
    '- 一键启动脚本：6 个服务 34 秒内全部就绪，网关鉴权/菜单/分析/AI 全链路正常',
    '- 详见 campus-mart/docs/07-verification.md',
    ''
)
[System.IO.File]::WriteAllText($Readme, ($lines -join "`r`n"), (New-Object System.Text.UTF8Encoding $true))

Write-Host "压缩中（Compress-Archive）..." -ForegroundColor Cyan
if (Test-Path $ZipPath) { Remove-Item $ZipPath -Force }
Compress-Archive -Path (Join-Path $Stage '*') -DestinationPath $ZipPath -CompressionLevel Optimal -Force

$size = [math]::Round((Get-Item $ZipPath).Length / 1MB, 2)
Write-Host ""
Write-Host "完成: $ZipPath  ($size MB)" -ForegroundColor Green
Write-Host "临时目录: $Stage （可删）"
