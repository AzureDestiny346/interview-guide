package interview.guide.modules.auth.filter;

import interview.guide.modules.auth.service.AuthPrincipal;
import interview.guide.modules.auth.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器:解析 Authorization: Bearer &lt;token&gt;,写入 SecurityContext
 *
 * <p>设计要点:
 * <ul>
 *   <li>无 header 或解析失败时不阻断请求(匿名放行),由授权规则决定 401——
 *       这样放行清单里的端点(auth/ws/swagger)不受影响</li>
 *   <li>认证成功后把用户名写入 request attribute "userId",
 *       供 RateLimitAspect 做按用户限流(替代原先可伪造的 X-User-Id header)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** RateLimitAspect 约定的用户维度 attribute 名 */
    public static final String USER_ID_ATTRIBUTE = "userId";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AuthPrincipal principal = jwtService.parse(header.substring(BEARER_PREFIX.length()));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal.username(), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // 限流挂接点:认证用户的请求按用户名限流
            request.setAttribute(USER_ID_ATTRIBUTE, principal.username());
        } catch (JwtException e) {
            // token 无效/过期:不设置认证,继续链;受保护端点由 entryPoint 统一输出 401
            log.debug("JWT 解析失败: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
