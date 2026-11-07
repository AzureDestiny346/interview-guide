package interview.guide.modules.auth.filter;

import interview.guide.modules.auth.config.JwtProperties;
import interview.guide.modules.auth.model.UserEntity;
import interview.guide.modules.auth.model.UserRole;
import interview.guide.modules.auth.service.JwtService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT 过滤器单元测试(纯对象,不依赖 Spring 上下文)
 */
@DisplayName("JWT 认证过滤器测试")
class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;
    private JwtService jwtService;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties());
        filter = new JwtAuthenticationFilter(jwtService);
        user = new UserEntity("tester", "any-hash", UserRole.USER);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        return request;
    }

    @Test
    @DisplayName("有效 token:写入 SecurityContext 与 userId attribute")
    void validTokenSetsAuthentication() throws ServletException, IOException {
        String token = jwtService.generateToken(user);
        MockHttpServletRequest request = requestWithToken(token);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("tester", auth.getName());
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertEquals("tester", request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE));
    }

    @Test
    @DisplayName("无 Authorization header:匿名放行,不设置认证")
    void noHeaderStaysAnonymous() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE));
    }

    @Test
    @DisplayName("非 Bearer 前缀的 header:忽略,不设置认证")
    void nonBearerHeaderIgnored() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("无效 token:不设置认证,请求继续走链")
    void invalidTokenContinuesChain() throws ServletException, IOException {
        MockHttpServletRequest request = requestWithToken("tampered.invalid.token");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // 链被继续调用(否则受保护端点会变成连接重置而非 401)
        assertFalse(chain.getRequest() == null);
    }
}
