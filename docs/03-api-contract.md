# CampusMart 接口契约 v1.1

> 本文件是**前后端唯一契约**。后端按此实现，前端按此调用。
> v1.1 相对 v1.0 的关键变化见文末「变更记录」，**字段命名一律以数据库列名为准（下划线转驼峰，不做二次改名）**。

## 1. 通用约定

### 1.1 服务与端口

| 服务 | 端口 | 网关前缀 | 数据库表前缀 | 说明 |
|---|---|---|---|---|
| campus-gateway | 8081 | - | - | 唯一入口，前端只访问网关 |
| campus-auth | 9200 | `/api/auth` | sys_* | 登录、验证码、令牌、动态菜单 |
| campus-system | 9201 | `/api/system` | sys_* | RBAC、字典、参数、日志、文件 |
| campus-item | 9202 | `/api/item` | biz_* | 商品、分类、收藏、评论、订单、举报、消息、公告、轮播 |
| campus-analysis | 9203 | `/api/analysis` | stat_* | 统计分析、数据大屏 |
| campus-ai | 9204 | `/api/ai` | ai_* | AI 能力（文案/客服/审核/推荐/估价） |

前端 BaseURL：
- 管理后台（Vue3）：`http://localhost:8081/api`（vite proxy 转发）
- uniapp H5：`http://localhost:8081/api`；小程序：`http://127.0.0.1:8081/api`（真机需改成电脑局域网 IP，如 `http://192.168.x.x:8081/api`）

### 1.2 统一响应体 `Result<T>`

```json
{ "code": 200, "msg": "操作成功", "data": {}, "timestamp": 1793000000000 }
```

- `code`：200 成功；其余见 1.6
- `data`：业务数据，可为 null / 对象 / 数组
- 前端判断：`res.code === 200`（axios 拦截器里统一解包 `data`）

### 1.3 分页 `PageResult<T>`

```json
{ "code": 200, "msg": "操作成功",
  "data": { "records": [], "total": 37, "current": 1, "size": 10, "pages": 4 } }
```

分页请求统一 Query：`current`（默认 1）、`size`（默认 10，最大 100），其余为业务筛选条件。

### 1.4 鉴权

- `POST /api/auth/login`（后台，**需要图形验证码**）
- `POST /api/auth/app/login`（小程序/App，**不需要验证码**）
- 登录成功返回 `token`（JWT HS256，默认 2h）。服务端同时在 Redis 存 `login:token:{userId}`，**改权限/退出/停用账号立即生效**。
- 后续请求：`Authorization: Bearer <token>`（uniapp 也可用 `?token=xxx`）
- 网关校验后向下游透传：`X-User-Id`、`X-Username`、`X-Nickname`(URL 编码)、`X-Dept-Id`、`X-Dept-Name`(URL 编码)、`X-Roles`、`X-Perms`、`X-Real-IP`
- 白名单（无需令牌）：`/api/auth/login`、`/api/auth/app/login`、`/api/auth/captcha`、`/api/upload/**`，以及 `/api/item/app/item|category|banner|notice|comment|home` 下的 **GET** 只读接口
- 白名单的**例外**（即使命中上面的只读前缀也必须带令牌）：路径以 `/my`、`/unread-count`、`/read`、`/read-all`、`/mine` 结尾——例如 `GET /api/item/app/item/my`（我的商品）、`GET /api/item/app/message/unread-count` 都不能匿名访问

### 1.5 权限标识

格式 `模块:资源:动作`，如 `system:user:list`、`item:item:audit`、`analysis:screen`。
- 后端：`@RequiresPerm("system:user:add")`，AOP 校验，**支持通配**（`*:*:*`、`item:order:*`）
- 前端：登录后 `GET /api/auth/me` 拿 `perms` 数组，用 `v-hasPerm="['system:user:add']"` 控制按钮；`perms` 含 `*:*:*` 即超级管理员
- 演示账号：`admin/admin123`（全部权限）、`operator/admin123`（运营）、`auditor/admin123`（审核）、`student01/admin123`（学生，仅前台）

