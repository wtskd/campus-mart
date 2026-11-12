# 07 · 验证与测试报告
> 本文件记录 2026-10-27 完成的三层自动化验证，以及复现方法。答辩时可直接引用这里的数字。
## 1. 验证体系（三层）
| 层 | 脚本 | 覆盖内容 | 结果 |
|---|---|---|---|
| L1 接口冒烟 | `tools/http-checks.js` | 起 Nacos/Redis + 6 个 Spring Boot 服务，逐项检查 33 个后端接口（登录、权限、商品、订单、评论、分析、AI） | **33 / 33 PASS** |
| L2 前后端契约 | `tools/check-method-match.mjs`、`tools/check-app-api.mjs`、`tools/check-admin-shapes.mjs` | 静态比对前端 141 个接口声明与后端 193 个映射；用户端 17 个 GET 带令牌实测；后台响应归一化 19 项断言 | **缺失 0 / 17 OK / 19 PASS** |
| L3 一键启动 | `campus-mart/scripts/start-all.ps1` | 校验 MySQL/Redis/Nacos 就绪 → 按序拉起 6 个 jar → 等待端口 → 输出访问地址 | 见第 4 节 |
构建产物：`campus-admin-web/dist`、`campus-app/dist/build/h5` 均可由 `npm run dev` 直接托管（dev server 已代理 `/api`、`/upload` 到网关 8081）。
## 2. L1 冒烟覆盖的 33 项
- 基础设施：Redis `PING`、Nacos 8848/9848 端口
- 认证：`GET /auth/captcha`、`POST /auth/app/login`（admin/admin123）、`GET /auth/me`、`GET /auth/menus`、无令牌访问受保护接口应 401
- 系统管理：用户/角色/菜单/字典/配置/操作日志/登录日志 的分页与详情
- 商品与交易：商品列表/详情/推荐、分类树、收藏、订单列表、订单详情、评论列表
- 分析：`/analysis/overview`、`/trend`、`/category`、`/funnel`、`/rfm`、`/ai-usage`
- AI：`POST /ai/copywrite`（真实远程大模型）、`POST /ai/chat`（RAG 客服）、`POST /ai/price`、`POST /ai/recommend`、`/ai/health`
## 3. 复现方法

```bash
# 0. 首次：建库 + 导数据（01~07 幂等，可重复执行）
bash scripts/init-db.sh

# 1. 一键启动：Redis + Nacos + 6 个后端服务（日志在 logs/）
bash scripts/start-all.sh

# 2. L1：33 项接口冒烟
node tools/http-checks.js http://127.0.0.1:8081

# 3. L2：契约一致性（静态映射 / 用户端 GET 实测 / 后台响应归一化）
node tools/check-method-match.mjs
node tools/check-app-api.mjs
node tools/check-admin-shapes.mjs

# 4. 功能自测：56 项断言（CRUD / 交易闭环 / AI），跑完自动清理测试数据
node tools/func-test.mjs

# 5. 停止（加 --with-infra 连 Redis/Nacos 一起停）
bash scripts/stop-all.sh
```

