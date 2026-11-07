package interview.guide.modules.auth.service;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.auth.dto.AuthResponse;
import interview.guide.modules.auth.dto.LoginRequest;
import interview.guide.modules.auth.dto.RegisterRequest;
import interview.guide.modules.auth.model.UserEntity;
import interview.guide.modules.auth.model.UserRole;
import interview.guide.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务:注册 + 登录
 *
 * <p>安全设计:
 * <ul>
 *   <li>密码 BCrypt 哈希存储,绝不落明文</li>
 *   <li>"用户不存在"与"密码错误"统一返回 AUTH_LOGIN_FAILED,不向外界暴露账号是否存在</li>
 *   <li>注册成功即签发 token(免二次登录)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * 注册:校验用户名唯一,BCrypt 加密落库,直接返回 token
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(ErrorCode.AUTH_USERNAME_EXISTS);
        }
        UserEntity user = new UserEntity(
                request.username(),
                passwordEncoder.encode(request.password()),
                UserRole.USER);
        UserEntity saved = userRepository.save(user);
        log.info("用户注册成功: username={}", saved.getUsername());
        return toResponse(saved);
    }

    /**
     * 登录:按用户名查库 → BCrypt 比对 → 校验账号状态 → 签发 token
     */
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_LOGIN_FAILED));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED);
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        log.info("用户登录成功: username={}", user.getUsername());
        return toResponse(user);
    }

    private AuthResponse toResponse(UserEntity user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                user.getUsername(),
                user.getRole().name());
    }
}
