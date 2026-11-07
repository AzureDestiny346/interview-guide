package interview.guide.modules.auth.service;

import interview.guide.modules.auth.config.JwtProperties;
import interview.guide.modules.auth.model.UserEntity;
import interview.guide.modules.auth.model.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * JWT 签发与校验服务(HMAC-SHA256 无状态 token)
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = properties.getExpirationMinutes();
    }

    /**
     * 为用户签发 token:subject 存用户名,role/uid 存自定义 claim
     */
    public String generateToken(UserEntity user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .claim("uid", user.getId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验 token(签名 + 有效期)。
     * 签名不符抛 SignatureException,已过期抛 ExpiredJwtException,格式非法抛 JwtException,
     * 均为 io.jsonwebtoken.JwtException 子类,由调用方统一处理。
     */
    public AuthPrincipal parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        return new AuthPrincipal(
                claims.getSubject(),
                UserRole.valueOf(claims.get("role", String.class)),
                claims.get("uid", Long.class));
    }
}
