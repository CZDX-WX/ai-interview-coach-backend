package com.czdxwx.aiinterviewcoachbackend.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDto {
    @NotBlank(message = "登录凭证不能为空 (用户名或邮箱)")
    private String usernameOrEmail;

    @NotBlank(message = "密码不能为空")
    private String password;
}