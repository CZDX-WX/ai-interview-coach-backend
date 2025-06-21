package com.czdxwx.aiinterviewcoachbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.czdxwx.aiinterviewcoachbackend.config.security.CustomUserDetails;
import com.czdxwx.aiinterviewcoachbackend.config.security.JwtTokenProvider;
import com.czdxwx.aiinterviewcoachbackend.entity.User;
import com.czdxwx.aiinterviewcoachbackend.mapper.UserAuthorityMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.UserMapper;
import com.czdxwx.aiinterviewcoachbackend.service.AuthService;
import com.czdxwx.aiinterviewcoachbackend.service.dto.AuthResponseDto;
import com.czdxwx.aiinterviewcoachbackend.service.dto.LoginRequestDto;
import com.czdxwx.aiinterviewcoachbackend.service.dto.RegisterRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager; // 用于登录认证
    private final JwtTokenProvider jwtTokenProvider;       // 用于生成Token
    private final UserAuthorityMapper userAuthorityMapper; // 直接注入 Mapper

    public AuthServiceImpl(UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtTokenProvider jwtTokenProvider,
                           // SqlSessionFactory sqlSessionFactory, // 如果直接调用 mapper，这个可能不再需要给这个方法
                           UserAuthorityMapper userAuthorityMapper) { // 注入 UserAuthorityMapper
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        // this.sqlSessionFactory = sqlSessionFactory;
        this.userAuthorityMapper = userAuthorityMapper; // 赋值
    }

    @Override
    @Transactional
    public User register(RegisterRequestDto registerRequestDto) {
        log.info("尝试注册新用户: {}", registerRequestDto.getUsername());

        // 转换为小写进行比较，以支持不区分大小写的唯一性检查（数据库层面也应有相应约束）
        String lowerUsername = registerRequestDto.getUsername().toLowerCase();
        String lowerEmail = registerRequestDto.getEmail().toLowerCase();

        if (userMapper.selectCount(new QueryWrapper<User>().apply("LOWER(username) = {0}", lowerUsername)) > 0) {
            log.warn("注册失败：用户名 '{}' 已被占用", registerRequestDto.getUsername());
            throw new IllegalArgumentException("用户名 \"" + registerRequestDto.getUsername() + "\" 已被占用。");
        }
        if (userMapper.selectCount(new QueryWrapper<User>().apply("LOWER(email) = {0}", lowerEmail)) > 0) {
            log.warn("注册失败：邮箱 '{}' 已被注册", registerRequestDto.getEmail());
            throw new IllegalArgumentException("邮箱 \"" + registerRequestDto.getEmail() + "\" 已被注册。");
        }

        User user = new User();
        user.setUsername(registerRequestDto.getUsername());
        user.setEmail(registerRequestDto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequestDto.getPassword()));
        user.setFullName(registerRequestDto.getFullName());
        user.setMajor(registerRequestDto.getMajor()); // 假设 DTO 有此字段
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        try {
            int inserted = userMapper.insert(user); // MyBatis-Plus 会自动设置自增ID到 user 对象
            if (inserted == 0 || user.getId() == null) {
                throw new RuntimeException("创建用户失败，未能获取用户ID。");
            }

            // 分配默认角色 "ROLE_USER"
            String defaultAuthority = "ROLE_USER";
            userAuthorityMapper.insertUserAuthority(user.getId(), defaultAuthority);

            // 为了返回给调用者，我们可以设置 authorities 字段
            Set<String> authorities = new HashSet<>();
            authorities.add(defaultAuthority);
            user.setAuthorities(authorities);

            log.info("用户 '{}' (ID: {}) 注册成功，并赋予角色: {}", user.getUsername(), user.getId(), defaultAuthority);
            return user;
        } catch (DataIntegrityViolationException e) {
            log.error("注册用户时发生数据完整性冲突: {}", e.getMessage());
            // 这个错误通常是由于 unique 约束 (如用户名或邮箱已存在) 引起的，
            // 尽管我们前面已经检查过了，但在并发情况下仍可能发生。
            throw new IllegalArgumentException("注册失败，用户名或邮箱可能已被占用。请尝试其他名称或邮箱。");
        } catch (Exception e) {
            log.error("注册用户时发生未知错误: {}", e.getMessage(), e);
            throw new RuntimeException("注册过程中发生未知错误，请稍后再试。");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto loginRequestDto) {
        log.info("尝试为 '{}' 进行认证", loginRequestDto.getUsernameOrEmail());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequestDto.getUsernameOrEmail(),
                        loginRequestDto.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("SecurityContext已设置");

        String jwt = jwtTokenProvider.generateToken(authentication);
        log.info("JWT 生成成功");

        // **核心修正点**: 将 Principal 转换为我们自己的 CustomUserDetails
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // **优化**: 不再需要重新查询数据库！所有需要的信息都在 userDetails 中
        log.info("用户 '{}' 登录流程即将完成，准备返回 DTO", userDetails.getUsername());

        return new AuthResponseDto(
                jwt,
                String.valueOf(userDetails.getId()), // 直接从 userDetails 获取 ID
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getFullName(),
                userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet())
        );
    }
}