> 所有脚本默认连网关 `http://127.0.0.1:8081`，可传第一个参数改地址；前台 H5 与后台管理页另开两个终端：
> `campus-admin-web` → `npm run dev`（5180），`campus-app` → `npm run dev:h5`（5181）。
## 4. 已知环境约束（答辩前请注意）
1. **网关端口是 8081，不是 8080**。本机 8080 被一个非本项目进程占用（返回 `{"error":"Unauthorized: missing authentication"}`），代码/脚本/文档已统一改为 8081。若在自己的电脑上 8080 空闲，把 `campus-gateway/src/main/resources/application.yml` 的 `server.port` 改回 8080，并同步 `campus-admin-web/vite.config.js` 代理与 `campus-app/src/config.js` 的 `APP_BASE`。
2. **后台登录需要图形验证码**（`campus.auth.captcha-enabled=true`）。自动化脚本走的是免验证码的 `POST /auth/app/login`；手动演示后台请先在登录页刷新验证码。压测/自动化可设 `CAPTCHA_ENABLED=false`。
3. **AI 密钥不在仓库里**：通过环境变量 `AI_PROVIDER / AI_BASE_URL / AI_API_KEY / AI_MODEL` 注入（`start-all.sh` 会顺带加载仓库外的 `_env/ai-key.env`，若存在）。没有密钥时 AI 服务自动降级到本地 Ollama，再降级到模板兜底，接口始终 200。
4. **AI 服务 3 个场景已用真实远程大模型跑通**：文案生成（344 tokens / 2363 ms）、RAG 客服（399 tokens / 1404 ms）、内容审核（153 tokens / 885 ms），调用记录落 `ai_call_log`。
5. 服务重启后首次访问某个 `lb://` 路由会触发 Spring Cloud LoadBalancer 的 `ServiceInstanceListSupplier` 懒初始化，本机实测首次约 1~2 s（已加 `LoadBalancerWarmup` 预热，避免首个用户请求超时）。
## 5. 测试数据规模
| 表 | 行数 | 说明 |
|---|---|---|
| sys_user | 20 | admin / operator / auditor + student01~student17（学院、学号、信用分随机） |
| sys_role / sys_menu / sys_role_menu | 4 / 61 / 58+ | 超管 / 运营 / 审核员 / 学生，四级权限体系 |
| biz_category | 40 | 8 个一级 + 32 个二级，每个叶子分类都有真实商品 |
| biz_item | 278 | 129 在售 / 88 已售出 / 22 待审核 / 13 已预订 / 13 下架 / 8 违规 / 5 草稿 |
| biz_order | 117 | 88 已完成（累计 GMV ¥41090）/ 7 待确认 / 6 待交付 / 6 取消 / 3 退款 |
| biz_favorite / biz_comment / biz_report / biz_message | 458 / 76 / 14 / 34 | 收藏与评论回填到商品，举报有 6 条待处理，消息覆盖系统/订单/审核 |
| ai_call_log | 580+（实测 592，含自测调用） | 2026-09-25 ~ 今天，copywrite 38% / chat 24% / price 16% / audit 16% / recommend 6% |
| stat_overview_daily 等 4 张预聚合表 | 94 / 250 / 94 / 17 | 支撑 `/analysis/*`、大屏与 RFM 分层（17 个活跃用户） |
数据由 `campus-mart/sql/05-rich-data.sql` 生成（生成脚本按固定随机种子，可重复生成；时间全部相对 `NOW()` 计算，换机器导入后趋势图、今日 GMV、RFM 都还有数据）。
导入后如果统计页是空的，执行一次 `node tools/refresh-stats.mjs`（起 Redis + Nacos + analysis，调用 `POST /analysis/refresh` 重算 4 张表）。
## 6. 本轮修复的问题（2026-10-30）
联调 + 功能自测（`tools/func-test.mjs`，覆盖 14 组 CRUD / 交易 / AI 场景）过程中发现并修复：
| # | 问题 | 表现 | 修复 |
|---|---|---|---|
| 1 | 小程序订单接口方法不一致 | 前端用 `POST /item/app/order/{id}/pay\|deliver\|confirm\|cancel`，后端只写了 `@PutMapping` → **405 不支持的请求方式** | 后端改为 `@RequestMapping(method = {POST, PUT})`，新旧前端都兼容 |
| 2 | 取消订单原因丢失 | 前端把 `{cancelReason}` 放在 JSON body，后端只读 query 参数 | `cancel` 现在同时读 query 与 body |
| 3 | 后台「退款」按钮 404 | 前端调 `PUT /item/order/refund`，后端没有该接口 | 新增 `OrderRefundDTO` + `PUT /item/order/refund`（复用状态机 1/2 → 4） |
| 4 | 后台 AI「连通性测试」按钮只弹警告 | 前端调 `POST /ai/config/test`，后端未实现 | 新增 `GET /ai/config` 与 `POST /ai/config/test`（返回 ok / provider / model / costMs / message） |
| 5 | 演示账号被停用 | `operator / auditor / student01~03` 的 `status` 被改成 '1'，前台无法登录 | 修复数据，并在自测脚本里自动恢复被停用的演示账号 |
| 6 | 重启脚本竞态 | 杀掉旧进程后端口还没释放就启动，出现 `[SKIP] 已在运行` 但服务其实没起 | `run-smoke.ps1` 杀进程后轮询等待端口真正释放 |
| 7 | 启动脚本不校验 jar | 构建失败会留下 0.2MB 瘦 jar，启动时报「没有主清单属性」 | `start-all.sh` 启动前校验 jar 大小，不合格自动执行 `mvn -DskipTests package` |
**验证结果**：订单接口回归 **10/10 PASS**（付款 / 取消 / 退款 / 商品状态恢复全覆盖）；`GET /ai/config` 与 `POST /ai/config/test` 均返回 200。
## 7. 富数据回归（2026-11-02）
导入 `05-rich-data.sql`（278 商品 / 117 订单 / 580 AI 日志）后重跑 `tools/http-checks.js`（33 项）与前端契约探测，发现并修复 1 个真实的缓存 bug：
| # | 问题 | 表现 | 修复 |
|---|---|---|---|
| 8 | 字典缓存反序列化失败 | `GET /system/dict/data/type/{type}` **第二次**调用必现 500：`SerializationException: Unexpected token (START_OBJECT), expected VALUE_STRING ... (for subtype of java.lang.Object)` | 根因是 `stream().toList()` 返回 final 的不可变集合，Jackson 的 `NON_FINAL` 默认类型不会给容器写类型标记，回读时按 `Object` 反序列化直接抛错。`RedisUtils` 新增 `normalize()` 把 List/Set/Map 统一换成可变容器再写缓存，并在 `get()/hGet()` 捕获反序列化异常（清脏键 + 按未命中处理），旧缓存自动自愈 |
| 9 | 启动脚本不重建旧包 | 改了代码但 jar 没重新打包时，`start-all.sh` 只校验大小，启动的还是旧 jar | 增加源码 mtime 检测：任一 `*.java / *.yml / *.xml` 比 jar 新就自动 `mvn -DskipTests package` |
**回归结果**：`http-checks.js` **32 / 33 PASS**（唯一失败项即上面的字典缓存 bug，修复后单测 200）；匿名接口 6/6、后台 CRUD 7/7、统计 9/9、AI 8/8 全绿。
## 8. 管理后台页面问题回归（2026-11-02，用户实测反馈）
用户点进管理后台后报 4 类现象，逐一定位到「前后端数据结构假设不一致」，全部在前端修掉（后端接口本身正常）：
| # | 现象 | 根因 | 修复 |
|---|---|---|---|
| 10 | 首页「交易趋势」「RFM 用户分层」一直转圈，图表空白 | 后端返回对象（`trend` → `{days,dates,points[]}`、`rfm` → `{segments[]}`、`retention` → `{points[]}`、`screen` → `hotItem/recentOrder` 单数），页面按数组 `.map()` 消费，渲染时抛错导致 `v-loading` 卡死 | 新增 `campus-admin-web/src/utils/analysisShape.js`（纯函数归一化），`api/analysis.js` 所有分析接口统一走它：趋势/RFM/留存/概览/大屏都变成数组 + 数字 |
| 11 | 学院管理表格有 5 行但每列空白 | 页面 props 用 `deptName/deptId`，而 `getDeptTree` 调的是 `/system/user/deptTree`，返回的是 `{id,label}`（给树选择器用的格式） | `getDeptTree` 改为走 `/system/dept/list` 前端组树（`getDeptTreeFull`），表格与用户表单下拉共用完整字段 |
| 12 | 进入「用户管理 / 角色管理」自动弹出一串「已停用」 | 后端 `status` 是 `String`（'0'/'1'），`el-switch` 写的是数字 `:active-value="0"`，首屏渲染时值不匹配，Element Plus 会把值纠正成 inactiveValue 并触发 `change` → 每行弹一次提示 | `composables/useTable.js` 增加 `normalizeRow()`：把 `status/delFlag/sex/isRead/msgType` 等纯数字字符串统一转 `number`，所有列表页共用 |
| 13 | 消息通知点进去 404 | 菜单 206 的 `component=item/message/index`，但前端没有这个页面文件 | 新增 `views/item/message/index.vue`（按接收人/类型/已读筛选 + 分页）与 `api/item.js` 的 `getMessagePage` |
> 关于「弹窗判断正不正常」：**不正常**，属第 12 条 bug，正常进入列表页不应有任何提示。
**验证方式**：新增 `tools/check-admin-shapes.mjs`，直接 import 前端同一份归一化函数，对真实后端响应断言（19 项），前端不需要开浏览器即可回归：
```bash
node tools/check-admin-shapes.mjs
# RESULT: PASS (pass=19, fail=0)
```
覆盖：overview 数字字段、trend 7 天且 `statDate` 齐全、rfm 9 个分层、retention 留存率 0~100、aiUsage.byScene 5 个场景、screen 热门商品/实时订单、dept/list 5 条含 `deptName`、message/page 34 条含 `receiverNickname/msgTypeText`。
## 9. 管理后台第二轮 UI 回归（2026-11-02，用户实测第二批反馈）
| # | 现象 | 根因 | 修复 |
|---|---|---|---|
| 14 | 商品管理里封面图全部「加载失败」 | 图片 URL 是 `/upload/demo/item-01.png`，但 `campus-admin-web/vite.config.js` 只代理了 `/api`，`/upload` 落到前端 dev server 的 SPA 回退，返回 `text/html` | dev server 增加 `/upload -> http://localhost:8081` 代理（与 `campus-app` 一致）；网关侧 `/upload/**` 白名单本来就有，直接 200 `image/png` |
| 15 | 用户管理不能按昵称搜 | 后端 `/system/user/list` 早就支持 `nickname` 参数，前端表单只有用户名/手机号/学院/状态 | 查询区补「昵称」输入框，`useTable` 初始 query 增加 `nickname` |
| 16 | 用户分析 RFM 环形图「不正常」：9 个分层标签互相压字、图例挤成 1/2 页、`roseType` 半径玫瑰图在 0 值分层上画成尖刺 | 饼图同时开了 rose 模式 + 外置标签 + 底部横向图例，空间不够 | 改成标准环形图（`radius 48%~68%`），标签默认隐藏、hover 才显示，图例改右侧纵向滚动并去掉「客户」后缀，中心显示分层总人数；同时修 `segmentCount('重要价值')` 精确匹配取不到「重要价值客户」导致 KPI 恒为 0 |
| 17 | 数据大屏进去之后出不来：没有「退出大屏」入口，全屏也只有个无文字图标 | 大屏页没有任何返回后台的按钮；全屏按钮没有状态同步，全屏后视觉无变化，也不知道 Esc 可退出 | 左上角新增「退出大屏」按钮（`router.push('/dashboard')`，全屏时先解除全屏再返回），右侧全屏按钮改为带文字的「全屏 / 退出全屏（Esc）」并监听 `fullscreenchange` 同步；Esc 键分两级：全屏时先退全屏，非全屏时退出大屏 |
| 18 | 用户分析「活跃与留存」折线末尾突然掉到 0 | 最后一天（今天）的次日留存还没产生，后端 `retained1=0`，前端按 `retained/active` 算成 0% | `normalizeRetention` 用后端 `rate1` 口径，最后一天无次日数据时置 `null`（折线断开），平均留存率跳过 null 点 |
| 19 | 数据大屏「AI 使用量」空白、「实时成交订单」状态列为空 | `ai-usage` 只返回 `scenes` 没有 `byDay`；`screen.recentOrder` 只回成交单、没有 `status` 字段 | AI 面板在无按天数据时退化为「按场景」柱状图（场景名中文映射）；订单状态缺省按「已完成」渲染，并按后端 `OrderStatus` 枚举纠正状态文案（0 待确认/1 待交付/2 已完成/3 已取消/4 退款中/5 已退款） |
> `fullscreenchange` + Esc 的处理说明：浏览器原生全屏本身支持 Esc 退出，但没有「可见按钮」时用户不知道；第 17 条同时补了按钮文案与状态同步，两个入口都能退出。

