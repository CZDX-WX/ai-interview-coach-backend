package com.czdxwx.aiinterviewcoachbackend.service.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class PostDto {
    private String id;
    private AuthorInfoDto author; // <-- 使用共享的 AuthorInfoDto
    private String content;
    private Instant createdAt;
    private Boolean isOp;

    // 移除内部的 public static class AuthorInfo
}