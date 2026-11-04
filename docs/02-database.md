# 数据库设计（CampusMart）

> 数据库：`campus_mart`　字符集：`utf8mb4_general_ci`　引擎：InnoDB　共 29 张表
> 建表脚本：`sql/01-schema.sql`��业务演示数据：`sql/02-seed-business.sql`；权限补丁：`sql/03-perms-extra.sql`、`sql/04-perms-item-trade.sql`

## 一、设计约定

| 约定 | 说明 |
|---|---|
| 主键 | 每表 `bigint auto_increment` 主键，命名 `xxx_id` |
| 逻辑删除 | `del_flag char(1)`：`'0'` 正常、`'2'` 删除（MyBatis-Plus `@TableLogic`） |
| 审计字段 | `create_time` / `update_time`（自动填充） |
| 状态字段 | 系统表用 `char(1)`（`'0'` 正常/`'1'` 停用），业务表用 `tinyint` 枚举 |
| 金额 | `decimal(10,2)`，单位：元 |
| 索引 | 唯一约束承载业务唯一性（`uk_*`），查询字段建普通索引（`idx_*`） |
| 中文检索 | `biz_item`、`ai_knowledge` 使用 `FULLTEXT ... WITH PARSER ngram` |
| 外键 | 不建物理外键，服务层保证一致性（微服务解耦） |

## 二、表清单

| 分组 | 表名 | 说明 | 字段数 |
|---|---|---|---|
| 系统表 | `sys_config` | 系统参数 | 8 |
| 系统表 | `sys_dept` | 学院/班级表 | 11 |
| 系统表 | `sys_dict_data` | 字典数据 | 10 |
| 系统表 | `sys_dict_type` | 字典类型 | 6 |
| 系统表 | `sys_file` | 文件表 | 11 |
| 系统表 | `sys_login_log` | 登录日志表 | 9 |
| 系统表 | `sys_menu` | 菜单权限表（动态菜单来源） | 16 |
| 系统表 | `sys_oper_log` | 操作日志表 | 16 |
| 系统表 | `sys_role` | 角色表 | 10 |
| 系统表 | `sys_role_menu` | 角色菜单关联 | 2 |
| 系统表 | `sys_user` | 用户表 | 19 |
| 系统表 | `sys_user_role` | 用户角色关联 | 2 |
| 业务表 | `biz_banner` | 首页轮播 | 9 |
| 业务表 | `biz_category` | 商品分类 | 8 |
| 业务表 | `biz_comment` | 评价表 | 11 |
| 业务表 | `biz_favorite` | 收藏表 | 4 |
| 业务表 | `biz_item` | 商品表 | 25 |
| 业务表 | `biz_message` | 站内消息 | 8 |
| 业务表 | `biz_notice` | 公告表 | 8 |
| 业务表 | `biz_order` | 订单表 | 16 |
| 业务表 | `biz_report` | 举报表 | 11 |
| 统计表 | `stat_category_daily` | 分类每日统计 | 6 |
| 统计表 | `stat_funnel_daily` | 转化漏斗每日统计 | 6 |
| 统计表 | `stat_overview_daily` | 每日概览统计 | 13 |
| 统计表 | `stat_user_rfm` | 用户RFM分层 | 9 |
| AI 表 | `ai_call_log` | AI调用日志（可观测性） | 14 |
| AI 表 | `ai_knowledge` | RAG知识库 | 8 |
| AI 表 | `ai_prompt_template` | Prompt模板（后台可运营） | 13 |
| AI 表 | `ai_recommend` | 个性化推荐结果 | 8 |

## 系统表（sys_）

用户、角色、菜单、部门、字典、参数、日志、文件 —— 支撑权限管理与动态菜单。