**验证**：`npm run build` 通过（7.4s）；`node tools/check-admin-shapes.mjs` → `PASS (19/19)`；`node tools/http-checks.js http://127.0.0.1:8081` → `PASS=33 FAIL=0`；`/upload/demo/item-01.png` 经网关与（重启后的）dev server 均返回 `image/png`。
## 10. 用户端（uniapp H5）回归（2026-11-02，用户实测第三批反馈）
| # | 现象 | 根因 | 修复 |
|---|---|---|---|
| 20 | 点任意商品进详情页，显示「商品不存在或已下架」，并弹出 toast「参数类型错误：itemId」 | `<item-card>` 上写的是 `@tap="goDetail"`，uni-app 把自定义组件上的 `@tap` 编译成原生 `onClick` 透传到组件根节点，回调收到的是 DOM 事件而不是商品对象 → `itemId=undefined` → 请求 `/item/app/item/undefined` → 后端 `MethodArgumentTypeMismatchException` 返回 400 | `item-card.vue` 改为 `defineEmits(['select','favorite'])`，根节点 `@tap="onTapCard"` 里 `emit('select', props.item)`；5 处调用点改为 `@select="goDetail"`（首页/分类/搜索/收藏/我的发布）。详情页 `onLoad` 另加数字校验，脏参数直接走空态而不是发请求 |
| 21 | 分类页除「全部」外，点任何一级/二级分类都显示「该分类下暂无商品」 | 278 件商品都挂在 31 个叶子分类上，而后端 `pageAppItems` 对 `categoryId` 是**精确匹配**（`eq(category_id, ?)`），点一级分类（如「数码电子」）自然查不到 | 前端 `category.vue` 改为：选中二级直接查；选中一级则对其全部子分类并发查询再按 `publishTime` 倒序本地分页合并。后端 `ItemServiceImpl.pageAppItems` 同步改为「有子分类就 `in(子分类 ids)`」，重启后由服务端直接支持 |
| 22 | PC 浏览器打开 H5，页面按 1920px 全宽铺开，图片巨大、比例全乱 | H5 端没有做宽屏适配，`uni-app` 根节点默认 100% 宽 | `App.vue` 增加 `@media (min-width: 600px)`：页面主体固定 480px 居中、灰底 + 投影，tabBar 与底部固定操作条同步居中限宽；小程序/真机不命中该断点 |
| 23 | 首页金刚区显示英文 `phone / education / shopping ...` | `biz_category.icon` 存的是英文 key，页面直接 `{{ c.icon }}` 输出 | 首页增加 `CATEGORY_ICONS` 映射（phone→📱 laptop→💻 education→📚 shopping→🧴 run→🏃 star→✨ home→🛏️ music→🎸 more→📦 等）+ `categoryIcon()`，已是 emoji 的原样输出，未知 key 兜底 📦 |
| 24 | 分页组件 `total` 传的是字符串（后端 Long 统一序列化成 String），`el-pagination` 的 `total` 声明是 Number | 全局 Jackson 配置把 Long 序列化为字符串（避免 JS 大整数精度丢失），前端 `normalizePage` 直接透传 | `api/system.js` 的 `normalizePage` 把 `total/current/size/pages` 统一 `Number()`，`PASS` 后分页页码与总数正常 |
| 25 | 评价弹窗里已经打了 5 星、也写了评语，提交仍提示「评分不能为空」 | 前端按返回体字段名传了 `score`，但创建接口的 DTO 是 `rating`（`CommentCreateDTO.java:42` 才是 `@NotNull(message="评分不能为空")`；`CommentVO` 返回体才叫 `score`），后端收不到评分 | `pages/order/detail.vue` 提交体改为 `rating: score.value`；`api/comment.js` 的 JSDoc 同步。实测 `POST /item/app/comment` → 200「评价成功」 |
| 26 | 改成 480px 手机宽度后：顶部返回箭头跑到窗口最左边、底部 tabBar 整个消失 | 上一版用 `transform: translateX(-50%)` 去居中 `uni-tabbar`，和 uni 框架自己给固定栏用的 `left/right` 互相打架；同时 `uni-page-head`、`uni-tabbar` 仍按整屏定位 | 改用框架自带的定位变量：`html { --window-left: calc(50vw - 240px); --window-right: calc(50vw - 240px) }`，`uni-page-head` 与 tabBar 自动收进居中列；`uni-tabbar{width:480px}` 覆盖框架写死的 `width:100%`；删除 transform 与 `overflow-x:hidden` |