### 1.6 错误码

| code | 含义 | 前端处理 |
|---|---|---|
| 200 | 成功 | - |
| 400 | 参数/业务校验失败 | 直接 toast `msg` |
| 401 | 未登录/令牌失效 | 清 token → 跳登录页 |
| 403 | 无权限 | toast `msg` |
| 404 | 资源不存在 | toast |
| 409 | 业务冲突（重复下单/用户名已存在） | toast |
| 429 | 请求过于频繁（AI 限流） | toast「请求太频繁，稍后再试」 |
| 500 | 服务器内部错误 | toast「系统繁忙」 |
| 1001 | AI 服务不可用（已降级/兜底） | 提示已使用模板兜底 |

### 1.7 数据约定（**重要，v1.1 变更**）

| 约定 | 值 |
|---|---|
| 时间 | `yyyy-MM-dd HH:mm:ss` 字符串 |
| 金额 | `decimal(10,2)`，单位元，前端格式化为 `¥1,234.00` |
| ID | `Long`，**后端已序列化为字符串**，前端一律当 string 处理 |
| 逻辑删除 | 字段 `delFlag`，`'0'` 正常 / `'2'` 删除（后端自动过滤，前端不用管） |
| 状态字段 | `status` 是 **`char(1)`**：`'0'` 正常/启用，`'1'` 停用（**不是数字 0/1**） |
| 菜单类型 | `menuType`：`'M'` 目录 / `'C'` 菜单 / `'F'` 按钮 |
| 昵称字段 | **`nickname`**（全小写，v1.0 的 `nickName` 作废） |
| 用户类型 | `userType`：`'00'` 后台用户 / `'01'` 学生用户 |

### 1.8 业务枚举（与数据库字典 `sys_dict_data` 一致，前端应通过 `/api/system/dict/data/type/{dictType}` 拉取）

`item_status`（`biz_item.status`，tinyint）：0 草稿 / 1 待审核 / 2 在售 / 3 已预订 / 4 已售出 / 5 已下架 / 6 违规
`order_status`（`biz_order.status`，tinyint）：0 待确认 / 1 待交付 / 2 已完成 / 3 已取消 / 4 退款中 / 5 已退款
`item_condition`：9 成新 9、8 成新 8 ……（1-10，**演示数据集中在 7-10**）
`trade_type`：0 校内自提 / 1 快递 / 2 均可
`msg_type`：1 系统通知 / 2 订单消息 / 3 审核结果 / 4 AI 消息
`report_reason`：fake 虚假信息 / illegal 违禁物品 / price 价格欺诈 / harass 骚扰谩骂 / other 其他

---

## 2. 认证 `/api/auth`（campus-auth，已实现）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/captcha` | 匿名 | 图形验证码 |
| POST | `/login` | 匿名 | 后台登录（校验验证码） |
| POST | `/app/login` | 匿名 | 小程序/App 登录（不校验验证码） |
| POST | `/logout` | 登录 | 退出（删 Redis 登录态） |
| GET | `/me` | 登录 | 当前用户信息 |
| GET | `/menus` | 登录 | 动态菜单树（按角色过滤） |

**GET /captcha** → `data: { "key": "uuid", "image": "data:image/png;base64,..." }`

**POST /login**（`/app/login` 请求体相同，可省略 `captchaKey`/`captchaCode`）
```json
// req
{ "username": "admin", "password": "admin123", "captchaKey": "uuid", "captchaCode": "8x2f" }
// resp.data
{ "token": "eyJhbGciOi...", "expiresIn": 7200,
  "userInfo": {
    "userId": "1", "username": "admin", "nickname": "超级管理员",
    "deptId": "101", "deptName": "校园易站运营部", "avatar": null,
    "roles": ["admin"], "perms": ["*:*:*"],
    "token": "eyJ...", "expireAt": 1793000000000
  } }
```

**GET /me** → `data` 同 `userInfo`