### `sys_config`　系统参数

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `config_id` | bigint | NO |  | PK |  |
| `config_name` | varchar(64) | NO |  |  | 参数名称 |
| `config_key` | varchar(128) | NO |  | UK | 参数键 |
| `config_value` | varchar(500) | NO |  |  | 参数值 |
| `config_type` | char(1) | NO | `N` |  | 是否内置 Y/N |
| `remark` | varchar(255) | YES |  |  |  |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |
| `update_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `sys_dept`　学院/班级表

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `dept_id` | bigint | NO |  | PK | 学院ID |
| `parent_id` | bigint | NO | `0` | IDX | 父ID，0为根 |
| `ancestors` | varchar(64) | NO |  |  | 祖级列表，逗号分隔 |
| `dept_name` | varchar(64) | NO |  |  | 学院/班级名称 |
| `order_num` | int | NO | `0` |  | 排序 |
| `leader` | varchar(32) | YES |  |  | 负责人 |
| `phone` | varchar(20) | YES |  |  | 联系电话 |
| `status` | char(1) | NO | `0` |  | 状态 0正常 1停用 |
| `del_flag` | char(1) | NO | `0` |  | 删除标记 0存在 1删除 |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |
| `update_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `sys_dict_data`　字典数据

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `dict_code` | bigint | NO |  | PK |  |
| `dict_type` | varchar(64) | NO |  | IDX | 字典类型编码 |
| `dict_label` | varchar(64) | NO |  |  | 显示值 |
| `dict_value` | varchar(64) | NO |  |  | 实际值 |
| `dict_sort` | int | NO | `0` |  |  |
| `css_class` | varchar(64) | YES |  |  | 样式类 |
| `list_class` | varchar(64) | YES |  |  | 标签样式 |
| `is_default` | char(1) | NO | `N` |  |  |
| `status` | char(1) | NO | `0` |  |  |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `sys_dict_type`　字典类型

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `dict_id` | bigint | NO |  | PK |  |
| `dict_name` | varchar(64) | NO |  |  | 字典名称 |
| `dict_type` | varchar(64) | NO |  | UK | 字典类型编码 |
| `status` | char(1) | NO | `0` |  |  |
| `remark` | varchar(255) | YES |  |  |  |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `sys_file`　文件表

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `file_id` | bigint | NO |  | PK |  |
| `file_name` | varchar(128) | NO |  |  | 存储文件名 |
| `original_name` | varchar(255) | NO |  |  | 原始文件名 |
| `file_path` | varchar(500) | NO |  |  | 存储路径 |
| `file_url` | varchar(500) | NO |  |  | 访问URL |
| `file_size` | bigint | NO | `0` |  | 字节 |
| `file_type` | varchar(64) | YES |  |  | MIME类型 |
| `storage` | varchar(16) | NO | `local` |  | local/minio |
| `biz_type` | varchar(32) | YES |  | IDX | 业务类型 item/avatar/... |
| `create_by` | bigint | YES |  |  |  |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `sys_login_log`　登录日志表

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `info_id` | bigint | NO |  | PK |  |
| `username` | varchar(32) | YES |  |  |  |
| `ipaddr` | varchar(64) | YES |  |  |  |
| `login_location` | varchar(255) | YES |  |  |  |
| `browser` | varchar(64) | YES |  |  |  |
| `os` | varchar(64) | YES |  |  |  |
| `status` | char(1) | NO | `0` |  | 0成功 1失败 |
| `msg` | varchar(255) | YES |  |  |  |
| `login_time` | datetime | NO | `CURRENT_TIMESTAMP` | IDX |  |

