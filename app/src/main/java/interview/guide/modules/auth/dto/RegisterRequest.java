package interview.guide.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求
 */
public record RegisterRequest(
        /** 用户名:3-50 位字母/数字/下划线/中划线 */
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度须为 3-50 位")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "用户名只能包含字母、数字、下划线和 中划线")
        String username,

        /** 密码:6-64 位,BCrypt 加密存储 */
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度须为 6-64 位")
        String password) {
}
