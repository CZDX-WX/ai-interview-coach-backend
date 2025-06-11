package com.czdxwx.aiinterviewcoachbackend.config.security;

import com.czdxwx.aiinterviewcoachbackend.entity.User;
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

        // **核心修正点**：
        // authentication.getPrincipal() 返回的是 UserDetails 接口的实例，
        // 在我们的配置中，它是由 CustomUserDetailsService 创建的 org.springframework.security.core.userdetails.User 对象。
        org.springframework.security.core.userdetails.User userPrincipal =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

        String username = userPrincipal.getUsername(); // 这个 username 是您 User 实体中的 username 字段值
        String authorities = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        log.debug("从Principal中提取的用户名: {}", username);
        log.debug("从Principal中提取的权限: {}", authorities);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        String token = Jwts.builder()
                .setSubject(username) // JWT 的主题，通常是用户名
                .claim("auth", authorities) // 自定义声明，存放权限信息
                // 您也可以在这里添加其他不敏感的用户信息作为 claim，例如用户ID
                // .claim("userId", yourDomainUser.getId()) // 如果需要，但要先获取到您的领域User对象
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
        } catch (JwtException ex) { // 更广泛的JWT相关异常
            log.error("JWT验证失败 (可能是签名问题或其他): {}", ex.getMessage());
        }
        return false;
    }
}
