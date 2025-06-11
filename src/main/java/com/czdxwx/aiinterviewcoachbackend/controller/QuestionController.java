package com.czdxwx.aiinterviewcoachbackend.controller;

import com.czdxwx.aiinterviewcoachbackend.service.QuestionGenerationService;
import com.czdxwx.aiinterviewcoachbackend.service.UserPracticeService;
import com.czdxwx.aiinterviewcoachbackend.service.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionGenerationService generationService;
    private final UserPracticeService practiceService;

    /**
     * AI 生成新的面试题
     * 如果发生异常，会被 GlobalExceptionHandler 自动捕获
     */
    @PostMapping("/generate")
    public ResponseEntity<QuestionGenerationResponse> generateQuestions(@Valid @RequestBody QuestionGenerationRequest request) throws Exception {
        // 直接调用服务并返回结果，不再需要 try-catch
        return ResponseEntity.ok(
                new QuestionGenerationResponse(generationService.generateAndSaveUniqueQuestions(request))
        );
    }

    /**
     * 更新用户对某道题的练习状态
     * 如果发生异常，会被 GlobalExceptionHandler 自动捕获
     */
    @PostMapping("/{questionId}/status")
    public ResponseEntity<Map<String, String>> updateUserQuestionStatus(
            @PathVariable Long questionId,
            @Valid @RequestBody UserStatusUpdateRequest request) {

        // 在真实应用中，userId 应该从 Spring Security 的认证信息中获取
        Long currentUserId = 1L; // 这里用一个模拟的用户ID

        // 直接调用服务，不再需要 try-catch
        practiceService.updateUserQuestionStatus(currentUserId, questionId, request);

        return ResponseEntity.ok(Map.of("message", "状态更新成功"));
    }
}