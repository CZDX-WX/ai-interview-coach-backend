package com.czdxwx.aiinterviewcoachbackend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.czdxwx.aiinterviewcoachbackend.config.security.CustomUserDetails;
import com.czdxwx.aiinterviewcoachbackend.service.AsyncTaskManager;
import com.czdxwx.aiinterviewcoachbackend.service.QuestionGenerationService;
import com.czdxwx.aiinterviewcoachbackend.service.QuestionQueryService;
import com.czdxwx.aiinterviewcoachbackend.service.dto.ProgressUpdateDTO;
import com.czdxwx.aiinterviewcoachbackend.service.dto.QuestionGenerationRequest;
import com.czdxwx.aiinterviewcoachbackend.service.dto.QuestionSearchRequest;
import com.czdxwx.aiinterviewcoachbackend.vo.QuestionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

/**
 * 负责所有与面试题资源相关的核心操作，如生成和搜索。
 * 注意：与用户个人练习状态相关的接口已移至 UserPracticeController。
 */
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionGenerationService generationService;
    private final QuestionQueryService queryService;
    private final AsyncTaskManager asyncTaskManager;

    // ====================================================================
    //              写操作接口 (Write Operations) - 异步任务
    // ====================================================================

    /**
     * 【管理员接口】触发一个异步的、后台的公共题目生成任务。
     * @param request 包含题目生成要求的请求体
     * @return 立即返回一个包含任务ID的响应，前端可用此ID监听WebSocket或轮询状态。
     */
    @PostMapping("/generate-public")
    public ResponseEntity<Map<String, String>> generatePublicQuestions(@Valid @RequestBody QuestionGenerationRequest request) {
        String taskId = UUID.randomUUID().toString();
        // 注册任务，以便可以查询进度
        asyncTaskManager.register(taskId);
        // 调用异步服务，此方法会立即返回，不会等待任务完成
        generationService.populatePublicQuestions(request, taskId);
        return ResponseEntity.ok(Map.of("taskId", taskId, "message", "公共题目生成任务已在后台启动。"));
    }

    /**
     * 【用户接口】为当前登录用户触发一个异步的个性化题目生成任务。
     * @param request 包含题目生成要求的请求体
     * @param currentUser 由 Spring Security 注入的当前用户信息
     * @return 立即返回一个包含任务ID的响应
     */
    @PostMapping("/generate-personalized-async")
    public ResponseEntity<Map<String, String>> generatePersonalizedQuestions(
            @Valid @RequestBody QuestionGenerationRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        String taskId = UUID.randomUUID().toString();
        asyncTaskManager.register(taskId);
        generationService.generatePersonalizedQuestions(request, currentUser.getId(), taskId);

        return ResponseEntity.ok(Map.of("taskId", taskId, "message", "个性化题目生成任务已在后台启动。"));
    }

    // ====================================================================
    //              读操作与状态查询接口 (Read & Status Operations)
    // ====================================================================

    /**
     * 【核心查询接口】统一的、强大的题目搜索接口。
     * 支持按岗位、按标签（与/或逻辑）、分页等多种组合查询。
     * @param request 包含所有查询条件的请求体
     * @return 分页的题目结果
     */
    @PostMapping("/search")
    public ResponseEntity<IPage<QuestionVO>> searchQuestions(
            @RequestBody QuestionSearchRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        // 【核心修正】如果用户已登录，将其ID设置到搜索请求中
        if (currentUser != null) {
            request.setUserId(currentUser.getId());
        }

        return ResponseEntity.ok(queryService.searchQuestions(request));
    }

    /**
     * 获取单个题目的详细信息。
     * @param id 题目的ID
     * @return 单个题目详情VO，包含所有标签
     */
    @GetMapping("/{id}")
    public ResponseEntity<QuestionVO> getQuestionById(@PathVariable Long id) {
        return queryService.findByIdWithTags(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 【新增】根据任务ID查询一个异步生成任务的当前状态。
     * 供前端在WebSocket连接中断或刷新页面后，轮询任务结果使用。
     * @param taskId 任务ID
     * @return 任务的实时进度信息
     */
    @GetMapping("/generation-task/{taskId}")
    public ResponseEntity<ProgressUpdateDTO> getGenerationTaskStatus(@PathVariable String taskId) {
        return asyncTaskManager.getTaskStatus(taskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build()); // 如果任务ID不存在，返回404
    }

}