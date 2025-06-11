package com.czdxwx.aiinterviewcoachbackend.mapper.mysql;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.czdxwx.aiinterviewcoachbackend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;
import java.util.Set;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查找用户（忽略大小写），并加载其权限。
     * @param username 用户名
     * @return 包含用户和权限的Optional<User>
     */
    default Optional<User> findOneByUsernameWithAuthorities(String username) {
        // 使用 QueryWrapper 进行查询，MyBatis-Plus 会自动处理驼峰到下划线的转换（如果配置了）
        // 或者数据库本身不区分大小写（通常不推荐依赖这个）
        // 更稳妥的方式是数据库查询时转换大小写
        User user = selectOne(new QueryWrapper<User>()
                .apply("LOWER(username) = LOWER({0})", username)); // 确保数据库端也转小写比较
        if (user != null) {
            user.setAuthorities(findAuthoritiesByUserId(user.getId()));
        }
        return Optional.ofNullable(user);
    }

    /**
     * 根据邮箱查找用户（忽略大小写），并加载其权限。
     * @param email 邮箱
     * @return 包含用户和权限的Optional<User>
     */
    default Optional<User> findOneByEmailWithAuthorities(String email) {
        User user = selectOne(new QueryWrapper<User>()
                .apply("LOWER(email) = LOWER({0})", email)); // 确保数据库端也转小写比较
        if (user != null) {
            user.setAuthorities(findAuthoritiesByUserId(user.getId()));
        }
        return Optional.ofNullable(user);
    }

    /**
     * 根据用户ID查找用户所有权限。
     * @param userId 用户ID
     * @return 权限名称集合
     */
    @Select("SELECT authority_name FROM user_authority WHERE user_id = #{userId}")
    Set<String> findAuthoritiesByUserId(@Param("userId") Long userId);
}