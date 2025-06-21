package com.czdxwx.aiinterviewcoachbackend.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookmarkRequest {
    @NotNull
    private Boolean bookmarked; // true 为收藏, false 为取消收藏
}
