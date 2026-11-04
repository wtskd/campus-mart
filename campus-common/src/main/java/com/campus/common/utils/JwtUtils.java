package com.campus.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具：签发 / 解析 / 校验
 *
 * <p>令牌只承载身份标识（userId、username、jti），权限每次从 Redis 缓存的登录态读取，
 * 这样管理员改权限后可以立即失效，无需等令牌过期。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Component
public class JwtUtils {

    /** 签名密钥，生产环境请通过配置中心/环境变量注入，长度需 >= 32 字节 */
    @Value("${campus.jwt.secret:CampusMart@2026#JwtSecretKeyForDemoOnly!}")
    private String secret;

    /** 过期秒数，默认 2 小时 */
    @Value("${campus.jwt.expire:7200}")
    private long expireSeconds;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    /**
     * 签发令牌
     */
    public String createToken(Long userId, String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireSeconds * 1000);
        Map<String, Object> claims = new HashMap<>(4);
        claims.put("userId", userId);
        claims.put("username", username);
        return Jwts.builder()
                .claims(claims)
                .id(String.valueOf(userId))
                .subject(username)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key())
                .compact();
    }

    /**
     * 解析令牌，失败返回 null
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    public Long getUserId(String token) {
        Claims c = parse(token);
        if (c == null) {
            return null;
        }
        Object v = c.get("userId");
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }

    public boolean validate(String token) {
        Claims c = parse(token);
        return c != null && c.getExpiration() != null && c.getExpiration().after(new Date());
    }
}
