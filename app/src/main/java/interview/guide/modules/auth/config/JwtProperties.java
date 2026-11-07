package interview.guide.modules.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** HMAC-SHA256 签名密钥,至少 32 字节;生产环境通过 APP_JWT_SECRET 环境变量注入 */
    private String secret = "dev-only-jwt-secret-key-min-32-bytes!!";

    /** token 有效期(分钟),默认 12 小时 */
    private long expirationMinutes = 720;
}