### `sys_menu`　菜单权限表（动态菜单来源）

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `menu_id` | bigint | NO |  | PK | 菜单ID |
| `menu_name` | varchar(64) | NO |  |  | 菜单名称 |
| `parent_id` | bigint | NO | `0` | IDX | 父菜单ID |
| `order_num` | int | NO | `0` |  | 排序 |
| `path` | varchar(200) | YES |  |  | 路由地址 |
| `component` | varchar(255) | YES |  |  | 组件路径 |
| `query_param` | varchar(255) | YES |  |  | 路由参数 |
| `is_frame` | char(1) | NO | `1` |  | 是否外链 0是 1否 |
| `is_cache` | char(1) | NO | `0` |  | 是否缓存 0缓存 1不缓存 |
| `menu_type` | char(1) | NO | `C` |  | 类型 M目录 C菜单 F按钮 |
| `visible` | char(1) | NO | `0` |  | 显示 0显示 1隐藏 |
| `status` | char(1) | NO | `0` |  | 状态 0正常 1停用 |
| `perms` | varchar(128) | YES |  |  | 权限标识 |
| `icon` | varchar(64) | YES |  |  | 图标 |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |
| `update_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `sys_oper_log`　操作日志表

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `oper_id` | bigint | NO |  | PK | 日志ID |
| `title` | varchar(64) | YES |  |  | 模块标题 |
| `business_type` | char(2) | NO | `0` |  | 业务类型 0其他 1新增 2修改 3删除 4查询 5导出 |
| `method` | varchar(200) | YES |  |  | 方法名 |
| `request_method` | varchar(10) | YES |  |  | HTTP方法 |
| `operator_type` | char(2) | NO | `00` |  | 操作人类别 |
| `oper_name` | varchar(32) | YES |  | IDX | 操作人 |
| `dept_name` | varchar(64) | YES |  |  | 学院 |
| `oper_url` | varchar(255) | YES |  |  | 请求URL |
| `oper_ip` | varchar(64) | YES |  |  | IP |
| `oper_param` | text | YES |  |  | 请求参数（脱敏） |
| `json_result` | text | YES |  |  | 返回结果 |
| `status` | char(1) | NO | `0` |  | 0成功 1失败 |
| `error_msg` | varchar(2000) | YES |  |  |  |
| `cost_time` | bigint | NO | `0` |  | 耗时ms |
| `oper_time` | datetime | NO | `CURRENT_TIMESTAMP` | IDX |  |

### `sys_role`　角色表

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `role_id` | bigint | NO |  | PK | 角色ID |
| `role_name` | varchar(32) | NO |  |  | 角色名称 |
| `role_key` | varchar(64) | NO |  | UK | 角色标识，如 admin/operator |
| `role_sort` | int | NO | `0` |  | 排序 |
| `data_scope` | char(1) | NO | `1` |  | 数据范围 1全部 2本学院 3本人 |
| `status` | char(1) | NO | `0` |  | 状态 0正常 1停用 |
| `remark` | varchar(255) | YES |  |  |  |
| `del_flag` | char(1) | NO | `0` |  |  |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |
| `update_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `sys_role_menu`　角色菜单关联

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `role_id` | bigint | NO |  | PK |  |
| `menu_id` | bigint | NO |  | PK |  |

