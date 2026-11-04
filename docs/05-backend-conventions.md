# CampusMart 后端开发约定（团队共用）

> 所有后端开发必须遵守本文件；发现约定缺失请找 lead 补充，不要各自造轮子。

## 1. 工程结构

```
campus-mart/
├── pom.xml                       聚合父 POM（版本统一管理，勿改）
├── campus-common/                公共：Result、异常、安全上下文、日志、配置（勿改）
├── campus-api/                   服务间契约：Feign 客户端 + DTO（勿改）
├── campus-gateway/               网关 8081（勿改）
├── campus-auth/                  认证 9200（勿改）
└── campus-modules/
    ├── campus-system/  9201      系统（lead 负责）
    ├── campus-item/    9202      商品交易
    ├── campus-analysis/9203      数据分析
    └── campus-ai/      9204      AI 能力
```

数据库已在本地 MySQL 运行：`127.0.0.1:3306`，账号 `root/123456`，库 `campus_mart`（29 张表 + 种子数据已导入）。
表结构与字段说明见 `campus-mart/sql/01-schema.sql`；精确列元数据可查 `information_schema.columns`。

## 2. 实体与 Mapper 已生成

`campus-modules/<模块>/src/main/java/<包>/domain/entity/*.java` 与 `mapper/*Mapper.java` 由脚本按数据库列自动生成（MyBatis-Plus，`@TableName` + `@TableId` + `@TableLogic` + `FieldFill`）。
- **不要手工改实体字段名**，字段与库列一一对应（camelCase ↔ snake_case）。
- 不要在业务模块里重复生成别的模块的实体；需要跨库表数据请用自定义 SQL（mapper XML）或 Feign。
- `sys_user_role`、`sys_role_menu` 两个中间表没有实体，用自定义 SQL 操作。

## 3. 统一返回与异常

```java
import com.campus.common.core.domain.Result;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.exception.ServiceException;

// 成功
return Result.ok(data);
return Result.ok("操作成功", data);
// 分页
PageQuery q = ...; Page<BizItem> page = mapper.selectPage(q.toPage(), wrapper);
return Result.ok(PageResult.of(page));
// 失败：抛异常，全局异常处理器已统一成 Result
throw new ServiceException("商品已被预订");
throw new ServiceException(400, "参数不合法");
```

`PageQuery` 字段：`current`（页码，从 1 开始）、`size`（每页条数，上限 100）、`orderBy`、`asc`。
Controller 直接接收：`@ModelAttribute PageQuery pageQuery`（前端传 `?current=1&size=10`）。

## 4. 登录用户与权限

网关 `AuthGlobalFilter` 校验 JWT 后把身份写入内部请求头，服务内用静态工具类读取（`UserContextInterceptor` 已自动装配）：

```java
import com.campus.common.security.SecurityUtils;
Long userId = SecurityUtils.getUserId();      // 可能为 null（未登录）
Long uid = SecurityUtils.requireUserId();     // 未登录抛 401
boolean admin = SecurityUtils.get().isAdmin();// admin 角色返回 true
```

接口级权限校验（后台管理接口用）：

```java
import com.campus.common.security.RequiresPerm;
@RequiresPerm("item:item:list")                       // 拥有任一即可
@RequiresPerm(value = {"item:item:edit"}, requireAll = true)
```

权限标识统一格式：`模块:资源:动作`，如 `item:item:list`、`item:item:audit`、`system:user:add`。
> 前端菜单/按钮的 `perms` 必须与此处一致。

## 5. 操作日志

后台管理写操作加注解，自动落库 `sys_oper_log`：

```java
import com.campus.common.log.OperLog;
@OperLog(title = "商品管理", businessType = 1)   // 1新增 2修改 3删除 4授权 5导出
```

## 6. 接口文档

Knife4j 已配置，每个 Controller 加 `@Tag`，每个方法加 `@Operation(summary = "...")`。
访问地址 `http://localhost:<port>/doc.html`。

## 7. URL 约定（网关前缀 vs 服务内路径）

网关按前缀转发并 `StripPrefix=1`：

| 前端调用 | 网关转发到 | 服务 | Controller 前缀 |
| --- | --- | --- | --- |
| `/api/auth/**` | `/auth/**` | campus-auth | `@RequestMapping("/auth")` |
| `/api/system/**` | `/system/**` | campus-system | `@RequestMapping("/system/...")` |
| `/api/item/**` | `/item/**` | campus-item | `@RequestMapping("/item/...")` |
| `/api/analysis/**` | `/analysis/**` | campus-analysis | `@RequestMapping("/analysis/...")` |
| `/api/ai/**` | `/ai/**` | campus-ai | `@RequestMapping("/ai/...")` |

服务内部接口（供其它微服务 Feign 调用）统一放 `/xxx/inner/**`，网关白名单不放行，只允许内网调用：

- `campus-system`：`/system/user/inner/login-info/{username}`、`/system/user/inner/login-info/id/{userId}`、`/system/user/inner/menu-ids/{userId}`、`/system/user/inner/record-login/{userId}?ip=`、`/system/menu/inner/tree/{userId}`
- `campus-item`：`/item/inner/item/{itemId}`、`/item/inner/item/on-sale?limit=`、`/item/inner/item/favorite/{userId}`
- `campus-ai`：`/ai/inner/moderate`

## 8. 编码与技术约定

- Java 17 + Spring Boot 3.2.5 + MyBatis-Plus 3.5.7；文件必须 UTF-8；允许中文注释。
- 金额一律 `BigDecimal`；时间 `LocalDateTime`（Jackson 已全局 `yyyy-MM-dd HH:mm:ss`）；Long 主键序列化成 String 防止前端精度丢失。
- 状态字段用 `char(1)`（`'0'` 正常 / `'1'` 停用）或 `tinyint`（业务状态），不要改成 boolean。
- 分页查列表用 MyBatis-Plus `LambdaQueryWrapper`；复杂统计用自定义 SQL（`@Select` 或 XML）。
- 缓存用 `com.campus.common.utils.RedisUtils`（key 统一 `业务:子业务:id`，如 `item:detail:12`）。
- 不要新增第三方依赖（需要就找 lead 加 pom）。

## 9. 编译验证（每个模块自行验证）

```bash
cd "D:/project/deepseek/person project/campus-mart"
mvn -o -q -s maven-settings.xml -pl campus-modules/campus-item -DskipTests compile
```

- 必须看到 `BUILD SUCCESS`（`-o` 离线，公共模块 jar 已在本地仓库）。
- **不要**跑全量 `mvn install`（lead 统一构建）；不要改父 pom。
- 不要启动 Spring Boot 服务（端口与数据由 lead 统一管理）；需要验证 SQL 可直接用 mysql 客户端跑。

## 10. 环境限制

- 沙箱内 `curl` / `pwsh` 的 HTTPS 不通；需要发 HTTP 请求请用 `node`（`fetch`/`http` 模块）。
- `git clone` 不可用；文件写入只允许在 workspace `D:\project\deepseek\person project` 内。
- 不要在 `ops_bash` 里用 `run_in_background` 跑常驻进程（会留下僵尸 bash 进程，导致后续命令报 `CreateFileMapping ... Win32 error 5`）。常驻进程一律交给 lead 用 pwsh `Start-Process` 启动。
