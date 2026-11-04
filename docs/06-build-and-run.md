# 构建与运行手册（Windows 本机一键启动）

> 目标：在一台 Windows 机器上，**不用 Docker**，把 CampusMart 全部微服务跑起来。
> 所有命令都在 `campus-mart/` 目录下执行。

## 0. 前置组件（已在本机就绪）

| 组件 | 版本 | 位置 | 端口 |
|---|---|---|---|
| JDK | 17.0.20 | `D:\project\jdk17` | - |
| Maven | 3.9.16 | `D:\project\maven` | - |
| MySQL | 8.0.44（Windows 服务 `MySQL80`） | `D:\project\mysql` | 3306 |
| Redis | 5.x | `D:\project\redis` | 6379 |
| Nacos | 2.3.2（standalone + MySQL 存储） | `<NACOS_HOME>` | 8848 |
| Node | 22.23.3 | - | - |

账号：MySQL `root/123456`；Nacos 控制台 `nacos/nacos`（http://127.0.0.1:8848/nacos）。

## 1. 初始化数据库（只做一次）

```bash
MYSQL="/d/project/mysql/bin/mysql.exe -uroot -p123456 --default-character-set=utf8mb4"
$MYSQL < campus-mart/sql/01-schema.sql        # 建库 + 29 张表 + 基础种子
$MYSQL < campus-mart/sql/02-seed-business.sql # 基础种子（29 表 + 分类/字典/菜单/公告）
$MYSQL < campus-mart/sql/03-perms-extra.sql   # 权限码归一 + 按钮级权限 + 字典补充（幂等）
$MYSQL < campus-mart/sql/04-perms-item-trade.sql # 公告/轮播/消息权限码归一（幂等）
$MYSQL < campus-mart/sql/05-rich-data.sql   # 富演示数据：278 商品/117 订单/460 收藏/580 AI 日志（可重复执行）
$MYSQL < campus-mart/sql/06-dict-fix.sql    # 补充 sys_normal_disable / sys_user_gender 字典（幂等，修复状态筛选「无数据」）
$MYSQL < campus-mart/sql/07-banner-links.sql # 首页轮播跳转地址修正为真实页面（幂等）
```

> `03`~`07` 都可重复执行；`02`、`05` 会先删后插（幂等）。`05` 跑完需要重算统计表：`node tools/refresh-stats.mjs`。
> 字典在 Redis 有 1 小时缓存：改了字典数据后如果前端仍是旧值，执行 `redis-cli DEL dict:sys_normal_disable` 清一下缓存。

## 2. 启动基础设施

```powershell
# Redis（配置文件必须带引号，路径含空格）
Start-Process -FilePath "D:\project\redis\redis-server.exe" `
  -ArgumentList '"scripts/redis.conf"' -WindowStyle Hidden