### `sys_user`　用户表

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `user_id` | bigint | NO |  | PK | 用户ID |
| `dept_id` | bigint | YES |  | IDX | 学院ID |
| `username` | varchar(32) | NO |  | UK | 登录账号 |
| `nickname` | varchar(32) | NO |  |  | 昵称 |
| `user_type` | char(2) | NO | `01` | IDX | 用户类型 00管理端 01小程序端 |
| `password` | varchar(100) | NO |  |  | BCrypt 密码 |
| `phone` | varchar(20) | YES |  | IDX | 手机号 |
| `email` | varchar(64) | YES |  |  | 邮箱 |
| `avatar` | varchar(255) | YES |  |  | 头像URL |
| `gender` | char(1) | NO | `2` |  | 性别 0男 1女 2未知 |
| `student_no` | varchar(32) | YES |  |  | 学号 |
| `credit_score` | int | NO | `100` |  | 信用分 0-100 |
| `status` | char(1) | NO | `0` |  | 状态 0正常 1停用 |
| `login_ip` | varchar(64) | YES |  |  | 最后登录IP |
| `login_date` | datetime | YES |  |  | 最后登录时间 |
| `remark` | varchar(500) | YES |  |  | 备注 |
| `del_flag` | char(1) | NO | `0` |  |  |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |
| `update_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `sys_user_role`　用户角色关联

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `user_id` | bigint | NO |  | PK |  |
| `role_id` | bigint | NO |  | PK |  |

## 业务表（biz_）

商品、分类、订单、收藏、评价、举报、消息、公告、轮播 —— 二手交易主流程。

### `biz_banner`　首页轮播

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `banner_id` | bigint | NO |  | PK |  |
| `title` | varchar(64) | NO |  |  |  |
| `image_url` | varchar(500) | NO |  |  |  |
| `link_url` | varchar(500) | YES |  |  |  |
| `sort` | int | NO | `0` |  |  |
| `status` | char(1) | NO | `0` |  | 0上架 1下架 |
| `start_time` | datetime | YES |  |  |  |
| `end_time` | datetime | YES |  |  |  |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `biz_category`　商品分类

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `category_id` | bigint | NO |  | PK |  |
| `parent_id` | bigint | NO | `0` | IDX | 父分类，0为一级 |
| `category_name` | varchar(32) | NO |  |  |  |
| `icon` | varchar(255) | YES |  |  |  |
| `order_num` | int | NO | `0` |  |  |
| `status` | char(1) | NO | `0` |  | 0正常 1停用 |
| `del_flag` | char(1) | NO | `0` |  |  |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `biz_comment`　评价表

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `comment_id` | bigint | NO |  | PK |  |
| `order_id` | bigint | NO |  | UK |  |
| `item_id` | bigint | NO |  | IDX |  |
| `from_user_id` | bigint | NO |  |  | 评价人 |
| `to_user_id` | bigint | NO |  | IDX | 被评价人 |
| `score` | tinyint | NO | `5` |  | 1-5 星 |
| `content` | varchar(500) | YES |  |  |  |
| `images` | json | YES |  |  |  |
| `reply_content` | varchar(500) | YES |  |  | 卖家回复 |
| `status` | char(1) | NO | `0` |  | 0正常 1隐藏 |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `biz_favorite`　收藏表

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `favorite_id` | bigint | NO |  | PK |  |
| `user_id` | bigint | NO |  | IDX |  |
| `item_id` | bigint | NO |  | IDX |  |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `biz_item`　商品表

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `item_id` | bigint | NO |  | PK | 商品ID |
| `item_no` | varchar(32) | NO |  | UK | 商品编号 |
| `seller_id` | bigint | NO |  | IDX | 卖家ID |
| `category_id` | bigint | NO |  | IDX | 分类ID |
| `title` | varchar(100) | NO |  | IDX | 标题 |
| `summary` | varchar(255) | YES |  |  | 摘要 |
| `description` | text | YES |  |  | 详细描述 |
| `price` | decimal(10,2) | NO |  | IDX | 售价 |
| `original_price` | decimal(10,2) | YES |  |  | 原价 |
| `item_condition` | tinyint | NO | `9` |  | 成色 1-10（10全新） |
| `trade_type` | tinyint | NO | `1` |  | 1出售 2求购 3交换 |
| `cover_url` | varchar(500) | YES |  |  | 封面图 |
| `images` | json | YES |  |  | 图片列表 |
| `tags` | json | YES |  |  | 标签数组 |
| `location` | varchar(64) | YES |  |  | 交易地点 |
| `contact_type` | tinyint | NO | `0` |  | 0站内 1手机 2微信 |
| `ai_generated` | char(1) | NO | `0` |  | 是否AI生成 0否 1是 |
| `view_count` | int | NO | `0` |  |  |
| `favorite_count` | int | NO | `0` |  |  |
| `status` | tinyint | NO | `1` | IDX | 0草稿 1待审核 2在售 3已预订 4已售出 5下架 6违规 |
| `audit_remark` | varchar(500) | YES |  |  | 审核意见 |
| `publish_time` | datetime | YES |  |  |  |
| `del_flag` | char(1) | NO | `0` |  |  |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |
| `update_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `biz_message`　站内消息

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `message_id` | bigint | NO |  | PK |  |
| `user_id` | bigint | NO |  | IDX | 接收人 |
| `msg_type` | tinyint | NO | `1` |  | 1系统 2订单 3举报 4AI |
| `title` | varchar(100) | NO |  |  |  |
| `content` | varchar(500) | YES |  |  |  |
| `biz_id` | bigint | YES |  |  | 关联业务ID |
| `is_read` | char(1) | NO | `0` |  | 0未读 1已读 |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `biz_notice`　公告表

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `notice_id` | bigint | NO |  | PK |  |
| `title` | varchar(100) | NO |  |  |  |
| `content` | text | NO |  |  |  |
| `notice_type` | tinyint | NO | `1` |  | 1公告 2活动 3规则 |
| `status` | char(1) | NO | `0` |  | 0发布 1草稿 |
| `publish_time` | datetime | YES |  |  |  |
| `create_by` | bigint | YES |  |  |  |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `biz_order`　订单表

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `order_id` | bigint | NO |  | PK |  |
| `order_no` | varchar(32) | NO |  | UK | 订单号 |
| `item_id` | bigint | NO |  | IDX | 商品ID（二手单品，一单一物） |
| `buyer_id` | bigint | NO |  | IDX |  |
| `seller_id` | bigint | NO |  | IDX |  |
| `amount` | decimal(10,2) | NO |  |  | 成交金额 |
| `status` | tinyint | NO | `0` |  | 0待确认 1待交付 2已完成 3已取消 4退款中 5已退款 |
| `pay_type` | tinyint | NO | `0` |  | 0线下 1模拟在线 |
| `trade_place` | varchar(64) | YES |  |  | 交易地点 |
| `buyer_remark` | varchar(255) | YES |  |  |  |
| `cancel_reason` | varchar(255) | YES |  |  |  |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` | IDX |  |
| `pay_time` | datetime | YES |  |  |  |
| `finish_time` | datetime | YES |  |  |  |
| `cancel_time` | datetime | YES |  |  |  |
| `update_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `biz_report`　举报表

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `report_id` | bigint | NO |  | PK |  |
| `item_id` | bigint | NO |  | IDX |  |
| `reporter_id` | bigint | NO |  |  |  |
| `reason_type` | varchar(32) | NO |  |  | 字典 report_reason |
| `content` | varchar(500) | YES |  |  |  |
| `images` | json | YES |  |  |  |
| `status` | tinyint | NO | `0` | IDX | 0待处理 1已处理 2驳回 |
| `handle_user_id` | bigint | YES |  |  |  |
| `handle_remark` | varchar(500) | YES |  |  |  |
| `handle_time` | datetime | YES |  |  |  |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

