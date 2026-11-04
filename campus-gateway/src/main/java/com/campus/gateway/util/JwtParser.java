package com.campus.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 网关侧 JWT 解析（与 campus-common 的 JwtUtils 保持同一密钥）
 *
 * @author campus-mart
 */
@Component
public class JwtParser {

    @Value("${campus.jwt.secret:CampusMart@2026#JwtSecretKeyForDemoOnly!}")
    private String secret;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 解析令牌，无效返回 null */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key()).build()
                    .parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isValid(String token) {
        Claims c = parse(token);
        return c != null && c.getExpiration() != null && c.getExpiration().after(new Date());
    }

    public Long getUserId(String token) {
        Claims c = parse(token);
        if (c == null) {
            return null;
        }
        Object v = c.get("userId");
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }
}
