package com.czdxwx.aiinterviewcoachbackend.service.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class ForumCategoryDto {
    private String id;
    private String name;
    private String description;
    private Integer threadCount;
    private Integer postCount;
    private LastThreadInfo lastThread;

    @Data
    public static class LastThreadInfo {
        private String threadId;
        private String title;
        private String authorName;
        private Instant timestamp;
    }
}