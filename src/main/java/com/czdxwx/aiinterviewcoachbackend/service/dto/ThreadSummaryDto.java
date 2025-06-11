package com.czdxwx.aiinterviewcoachbackend.service.dto;

import com.czdxwx.aiinterviewcoachbackend.service.dto.AuthorInfoDto;
import lombok.Data;
import java.time.Instant;

@Data
public class ThreadSummaryDto {
    private String id;
    private String title;
    private String categoryId;
    private AuthorInfoDto author; // <-- 使用共享的 AuthorInfoDto
    private Instant createdAt;
    private Integer replyCount;
    private Integer viewCount;
    private Instant lastReplyAt;
    private AuthorInfoDto lastReplyAuthor; // <-- 使用共享的 AuthorInfoDto
    private Boolean isPinned;
    private Boolean isLocked;

    // 移除内部的 public static class AuthorInfo
}