**验证**：
- `node tools/check-method-match.mjs` → 前端声明 141 / 后端映射 193 / **缺失 0**（解析器新增 `@RequestMapping(value=..., method={POST,PUT})` 多方法识别，消除 4 条订单动作接口误报）。
- `node tools/check-app-api.mjs` → 用户端 17 个 GET **OK=17 FAIL=0**。
- 分类合并实测：数码电子（5 个子类）合并 129 件可见，教材书籍、生活用品同理；单个二级「手机」8 件、「平板电脑」4 件。
- `npm run build:h5` → `DONE Build complete.`；vite dev 热更新已确认 `item-card` 编译产物为 `emits:['select','favorite']`、首页为 `onSelect: $setup.goDetail`、`App.vue` 样式含 `min-width: 600px`。

> 说明：PC 端 480px 限宽只在 H5 生效，微信小程序端由微信自身按手机宽度渲染，不受影响。

## 11. 用户端第二轮代码走查（2026-11-03，无需用户操作即发现）
> 这一轮不是用户报的 bug，是按「发布 → 详情 → 下单 → 评价」主链路逐个字段比对前后端发现的，共 6 条。

| # | 现象 | 根因 | 修复 |
|---|---|---|---|
| 27 | 发布商品选「交易方式」后，接口返回 400「交易类型只能为 1、2、3」 | 页面选项是 `['校内自提','快递','均可']`，取 `tapIndex`（0/1/2）直接当 `tradeType` 提交；而后端 `biz_item.trade_type` 的枚举是 **1 出售 / 2 求购 / 3 交换**（`ItemPublishDTO` `@Min(1) @Max(3)`），选第一项就发 0 | `pages/item/publish.vue` 选项改为 `['出售','求购','交换']`、`tradeType = tapIndex + 1`、默认值 1、label 改「交易类型」；`config.js` 的 `tradeType` 映射同步为 1/2/3（与后台 `TRADE_TYPE_MAP` 一致） |
| 28 | 标题写 4 个字能通过前端校验，但提交后端报「商品标题长度需在 5-50 个字符之间」 | 前端 `validate()` 用 `length < 4`，后端 `@Size(min = 5)` | 前端改 `length < 5`，提示「标题至少 5 个字」 |
| 29 | 商品详情的卖家卡片：昵称永远「校园卖家」、头像永远默认图、信用分永远「—」 | 接口把卖家信息放在嵌套对象 `seller:{nickname,avatar,creditScore}`（`ItemDetailVO.seller`），模板读的是扁平字段 `item.sellerNickname / sellerAvatar / sellerCreditScore` | `pages/item/detail.vue` 拿到详情后拍平：`item.value = { ...res, sellerNickname: seller.nickname, sellerAvatar: seller.avatar, sellerCreditScore: seller.creditScore }` |
| 30 | 订单列表 / 订单详情的商品封面图空白 | 前端读 `itemCoverUrl`，`OrderVO` 的字段名是 `itemCover` | 两处改为 `mediaUrl(o.itemCover || o.itemCoverUrl)`（兼容两种命名） |
| 31 | 学生账号发布商品、评价晒图时上传图片返回 `403 没有操作权限：system:file:upload,system:file:list` | 上传接口挂了 `@RequiresPerm({"system:file:upload","system:file:list"})`，而这两个权限只授给了 admin/operator，`student` 角色 `perms` 为空 | `SysFileController` 的上传/批量上传改为**只要求登录**（网关已校验令牌），列表/删除仍走权限码；文件类型白名单与 10MB 上限校验不变 |
| 32 | 个人中心头像一直显示默认图 | `/auth/me` 返回的 `LoginUser` 没有 `avatar` 字段，前端 `userStore.avatar` 取不到 | `LoginUser` 增加 `avatar`；`AuthService.login()` 写入 `info.getAvatar()`（登录态存 Redis，重新登录后生效） |

**关于第 26 条（PC 480px 居中）的最终做法**：uni-h5 会在 `documentElement` 上写**行内** `--window-left/--window-right`（`updateCssVar`），行内变量优先级高于样式表，所以「在 CSS 里改 `--window-*`」无效，tabBar 会跑到最左边。正确做法是用框架自带的 `pages.json` → `globalStyle.maxWidth: 480`：uni 会给 `uni-app` 写 `max-width:480px;margin:0 auto`，并把两个变量设成真实留白，导航栏与 tabBar 自动收进中间列。`App.vue` 里同时留一条兜底 `.uni-page-head, uni-tabbar { left: calc(50% - 240px) !important; width: 480px !important }`（`50%` 取的是不含滚动条的 clientWidth，与 `margin:auto` 的居中一致），以及页面内 `.bottom-bar/.mask` 等 fixed 元素的同款规则。

**验证**：`mvn -o -q -DskipTests compile` 全模块通过；`npm run build:h5` → `DONE Build complete.`；vite dev 编译产物已确认包含 `tapIndex + 1`、`sellerNickname: seller.nickname`、`itemCover || o.itemCoverUrl`、`calc(50% - 240px)` 与 `max-width: 480px`；dist 产物 `__uniConfig.globalStyle.maxWidth = 480`。

