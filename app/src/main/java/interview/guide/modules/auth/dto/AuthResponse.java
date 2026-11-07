package interview.guide.modules.auth.dto;

/**
 * 认证响应:token + 用户基本信息(注册/登录共用)
 */
public record AuthResponse(String token, String username, String role) {
}
