package com.czdxwx.aiinterviewcoachbackend.service.impl;

import com.czdxwx.aiinterviewcoachbackend.entity.User;
import com.czdxwx.aiinterviewcoachbackend.mapper.mysql.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service("userDetailsService")
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private final UserMapper userMapper;

    public CustomUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        log.debug("尝试通过 '{}' 加载用户", usernameOrEmail);
        final String lowerUsernameOrEmail = usernameOrEmail.toLowerCase();

        // 1. 尝试按用户名查找
        Optional<User> userOptional = userMapper.findOneByUsernameWithAuthorities(lowerUsernameOrEmail);

        if (!userOptional.isPresent()) {
            // 2. 如果按用户名找不到，再尝试按邮箱查找
            log.debug("按用户名 '{}' 未找到用户，尝试按邮箱查找", lowerUsernameOrEmail);
            userOptional = userMapper.findOneByEmailWithAuthorities(lowerUsernameOrEmail);
        }

        User user = userOptional.orElseThrow(() -> {
            log.warn("数据库中未找到用户: '{}'", lowerUsernameOrEmail);
            return new UsernameNotFoundException("用户 " + usernameOrEmail + " 未找到，或凭据无效。");
        });

        if (!user.isEnabled()) {
            log.warn("用户 '{}' 已被禁用", lowerUsernameOrEmail);
            throw new UsernameNotFoundException("用户 " + usernameOrEmail + " 已被禁用。");
        }

        Set<GrantedAuthority> grantedAuthorities = user.getAuthorities().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        log.debug("为用户 '{}' 加载的权限: {}", user.getUsername(), grantedAuthorities);

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), // **重要**: Spring Security UserDetails 的 username 字段应使用数据库中唯一的用户名
                user.getPasswordHash(),
                user.isEnabled(),
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                grantedAuthorities
        );
    }
}