> 注意：第 27–32 条里有 4 条是后端改动（`campus-common`/`campus-auth`/`campus-system`），必须重新打包并重启服务才生效；前端改动 dev server 热更新即可。

补充两条（同轮走查发现，后端列表 VO 缺字段）：

| # | 现象 | 根因 | 修复 |
|---|---|---|---|
| 33 | 首页/搜索/收藏页的收藏心形永远是空心，收藏过的商品也显示未收藏 | 列表接口 `ItemListVO` 没有 `favorited` 字段（只有详情 `ItemDetailVO` 有），前端 `item.favorited` 恒为 undefined | `ItemListVO` 增加 `favorited`；`pageAppItems` 在已登录时按当前用户批量查 `biz_favorite` 回填 |
| 34 | 「我的发布」里被驳回的商品看不到驳回原因 | `ItemListVO` 没有 `auditRemark`，`toListVO` 也没赋值 | 同上补 `auditRemark` 字段与赋值（`pageMyItems` 共用 `toListVO`，一并生效） |

## 12. AI 客服「一直思考」排查（2026-11-03，用户实测）

用户问题："这个怎么思考不出来，是我的 ai 没配置好还是代码有问题"。

**先排除配置问题**：`ai_call_log` 里 2026-11-03 16:47:26 有一条 `scene=chat / provider=remote / status=0 / 1814ms / 520 tokens`，说明 API Key、base-url、模型名都正确，真实大模型调用成功过。同一分钟往后（16:54:27、16:54:33×2、16:54:54）4 条全是：

```
AI provider[remote] 调用失败，尝试下一个兜底：HTTP 503 {"error":{"message":"Service is too busy.
We advise users to temporarily switch to alternative LLM API service providers. ..."}}
AI 调用降级成功：remote → mock（耗时 1ms）
```

即 DeepSeek 侧限流/繁忙，属于外部服务波动；后端降级链按设计生效（`status=2`，返回本地模板）。用网关实测同一条问题：HTTP 200、3035ms、`provider=mock`、`sources` 3 条，**接口本身没坏**。

**真正的代码问题在前端**（两个都修了）：

| # | 现象 | 根因 | 修复 |
|---|---|---|---|
| 35 | 后端 3 秒就返回了，气泡却一直停在「AI 正在思考…」 | `components/ai-chat.vue` 把消息对象先 `messages.value.push(aiMsg)` 再用**原始对象** `aiMsg.loading = false` 改状态。`ref([...])` 里的元素在模板中是通过响应式代理读取的，改原始对象不会触发该字段的依赖通知，视图可能停留在 loading 分支 | 改为 `const aiMsg = messages.value[messages.value.length - 1]`（拿代理再改）；再加 `Promise.race` 客户端超时 35s、失败文案 + 「回答失败，点此重试」、`sources` 做 `Array.isArray` 过滤、`meta` 显示 `provider · 本地模板回答 · costMs` |
| 36 | 每次提问都要白等 3 秒（远程 503 才降级） | 降级链每次都先试 remote，503 往返 3s 才轮到 mock | `AiProviderRouter` 增加 60s 冷却：远程失败后 `remoteCoolUntilMs = now + 60_000`，冷却期内直接跳过 remote 走兜底；远程成功后清零。日志会写 `remote:冷却中（上次调用失败，直接走兜底）` |

**验证**：`mvn -o -B -q -pl campus-modules/campus-ai -DskipTests compile` → exit 0；`npm run build:h5` → `DONE Build complete.`，产物 `dist/build/h5/assets/ai-chat.*.js` 含「回答失败」。后端改动需重新打包重启 `campus-ai` 才生效（`bash scripts/stop-all.sh && bash scripts/start-all.sh`）。

**给用户的结论**：AI 配置没问题，是 DeepSeek 短时限流 + 前端 loading 状态写法导致气泡不消失。远程恢复后自动回到真实大模型，无需改配置。

## 13. 订单评价状态闭环（2026-11-03，用户实测反馈）

用户："不允许重复评价做得很好，但评价完应该把「评价卖家」和外面的「去评价」换成「已评价」，并支持查看评价内容"。

| # | 现象 | 根因 | 修复 |
|---|---|---|---|
| 37 | 评价成功（接口 200「评价成功」，库里有 `biz_comment` 记录）后，订单详情仍是「评价卖家」、订单列表仍是「去评价」；再次点开弹窗提交才提示「该订单已评价，请勿重复评价」 | 前端 `canComment` 依赖 `order.commented`，但 `OrderVO` **根本没有这个字段**（也没有评价内容字段），后端从未回填，前端恒为 `undefined` → 条件成立 | ①`OrderVO` 增加 `commented/commentId/commentScore/commentContent/commentTime/commentReply`；②`OrderServiceImpl` 注入 `BizCommentMapper`，`toVOList` 里按 orderIds 批量查 `biz_comment` 中间表填充（列表与详情共用）；③前端：详情页 `canComment` →「评价卖家」、新增 `hasComment` →「已评价 · 查看」弹窗（星级 + 内容 + 时间 + 卖家回复）；列表页 `hasComment(o)` →「已评价」按钮（点击进详情看内容） |

**兼容旧后端的兜底**：`pages/order/detail.vue` 增加 `fillCommentFallback(order)`——若订单已完成且 `commented` 为空，则调用 `GET /item/app/comment/list/{itemId}?current=1&size=50`，用 `String(c.orderId) === String(o.orderId)` 找自己那条评价并回填。`pages/order/list.vue` 增加同名思路的 `fillCommented(records)`：只处理本页「已完成 + 我买到的 + `commented === undefined`」的订单（最多 10 条，`Promise.all` 并发），匹配到就改响应式数组里的 `target.commented/commentScore/commentContent/commentTime/commentReply`。这样即使没重启 item 服务，列表与详情也能立刻显示「已评价」。

**实测数据**：用户测试产生的订单 4119（orderNo `CM202611031719211309`，商品 3161，买家 11）已有评价 `comment_id=5081, score=5, content='111'`；`GET /api/item/app/comment/list/3161` 返回的 `CommentVO` 含 `orderId:"4119"`，兜底匹配可用。库内 `biz_comment` 共 77 条，其中 76 条为演示数据。旧 jar 实测 `order/my` 的 10 条记录**没有 `commented` 键**（`'commented' in rec[0] === false`），故兜底必然触发。

