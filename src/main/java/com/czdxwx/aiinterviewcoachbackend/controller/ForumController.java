package com.czdxwx.aiinterviewcoachbackend.controller;

import com.czdxwx.aiinterviewcoachbackend.service.ForumService;
import com.czdxwx.aiinterviewcoachbackend.service.dto.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/discussion")
public class ForumController {

    private final Logger log = LoggerFactory.getLogger(ForumController.class);
    private final ForumService forumService;

    public ForumController(ForumService forumService) {
        this.forumService = forumService;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<ForumCategoryDto>> getAllCategories() {
        log.debug("REST 请求: 获取所有论坛分类");
        return ResponseEntity.ok(forumService.getAllCategories());
    }

    @GetMapping("/categories/{categoryId}/threads")
    public ResponseEntity<Page<ThreadSummaryDto>> getThreadsByCategory(@PathVariable Long categoryId, Pageable pageable) {
        log.debug("REST 请求: 获取分类 {} 下的主题列表，分页信息: {}", categoryId, pageable);
        return ResponseEntity.ok(forumService.getThreadsByCategory(categoryId, pageable));
    }

    @GetMapping("/threads/{threadId}")
    public ResponseEntity<?> getThreadDetails(@PathVariable Long threadId, Pageable pageable) {
        log.debug("REST 请求: 获取主题 {} 的详情，帖子分页信息: {}", threadId, pageable);
        try {
            return ResponseEntity.ok(forumService.getThreadDetails(threadId, pageable));
        } catch (RuntimeException e) {
            log.warn("获取主题详情失败: {}", e.getMessage());
            // 返回一个包含错误信息的对象，方便前端处理
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/categories/{categoryId}/threads")
    @PreAuthorize("isAuthenticated()") // 确保只有登录用户才能发帖
    public ResponseEntity<?> createNewThread(@PathVariable Long categoryId, @Valid @RequestBody CreateThreadRequestDto dto) {
        log.debug("REST 请求: 在分类 {} 下创建新主题，标题: {}", categoryId, dto.getTitle());
        try {
            ThreadSummaryDto createdThread = forumService.createThread(categoryId, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdThread);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) { // 用户未登录等状态异常
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/threads/{threadId}/posts")
    @PreAuthorize("isAuthenticated()") // 确保只有登录用户才能回复
    public ResponseEntity<?> createNewPost(@PathVariable Long threadId, @Valid @RequestBody CreatePostRequestDto dto) {
        log.debug("REST 请求: 回复主题 {}", threadId);
        try {
            PostDto createdPost = forumService.createPost(threadId, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }
}