## 统计表（stat_）

T+1 预聚合，供数据分析和数据大屏直接查询。

### `stat_category_daily`　分类每日统计

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `id` | bigint | NO |  | PK |  |
| `stat_date` | date | NO |  | IDX |  |
| `category_id` | bigint | NO |  |  |  |
| `item_count` | int | NO | `0` |  | 发布数 |
| `order_count` | int | NO | `0` |  |  |
| `gmv` | decimal(14,2) | NO | `0.00` |  |  |

### `stat_funnel_daily`　转化漏斗每日统计

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `stat_date` | date | NO |  | PK |  |
| `view_count` | int | NO | `0` |  | 浏览 |
| `favorite_count` | int | NO | `0` |  | 收藏 |
| `order_count` | int | NO | `0` |  | 下单 |
| `pay_count` | int | NO | `0` |  | 支付 |
| `finish_count` | int | NO | `0` |  | 完成 |

### `stat_overview_daily`　每日概览统计

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `stat_date` | date | NO |  | PK | 统计日期 |
| `gmv` | decimal(14,2) | NO | `0.00` |  | 成交额 |
| `order_count` | int | NO | `0` |  | 下单数 |
| `finish_order_count` | int | NO | `0` |  | 完成订单数 |
| `cancel_order_count` | int | NO | `0` |  | 取消订单数 |
| `item_publish_count` | int | NO | `0` |  | 商品发布数 |
| `item_sold_count` | int | NO | `0` |  | 成交商品数 |
| `new_user_count` | int | NO | `0` |  | 新增用户数 |
| `active_user_count` | int | NO | `0` |  | 活跃用户数 |
| `view_count` | int | NO | `0` |  | 商品浏览数 |
| `favorite_count` | int | NO | `0` |  | 收藏数 |
| `ai_call_count` | int | NO | `0` |  | AI调用次数 |
| `update_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `stat_user_rfm`　用户RFM分层

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `user_id` | bigint | NO |  | PK |  |
| `recency_days` | int | NO | `999` |  | 最近一次交易距今天数 |
| `frequency` | int | NO | `0` |  | 交易次数 |
| `monetary` | decimal(14,2) | NO | `0.00` |  | 交易金额 |
| `r_score` | tinyint | NO | `1` |  |  |
| `f_score` | tinyint | NO | `1` |  |  |
| `m_score` | tinyint | NO | `1` |  |  |
| `segment` | varchar(16) | NO | `一般发展` |  | 用户分层 |
| `calc_date` | date | NO |  | PK |  |

## AI 表（ai_）

Prompt 模板、调用日志、RAG 知识库、推荐结果。

### `ai_call_log`　AI调用日志（可观测性）

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `id` | bigint | NO |  | PK |  |
| `user_id` | bigint | YES |  |  |  |
| `scene` | varchar(32) | NO |  | IDX | 场景 |
| `template_code` | varchar(64) | YES |  |  |  |
| `provider` | varchar(16) | NO | `remote` |  | remote/local |
| `model` | varchar(64) | YES |  |  |  |
| `request_summary` | varchar(500) | YES |  |  | 请求摘要（脱敏截断） |
| `prompt_tokens` | int | NO | `0` |  |  |
| `completion_tokens` | int | NO | `0` |  |  |
| `total_tokens` | int | NO | `0` |  |  |
| `cost_ms` | bigint | NO | `0` |  |  |
| `status` | char(1) | NO | `0` |  | 0成功 1失败 2降级兜底 |
| `error_msg` | varchar(500) | YES |  |  |  |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` | IDX |  |