**验证**：`mvn -o -B -q -pl campus-modules/campus-item -DskipTests compile` → exit 0；`npm run build:h5` → `DONE Build complete.`；dev server 编译产物 `pages/order/detail.vue` 含 `fillCommentFallback`/`我的评价`、`pages/order/list.vue` 含 `已评价`。**后端字段需重启 item 服务生效**（`bash scripts/stop-all.sh && bash scripts/start-all.sh`）。

## 14. 首页轮播点击跳转（2026-11-04）

用户："这个横幅你推荐加可以点击跳转功能吗" → 做成「零迁移、按前缀约定解析」的轻量方案。

| # | 问题 | 修复 |
|---|---|---|
| 38 | 演示数据 `link_url` 指向不存在的页面（`/pages/item/list`、`/pages/publish/index`、`/pages/item/list?tag=clearance`），点击后白屏或落 404 | 新增 `campus-mart/sql/07-banner-links.sql`（幂等 UPDATE）：1 → `/pages/category/category`、2 → `/pages/item/publish`、3 → `search:教材`；`sql/02-seed-business.sql` 初版数据同步修正 |
| 39 | `openBanner` 用 `uni.navigateTo` 跳 tabBar 页面（分类/发布），uni-app 会静默失败 | `pages/index/index.vue` 增加 `TAB_PAGES` 常量与 `goPage()`：tabBar 页面走 `uni.switchTab`，其余 `navigateTo`；分类页的 `categoryId` 先写 `pendingCategoryId` 缓存 |
| 40 | 只有 `/pages/` 一种写法，无法直达商品/分类/搜索；后台自由文本输入容易写错 | 约定四种前缀：`item:ID`、`category:ID`、`search:关键词`、`/pages/...`；后台 `views/system/banner/index.vue` 改为「跳转类型下拉 + 跳转目标输入 + 说明」，保存用 `buildLink()` 拼 `link_url`，编辑用 `parseLink()` 还原 |
| 41 | 后台轮播页仍用旧权限码 `system:banner:*`（菜单已归一到 `item:banner:*`），非 admin 账号看不到按钮 | 四处 `v-hasPerm` 改为 `item:banner:add|edit|remove` |

**验证**：`GET /api/item/app/banner/list` 返回三条新链接（`/pages/category/category`、`/pages/item/publish`、`search:教材`）；`npm run build:h5` 与 admin `npm run build` 均通过；dev server 编译产物 `pages/index/index.vue` 含 `TAB_PAGES`/`goPage`，admin `banner/index.vue` 含 `linkType`/`buildLink`/`item:banner:add`。

**演示效果**：首页点第 1 张 → 分类页；第 2 张 → 发布页；第 3 张 → 搜索页并自动搜「教材」。

## 15. 文件管理页：上传成功但点链接 404、刷新后列表清空（2026-11-12）

用户反馈：「我在系统管理的文件管理上传 pdf，然后刷新就 404，然后就没有上传的文件了」。

| # | 现象 | 根因 | 修复 |
|---|---|---|---|
| 42 | 上传成功的 pdf，点「地址」链接打开是前端 404 页面（`页面不存在，可能已被删除或没有访问权限`） | `sys_file.file_url` 存的是相对路径 `/upload/2026/11/12/xxx.pdf`，这是给网关/nginx 用的前缀；把相对地址直接放进 `<a href>` 后，浏览器请求的是**前端 dev server（5180）**，Vite 找不到文件就回退到 `index.html` → 渲染 404 页 | 新增 `campus-admin-web/src/utils/file.js`：`resolveFileUrl(url)` 把相对路径拼成后端绝对地址（开发态默认 `http://<host>:8081`，可用 `VITE_FILE_BASE` 覆盖；生产态留空走 nginx 同域代理）。页面上所有 `href`、复制链接、图片预览都过这个函数 |
| 43 | 刷新页面后上传记录全部消失，只剩空表 | 页面把上传结果只存在组件内的 `fileList` ref（原注释写着「契约未提供文件列表分页接口，此处仅展示当前会话内上传结果」），但后端其实早就有 `GET /system/file/list` 分页接口与 `DELETE /system/file/{ids}` | 重写 `views/system/file/index.vue`：改用 `useTable(getFilePage)` 拉真实分页（文件名 + 类型筛选、分页器、刷新）；上传成功后回到第一页重新查询；新增「删除」（`ElMessageBox` 二次确认，磁盘文件一并删除）；图片类型显示 40×40 缩略图并可点击放大；`createBy` 经 `GET /system/user/list` 翻译成昵称 |

**排查过程里的两个细节**：

1. 后端其实把文件存下来了：`sys_file` 有 6 条记录（file_id 1–6，均 `bizType=common`、`storage=local`），磁盘 `campus-mart/upload/2026/11/12/*.pdf` 也在；`GET http://127.0.0.1:8081/upload/2026/11/12/e9a….pdf` 返回 `200 application/pdf 237298`。所以「文件丢了」是错觉，丢的只是**前端表格**。
2. 静态资源缺失时后端返回的是 **HTTP 200 + JSON** `{"code":404,"msg":"资源不存在：xxx.png"}`（`campus-common` 的 `GlobalExceptionHandler.handleNoResource` 把 `NoResourceFoundException` 统一包成 Result）。浏览器会「看起来加载成功」，排查时容易被误导；功能上不影响本页（链接现在都指向真实存在的文件），保留现状以免影响其他接口的错误码语义。

**验证**（`node` 直接打网关 8081）：

- `GET /api/system/file/list?current=1&size=5` → 200，total=6，首条含 `originalName=江锦隆安全责任承诺书.pdf`、`fileUrl=/upload/2026/11/12/e9a….pdf`、`fileSize="237298"`、`createBy="1"`、`createTime=2026-11-12 09:36:09`
- `GET http://127.0.0.1:8081/upload/2026/11/12/e9a….pdf` → 200 `application/pdf`，237298 字节
- 上传 → 静态访问 → 删除 → 再访问：`POST /api/system/file/upload` 200 → 静态 200 → `DELETE /api/system/file/7` 200「删除成功」→ 磁盘文件消失
- 前端：`npm run build` ✓ 7.4s；dev server（5180）对 `/src/views/system/file/index.vue`、`/src/utils/file.js` 均返回 200 且无 Transform failed

