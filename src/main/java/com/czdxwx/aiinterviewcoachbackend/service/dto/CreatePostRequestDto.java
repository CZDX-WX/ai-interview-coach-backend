package com.czdxwx.aiinterviewcoachbackend.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePostRequestDto {
    @NotBlank(message = "回复内容不能为空")
    @Size(min = 1, max = 5000, message = "内容长度需在1到5000个字符之间")
    private String content;
    // threadId 将从 URL 路径中获取
}