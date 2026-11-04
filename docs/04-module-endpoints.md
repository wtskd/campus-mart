# 04 · 模块与接口清单（自动生成）

> 由脚本扫描各模块 `controller` 源码生成，共 **165** 个接口映射（含一个方法多个路径的情况）。
> 前端实际调用的接口契约以 `03-api-contract.md` 为准；本文件用于答辩时快速说明「每个模块做了什么」。

## 认证授权（campus-auth）

### AuthController　`/auth`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/captcha` | - | 图形验证码 |
| POST | `/login` | - | 账号密码登录 |
| POST | `/app/login` | - | App/小程序登录（无需图形验证码） |
| POST | `/logout` | - | 退出登录 |
| GET | `/me` | - | 当前登录用户 |
| GET | `/menus` | - | 动态菜单树 |

## 系统管理（campus-system）

### SysConfigController　`/system/config`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/list` | `system:config:list` | 参数分页列表 |
| GET | `/page` | `system:config:list` | 参数分页列表（前端兼容路径） |
| GET | `/configKey/{configKey}` | - | 按参数键名取值（走 Redis 缓存；不存在返回 null） |
| GET | `/key/{configKey}` | - | 按参数键名取值（前端兼容路径） |
| GET | `/{configId}` | `system:config:query / system:config:list` | 参数详情 |
| DELETE | `/{configIds}` | `system:config:remove / system:config:list` | 删除参数（逗号分隔批量） |

### SysDeptController　`/system/dept`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/list` | `system:dept:list` | 学院扁平列表 |
| GET | `/tree` | `system:dept:list` | 学院树（前端兼容路径，等价 /treeselect） |
| GET | `/treeselect` | `system:dept:list` | 学院下拉树 |
| GET | `/{deptId}` | `system:dept:query / system:dept:list` | 学院详情 |
| DELETE | `/{deptId}` | `system:dept:remove / system:dept:list` | 删除学院 |

### SysDictDataController　`/system/dict/data`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/list` | `system:dict:list` | 字典数据分页列表 |
| GET | `/page` | `system:dict:list` | 字典数据分页列表（前端兼容路径） |
| GET | `/type/{dictType}` | - | 按字典类型取下拉项（走 Redis 缓存） |
| GET | `/{dictCode}` | `system:dict:query / system:dict:list` | 字典数据详情 |
| DELETE | `/{dictCodes}` | `system:dict:remove / system:dict:list` | 删除字典数据（逗号分隔批量） |

### SysDictTypeController　`/system/dict/type`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/list` | `system:dict:list` | 字典类型分页列表 |
| GET | `/page` | `system:dict:list` | 字典类型分页列表（前端兼容路径） |
| GET | `/optionselect` | `system:dict:list` | 全部启用字典类型（下拉） |
| GET | `/{dictId}` | `system:dict:query / system:dict:list` | 字典类型详情 |
| DELETE | `/{dictIds}` | `system:dict:remove / system:dict:list` | 删除字典类型（级联删除字典数据） |

### SysFileController　`/system/file`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| POST | `/upload` | `system:file:upload / system:file:list` | 单文件上传 |
| POST | `/uploads` | `system:file:upload / system:file:list` | 多文件上传（前端兼容路径，字段名 files） |
| GET | `/list` | `system:file:list` | 文件库分页列表 |
| DELETE | `/{fileIds}` | `system:file:remove / system:file:list` | 删除文件记录（逗号分隔批量） |

### SysLogCompatController　`/system/log`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/oper/page` | `system:operlog:list` | 操作日志分页（兼容路径） |
| DELETE | `/oper/{operIds}` | `system:operlog:remove / system:operlog:list` | 删除操作日志（兼容路径） |
| DELETE | `/oper/clean` | `system:operlog:remove / system:operlog:list` | 清空操作日志（兼容路径） |
| GET | `/login/page` | `system:loginlog:list` | 登录日志分页（兼容路径） |
| DELETE | `/login/{infoIds}` | `system:loginlog:remove / system:loginlog:list` | 删除登录日志（兼容路径） |
| DELETE | `/login/clean` | `system:loginlog:remove / system:loginlog:list` | 清空登录日志（兼容路径） |

### SysLoginLogController　`/system/loginlog`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/list` | `system:loginlog:list` | 登录日志分页列表（支持时间区间） |
| DELETE | `/clean` | `system:loginlog:remove / system:loginlog:list` | 清空登录日志 |
| DELETE | `/{infoIds}` | `system:loginlog:remove / system:loginlog:list` | 删除登录日志（逗号分隔批量） |

