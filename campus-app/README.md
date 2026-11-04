# 校园易站 CampusMart · 前台用户端（uniapp + Vue3 + Vite）

校园二手交易平台的移动端前台，一套代码同时输出 **H5（开发 / 演示）** 与 **微信小程序** 产物。
接口严格遵循 `campus-mart/docs/03-api-contract.md`（契约 v1.1），直连网关 `http://localhost:8081`，**不含任何 mock 数据**；后端未启动或接口未就绪时统一走空态 + toast 兜底。

## 一、技术栈与版本

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| vue | 3.4.21 | 组合式 API + `<script setup>` |
| @dcloudio/uni-app | 3.0.0-5030320261023001 | 所有 `@dcloudio/*` 包**必须同版本** |
| @dcloudio/uni-h5 | 3.0.0-5030320261023001 | H5 运行时 |
| @dcloudio/uni-mp-weixin | 3.0.0-5030320261023001 | 微信小程序运行时 |
| @dcloudio/uni-components | 3.0.0-5030320261023001 | 内置组件库 |
| @dcloudio/vite-plugin-uni | 3.0.0-5030320261023001 | uni 的 Vite 插件 |
| vite | 5.2.8 | 受 vite-plugin-uni 的 peerDependencies 精确约束 |
| sass | 1.77.8 | `uni.scss` 与页面 scss |
| pinia | 2.1.7 | 登录态与用户信息 |

## 二、启动与构建

```bash
cd campus-mart/campus-app
npm install                 # 安装依赖
npm run dev:h5              # H5 开发服务：http://localhost:5181
npm run build:h5            # H5 生产构建：dist/build/h5
npm run dev:mp-weixin       # 微信小程序开发产物：dist/dev/mp-weixin
npm run build:mp-weixin     # 微信小程序生产产物：dist/build/mp-weixin
```

- **H5**：devServer 已配置代理（`/api`、`/upload` → `http://localhost:8081`），开发态无跨域问题。
- **微信小程序**：用微信开发者工具「导入项目」选择 `dist/dev/mp-weixin` 或 `dist/build/mp-weixin`，
  `src/manifest.json` 中 `mp-weixin.appid` 为占位 `touristappid`，正式联调时替换为真实 AppID。
  小程序端请求的是完整地址，真机调试请把 `src/config.js` 里的 `LAN_IP` 改成电脑局域网 IP（如 `192.168.1.100`）。
- 演示账号：`student01 / admin123`、`student02 / admin123`、`student03 / admin123`（登录页可一键填充）。

## 三、目录结构

```
campus-app/
├── index.html                 # H5 模板（uni 官方结构）
├── vite.config.js             # uni() 插件 + /api、/upload 代理
├── scripts/local-build/       # 受限沙箱环境的构建适配脚本（普通机器不需要）
└── src/
    ├── main.js                # createSSRApp + Pinia
    ├── App.vue                # 启动恢复登录态 + 全局通用样式
    ├── pages.json             # 页面与 tabBar 配置（13 页 / 5 个 tab）
    ├── manifest.json          # 应用信息、H5、微信小程序配置
    ├── uni.scss               # 「校园清新」主题变量（主色 #2b7fff）
    ├── config.js              # baseUrl / 超时 / 演示账号 / 业务字典
    ├── api/                   # 接口层（按契约模块拆分）
    │   ├── auth.js            # /auth/app/login、/auth/logout、/auth/me
    │   ├── item.js            # /item/app/item*、/item/app/category/tree、banner、notice
    │   ├── favorite.js        # /item/app/favorite*
    │   ├── order.js           # /item/app/order*（pay / deliver / confirm / cancel）
    │   ├── comment.js         # /item/app/comment*
    │   ├── message.js         # /item/app/message*
    │   ├── report.js          # /item/app/report
    │   ├── ai.js              # /ai/copywrite、/ai/chat、/ai/recommend、/ai/moderate、/ai/price
    │   └── file.js            # /system/file/upload
    ├── components/            # item-card / empty-state / skeleton-box / ai-chat
    ├── store/user.js          # Pinia：token、userInfo、登录/退出/requireLogin
    ├── utils/
    │   ├── request.js         # uni.request 封装：自动 Bearer、统一 code、401 跳登录、上传
    │   └── format.js          # 金额/时间/成色/状态字典/图片地址补全
    ├── pages/                 # 13 个页面（见下表）
    └── static/                # tabBar 图标（10 张 PNG）+ 默认头像
```

## 四、页面清单

