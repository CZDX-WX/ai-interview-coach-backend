package com.czdxwx.aiinterviewcoachbackend.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequestDto {
    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "新密码长度必须在6到100个字符之间")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmNewPassword;
}