### SysMenuController　`/system/menu`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/list` | `system:menu:list` | 菜单扁平列表（前端自行建树，带 parentId） |
| GET | `/tree` | `system:menu:list` | 菜单树（菜单管理页表格树） |
| GET | `/treeselect` | `system:menu:list` | 菜单下拉树（含按钮，角色授权勾选） |
| GET | `/roleMenuTreeselect/{roleId}` | `system:menu:list / system:role:list` | 角色菜单授权树（菜单树 + 已勾选ID） |
| GET | `/routers` | - | 当前登录用户动态路由 |
| GET | `/{menuId}` | `system:menu:query / system:menu:list` | 菜单详情 |
| DELETE | `/{menuId}` | `system:menu:remove / system:menu:list` | 删除菜单 |
| GET | `/inner/tree/{userId}` | - | 内部-用户菜单树 |

### SysOperLogController　`/system/operlog`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/list` | `system:operlog:list` | 操作日志分页列表（支持时间区间） |
| DELETE | `/clean` | `system:operlog:remove / system:operlog:list` | 清空操作日志 |
| DELETE | `/{operIds}` | `system:operlog:remove / system:operlog:list` | 删除操作日志（逗号分隔批量） |

### SysRoleController　`/system/role`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/list` | `system:role:list` | 角色分页列表 |
| GET | `/page` | `system:role:list` | 角色分页列表（前端兼容路径，等价 /list） |
| GET | `/optionselect` | `system:role:list` | 全部启用角色（下拉） |
| GET | `/{roleId}/menuIds` | `system:role:query / system:role:list` | 角色已授权菜单ID |
| GET | `/{roleId}` | `system:role:query / system:role:list` | 角色详情 |
| DELETE | `/{roleIds}` | `system:role:remove / system:role:list` | 删除角色（逗号分隔批量） |
| PUT | `/changeStatus` | `system:role:edit / system:role:list` | 修改角色状态（0正常 1停用） |
| PUT | `/{roleId}/status/{status}` | `system:role:edit / system:role:list` | 修改角色状态（前端兼容路径） |
| PUT | `/menu` | `system:role:edit / system:role:list` | 角色菜单授权（整体重写） |

### SysUserController　`/system/user`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/list` | `system:user:list` | 用户分页列表 |
| GET | `/page` | `system:user:list` | 用户分页列表（前端兼容路径，等价 /list） |
| GET | `/{userId}` | `system:user:query / system:user:list` | 用户详情（含角色ID） |
| GET | `/deptTree` | `system:user:list` | 学院下拉树（用户表单归属学院） |
| DELETE | `/{userIds}` | `system:user:remove / system:user:list` | 删除用户（逗号分隔批量） |
| PUT | `/resetPwd` | `system:user:resetPwd / system:user:edit / system:user:list` | 重置密码 |
| PUT | `/changeStatus` | `system:user:edit / system:user:list` | 修改用户状态（0正常 1停用） |
| PUT | `/{userId}/status/{status}` | `system:user:edit / system:user:list` | 修改用户状态（前端兼容路径） |
| GET | `/inner/login-info/{username}` | - | 内部-按用户名取登录信息 |
| GET | `/inner/login-info/id/{userId}` | - | 内部-按用户ID取登录信息 |
| GET | `/inner/menu-ids/{userId}` | - | 内部-用户拥有的菜单ID |
| GET | `/inner/record-login/{userId}` | - | 内部-记录登录信息并写登录日志 |

## 二手交易（campus-item）

### AdminBannerController　`/item/banner`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/page` | - | 轮播分页 |
| GET | `/list` | - | 轮播列表 |
| POST | `` | - | 新增轮播 |
| PUT | `` | - | 修改轮播 |
| DELETE | `/{id}` | - | 删除轮播 |

### AdminMessageController　`/item/message`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/page` | - | 消息分页 |

### AdminNoticeController　`/item/notice`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/page` | - | 公告分页 |
| GET | `/{id}` | - | 公告详情 |
| POST | `` | - | 新增公告 |
| PUT | `` | - | 修改公告 |
| DELETE | `/{id}` | - | 删除公告 |

### AdminOrderController　`/item/order`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/page` | - | 订单分页 |
| PUT | `/status` | - | 人工修改订单状态 |
| GET | `/stat/today` | - | 今日订单统计 |

### AdminReportController　`/item/report`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/page` | - | 举报分页 |
| PUT | `/handle` | - | 处理举报 |

### AppBannerController　`/item/app/banner`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/list` | - | 轮播列表 |

### AppCategoryController　`/item/app/category`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/tree` | - | 分类树 |

### AppCommentController　`/item/app/comment | /item/comment`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/list/{itemId}` | - | 商品评价列表 |

### AppFavoriteController　`/item/app/favorite`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| POST | `/{itemId}` | - | 收藏商品 |
| DELETE | `/{itemId}` | - | 取消收藏 |
| GET | `/my` | - | 我的收藏 |
| GET | `/check/{itemId}` | - | 是否已收藏 |