# Nacos（-D 参数含空格时必须用反引号引号包裹，否则报“找不到或无法加载主类”）
$ws = "D:\project\deepseek\person project"
$nacos = "<你的 Nacos 目录>"
Start-Process -FilePath "D:\project\jdk17\bin\java.exe" -WorkingDirectory $nacos -ArgumentList @(
  '-Xms512m','-Xmx512m','-Xmn256m',
  '-Dnacos.standalone=true',
  "`"-Dnacos.home=$nacos`"",
  '-Dloader.path=plugins,plugins/health,plugins/cmdb,plugins/selector',
  '-Dlogging.config=conf/nacos-logback.xml',
  '-jar','target/nacos-server.jar',
  '--spring.config.additional-location=file:conf/'
) -WindowStyle Hidden
```

验证：`redis-cli ping` → PONG；`http://127.0.0.1:8848/nacos` 可登录。

## 3. 构建

```bash
cd campus-mart
mvn -s maven-settings.xml -DskipTests clean package
```

产物：各模块 `target/*.jar`（`campus-gateway`、`campus-auth`、`campus-modules/campus-system|item|analysis|ai`）。

只编译某个模块（快速迭代）：

```bash
mvn -o -q -s maven-settings.xml -pl campus-modules/campus-item -DskipTests compile
```

## 4. 启动 5 个业务服务 + 网关

```powershell
# 一键启动（脚本会检查端口、按顺序拉起、日志写入 logs/）
powershell -ExecutionPolicy Bypass -File campus-mart\scripts\start-all.ps1

# 一键停止
powershell -ExecutionPolicy Bypass -File campus-mart\scripts\stop-all.ps1
```

端口：gateway 8081 / auth 9200 / system 9201 / item 9202 / analysis 9203 / ai 9204。

## 5. 启动前端

```bash
# 管理后台
cd campus-mart/campus-admin-web
npm install
npm run dev     # http://localhost:5180

# uniapp 用户端（H5）
cd campus-mart/campus-app
npm install
npm run dev:h5  # http://localhost:5181
```

## 6. 访问地址

| 用途 | 地址 |
|---|---|
| 管理后台 | http://localhost:5180 |
| uniapp H5 | http://localhost:5181 |
| 网关 API | http://localhost:8081/api |
| 接口文档（Knife4j） | http://localhost:8081/doc.html |
| 数据大屏 | http://localhost:5180/#/screen |
| Nacos 控制台 | http://127.0.0.1:8848/nacos |

## 7. 演示账号

| 账号 | 密码 | 角色 | 权限范围 |
|---|---|---|---|
| admin | admin123 | 超级管理员 | 全部（`*:*:*`） |
| operator | admin123 | 运营 | 商品/订单/公告/数据/AI 查看 |
| auditor | admin123 | 审核员 | 商品审核、评价审核、举报处理 |
| student01 ~ student03 | admin123 | 学生（前台用户） | 仅用户端 |
| student04 ~ student17 | admin123 | 学生（富演示数据里的买卖双方） | 仅用户端 |

## 8. 常见问题

| 现象 | 原因 / 解决 |
|---|---|
| `bash: fatal error - CreateFileMapping ... Win32 error 5` | 沙箱残留 bash 进程，`Get-Process bash \| Stop-Process -Force` 后重试 |
| 服务起不来，日志 `Connection refused: 127.0.0.1:8848` | Nacos 未启动或还在启动中，等 20-30 秒 |
| `WeakKeyException: The specified key byte array is ... bits` | JWT 密钥不足 32 字节，用 `CAMPUS_JWT_SECRET` 注入 ≥32 字符密钥 |
| 登录报「验证码错误」 | 后台登录需要图形验证码；小程序/App 用 `/api/auth/app/login` 免验证码 |
| 图片 404 | 确认 `campus.upload.path` 指向 `campus-mart/upload`，URL 为 `/upload/demo/item-01.png` |
| 接口 401 | 令牌过期（2h）或 Redis 里 `login:token:{userId}` 已失效，重新登录 |
| 接口 403 | 角色缺少权限码；管理员账号用 admin 验证是否为权限配置问题 |
| AI 接口返回 `provider: "mock"` | 未配置远程密钥且本地无 Ollama，走了模板兜底（功能正常，演示时说明降级策略） |

## 9. 环境变量（可选覆盖）

| 变量 | 默认值 | 说明 |
|---|---|---|
| `NACOS_ADDR` | `127.0.0.1:8848` | Nacos 地址 |
| `REDIS_HOST` / `REDIS_PORT` | `127.0.0.1` / `6379` | Redis |
| `DB_HOST` / `DB_PORT` | `127.0.0.1` / `3306` | MySQL |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `123456` | MySQL 账号 |
| `JWT_SECRET` | `CampusMart@2026#JwtSecretKeyForDemoOnly!` | ≥32 字符 |
| `CAPTCHA_ENABLED` | `true` | 后台登录图形验证码开关 |
| `AI_PROVIDER` | `remote` | `remote` / `local` / `mock` |
| `AI_BASE_URL` / `AI_API_KEY` / `AI_MODEL` | - | 远程大模型（OpenAI 兼容） |
| `OLLAMA_BASE_URL` | `http://127.0.0.1:11434/v1` | 本地模型端点 |

## 10. 一键脚本与验收

> 想照着一步一步敲（含每种报错的处理方式），看 [08-startup-guide.md](08-startup-guide.md)；本节是速查版。

| 脚本 | 用途 |
|---|---|
| `scripts/start-all.ps1` | 检查 MySQL/Redis/Nacos → 按序拉起 6 个 jar → 等待端口 → 打印访问地址。**会读取 `ai-key.env`（仓库外，可选）（若存在）注入 AI 配置** |
| `scripts/stop-all.ps1` | 按端口停掉 6 个 Java 进程 |
| `scripts/pack-deliverables.ps1` | 打「结课成果物」zip 到 `<workspace>/deliverables/`（自动剔除 node_modules/target/dist/logs） |

验收（答辩前自查，三层）：

```bash
# 1. 后端 33 项接口冒烟（需先起全栈：bash scripts/start-all.sh）
node tools/http-checks.js http://127.0.0.1:8081

# 2. 功能自测（CRUD + 交易闭环 + AI，56 项断言，跑完自动清理测试数据）
node tools/func-test.mjs

# 3. 契约一致性：前端声明 vs 后端映射、用户端 GET 实测、后台响应归一化
node tools/check-method-match.mjs
node tools/check-app-api.mjs
node tools/check-admin-shapes.mjs

# 4. 重算统计数据（导入 05-rich-data.sql 之后执行）
node tools/refresh-stats.mjs
```


最近一次全量验收结果（2026-10-28）：后端 **33/33 PASS**、前端契约 **63/64 通过**（1 条为正确的 403 业务规则）、静态资源 `missing=0`、`start-all.ps1` 6 服务 34 秒内全部 `[UP]`。详见 [07-verification.md](07-verification.md)。

> Windows PowerShell 5.1 下 `scripts/*.ps1` 必须以 **UTF-8 with BOM** 保存，否则中文注释会被按 ANSI 解码导致解析报错。