### `ai_knowledge`　RAG知识库

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `id` | bigint | NO |  | PK |  |
| `kb_type` | varchar(32) | NO | `faq` |  | faq/rule/category |
| `title` | varchar(200) | NO |  | IDX |  |
| `content` | text | NO |  |  |  |
| `keywords` | varchar(255) | YES |  |  |  |
| `status` | char(1) | NO | `0` |  |  |
| `update_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `ai_prompt_template`　Prompt模板（后台可运营）

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `id` | bigint | NO |  | PK |  |
| `code` | varchar(64) | NO |  | UK | 模板编码，如 ITEM_COPYWRITE |
| `name` | varchar(64) | NO |  |  |  |
| `scene` | varchar(32) | NO |  |  | 场景 copywrite/chat/audit/price |
| `system_prompt` | text | NO |  |  |  |
| `user_prompt` | text | NO |  |  | 支持 {{变量}} 占位 |
| `model` | varchar(64) | YES |  |  | 为空用默认模型 |
| `temperature` | decimal(3,2) | NO | `0.70` |  |  |
| `max_tokens` | int | NO | `1024` |  |  |
| `status` | char(1) | NO | `0` |  | 0启用 1停用 |
| `remark` | varchar(255) | YES |  |  |  |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |
| `update_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

### `ai_recommend`　个性化推荐结果

| 字段 | 类型 | 允许空 | 默认值 | 键 | 说明 |
|---|---|---|---|---|---|
| `id` | bigint | NO |  | PK |  |
| `user_id` | bigint | NO |  | IDX |  |
| `item_id` | bigint | NO |  |  |  |
| `score` | decimal(10,4) | NO | `0.0000` |  |  |
| `reason` | varchar(64) | YES |  |  | 推荐理由，如 同校同分类 |
| `scene` | varchar(16) | NO | `home` |  | home/detail/cart |
| `batch_date` | date | NO |  |  |  |
| `create_time` | datetime | NO | `CURRENT_TIMESTAMP` |  |  |

## 三、关键关系

```
sys_dept 1 ── n sys_user          （用户归属部门/学院）
sys_user n ── n sys_role          （sys_user_role）
sys_role n ── n sys_menu          （sys_role_menu，权限来源）
sys_menu 自关联 parent_id          （目录/菜单/按钮三级）

biz_category 自关联 parent_id      （两级分类）
sys_user 1 ── n biz_item          （seller_id 发布商品）
biz_category 1 ── n biz_item
biz_item 1 ── n biz_order         （一物可多次流转：取消后可再次下单）
biz_order 1 ── 1 biz_comment      （uk_order_id：订单完成后唯一评价）
sys_user n ── n biz_item          （biz_favorite 收藏）
biz_report ──▶ biz_item           （target_id 指向被举报商品）

stat_overview_daily / stat_category_daily / stat_funnel_daily / stat_user_rfm
        ▲ 每日 02:00 由 analysis 服务从 biz_* 预聚合
ai_prompt_template  scene ──▶ 各 AI 场景 System Prompt
ai_knowledge        FULLTEXT 召回 ──▶ RAG 智能客服
ai_call_log         每次模型调用（含 token/耗时/降级状态）
ai_recommend        离线 ItemCF 结果缓存
```

## 四、关键状态机

**商品 `biz_item.status`**：0 草稿 → 1 待审核 → 2 在售 → 3 已预订 → 4 已售出
　　　　　　　　　1 待审核 --审核驳回--> 5 已下架　　AI 审核不通过 --> 6 违规

**订单 `biz_order.status`**：
```
0 待确认 ──pay/deliver──▶ 1 待交付 ──confirm──▶ 2 已完成 ──▶（可评价）
   │                          │                    │
   └────────cancel────────────┴──▶ 3 已取消         └─refund─▶ 4 退款中 ─▶ 5 已退款
```
- 下单：商品 2 → 3；取消/退款完成：商品 3 → 2（若存在其它进行中订单则不回滚）
- 确认收货：商品 3 → 4，卖家 `credit_score` +1（上限 100）

## 五、数据量（演示数据）

| 表 | 行数 | 说明 |
|---|---|---|
| `biz_item` | 60 | 覆盖全部 7 种状态 |
| `biz_order` | 37 | 24 完成 / 3 待交付 / 3 待确认 / 5 取消 / 2 退款 |
| `biz_favorite` | 60 | 收藏关系 |
| `biz_comment` | 18 | 已成交订单评价 |
| `ai_call_log` | 75 | 近 7 天，供 AI 消耗统计出图 |
| `sys_menu` | 60+ | 目录/菜单/按钮 |
| 其余 | 见 `sql/02-seed-business.sql` | - |