### AppItemController　`/item/app/item`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/page` | - | 在售商品分页 |
| GET | `/{itemId}` | - | 商品详情 |
| POST | `/{itemId}/off-shelf` | - | 下架商品 |
| GET | `/my` | - | 我发布的商品 |
| GET | `/recommend` | - | 推荐商品（兜底） |

### AppMessageController　`/item/app/message`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/list` | - | 我的消息分页 |
| GET | `/my` | - | 我的消息分页 |
| GET | `/unread-count` | - | 未读消息数 |
| PUT | `/read/{messageId}` | - | 标记单条已读 |
| PUT | `/read-all` | - | 全部标记已读 |

### AppNoticeController　`/item/app/notice`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/list` | - | 公告列表 |
| GET | `/{noticeId}` | - | 公告详情 |

### AppOrderController　`/item/app/order`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| POST | `` | - | 创建订单 |
| PUT | `/{orderId}/pay` | - | 模拟付款 |
| PUT | `/{orderId}/deliver` | - | 卖家标记交付 |
| PUT | `/{orderId}/confirm` | - | 买家确认收货 |
| PUT | `/{orderId}/cancel` | - | 取消订单 |
| GET | `/my` | - | 我的订单 |
| GET | `/{orderId}` | - | 订单详情 |

### AppReportController　`/item/app/report`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| POST | `` | - | 提交举报 |

### CategoryController　`/item/category`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/list` | - | 分类列表 |
| DELETE | `/{categoryId}` | - | 删除分类 |
| PUT | `/status` | - | 修改分类状态 |

### CommentController　`/item/comment`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/page` | - | 评价分页 |
| PUT | `/audit` | - | 评价审核 |
| DELETE | `/{commentId}` | - | 删除评价 |

### InnerItemController　`/item/inner`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/item/{itemId}` | - | 商品概要 |
| GET | `/item/on-sale` | - | 在售商品列表 |
| GET | `/item/favorite/{userId}` | - | 用户收藏商品列表 |

### ItemController　`/item/item`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/page` | - | 商品分页 |
| POST | `/audit` | - | 商品审核 |
| PUT | `/status` | - | 强制修改商品状态 |
| DELETE | `/{itemId}` | - | 删除商品 |

## 数据分析（campus-analysis）

### AnalysisController　`/analysis`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/overview` | - | 总览卡片 |
| GET | `/trend` | - | 趋势折线 |
| GET | `/category` | - | 分类成交占比 |
| GET | `/funnel` | - | 转化漏斗 |
| GET | `/retention` | - | 用户留存 |
| GET | `/rfm` | - | RFM 用户分层 |
| GET | `/hot-item` | - | 热门商品榜 |
| GET | `/hot-items` | - | 热门商品榜 |
| GET | `/seller-rank` | - | 卖家榜 |
| GET | `/ai-usage` | - | AI 使用情况 |
| GET | `/dashboard` | - | 数据大屏 |
| GET | `/screen` | - | 数据大屏 |
| POST | `/refresh` | - | 手动重算统计 |
| POST | `/rebuild` | - | 手动重算统计 |
| GET | `/export/order` | - | 导出订单 CSV |

## AI 能力（campus-ai）

### AiAdminController　`/ai`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/log/page` | - | AI 调用日志分页 |
| GET | `/prompt/page` | - | 提示词模板分页 |
| POST | `/prompt` | - | 新增提示词模板 |
| PUT | `/prompt` | - | 修改提示词模板 |
| DELETE | `/prompt/{id}` | - | 删除提示词模板 |
| GET | `/knowledge/page` | - | 知识库分页 |
| POST | `/knowledge` | - | 新增知识条目 |
| PUT | `/knowledge` | - | 修改知识条目 |
| DELETE | `/knowledge/{id}` | - | 删除知识条目 |
| POST | `/knowledge/search` | - | 知识库召回测试 |
| GET | `/scene/list` | - | AI 场景列表 |
| GET | `/stat/overview` | - | AI 使用概览 |

### ChatController　`/ai`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| POST | `/chat` | - | 智能客服问答（RAG 知识库召回） |

### CopywritingController　`/ai`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| POST | `/copywrite` | - | 一键生成商品发布文案 |
| POST | `/copywriting/item` | - | 一键生成商品发布文案 |
| POST | `/summary/item/{itemId}` | - | 生成商品卖点摘要 |

### HealthController　`/ai`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/health` | - | AI 服务健康状态（remote/local/mock） |

### ModerationController　`/ai`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| POST | `/moderate` | - | 商品内容审核（内部接口，item 服务调用） |
| POST | `/inner/moderate` | - | 商品内容审核（内部接口，item 服务调用） |

### PriceController　`/ai`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| POST | `/price` | - | 生成建议售价区间 |

### RecommendController　`/ai`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/recommend` | - | 个性化推荐（ItemCF + 分类/价格画像，冷启动走热度） |
| GET | `/similar/{itemId}` | - | 相似商品（同分类 + 价格 ±50%） |
