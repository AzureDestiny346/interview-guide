package interview.guide.modules.auth.service;

import interview.guide.modules.auth.config.JwtProperties;
import interview.guide.modules.auth.model.UserEntity;
import interview.guide.modules.auth.model.UserRole;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JWT 服务单元测试(纯对象测试,不依赖 Spring 上下文)
 */
@DisplayName("JWT 服务测试")
class JwtServiceTest {

    private JwtService jwtService;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties());
        user = new UserEntity("tester", "any-hash", UserRole.ADMIN);
    }

    @Test
    @DisplayName("签发后可解析出用户名与角色(回环)")
    void generateThenParseRoundTrip() {
        String token = jwtService.generateToken(user);

        AuthPrincipal principal = jwtService.parse(token);

        assertNotNull(token);
        assertEquals("tester", principal.username());
        assertEquals(UserRole.ADMIN, principal.role());
    }

    @Test
    @DisplayName("过期 token 解析失败")
    void expiredTokenShouldFail() {
        JwtProperties props = new JwtProperties();
        props.setExpirationMinutes(-1); // 有效期为负 → 签发即过期
        JwtService expiredService = new JwtService(props);

        String token = expiredService.generateToken(user);

        assertThrows(ExpiredJwtException.class, () -> expiredService.parse(token));
    }

    @Test
    @DisplayName("篡改签名后解析失败")
    void tamperedTokenShouldFail() {
        String token = jwtService.generateToken(user);
        char last = token.charAt(token.length() - 1);
        char replacement = last == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, token.length() - 1) + replacement;

        assertThrows(JwtException.class, () -> jwtService.parse(tampered));
    }

    @Test
    @DisplayName("不同密钥签发的 token 解析失败")
    void wrongKeyTokenShouldFail() {
        String token = jwtService.generateToken(user);

        JwtProperties otherProps = new JwtProperties();
        otherProps.setSecret("another-secret-key-that-is-long-enough!!");
        JwtService otherService = new JwtService(otherProps);

        assertThrows(JwtException.class, () -> otherService.parse(token));
    }

    @Test
    @DisplayName("格式非法的字符串解析失败")
    void garbageTokenShouldFail() {
        assertThrows(JwtException.class, () -> jwtService.parse("not-a-jwt-token"));
    }
}
