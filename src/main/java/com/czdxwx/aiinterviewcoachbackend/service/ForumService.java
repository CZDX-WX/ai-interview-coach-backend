package com.czdxwx.aiinterviewcoachbackend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.czdxwx.aiinterviewcoachbackend.service.dto.*;

import java.util.List;

public interface ForumService {
    List<ForumCategoryDto> getAllCategories();
    Page<ThreadSummaryDto> getThreadsByCategory(Long categoryId, Pageable pageable);
    ThreadDetailDto getThreadDetails(Long threadId, Pageable pageable);
    ThreadSummaryDto createThread(Long categoryId, CreateThreadRequestDto dto);
    PostDto createPost(Long threadId, CreatePostRequestDto dto);
}