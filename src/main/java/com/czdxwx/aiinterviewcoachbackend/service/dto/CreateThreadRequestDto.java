package com.czdxwx.aiinterviewcoachbackend.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateThreadRequestDto {
    @NotBlank(message = "主题标题不能为空")
    @Size(min = 5, max = 100, message = "标题长度需在5到100个字符之间")
    private String title;

    @NotBlank(message = "帖子内容不能为空")
    @Size(min = 10, max = 10000, message = "内容长度需在10到10000个字符之间")
    private String content;

    // categoryId 将从 URL 路径中获取，而不是请求体
}