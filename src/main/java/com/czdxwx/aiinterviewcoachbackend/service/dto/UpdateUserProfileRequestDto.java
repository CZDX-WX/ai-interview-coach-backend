package com.czdxwx.aiinterviewcoachbackend.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserProfileRequestDto {
    // username 通常不允许修改，或有单独流程

    @Size(max = 100, message = "姓名长度不能超过100个字符")
    private String fullName;

    @Email(message = "邮箱格式不正确") // 如果允许修改邮箱，需要校验是否已被占用
    @Size(max = 254)
    private String email;

    @Size(max = 256)
    private String avatarUrl; // 前端可能只传递URL，或由专门的头像上传接口处理

    @Size(max = 255)
    private String school;

    @Size(max = 255)
    private String major;

    @Size(max = 10)
    private String graduationYear;
}