| # | 路径 | 页面 | 关键能力 |
| --- | --- | --- | --- |
| 1 | `pages/index/index` | 首页 | 搜索框、公告条、轮播、分类金刚区、**AI 推荐双列流**（失败回退热销推荐） |
| 2 | `pages/category/category` | 分类 | 左侧一级分类 + 右侧二级 chips + 商品双列流、分页 |
| 3 | `pages/search/search` | 搜索 | 关键词、价格区间、排序（最新 / 价格升降 / 热度）、搜索历史 |
| 4 | `pages/item/detail` | 商品详情 | 图片轮播、价格成色、卖家信息、收藏、评论、**AI 商品问答**、下单入口 |
| 5 | `pages/item/publish` | 发布 / 编辑 | 图片上传、分类成色、**AI 帮我写文案**（`/ai/copywrite` 回填标题/卖点/描述/标签） |
| 6 | `pages/item/my` | 我的发布 | 状态 tab（待审核/在售/已预订/已售出/已下架/违规）、编辑、下架、违规原因 |
| 7 | `pages/order/confirm` | 确认下单 | 商品信息、交易地点、买家留言、金额 |
| 8 | `pages/order/list` | 订单列表 | 我买到的 / 我卖出的、状态筛选、付款 / 交付 / 确认 / 取消 |
| 9 | `pages/order/detail` | 订单详情 | 状态头、进度时间轴、订单操作、**已完成订单评价（orderId 必填）** |
| 10 | `pages/message/message` | 消息中心 | 消息列表（未读/全部已读）+ **AI 智能客服**（展示 sources 引用来源） |
| 11 | `pages/user/user` | 个人中心 | 头像昵称、订单快捷入口、我的发布/收藏、设置、退出登录 |
| 12 | `pages/login/login` | 登录 | 账号密码（`/auth/app/login` 免验证码）、演示账号一键填充、redirect 回跳 |
| 13 | `pages/favorite/favorite` | 我的收藏 | 收藏列表、取消收藏、分页 |

tabBar：首页 / 分类 / 发布 / 消息 / 我的（图标为 `src/static/tabbar/*.png`，选中色 #2b7fff）。

## 五、AI 能力对接

| 场景 | 接口 | 前端表现 |
| --- | --- | --- |
| 首页个性化推荐 | `GET /ai/recommend?limit=8` | 失败或为空时自动回退 `/item/app/item/recommend`，标题副文案同步切换 |
| 一键写文案 | `POST /ai/copywrite` | 回填标题/卖点/描述/标签/建议价，展示 provider 与耗时 |
| 商品问答 / 智能客服 | `POST /ai/chat` | 聊天气泡 + `sources[]` 引用来源（标题 + 相关度） |
| 发布审核 | `POST /ai/moderate`（后端内部） | 发布后进入待审核状态 |

统一约定：AI 降级返回 `{code:1001}` 或 `fallback:true` 时，前端提示「AI 繁忙，已使用本地模板」并展示兜底内容。

## 六、交互与体验约定

- `utils/request.js`：自动携带 `Authorization: Bearer <token>`（本地键 `cm_token`）；`code!==200` 统一 toast；
  **401 清缓存并 reLaunch 到登录页**；网络异常提示「网络异常，请确认后端服务已启动」。
- 列表页统一支持：下拉刷新（`enablePullDownRefresh`）、上拉加载更多（`onReachBottom`）、骨架屏（`skeleton-box`）、空状态（`empty-state`）。
- 图片统一 `lazy-load`，地址经 `mediaUrl()` 补全（H5 走代理，小程序补网关域名）。
- 金额 `¥1,234.50` 格式；时间展示「刚刚 / x 分钟前 / 昨天 / yyyy-MM-dd HH:mm:ss」。
- tabBar 页面不能 `navigateTo` 带参：编辑商品、AI 客服入口等通过 `uni.setStorageSync('pendingEditItemId' | 'cm_message_tab')` 传参，目标页 `onShow` 读取后立即清除。

## 七、受限沙箱环境的构建说明（普通机器可忽略）

本项目在一台**禁止创建管道子进程**的 Windows 沙箱内开发（GitHub 不可达，`spawn` 带 pipe 直接 EPERM），因此额外提供了 `scripts/local-build/`：

- `install-shim.js`：把 `node_modules/esbuild/lib/main.js` 换成调用 `esbuild.exe` + 文件重定向的等价实现；
  并给 vite 打补丁跳过 `exec('net use')` 网络盘探测（否则会误判 sass 未安装）。
- `restore.js`：还原上述两处官方文件。

```bash
# 仅在这类受限环境里需要
npm install --ignore-scripts     # postinstall 会 EPERM
node scripts/local-build/install-shim.js
npm run dev:h5 / npm run build:h5   # 正常通过
node scripts/local-build/restore.js # 需要时还原 node_modules
```

普通开发机（能正常 `spawn`）：`npm install && npm run dev:h5` 即可，无需任何脚本。
