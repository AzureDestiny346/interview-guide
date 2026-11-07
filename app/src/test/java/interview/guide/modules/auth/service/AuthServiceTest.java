package interview.guide.modules.auth.service;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.auth.dto.AuthResponse;
import interview.guide.modules.auth.dto.LoginRequest;
import interview.guide.modules.auth.dto.RegisterRequest;
import interview.guide.modules.auth.model.UserEntity;
import interview.guide.modules.auth.model.UserRole;
import interview.guide.modules.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 认证服务单元测试(Mockito,不依赖 Spring 上下文与数据库)
 *
 * <p>PasswordEncoder 使用真实 BCrypt 实例(验证哈希行为),仅 mock 仓储与 JWT。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("认证服务测试")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    private UserEntity persistedUser(String rawPassword) {
        UserEntity user = new UserEntity("tester", new BCryptPasswordEncoder().encode(rawPassword), UserRole.USER);
        user.setEnabled(true);
        return user;
    }

    @Test
    @DisplayName("注册成功:密码 BCrypt 加密落库并返回 token")
    void registerSuccess() {
        when(userRepository.existsByUsername("tester")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(UserEntity.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(new RegisterRequest("tester", "secret123"));

        assertEquals("jwt-token", response.token());
        assertEquals("tester", response.username());
        assertEquals("USER", response.role());
    }

    @Test
    @DisplayName("注册时密码必须加密存储(与明文不同且可被 BCrypt 校验)")
    void registerShouldHashPassword() {
        when(userRepository.existsByUsername("tester")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(UserEntity.class))).thenReturn("jwt-token");

        authService.register(new RegisterRequest("tester", "secret123"));

        var savedCaptor = org.mockito.ArgumentCaptor.forClass(UserEntity.class);
        org.mockito.Mockito.verify(userRepository).save(savedCaptor.capture());
        String stored = savedCaptor.getValue().getPasswordHash();
        assertNotEquals("secret123", stored);
        assertTrue(new BCryptPasswordEncoder().matches("secret123", stored));
    }

    @Test
    @DisplayName("注册用户名重复抛 AUTH_USERNAME_EXISTS")
    void registerDuplicateUsername() {
        when(userRepository.existsByUsername("tester")).thenReturn(true);

        BusinessException e = assertThrows(BusinessException.class,
                () -> authService.register(new RegisterRequest("tester", "secret123")));
        assertEquals(ErrorCode.AUTH_USERNAME_EXISTS.getCode(), e.getCode());
    }

    @Test
    @DisplayName("登录成功返回 token")
    void loginSuccess() {
        UserEntity user = persistedUser("secret123");
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("tester", "secret123"));

        assertEquals("jwt-token", response.token());
        assertEquals("tester", response.username());
    }

    @Test
    @DisplayName("用户不存在与密码错误统一抛 AUTH_LOGIN_FAILED(不暴露账号是否存在)")
    void loginShouldNotRevealUserExistence() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        BusinessException notFound = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("nobody", "whatever123")));
        assertEquals(ErrorCode.AUTH_LOGIN_FAILED.getCode(), notFound.getCode());

        UserEntity user = persistedUser("secret123");
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(user));
        BusinessException wrongPassword = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("tester", "wrong-password")));
        assertEquals(ErrorCode.AUTH_LOGIN_FAILED.getCode(), wrongPassword.getCode());
    }

    @Test
    @DisplayName("账号禁用抛 AUTH_ACCOUNT_DISABLED")
    void loginDisabledAccount() {
        UserEntity user = persistedUser("secret123");
        user.setEnabled(false);
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(user));

        BusinessException e = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("tester", "secret123")));
        assertEquals(ErrorCode.AUTH_ACCOUNT_DISABLED.getCode(), e.getCode());
    }
}
