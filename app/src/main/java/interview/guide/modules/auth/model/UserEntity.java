package interview.guide.modules.auth.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 用户实体(Spring Security + JWT 认证)
 *
 * <p>设计决策:认证先行,业务数据(简历/面试/知识库)暂不加归属字段,
 * 后续做多租户隔离时再扩展。
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_username", columnList = "username", unique = true)
})
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 登录用户名,全局唯一
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // BCrypt 哈希后的密码(60 字符),绝不存明文
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    // 角色:普通用户 / 管理员
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    // 账号是否可用(禁用后即使密码正确也拒绝登录)
    @Column(nullable = false)
    private Boolean enabled = true;

    // 注册时间
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public UserEntity() {
    }

    public UserEntity(String username, String passwordHash, UserRole role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = true;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
