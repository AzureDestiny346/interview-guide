package interview.guide.modules.auth.service;

import interview.guide.modules.auth.model.UserRole;

/**
 * JWT 解析出的认证主体(用户名 + 角色 + 用户 ID)
 */
public record AuthPrincipal(String username, UserRole role, Long userId) {
}