**GET /menus** → `data: [菜单节点]`
```json
{ "menuId":"1", "parentId":"0", "menuName":"系统管理", "path":"/system", "component":null,
  "queryParam":null, "menuType":"M", "perms":null, "icon":"Setting", "orderNum":1,
  "visible":"0", "isFrame":"1", "isCache":"0", "children":[
    { "menuId":"100", "parentId":"1", "menuName":"用户管理", "path":"user",
      "component":"system/user/index", "menuType":"C", "perms":"system:user:list",
      "icon":"User", "orderNum":1, "children":[] } ] }
```
> 后台路由由 `path` + `component` 拼装：目录 `path=/system` 对应布局，菜单 `component=system/user/index` 对应 `views/system/user/index.vue`。

---

## 3. 系统管理 `/api/system`（campus-system）

### 3.1 用户 `/system/user`
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/list` | system:user:list | 分页：username / nickname / phone / status / deptId |
| GET | `/{userId}` | system:user:query | 详情（含 `roleIds`: ["2"]，不含 password） |
| POST | `/` | system:user:add | 新增（密码缺省 123456） |
| PUT | `/` | system:user:edit | 修改 |
| DELETE | `/{userIds}` | system:user:remove | 批量，逗号分隔 |
| PUT | `/resetPwd` | system:user:resetPwd | `{ "userId":"11", "password":"123456" }` |
| PUT | `/changeStatus` | system:user:edit | `{ "userId":"11", "status":"0" }` |
| GET | `/deptTree` | system:user:list | 部门下拉树 |

用户对象字段：`userId, deptId, username, nickname, userType, phone, email, avatar, gender, studentNo, creditScore, status, loginIp, loginDate, remark, roleIds`

### 3.2 角色 `/system/role`
`GET /list`（分页 roleName/roleKey/status）、`GET /{roleId}`、`POST /`、`PUT /`、`DELETE /{roleIds}`、`PUT /changeStatus`、`GET /optionselect`、`GET /{roleId}/menuIds`（已授权菜单 id 数组）、`PUT /menu`（`{ "roleId":"2", "menuIds":["2","201","501"] }`）
权限：`system:role:list|query|add|edit|remove`

### 3.3 菜单 `/system/menu`
`GET /list`（扁平，前端自建树）、`GET /{menuId}`、`POST /`、`PUT /`、`DELETE /{menuId}`、`GET /treeselect`、`GET /roleMenuTreeselect/{roleId}`（`{ menus:[树], checkedKeys:[id] }`）、`GET /routers`（当前用户菜单树）
权限：`system:menu:list|query|add|edit|remove`

### 3.4 部门 `/system/dept`
`GET /list`、`GET /{deptId}`、`POST /`、`PUT /`、`DELETE /{deptId}`、`GET /treeselect`
权限：`system:dept:list|query|add|edit|remove`

### 3.5 字典
- 类型 `/system/dict/type/list`、`/{dictId}`、`POST /`、`PUT /`、`DELETE /{dictIds}`、`/optionselect`
- 数据 `/system/dict/data/list?dictType=item_status`（分页）、`/{dictCode}`、`POST /`、`PUT /`、`DELETE /{dictCodes}`
- **前台下拉专用**：`GET /system/dict/data/type/{dictType}` → `data: [{ "label":"在售", "value":"2", "cssClass":null, "isDefault":"N" }]`（Redis 缓存 1h）
权限：`system:dict:list|query|add|edit|remove`

### 3.6 参数 `/system/config`
`GET /list`、`GET /{configId}`、`POST /`、`PUT /`、`DELETE /{configIds}`、`GET /configKey/{configKey}`
权限：`system:config:list|query|add|edit|remove`

### 3.7 日志
- 操作日志：`GET /system/operlog/list`（分页，title/operName/status/businessType/beginTime/endTime）、`DELETE /system/operlog/{operIds}`、`DELETE /system/operlog/clean`
- 登录日志：`GET /system/loginlog/list`（分页，userName/ipaddr/status/beginTime/endTime）、`DELETE /system/loginlog/{infoIds}`、`DELETE /system/loginlog/clean`
权限：`system:operlog:list|remove`、`system:loginlog:list|remove`

### 3.8 文件 `/system/file`
`POST /upload`（multipart，字段名 `file`；白名单 jpg/jpeg/png/gif/webp/pdf/zip，≤10MB）→ `data: { "fileName":"uuid.png", "url":"/upload/2026/10/27/uuid.png", "size":12345 }`
`GET /list`（分页）、`DELETE /{fileIds}`
> 图片访问：网关把 `/upload/**` 转发到 item 服务静态映射；演示图片在 `campus-mart/upload/demo/`，URL 形如 `/upload/demo/item-01.png`。

---

## 4. 商品 `/api/item`（campus-item）

### 4.1 前台商品 `/item/app/item`（匿名可 GET）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/page` | 在售分页：keyword / categoryId / minPrice / maxPrice / itemCondition / sort=new\|priceAsc\|priceDesc\|hot / current / size |
| GET | `/{itemId}` | 详情（分类名、卖家昵称/信用分、`favorited`；公开可见时 `view_count+1`） |
| GET | `/recommend?limit=8` | 推荐（AI 服务可用时走个性化，否则热度兜底） |
| GET | `/my` | 我发布的（需登录，参数同后台分页） |
| POST | `/` | 发布（需登录，AI 内容审核） |
| PUT | `/` | 编辑（仅本人，status ∈ 1/2/5；标题/描述变更需重审） |
| POST | `/{itemId}/off-shelf` | 下架（仅本人） |

**POST /item/app/item 请求体**（`ItemPublishDTO`）
```json
{ "title":"iPhone 12 128G 国行", "summary":"95新无拆修", "description":"2023年购入……",
  "categoryId":"101", "price":2400.00, "originalPrice":4999.00, "itemCondition":9,
  "tradeType":2, "coverUrl":"/upload/demo/item-01.png",
  "images":["/upload/demo/item-01.png"], "tags":["国行","无拆修"],
  "location":"东区宿舍", "contactType":0 }
```
**列表项 `ItemListVO`**：`itemId, itemNo, title, summary, price, originalPrice, coverUrl, itemCondition, tradeType, status, viewCount, favoriteCount, location, publishTime, sellerId, sellerNickname, sellerCreditScore, categoryId, categoryName, tags[]`
**详情 `ItemDetailVO`**：列表项字段 + `description, images[], favorited(boolean), sellerAvatar, aiGenerated`

### 4.2 后台商品 `/item/item`
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/page` | item:item:list | status/keyword/categoryId/sellerId |
| POST | `/audit` | item:item:audit | `{ "itemId":"1001", "pass":true, "auditRemark":"" }`；通过→status 2 并写 publish_time，驳回→status 5 |
| PUT | `/status` | item:item:edit | `{ "itemId":"1001", "status":5 }` |
| DELETE | `/{itemId}` | item:item:remove | 先置 6 再逻辑删除 |

### 4.3 分类
- 前台：`GET /item/app/category/tree`（两级树，Redis 缓存）
- 后台：`GET /item/category/list?tree=false`、`POST /item/category`、`PUT /item/category`、`PUT /item/category/status`、`DELETE /item/category/{categoryId}`
- 权限：`item:category:list|add|edit|remove`
- 节点：`{ categoryId, parentId, categoryName, icon, orderNum, status, children[] }`

### 4.4 收藏 `/item/app/favorite`（需登录）
`POST /{itemId}`（收藏，幂等）、`DELETE /{itemId}`（取消）、`GET /my`（我的收藏分页）、`GET /check/{itemId}`（`data: true/false`）

### 4.5 评论
- 前台：`GET /item/app/comment/list/{itemId}`（分页，status='0'）、`POST /item/app/comment`（发表评价，**orderId 必填**）
```json
// POST /item/app/comment
{ "orderId":"2001", "itemId":"1001", "score":5, "content":"成色和描述一致，交易很快", "images":[] }
```
- 后台：`GET /item/comment/page`、`PUT /item/comment/audit`（`{ "commentId":"3001", "status":"0" }` 0 通过 / 1 隐藏）、`DELETE /item/comment/{commentId}`
- 权限：`item:comment:list|audit|reply|remove`
- 评论项：`{ commentId, orderId, itemId, fromUserId, fromNickname, fromAvatar, toUserId, toNickname, score, content, images[], replyContent, status, createTime }`

### 4.6 订单 `/item/app/order`（需登录，前台）+ `/item/order`（后台）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/item/app/order` | 下单 `{ "itemId":"1038", "tradePlace":"东区宿舍楼下", "buyerRemark":"周末交易" }` → 返回 orderId/orderNo |
| POST | `/item/app/order/{orderId}/pay` | 付款（0→1，写 pay_time/pay_type=1） |
| POST | `/item/app/order/{orderId}/deliver` | 卖家确认交付（0/1→1） |
| POST | `/item/app/order/{orderId}/confirm` | 买家确认收货（1→2，商品 3→4，卖家信用+1） |
| POST | `/item/app/order/{orderId}/cancel` | 取消（0/1→3，商品 3→2 恢复在售） |
| GET | `/item/app/order/my?role=buy&status=2` | 我的订单（role=buy 我买到的 / sell 我卖出的） |
| GET | `/item/app/order/{orderId}` | 订单详情（含商品简要、买卖双方昵称） |
| GET | `/item/order/page` | 后台分页（orderNo/status/buyerId/sellerId/时间范围） |
| PUT | `/item/order/status` | 后台改状态（严格校验流转） |
| PUT | `/item/order/refund` | 退款（2→4→5） |
权限：`item:order:list|confirm|deliver|finish|cancel|refund|export`
订单对象：`{ orderId, orderNo, itemId, itemTitle, itemCoverUrl, buyerId, buyerNickname, sellerId, sellerNickname, amount, status, payType, tradePlace, buyerRemark, cancelReason, createTime, payTime, finishTime, cancelTime, commented, commentId, commentScore, commentContent, commentTime, commentReply }`
> 评价字段（`commented` 起）由 `OrderServiceImpl.toVOList` 按 orderId 批量回填 `biz_comment`，订单列表与详情共用；已评价后前端按钮显示「已评价」，可查看评价内容与卖家回复。
**下单校验**：商品 status 必须 2；同一商品存在 status ∈ (0,1,2) 的订单 → 409「该商品已被预订或已售出」；不能买自己的商品。

### 4.7 举报 `/item/app/report` + `/item/report`
`POST /item/app/report`：`{ "targetType":"item", "targetId":"1058", "reason":"illegal", "description":"疑似盗版" }`（仅支持 item）
后台：`GET /item/report/page`、`PUT /item/report/handle`（`{ "reportId":"5001", "status":1, "handleRemark":"已核实下架" }`，1 成立 / 2 不成立）
权限：`item:report:list|handle`

### 4.8 消息 `/item/app/message`
`GET /my`（分页，可 `msgType` 过滤）、`GET /unread-count`、`PUT /read/{messageId}`、`PUT /read-all`
权限（后台）：`item:message:list`（`GET /item/message/page`）

### 4.9 公告/轮播
- 前台：`GET /item/app/notice/list`（分页）、`GET /item/app/notice/{noticeId}`、`GET /item/app/banner/list`
- 后台：`GET /item/notice/page`、`GET /item/notice/{noticeId}`、`POST /item/notice`、`PUT /item/notice`、`DELETE /item/notice/{noticeId}`；`GET /item/banner/list`、`POST /item/banner`、`PUT /item/banner`、`DELETE /item/banner/{bannerId}`
- 权限：`item:notice:list|add|edit|remove`、`item:banner:list|add|edit|remove`（**v1.1 起不再是 `system:notice:*`**）
- `biz_banner.link_url` 跳转约定（后台「跳转类型」下拉生成，用户端 `openBanner()` 解析）：
  - `item:1234` → 商品详情 `/pages/item/detail?itemId=1234`
  - `category:2` → 分类页并写入 `pendingCategoryId=2`（一级分类）
  - `search:教材` → 搜索页 `/pages/search/search?keyword=教材`
  - `/pages/xxx/yyy?k=v` → 站内页面；tabBar 页面（首页/分类/发布/消息/我的）自动改用 `uni.switchTab`
  - 空 → 不跳转（点击只弹标题 toast）

---

## 5. AI `/api/ai`（campus-ai）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/ai/copywrite` | 一键发布文案 → `{ title, summary, description, tags[], suggestPrice, provider, costMs }` |
| POST | `/ai/chat` | 智能客服 RAG → `{ answer, sources:[{title, score}], provider, costMs }`（`{ "question":"怎么发布商品？" }`） |
| POST | `/ai/moderate` | 内容审核 → `{ pass, riskLevel, words[], reason, provider, costTime }` |
| POST | `/ai/price` | 智能估价 → `{ suggestPrice, minPrice, maxPrice, reason, provider }` |
| GET | `/ai/recommend?limit=8` | 首页个性化推荐（登录后自动带 userId，未登录走热度） |
| GET | `/ai/scene/list` | 已配置的 Prompt 场景列表 |
| GET | `/ai/log/page` | 后台：AI 调用日志分页（scene/status/时间范围） |
| GET | `/ai/stat/overview` | 后台：AI 消耗统计 `{ totalCalls, successCalls, avgCostMs, totalTokens, byScene:[{scene,count}], byDay:[{date,count}] }` |
| GET/POST/PUT/DELETE | `/ai/prompt/...` | Prompt 模板管理（`/ai/prompt/page`、`/ai/prompt`、`/ai/prompt/{id}`） |
| GET/POST/PUT/DELETE | `/ai/knowledge/...` | 知识库管理（`/ai/knowledge/page`、`/ai/knowledge`、`/ai/knowledge/{id}`） |

权限：`ai:log:list`、`ai:prompt:list|add|edit`、`ai:knowledge:list|add|edit|remove`
scene 取值：`copywrite`（文案）/ `chat`（客服 RAG）/ `audit`（审核）/ `price`（估价）/ `recommend` / `summary`
provider 取值：`remote`（远程大模型）/ `local`（本地 Ollama）/ `mock`（离线模板兜底）/ `rule`（规则命中，未进模型）
> 降级策略：远程超时（连接 3s / 读 30s）→ 本地 → 模板/mock 兜底，**接口永远返回 200 + 可用的兜底结果**，同时 `ai_call_log.status` 记为 2（降级）。

---

## 6. 数据分析 `/api/analysis`（campus-analysis）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/overview` | analysis:overview | 概览卡片：`{ todayGmv, totalGmv, orderCount, finishCount, cancelCount, itemCount, onSaleCount, userCount, aiCallCount, gmvGrowth, orderGrowth }` |
| GET | `/trend?days=30` | analysis:overview | 趋势：`[{ statDate, gmv, orderCount, finishOrderCount, itemPublishCount, newUserCount, activeUserCount }]` |
| GET | `/category` | analysis:trade | 分类成交：`[{ categoryId, categoryName, itemCount, orderCount, gmv }]` |
| GET | `/hot-item?limit=10` | analysis:trade | 热门商品（浏览量/收藏/成交） |
| GET | `/seller-rank?limit=10` | analysis:trade | 卖家成交排行 |
| GET | `/funnel?days=30` | analysis:funnel | 转化漏斗：`{ viewCount, favoriteCount, orderCount, finishCount, viewToOrderRate, orderToFinishRate }` |
| GET | `/rfm` | analysis:user | RFM 用户分层：`[{ segment, userCount, avgMonetary, userRatio }]`（segments：重要价值/重要保持/重要发展/重要挽留/一般价值/流失） |
| GET | `/retention?days=7` | analysis:user | 留存：`[{ date, activeUser, newUser, retentionRate }]` |
| GET | `/ai-usage` | analysis:trade | AI 消耗：`{ totalCalls, totalTokens, byScene:[...], byDay:[...], successRate }` |
| GET | `/screen` | analysis:screen | 大屏聚合（一次返回：概览+趋势+分类+漏斗+热词+实时订单） |
| POST | `/refresh` | analysis:refresh | 立即重算 T+1 预聚合表（`stat_overview_daily` / `stat_category_daily` / `stat_funnel_daily` / `stat_user_rfm`） |
| GET | `/export/order?startDate=2026-09-27&endDate=2026-10-27` | analysis:export | 订单明细 CSV 导出（`text/csv;charset=UTF-8` + BOM，Excel 打开不乱码） |
> 定时任务：每天 02:00 自动预聚合（`campus.analysis.stat-cron`）。

---

## 7. 变更记录

### v1.1（2026-10-27）
1. **字段命名统一为数据库列名驼峰**：`nickName → nickname`、菜单 `id/name/type/hidden → menuId/menuName/menuType/visible`、`count → total`。
2. **状态/逻辑删除类型变更**：`deleted(0/1 数字) → delFlag('0'/'2' 字符串)`；`status` 统一 `char(1)`（`'0'` 正常 / `'1'` 停用）；`menuType` 改为 `'M'/'C'/'F'`。
3. 认证新增 `POST /auth/app/login`（小程序/App 免验证码登录）。
4. 商品/订单/收藏/评论路径按**任务书**调整：`/item/item`、`POST /item/audit`、`POST|DELETE /item/app/favorite/{itemId}`、`POST /item/app/item/{itemId}/off-shelf`、订单 `/pay`、`/deliver`、`/confirm`、`/cancel`、`/my`。
5. 评论 `orderId` **必填**（先成交后评价，`biz_comment.order_id` 唯一）。
6. `order_status` 口径确定为 0 待确认 / 1 待交付 / 2 已完成 / 3 已取消 / 4 退款中 / 5 已退款（**成交 = status 2**）。
7. 商品审核驳回置 `status=5`（已下架），AI 审核不通过置 `status=6`（违规）。
8. 权限码归一化为 `模块:资源:动作`，并支持通配；`item` 模块新增 `item:item:*`、`item:category:*`、`item:comment:*`、`item:order:*`、`item:report:*`。
9. 新增 `sys_dict_data.msg_type` 字典（1/2/3/4）。
10. 演示图片：`/upload/demo/item-01.png` ~ `item-12.png`、`banner-01~03.png`（本地 12 张占位图 + 3 张横图，离线可用）。

### v1.2（2026-10-27 · 前端联调回归后）

联调方式：`tools/` 下的脚本解析前端 `src/api/*.js` 声明的全部接口（64 个 GET），登录后逐个真实探测，并校验 `dist` 静态资源。结果 **63/64 通过**（未通过的 1 条是正确业务规则：访问他人订单返回 403），据此修正 5 处契约/实现不一致：

1. **`/api/item/app/message/my` 404** → `AppMessageController` 改为 `@GetMapping({"/list","/my"})` 双路径。
2. **`/api/item/app/item/my`、`/api/item/app/message/unread-count` 被当成匿名只读接口** → 网关 `AuthGlobalFilter` 增加 `LOGIN_REQUIRED_SUFFIXES = ["/my","/unread-count","/read","/read-all","/mine"]`，命中者即使匹配只读白名单也必须带令牌。
3. **`/api/item/notice/{id}` 405** → 后台补 `GET /item/notice/{noticeId}`（`item:notice:list`）。
4. **`/api/item/banner/list` 405** → 后台补 `GET /item/banner/list`（`sort` 升序）。
5. **`/api/analysis/export` 404** → 导出实际路径为 `GET /analysis/export/order?startDate=&endDate=`（CSV）；管理后台 `src/api/analysis.js` 已按时间区间调用并 `responseType: 'blob'`。

另：补 `campus-admin-web/public/favicon.svg`（`index.html` 有引用但文件缺失，会 404）。
