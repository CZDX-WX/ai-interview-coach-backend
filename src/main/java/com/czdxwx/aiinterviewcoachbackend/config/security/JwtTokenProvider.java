package com.czdxwx.aiinterviewcoachbackend.config.security;


import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey key() {
        byte[] keyBytes = Decoders.BASE64.decode(this.jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        log.debug("开始为认证对象生成JWT Token...");

        // **核心修正点**: 将 Principal 转换为我们自定义的 CustomUserDetails 类型
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();

        String username = userPrincipal.getUsername();
        String authorities = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        // (推荐) 从 CustomUserDetails 中获取用户ID并添加到Token中
        Long userId = userPrincipal.getId();

        log.debug("从Principal中提取的用户信息 -> 用户名: {}, 权限: {}, 用户ID: {}", username, authorities, userId);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        String token = Jwts.builder()
                .setSubject(username) // JWT 的主题，通常是用户名
                .claim("auth", authorities) // 自定义声明，存放权限信息
                .claim("userId", userId) // **新增**: 将用户ID存入Token，方便后端其他服务快速获取
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key(), SignatureAlgorithm.HS512)
                .compact();

        log.debug("JWT Token 生成完毕 (部分): {}...", token.substring(0, Math.min(20, token.length())));
        return token;
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // 新增一个方法，用于从JWT中获取用户ID
    public Long getUserIdFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", Long.class);
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("无效的JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("JWT token已过期: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("不支持的JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims字符串为空: {}", ex.getMessage());
        } catch (JwtException ex) {
            log.error("JWT验证失败: {}", ex.getMessage());
        }
        return false;
    }
}