package com.czdxwx.aiinterviewcoachbackend.service.dto;

import com.czdxwx.aiinterviewcoachbackend.service.dto.PostDto;
import com.czdxwx.aiinterviewcoachbackend.service.dto.ThreadSummaryDto;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
public class ThreadDetailDto {
    private ThreadSummaryDto threadInfo;
    private Page<PostDto> posts; // 包含分页信息的帖子列表
}