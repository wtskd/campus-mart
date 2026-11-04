package com.campus.gateway.filter;

import com.campus.gateway.util.JwtParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 全局鉴权过滤器
 *
 * <p>职责：<br>
 * 1. 白名单放行；<br>
 * 2. 校验 JWT 签名与有效期；<br>
 * 3. 从 Redis 读取登录态（支持退出登录即时失效、权限变更即时生效）；<br>
 * 4. 把用户身份写入内部请求头，向下游服务透传。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** 登录态 Redis Key 前缀，与 campus-auth 保持一致 */
    public static final String LOGIN_TOKEN_KEY = "login:token:";

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** Redis 未命中时的哨兵值（用 defaultIfEmpty 转换，避免 switchIfEmpty 误伤 Mono&lt;Void&gt;） */
    private static final String EMPTY_TOKEN_STATE = "__NO_LOGIN_STATE__";

    /** 完全放行的路径（含登录、验证码、接口文档） */
    private static final List<String> WHITE_LIST = List.of(
            "/api/auth/login",
            "/api/auth/app/login",
            "/api/auth/captcha",
            "/upload/**",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/favicon.ico",
            "/actuator/**"
    );

    /** 用户端只读浏览接口（未登录也能看）：只放行 GET */
    private static final List<String> APP_READ_PATHS = List.of(
            "/api/item/app/item/**",
            "/api/item/app/category/**",
            "/api/item/app/banner/**",
            "/api/item/app/notice/**",
            "/api/item/app/comment/**",
            "/api/item/app/home/**",
            "/api/ai/recommend"
    );

    /**
     * 带这些后缀的路径必须登录，即使前缀落在 APP_READ_PATHS 里。
     * 例：/api/item/app/item/my 命中 /api/item/app/item/**，但它属于「我的商品」，必须带令牌。
     */
    private static final List<String> LOGIN_REQUIRED_SUFFIXES = List.of(
            "/my", "/unread-count", "/read", "/read-all", "/mine"
    );

    private static final String[] SPOOF_HEADERS = {
            "X-User-Id", "X-Username", "X-Nickname", "X-Dept-Id", "X-Dept-Name", "X-Roles", "X-Perms"
    };

    private final JwtParser jwtParser;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 跨域预检直接放行
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }
        if (isWhite(path, request.getMethod())) {
            return chain.filter(removeSpoofHeaders(exchange));
        }

        String token = resolveToken(request);
        if (token == null) {
            return unauthorized(exchange, "未登录，请先登录");
        }
        Long userId = jwtParser.getUserId(token);
        if (userId == null || !jwtParser.isValid(token)) {
            return unauthorized(exchange, "登录已过期，请重新登录");
        }

        String key = LOGIN_TOKEN_KEY + userId;
        // 注意：chain.filter(...) 返回的 Mono<Void> 本身就是"空"的，
        // 若把 switchIfEmpty 挂在 flatMap 之后，响应写完也会被误判为"无登录态"而再写 401，
        // 导致 ReadOnlyHttpHeaders.set 抛 UnsupportedOperationException、连接被关闭。
        // 因此先用 defaultIfEmpty 把 Redis 未命中转成哨兵值，再在 flatMap 内部处理。
        return redisTemplate.opsForValue().get(key)
                .defaultIfEmpty(EMPTY_TOKEN_STATE)
                .flatMap(json -> {
                    if (EMPTY_TOKEN_STATE.equals(json)) {
                        return unauthorized(exchange, "登录已过期，请重新登录");
                    }
                    try {
                        JsonNode node = objectMapper.readTree(json);
                        ServerWebExchange mutated = withUserHeaders(exchange, node);
                        return chain.filter(mutated);
                    } catch (Exception e) {
                        log.warn("解析登录态失败: {}", e.getMessage());
                        return unauthorized(exchange, "登录状态异常，请重新登录");
                    }
                });
    }

    private ServerWebExchange removeSpoofHeaders(ServerWebExchange exchange) {
        ServerHttpRequest req = exchange.getRequest().mutate()
                .headers(h -> {
                    for (String name : SPOOF_HEADERS) {
                        h.remove(name);
                    }
                })
                .build();
        return exchange.mutate().request(req).build();
    }

    /** 把登录态写入请求头（中文做 URL 编码，避免 HTTP 头编码问题） */
    private ServerWebExchange withUserHeaders(ServerWebExchange exchange, JsonNode node) {
        String userId = text(node, "userId", "");
        String username = text(node, "username", "");
        String nickname = encode(text(node, "nickname", ""));
        String deptId = text(node, "deptId", "");
        String deptName = encode(text(node, "deptName", ""));
        String roles = String.join(",", array(node, "roles"));
        String perms = String.join(",", array(node, "perms"));

        ServerHttpRequest req = exchange.getRequest().mutate()
                .headers(h -> {
                    for (String name : SPOOF_HEADERS) {
                        h.remove(name);
                    }
                    h.set("X-User-Id", userId);
                    h.set("X-Username", username);
                    h.set("X-Nickname", nickname);
                    h.set("X-Dept-Id", deptId);
                    h.set("X-Dept-Name", deptName);
                    h.set("X-Roles", roles);
                    h.set("X-Perms", perms);
                    h.set("X-Real-IP", clientIp(exchange));
                })
                .build();
        return exchange.mutate().request(req).build();
    }

    private String text(JsonNode node, String field, String def) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? def : v.asText();
    }

    private List<String> array(JsonNode node, String field) {
        List<String> list = new ArrayList<>();
        JsonNode v = node.get(field);
        if (v != null && v.isArray()) {
            v.forEach(e -> list.add(e.asText()));
        }
        return list;
    }

    private String encode(String v) {
        return v == null ? "" : URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    private boolean isWhite(String path, HttpMethod method) {
        for (String p : WHITE_LIST) {
            if (MATCHER.match(p, path)) {
                return true;
            }
        }
        if (HttpMethod.GET.equals(method)) {
            if (isLoginRequired(path)) {
                return false;
            }
            for (String p : APP_READ_PATHS) {
                if (MATCHER.match(p, path)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 只读通配前缀下仍强制登录的子路径（/my、/unread-count ...） */
    private boolean isLoginRequired(String path) {
        String p = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        for (String suffix : LOGIN_REQUIRED_SUFFIXES) {
            if (p.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private String resolveToken(ServerHttpRequest request) {
        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        return request.getQueryParams().getFirst("token");
    }

    private String clientIp(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String xff = headers.getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress() == null ? "127.0.0.1"
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        // 响应已开始写出时不允许再改头，直接结束，避免 UnsupportedOperationException
        if (response.isCommitted()) {
            return response.setComplete();
        }
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"msg\":\"" + msg + "\",\"data\":null,\"timestamp\":"
                + System.currentTimeMillis() + "}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
