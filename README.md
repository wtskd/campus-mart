# 校园易站 CampusMart —— 校园二手交易平台（多端微服务）

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2023.0.1.0-6DB33F)
![Vue](https://img.shields.io/badge/Vue-3.4-42b883)
![uni-app](https://img.shields.io/badge/uni--app-H5%20%2B%20%E5%BE%AE%E4%BF%A1%E5%B0%8F%E7%A8%8B%E5%BA%8F-2b9939)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1)
![LLM](https://img.shields.io/badge/LLM-DeepSeek%20%2B%20Ollama-1f6feb)

> 仓库地址：<https://github.com/wtskd/campus-mart>
>
> 企业实习结课项目 · 个人项目 · 2026.10.12 – 2026.12.30
>
> 技术栈：Spring Cloud Alibaba + Vue3 + uni-app + MySQL + Redis + Nacos + 大模型（DeepSeek / Ollama / 模板兜底）

## 一、项目简介

校园二手交易平台，面向高校学生提供二手物品的发布、检索、交易、评价全流程服务，同时提供一套完整的后台管理、数据分析和 AI 能力。

**多端**：管理后台（Vue3 + Element Plus，PC 浏览器）＋ 用户端（uni-app，一套代码同时产出 **H5 / 微信小程序**）。

## 二、任务要求对照表

| 课程要求 | 本项目实现 | 证据 |
|---|---|---|
| **后台管理：权限管理** | RBAC 用户-角色-菜单三级模型，按钮级权限码 `模块:资源:动作`，支持通配；动态路由、`v-hasPerm` 按钮控制、权限变更即时生效 | `campus-system` 模块、`sys_user/role/menu/user_role/role_menu`、`campus-admin-web/src/directive/permission` |
| **后台管理：动态菜单** | 菜单树存库，登录后按角色下发，前端动态生成路由与侧边栏；支持目录/菜单/按钮三种类型与显示状态 | `GET /api/auth/menus`、`sys_menu.menu_type = M/C/F` |
| **后台管理：日志管理** | AOP 切面采集操作日志（操作人/耗时/IP/参数/返回值/异常），@Async 异步落库；登录日志含 IP、UA、成功失败 | `campus-common` 的 `OperLog`/`OperLogAspect`/`OperLogEvent`/`OperLogEventListener`、`sys_oper_log`、`sys_login_log` |
| **后台管理：项目基础数据管理** | 用户、角色、菜单、部门（学院/班级）、字典、参数、文件、公告、轮播、商品、分类、订单、评价、举报 | `campus-admin-web/src/views/**`、29 张表 |
| **前台用户端** | uni-app（Vue3）一套代码，H5 + 微信小程序双端；首页/分类/搜索/详情/发布/收藏/订单/消息/我的 | `campus-app/` |
| **数据分析统计模块** | 概览、GMV/订单趋势、分类成交、热门商品、卖家排行、转化漏斗、RFM 用户分层、留存、AI 消耗统计；T+1 预聚合表 + 定时任务；独立数据大屏 | `campus-analysis` 模块、`stat_*` 四张表、`/screen` 大屏页 |
| **AI 模型调用（远程/本地）** | 远程 DeepSeek（OpenAI 兼容）+ 本地 Ollama 兜底 + 模板兜底三级降级；4 个场景 + 1 个 Agent 式推荐：一键发布文案、RAG 智能客服、内容审核、智能估价、个性化推荐 | `campus-ai` 模块、`ai_call_log`、`ai_knowledge`、`ai_prompt_template` |

## 三、架构

```
                        ┌────────────────┐   ┌────────────────┐
   管理后台 Vue3  ──────▶│                │   │  uniapp H5/小程序│
   (5180)               │  Nginx / Vite   │◀──│   (5181)        │
                        └───────┬────────┘   └────────────────┘
                                │  /api  /upload
                        ┌───────▼─────────┐
                        │  campus-gateway │ :8081  鉴权/路由/CORS/静态资源
                        └───────┬─────────┘
        ┌───────────┬───────────┼───────────┬────────────┐
        ▼           ▼           ▼           ▼            ▼
   campus-auth  campus-system campus-item campus-analysis campus-ai
     :9200        :9201        :9202        :9203          :9204
    登录令牌      RBAC/字典    商品订单     统计大屏       大模型调用
        │           │           │           │            │
        └───────────┴─────┬─────┴───────────┴────────────┘
                          ▼
            ┌──────────┬──────────┬──────────┐
            │  MySQL   │  Redis   │  Nacos   │
            │ 29 张表   │ 令牌/缓存 │ 注册/配置 │
            └──────────┴──────────┴──────────┘
                                   │
                     ┌─────────────▼──────────────┐
                     │ DeepSeek(远程) / Ollama(本地) │
                     └────────────────────────────┘
```

- **网关**：统一入口、JWT 校验、Redis 登录态校验、用户上下文透传（`X-User-Id` 等）、白名单、CORS、`/upload/**` 静态资源直通。
- **服务间**：OpenFeign + Nacos 服务发现；鉴权服务登录时通过 Feign 取用户/权限/菜单。
- **降级**：AI 三级降级；Redis 不可用时分类树自动回源 DB；Feign 调用异常不阻断主流程。

## 四、技术栈

| 层次 | 技术 | 版本 |
|---|---|---|
| 语言/运行时 | JDK | 17 |
| 微服务框架 | Spring Boot / Spring Cloud / Spring Cloud Alibaba | 3.2.5 / 2023.0.1 / 2023.0.1.0 |
| 注册/配置中心 | Nacos | 2.3.2（standalone + MySQL 存储） |
| 网关 | Spring Cloud Gateway | 4.x |
| 服务调用 | OpenFeign + LoadBalancer | - |
| ORM | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | 8.0.44（InnoDB / utf8mb4 / ngram 全文索引） |
| 缓存 | Redis（RedisTemplate + 自写 RedisUtils） | 5.x |
| 鉴权 | JWT（jjwt） + Redis 登录态 | 0.12.5 |
| 接口文档 | Knife4j（OpenAPI 3） | 4.5.0 |
| 工具库 | Hutool | 5.8.27 |
| 管理后台 | Vue3 + Vite + Element Plus + ECharts + Pinia | - |
| 用户端 | uni-app（Vue3）+ uview-plus | - |
| AI | DeepSeek Chat API / Ollama（OpenAI 兼容协议） | - |

## 五、目录结构

```
campus-mart/
├── pom.xml                     # Maven 父工程（多模块聚合）
├── campus-common/              # 公共模块：Result/分页/异常/鉴权/日志切面/配置
├── campus-api/                 # 服务间契约：DTO + Feign 客户端
├── campus-gateway/             # 网关（8081）
├── campus-auth/                # 认证服务（9200）
├── campus-modules/
│   ├── campus-system/          # 系统管理（9201）
│   ├── campus-item/            # 商品/订单/评价/公告（9202）
│   ├── campus-analysis/        # 数据分析（9203）
│   └── campus-ai/              # AI 能力（9204）
├── campus-admin-web/           # 管理后台前端（Vue3）
├── campus-app/                 # 用户端 uni-app
├── sql/                        # 01 建表 / 02 业务种子 / 03-04 权限补丁 / 05 富演示数据 / 06-07 数据修正
├── docs/                       # 设计、接口契约、构建运行、测试报告
├── scripts/                    # 一键启动/停止/建库脚本（Git Bash 与 PowerShell 两套）
├── tools/                      # 自测与校验脚本（冒烟 / 契约 / 功能 / 统计重算 / 演示数据生成）
├── maven-settings.xml          # 可选：阿里云镜像加速
└── upload/                     # 上传目录（含 demo 占位图）
```

## 六、快速开始

环境要求：JDK 17、Maven 3.9+、Node 18+、MySQL 8（Redis / Nacos 由启动脚本拉起，需事先安装到本机）。

```bash
# 1. 建库 + 导入全部数据（01~07 按序执行，脚本内置幂等与行数校验）
bash scripts/init-db.sh

# 2. 构建后端（首次约 2-4 分钟；加 -s maven-settings.xml 走阿里云镜像更快）
mvn -DskipTests clean package

# 3. 启动：Redis + Nacos + 6 个后端服务，日志写到 logs/
bash scripts/start-all.sh

# 4. 前端（两个新终端）
cd campus-admin-web && npm i && npm run dev      # 管理后台 http://localhost:5180
cd campus-app       && npm i && npm run dev:h5   # 用户端   http://localhost:5181
```

> Windows 下也可以用 `powershell -ExecutionPolicy Bypass -File scripts/start-all.ps1`；停止用 `bash scripts/stop-all.sh`（加 `--with-infra` 连 Redis/Nacos 一起停）。
> 微信小程序：`cd campus-app && npm run dev:mp-weixin`，再用微信开发者工具导入 `dist/dev/mp-weixin`。

自测（服务启动后执行，全部走网关 8081）：

```bash
node tools/http-checks.js        # 33 项接口冒烟
node tools/func-test.mjs         # 56 项功能断言（CRUD/交易/AI，跑完自动清理）
node tools/check-method-match.mjs # 前端声明 vs 后端映射，缺失应为 0
node tools/refresh-stats.mjs     # 重算统计预聚合表
node tools/check-page-fields.mjs  # 21 个列表页：表格字段 vs 接口返回字段
node tools/check-query-params.mjs # 17 个列表页：筛选参数是否被后端识别
node tools/check-app-api.mjs      # 用户端 17 个接口连通性
node tools/check-admin-shapes.mjs # 后台 19 项数据结构断言（趋势/RFM/概览/大屏）
node tools/edge-test.mjs          # 异常路径体检（越权/非法参数/状态机/上传类型）
```

演示数据是脚本生成的，可复现（固定随机种子）：

```bash
node tools/gen-rich-seed.mjs      # 重新生成 sql/05-rich-data.sql（278 商品 / 117 订单 / 580+ AI 日志）
python tools/gen-seed-business.py # 重新生成 sql/02-seed-business.sql（基础业务种子）
```

详细步骤与常见问题见 [docs/06-build-and-run.md](docs/06-build-and-run.md)，验证结果见 [docs/07-verification.md](docs/07-verification.md)。

- 网关 API：http://localhost:8081/api
- 接口文档（Knife4j）：http://localhost:8081/doc.html
- 演示账号：`admin / admin123`（超管）、`operator / admin123`（运营）、`auditor / admin123`（审核）、`student01 / admin123`（学生）
- 另有 `student04 ~ student17`（14 个学生，密码同为 `admin123`），用于让商品/订单/评价/收藏/AI 调用看起来像真实在跑；数据来自 `sql/05-rich-data.sql`（278 商品 / 117 订单 / 458 收藏 / 580+ AI 日志）

## 七、AI 能力说明

| 场景 | 接口 | 说明 |
|---|---|---|
| 一键发布文案 | `POST /api/ai/copywrite` | 输入关键词/成色/价格 → 生成标题、卖点、描述、标签、建议价 |
| 智能客服（RAG） | `POST /api/ai/chat` | 问题 → `ai_knowledge` 全文检索召回 → 拼 Prompt → 回答 + 引用来源 |
| 内容审核 | `POST /api/ai/moderate` | 商品发布/评价内容送审，输出 `pass/riskLevel/words/reason`，违规置违规状态 |
| 智能估价 | `POST /api/ai/price` | 按品类/成色/原价给建议价与区间 |
| 个性化推荐 | `GET /api/ai/recommend` | 基于用户收藏/浏览的 ItemCF 协同过滤，冷启动走热度 |

**三级降级**：远程大模型（DeepSeek） → 本地模型（Ollama） → 模板/规则兜底。所有调用写入 `ai_call_log`（含 token 消耗、耗时、状态），后台可查消耗统计——保证演示环境断网也能跑通，且降级行为可被证明。

## 八、数据分析指标

| 指标 | 口径 |
|---|---|
| GMV | `biz_order.status=2`（已完成）的 `amount` 求和 |
| 订单量/完成率 | 按 `order_status` 分组；完成率 = 已完成 / 全部订单 |
| 商品供给 | 在售（status=2）、待审核（1）、已售出（4）数量 |
| 转化漏斗 | 浏览 → 收藏 → 下单 → 完成 |
| 用户分层 | RFM（最近购买/购买频次/购买金额）八分类 |
| 留存 | 按日新增用户与其次日/7 日活跃 |
| AI 消耗 | `ai_call_log` 的调用次数、token 总量、成功率、按场景分布 |

## 九、开发进度

| 周次 | 内容 |
|---|---|
| W1 | 需求分析、库表设计、环境搭建、项目骨架 |
| W2 | 认证与权限（JWT、RBAC、动态菜单） |
| W3 | 系统管理模块（用户/角色/菜单/部门/字典/日志） |
| W4 | 商品与分类、文件上传、审核流 |
| W5 | 交易闭环（订单状态机、评价、举报、消息） |
| W6 | AI 服务（文案/客服/审核/估价/推荐） |
| W7 | 数据分析与数据大屏 |
| W8 | uniapp 用户端、管理后台联调 |
| W9 | 联调测试、部署脚本、答辩材料整理 |

**验收状态（2026-10-28）**：后端接口冒烟 33/33 通过；前端声明的 64 个接口逐个真实探测 63 通过（1 条为正确的 403 业务规则）；`dist` 静态资源 `missing=0`；`start-all.ps1` 6 服务 34 秒内全部就绪。详见 [docs/07-verification.md](docs/07-verification.md)。


## 十、文档索引

| 文档 | 内容 |
|---|---|
| [docs/01-design.md](docs/01-design.md) | 总体设计、需求映射、架构、模块划分 |
| [docs/02-database.md](docs/02-database.md) | 数据库设计（29 张表字段说明） |
| [docs/03-api-contract.md](docs/03-api-contract.md) | **前后端接口契约 v1.2**（含联调后修正） |
| [docs/04-module-endpoints.md](docs/04-module-endpoints.md) | 模块与接口清单（自动生成，165 个映射） |
| [docs/05-backend-conventions.md](docs/05-backend-conventions.md) | 后端编码约定 |
| [docs/06-build-and-run.md](docs/06-build-and-run.md) | 构建与运行手册 |
| [docs/07-verification.md](docs/07-verification.md) | 验证与测试报告（三层验证、已知环境约束） |
| [docs/08-startup-guide.md](docs/08-startup-guide.md) | **Git Bash 启动手册**（4 个窗口，命令可直接粘贴） |
