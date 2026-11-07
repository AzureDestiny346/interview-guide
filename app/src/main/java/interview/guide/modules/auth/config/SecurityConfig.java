package interview.guide.modules.auth.config;

import interview.guide.common.exception.ErrorCode;
import interview.guide.common.result.Result;
import interview.guide.modules.auth.filter.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

import java.io.IOException;

/**
 * Spring Security 配置(无状态 JWT)
 *
 * <p>响应格式约定:与项目全局约定一致,认证失败/无权限均返回 HTTP 200 +
 * Result{code:401/403},由前端按业务码处理(见 GlobalExceptionHandler)。
 *
 * <p>放行清单:
 * <ul>
 *   <li>/api/auth/** —— 注册与登录</li>
 *   <li>/ws/** —— WebSocket 握手(浏览器 WS API 无法携带自定义 header;
 *       会话级鉴权由后续 ticket 机制增强)</li>
 *   <li>Swagger 与 actuator 健康检查 —— 文档与探活</li>
 *   <li>OPTIONS 预检 —— CORS</li>
 * </ul>
 * 注意:SSE 端点走原生 fetch(可带 Authorization header),不做放行,按标准认证。
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsFilter corsFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // 无 cookie 会话,纯 token 认证,无 CSRF 面
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS 预检
                .requestMatchers("/api/auth/**").permitAll()            // 注册/登录
                .requestMatchers("/ws/**").permitAll()                  // WebSocket 握手
                .requestMatchers(
                    "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                    "/actuator/health/**", "/actuator/info").permitAll() // 文档与探活
                // LLM Provider 全局配置管理端点:仅管理员
                .requestMatchers("/api/llm-provider/**").hasRole("ADMIN")
                // 其余 /api/** 均需认证
                .requestMatchers("/api/**").authenticated()
                // 非 /api 路径(Spring 不承载前端静态资源,nginx 负责)
                .anyRequest().permitAll())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) ->
                    writeResult(response, ErrorCode.UNAUTHORIZED, "未登录或登录已过期"))
                .accessDeniedHandler((request, response, e) ->
                    writeResult(response, ErrorCode.FORBIDDEN, "无权限访问")))
            // 顺序:CORS 最外层 → JWT 认证 → UsernamePasswordAuthenticationFilter 位置
            .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 按项目约定输出 HTTP 200 + Result JSON(而非 Security 默认的 401/403 空响应)
     */
    private void writeResult(jakarta.servlet.http.HttpServletResponse response,
                             ErrorCode errorCode, String message) throws IOException {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(errorCode, message)));
    }
}
