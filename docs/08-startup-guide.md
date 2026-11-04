# Git Bash 启动手册

> 全部命令在 **Git Bash** 里执行，项目目录：`/d/project/deepseek/person project/campus-mart`
> 一共开 **4 个窗口**：① 后端 ② 管理后台 ③ 用户端（④ 备用/排错）

---

## 0. 进入项目目录

所有脚本都相对项目根目录，先 cd 进去。

```bash
cd "/d/project/deepseek/person project/campus-mart"
```

---

## 1. 检查并清理残留进程

**为什么**：之前跑过或异常退出时，`java.exe` 会残留并占着端口，新服务起不来（表现为 `[SKIP]` 但接口报错）。

先看有没有占用（没有任何输出 = 干净，跳到第 2 步）：

```bash
netstat -ano | grep -E ":(8081|9200|9201|9202|9203|9204) " | grep LISTENING
```

有输出就把最后一列的 PID 结束掉（`14968` 换成你看到的数字，多个就多执行几次）：

```bash
taskkill //F //PID 14968
```

> `//F` 是两个斜杠，Git Bash 会转成 Windows 的 `/F`，写单斜杠会报错。

---

## 2. 初始化数据库（只跑一次）

**为什么**：建 29 张表 + 导入演示数据（278 商品 / 117 订单 / 460 收藏 / 580 条 AI 调用日志 + 权限菜单）。换机器或删库后才需要重跑。

```bash
bash scripts/init-db.sh
```

期望输出结尾：

```
  导入 01-schema.sql          ... OK
  导入 02-seed-business.sql   ... OK
  导入 03-perms-extra.sql     ... OK
  导入 04-perms-item-trade.sql ... OK

校验数据：
  商品 biz_item      = 60
  订单 biz_order     = 37
```

---

## 3. 启动后端（窗口 ①，之后别关）

**为什么**：一条命令拉起 Redis + Nacos + 6 个 Spring Boot 服务（gateway 8081、auth 9200、system 9201、item 9202、analysis 9203、ai 9204），日志写 `logs/`。

```bash
bash scripts/start-all.sh
```

脚本会先检查 6 个 jar（fat jar 都在 50MB 以上）；如果缺失或是残留的瘦 jar，会自动执行 `mvn -DskipTests package` 重新构建。

期望输出（最后 6 行都是 `[UP]`）：

```
[3/3] 等待端口就绪（最多 120 秒）
  [UP]   system           :9201
  [UP]   item             :9202
  [UP]   analysis         :9203
  [UP]   ai               :9204
  [UP]   auth             :9200
  [UP]   gateway          :8081

 全部就绪 ✅
```

---

## 4. 启动管理后台（窗口 ②）

**为什么**：Vue3 后台，5180 端口，`/api` 自动代理到网关 8081。

```bash
cd "/d/project/deepseek/person project/campus-mart/campus-admin-web"
npm run dev
```

看到 `Local: http://127.0.0.1:5180/` 即可。

---

## 5. 启动用户端 H5（窗口 ③）

**为什么**：uniapp 编译出的 H5，5181 端口，模拟小程序。

```bash
cd "/d/project/deepseek/person project/campus-mart/campus-app"
npm run dev:h5
```

看到 `Local: http://127.0.0.1:5181/` 即可。

---

## 6. 浏览器验收

**后台** http://127.0.0.1:5180/login → `admin` / `admin123`（**图形验证码必填**，看不清点图片刷新）

| 点这里 | 应该看到 |
|---|---|
| 系统管理 → 用户管理 / 角色管理 / 菜单管理 | 能增删改，角色能勾菜单权限树 |
| 退出，用 `operator` / `admin123` 再登录 | 侧边栏**菜单变少**（动态菜单生效） |
| 系统管理 → 操作日志 / 登录日志 | 有你刚才的操作记录（操作人/IP/耗时） |
| 商品管理 → 商品审核 | 4 条待审核商品，能通过/驳回 |
| 商品管理 → 订单管理 | 37 条订单，能筛选、能看状态 |
| 数据统计 → 数据概览 / 交易分析 / 转化漏斗 | GMV 趋势折线、分类饼图、漏斗 |
| 数据统计 → 数据大屏 | 深色大屏，数字翻牌 + 多图表 |
| AI 管理 → AI 调用日志 | 有调用记录，`provider=remote`、耗时、token |

**用户端** http://127.0.0.1:5181 → `student01` / `admin123`

| 点这里 | 应该看到 |
|---|---|
| 首页 | 轮播图 + 分类 + 商品列表（有图有价） |
| 点任一商品 | 详情：图片、价格、成色、卖家、收藏按钮 |
| 底部「发布」→ 点「AI 帮我写」 | 2 秒左右自动填好标题/描述/标签 |
| 底部「我的」 | 我的发布、我的收藏、我的订单 |

接口文档（答辩可现场调）：http://127.0.0.1:8081/doc.html

---

## 7. 停止

**为什么**：按端口把 6 个服务停掉，避免下次启动时端口被占。

```bash
bash scripts/stop-all.sh
```

连 Redis 和 Nacos 一起停：

```bash
bash scripts/stop-all.sh --with-infra
```

前端两个窗口按 `Ctrl + C`。

---

## 8. 报错对照

| 现象 | 处理 |
|---|---|
| `start-all.sh` 提示 `[ERR] MySQL :3306 未监听` | 管理员 CMD 执行 `net start MySQL80` |
| 某个服务 `[DOWN]` | `tail -n 40 logs/campus-xxx.out.log` 看最后 40 行 |
| 提示 `[SKIP] xxx 已在 :xxxx 运行` 但接口报错 | 回第 1 步清残留进程 |
| `taskkill` 报 `拒绝访问 / Access denied` | 用管理员身份打开 Git Bash 再执行 |
| 登录报「验证码错误」 | 后台登录必须填验证码；H5 走免验证码接口 |
| 页面能开但列表空、F12 报 404 | 后端没起，或网关不是 8081；先访问 http://127.0.0.1:8081/api/auth/captcha 看有无返回 |
| `npm run dev` 报 `Cannot find module 'vite'` | 缺依赖：在该目录执行 `npm install` |
| 构建时报 `Unable to rename ...jar to ...jar.original` / `文件正由另一进程使用` | 有残留 java 进程占着 jar：任务管理器结束所有 `java.exe`，或在 Git Bash 里 `taskkill //F //IM java.exe`（可能提示拒绝访问，那就用任务管理器） |
| `[BAD] xxx.jar 缺失或不是可执行的 fat jar` | 上次构建失败留下的瘦 jar（约 0.2MB）。结束残留 java 后重跑 `bash scripts/start-all.sh`，脚本会自动重建 |
| 端口被占用但服务没响应 | `bash scripts/stop-all.sh` 先停；停不掉说明进程在别的会话里，用任务管理器结束 `java.exe` |
| AI 返回 `provider: mock` | 没读到 `ai-key.env`（仓库外，可选） 里的 key，走离线兜底（功能仍可用） |

---

## 9. 看日志

**为什么**：服务起不来 90% 的答案都在这里，每个服务一个文件。

```bash
tail -n 40 logs/campus-gateway.out.log
```

一次搜所有服务的异常：

```bash
grep -iE "error|exception|caused by" logs/campus-*.out.log | tail -n 30
```

服务列表与端口：

```bash
netstat -ano | grep -E ":(8081|9200|9201|9202|9203|9204) " | grep LISTENING
```
