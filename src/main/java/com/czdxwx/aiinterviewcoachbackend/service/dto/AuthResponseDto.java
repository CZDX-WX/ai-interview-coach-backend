package com.czdxwx.aiinterviewcoachbackend.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Data
public class AuthResponseDto {
    private String accessToken;
    private String tokenType = "Bearer";
    private String userId;
    private String username;
    private String email;
    private String fullName;
    private Set<String> authorities;

    public AuthResponseDto(String jwt, String id, String username, String email, String fullName, Set<String> collect) {
        this.accessToken = jwt;
        this.userId = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.authorities = collect;
    }
    // 可以添加其他需要在登录后立即返回给前端的用户信息
    
    
}
