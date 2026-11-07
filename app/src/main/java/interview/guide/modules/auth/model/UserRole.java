package interview.guide.modules.auth.model;

/**
 * 用户角色
 */
public enum UserRole {
    /** 普通用户 */
    USER,
    /** 管理员:可访问 LLM Provider 全局配置等管理端点 */
    ADMIN
}