**注意**：`fileId` / `createBy` 是 Long，后端序列化成字符串（`"6"`），所以前端 `uploaderName()` 与用户昵称映射表统一按 `String(id)` 取键。

## 16. 后台列表页：字段名/筛选参数与后端不一致（2026-11-12，用户实测）

用户看着操作日志页问：「操作日志的操作模块、操作类型和操作人为什么是空的，空的就不说了，为什么还要用空的来进行筛选」。

真相：不是没数据，是**页面读的字段名和后端返回的不一样**（表格列空），同时**筛选参数名后端也不认识**（点了查询啥都没变，看起来像"用空值在筛选"）。

| # | 现象 | 根因 | 修复 |
|---|---|---|---|
| 44 | 操作日志「操作模块 / 操作类型 / 操作人」三列全空 | 接口返回 `title / businessType / operName / costTime`，页面写的是 `module / operType / operator / costMs` | 重写 `views/system/operlog/index.vue`：列改为 `operId/title/businessType(tag)/operName/deptName/requestMethod/operUrl/operIp/status/costTime/operTime`，另加详情弹窗（请求参数、返回 JSON、错误信息） |
| 45 | 操作日志的模块/操作人筛选点了没反应 | 前端发 `module=`、`operator=`，后端 `SysOperLogController.list` 只认 `title/operName/status/businessType/beginTime/endTime` | query 改为 `{title, operName, businessType, status}`，并补「操作类型」下拉（1新增/2修改/3删除/4查询/5导出/9清空） |
| 46 | 登录日志「编号」列空 | 页面 `prop="loginId"`，后端返回 `infoId` | `views/system/loginlog/index.vue` 改为 `prop="infoId"` |
| 47 | 登录日志「登录账号」筛选无效 | 前端发 `username=`，控制器参数名是 `userName`（`SysLoginLogController.java:37`） | 前端 query key 改为 `userName`，状态下拉 `:value="0"` 改成 `value="0"`（字符串，与 `status char(1)` 一致） |
| 48 | 学院管理出现「邮箱」列/表单项，填了不保存 | `sys_dept` 表没有 `email` 列 | 删列、删表单项、删 `email: ''` 初始值 |
| 49 | 轮播管理出现「备注」列，永远空 | `biz_banner` 表没有 `remark` 列 | 删列、删备注 textarea |
| 50 | 商品分类出现「创建时间」列，永远空 | `CategoryTreeVO` 只有 `categoryId/parentId/categoryName/icon/orderNum/status/children` | 删列 |
| 51 | 举报管理的「举报原因」筛选无效 | `ReportQueryDTO` 没有 `reasonType` 字段（只有 itemId/reporterId/status/时间） | DTO 加 `reasonType`，`ReportServiceImpl.adminPage` 加等值条件 |
| 52 | AI 提示词模板的「模板名称 / 状态」筛选无效 | 接口只认 `scene/code/keyword`，页面发的是 `name/status` | `AiAdminController.promptPage` 增加 `name`（兼容 `keyword`）与 `status` 参数，`PromptTemplateService.page` 增加重载带 `status` 等值过滤 |

**新增两个体检脚本（黑盒，不需要读后端源码）**：

- `node tools/check-page-fields.mjs`：解析 21 个列表页的 `<el-table-column prop>`（无 prop 的表格退化为扫 `row.x`），真实调一次接口，比对「页面声明了但接口没返回」的字段 → 本轮查出 4 处（#46 #48 #49 #50），修复后 **checked=21 problems=0**。
- `node tools/check-query-params.mjs`：取出每个页面 query 对象里的筛选 key，先用无筛选请求记下 total，再带上 `key=不可能命中的值` 请求一次；total 变小=参数生效，total 不变=后端不认识这个参数名 → 本轮查出 3 处（登录日志 `username`、举报 `reasonType`、提示词 `name/status`），修复后 **checked=49 problems=0**（另有 4 个 WARN：`status=-999999` 这类非法枚举会被参数校验直接 400，属于「参数已绑定」，不计为问题）。

**验证**：

- 操作日志筛选（真实数据 178 条）：`title=文件` → 16；`operName=admin` → 168；`businessType=3` → 23；`status=0` → 145；`title=商品&businessType=2` → 5
- 举报原因（用临时端口 9302 起新编译的 item 服务验证，未打扰正在运行的旧进程）：全部 15 → `reasonType=fake` 3 / `price` 3 / `other` 4
- 提示词模板（临时端口 9304 起新编译的 ai 服务）：全部 4 → `status=1` 0、`status=0` 4、`name=文案` 1、`keyword=文案` 1
- `npm run build` ✓ 7.5s；dev server 对 operlog/loginlog/dept/banner 四个页面均 200 且无 Transform failed

**注意**：`item` 与 `ai` 两个服务改了 Java 代码，需要重启才生效（`bash scripts/stop-all.sh && bash scripts/start-all.sh`）；只改了前端页面的部分（操作日志、登录日志、学院、轮播、分类）刷新浏览器即可。

### 16.1 分类管理：展开/折叠无反应、图标列显示破图（2026-11-12，用户实测）

| # | 现象 | 根因 | 修复 |
|---|---|---|---|
| 53 | 点「展开/折叠」按钮，表格没有任何变化 | `el-table` 的 `default-expand-all` **只在组件初始化时读一次**，运行中把 `expandAll` 从 true 改成 false 不会让已展开的行收起来（Element Plus 需要主动 `toggleRowExpansion`） | 给表格加 `:key="\`tree-${expandAll}\`"`，值一变就按新的 `default-expand-all` 重建一次（树只有两级、40 个节点，重建没有可感知开销）；`toggleExpand()` 只负责翻转 `expandAll` |
| 54 | 「图标」列一级分类显示破图，二级分类显示 `-` | `biz_category.icon` 存的是**图标名**（`phone / education / shopping / run / star / home / music / more`，供用户端按名字渲染），后台页面却当成图片地址塞进 `<el-image :src>` | 图标列分两种渲染：`http(s)://` 或 `/` 开头才当图片（经 `resolveFileUrl` 拼绝对地址），否则用 `el-tag` 显示图标名；表单标签由「图标 URL」改为「图标」，占位提示「图标名（phone / education）或图片地址」 |

**验证**：`GET /api/item/category/list` → 40 条，一级 8 个的 `icon` 依次为 `phone/education/shopping/run/star/home/music/more`（确认为图标名而非地址）；`npm run build` ✓ 7.8s；dev server（5180）对 `src/views/item/category/index.vue` 返回 200 且无 Transform failed。

