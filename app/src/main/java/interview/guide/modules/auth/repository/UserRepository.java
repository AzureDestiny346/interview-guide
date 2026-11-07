package interview.guide.modules.auth.repository;

import interview.guide.modules.auth.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 用户数据访问接口
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /** 按用户名查询(登录与重名校验) */
    Optional<UserEntity> findByUsername(String username);

    /** 注册时快速判断用户名是否已占用 */
    boolean existsByUsername(String username);
}