### 16.2 评价管理：筛选条件改成「商品 ID / 订单 / 状态」（2026-11-12，用户实测）

用户反馈：「这个筛选条件不合理吧，应该是商品 id 筛选和订单 4119 这个和状态筛选」。原来的搜索区是「评价内容关键字 + 商品 ID + 状态」，而实际排查评价都是按**哪笔订单的哪件商品**去定位，内容关键字基本用不上；状态下拉还错用了 `sys_normal_disable`（正常/停用），与 `biz_comment.status`（0 正常 / 1 已隐藏 / 2 审核未通过）语义对不上。

| # | 改动 | 说明 |
|---|---|---|
| 55 | 搜索区改为「商品 ID + 订单 ID + 状态」 | 商品 ID 与订单 ID 都支持回车搜索；订单 ID 占位符直接写「如 4119」，对应列表里「订单 4119」那一栏 |
| 56 | 状态下拉换成专用字典 `item_comment_status` | `sql/06-dict-fix.sql` 新增第 4 段（0 正常 / 1 已隐藏 / 2 审核未通过），执行后需 `redis-cli DEL dict:item_comment_status`（字典空结果也会缓存 1 小时）；列表状态列同步支持三种状态与对应颜色 |
| 57 | 后端 `CommentQueryDTO` 增加 `orderId` | 原来只能按 itemId / fromUserId / status / keyword 过滤，没法按订单定位 |
| 58 | `CommentVO` 增加 `orderNo`，列表展示订单号 | 后台分页时批量 `selectBatchIds` 取订单号（一次查询，不做 N+1），列表「订单 4119（CM202611031719211309）」 |
| 59 | 评价关系列排版 | 原来「评价人 X / 被评价人 Y ｜ 订单 Z」两行挤在一起，改为「评价人 X → 被评价人 Y」+ 第二行订单 ID 与订单号 |

**验证**：在临时端口 9302 起新编译的 item 服务（不影响正在运行的旧进程）：`/item/comment/page` 全部 77 条 → `orderId=4119` 1 条（`orderNo=CM202611031719211309`）、`itemId=3161` 1 条、`status=1` 0 条、`orderId=999999` 0 条；`GET /api/system/dict/data/type/item_comment_status` → 3 条（正常/已隐藏/审核未通过）；`npm run build` ✓ 7.8s。

**注意**：`orderId` 与 `orderNo` 是后端改动，需要重启 item 服务生效；字典已直接写库并清了缓存，刷新页面就能看到状态下拉。

## 17. 内容审核「辱骂词也返回 pass=true」排查（2026-11-12，用户实测）

用户问「这个内容审核真会进行审核吗，我进行辱骂的词语和简写也返回 true 吗」，并贴出「AI 配置 → 在线试用 → 内容审核」输入 `sb` 返回 `pass: true, provider: remote, costTime: 830` 的截图。

**结论**：审核链路本身是两级（规则引擎 + 大模型复核），直接调接口时辱骂词是能拦住的；页面上之所以放行，是**字段名传错**导致后端审核了一段空文本。

| # | 问题 | 根因 | 修复 |
|---|---|---|---|
| 60 | 后台「内容审核」试用对任何内容都返回 `pass=true` | `views/ai/config/index.vue` 调 `aiModerate({ content: demo.text })`，而接口只认 `title`/`description` → 两个字段都是空串，规则引擎对着空文本判定通过，大模型也收到空的标题与描述，回 `pass=true`（截图里 `costTime=830` 正是那次空调用） | 前端改传 `{ title:'', description: demo.text, content: demo.text, scene:'item' }`；后端 `ModerationRequest` 增加 `content` 字段，`ModerationService` 在 `title`/`description` 都为空时回退到 `content`，避免再次出现「审空文本」 |
| 61 | 辱骂词只有大模型能识别，模型被限流/不可用时全部放行 | 规则库只有涉黄/赌博/毒品/违禁品/诈骗/代写代考/站外引流七类，没有辱骂类；降级到 `mock` 时 `mock` 恒返回通过 | `SensitiveWordFilter` 新增「辱骂与人身攻击」高危词（傻逼/傻b/煞笔/草泥马/操你妈/你妈死了/去你妈的/贱人/智障…）与缩写正则 `ABUSE_ABBR`（`sb|cnm|cnmb|nmsl|tmd|nmb|wcnm|fuck|shit|bitch|asshole`），命中即 `pass=false, riskLevel=2` |
| 62 | 缩写子串误伤 | `sb` 若按子串匹配，`usb`、`5sb` 会被判违规 | `ABUSE_ABBR` 用 `(?<![a-z0-9])…(?![a-z0-9])` 限定为独立词；归一化仍会去掉空格与点线，`加 微 信`、`s.b` 这类绕过依旧能命中 |

**验证**（把源码复制到 `_env/fixbuild4` 编译，起两个临时实例：9304 关掉 LLM 复核只看规则、9305 规则 + 模型）：

```
端口 9304（仅规则引擎）
  sb / 傻逼 / cnm / nmsl / 草泥马 / 你妈死了 → pass=false risk=2 provider=rule
  usb 数据线 / 5sb 电池 / 宿舍垃圾袋          → pass=true  （无子串误伤）
  加微信 abc12345 详聊                      → pass=false risk=1 words=["加微信","微信 ***"]

端口 9305（规则 + LLM 复核；临时实例无 API Key，remote 失败自动降级 mock，正好验证降级链）
  同上，辱骂词被规则先拦（provider=rule），正常商品走 mock 兜底 pass=true
```

正在运行的生产 9204 实例（改动前代码 + 真实 DeepSeek Key）实测同样能拦住：`sb`→risk=1「标题包含辱骂性词汇」1882ms、`傻逼`→760ms、`cnmb`→「辱骂性脏话缩写（cnmb）」1007ms、`nmsl`→1210ms，正常商品 681ms 通过。

**注意**：`ModerationRequest.content` 与规则库属于后端改动，需要重启 ai 服务生效（`bash scripts/stop-all.sh && bash scripts/start-all.sh`）；前端「在线试用」的字段修正刷新页面即可。临时实例 9304/9305 由沙箱启动、`taskkill` 报「拒绝访问」无法回收，重启电脑后自然释放（对应 `_env/fixbuild4` 目录也需重启后删除）。
