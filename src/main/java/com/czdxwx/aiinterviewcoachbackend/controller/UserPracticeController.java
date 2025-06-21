package com.czdxwx.aiinterviewcoachbackend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.czdxwx.aiinterviewcoachbackend.config.security.CustomUserDetails;
import com.czdxwx.aiinterviewcoachbackend.service.UserPracticeService;
import com.czdxwx.aiinterviewcoachbackend.service.dto.BookmarkRequest;
import com.czdxwx.aiinterviewcoachbackend.service.dto.UserStatusUpdateRequest;
import com.czdxwx.aiinterviewcoachbackend.vo.PracticeStatsVO;
import com.czdxwx.aiinterviewcoachbackend.vo.QuestionVO;
import com.czdxwx.aiinterviewcoachbackend.vo.UserQuestionStatusVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class UserPracticeController {

    private final UserPracticeService practiceService;

    /**
     * 【新增】按状态筛选题目列表，用于前端的标签页功能
     * status 可选值: NOT_PRACTICED, NEEDS_REVIEW, MASTERED, BOOKMARKED
     */
    @GetMapping("/questions")
    public ResponseEntity<IPage<QuestionVO>> getQuestionsByStatus(
            @RequestParam String status,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(practiceService.getQuestionsByStatusForUser(currentUser.getId(), status, current, size));
    }

    /**
     * 获取当前用户的完整刷题历史
     */
    @GetMapping("/history")
    public ResponseEntity<List<UserQuestionStatusVO>> getMyPracticeHistory(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(practiceService.findPracticeHistoryForUser(currentUser.getId()));
    }

    /**
     * 更新用户对某道题的熟练度状态
     */
    @PostMapping("/questions/{questionId}/status")
    public ResponseEntity<Map<String, String>> updateUserQuestionStatus(
            @PathVariable Long questionId,
            @Valid @RequestBody UserStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        practiceService.updateUserQuestionStatus(currentUser.getId(), questionId, request);
        return ResponseEntity.ok(Map.of("message", "熟练度状态更新成功"));
    }

    /**
     * 【新增】更新用户对某道题的收藏状态
     */
    @PostMapping("/questions/{questionId}/bookmark")
    public ResponseEntity<Map<String, String>> toggleBookmark(
            @PathVariable Long questionId,
            @Valid @RequestBody BookmarkRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        practiceService.toggleBookmark(currentUser.getId(), questionId, request.getBookmarked());
        String message = request.getBookmarked() ? "收藏成功" : "取消收藏成功";
        return ResponseEntity.ok(Map.of("message", message));
    }

    /**
     * 【修改】将指定题目的练习状态重置为“未学习”
     * 使用 PUT 方法，因为它是一个更新操作
     */
    @PutMapping("/questions/{questionId}/status/reset")
    public ResponseEntity<Map<String, String>> resetQuestionStatus(
            @PathVariable Long questionId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        practiceService.resetStatusToUnpracticed(currentUser.getId(), questionId);
        return ResponseEntity.ok(Map.of("message", "已重置为未学习状态"));
    }

    /**
     * 【新增】获取当前用户的练习进度统计
     */
    @GetMapping("/progress-stats")
    public ResponseEntity<PracticeStatsVO> getMyPracticeStats(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(practiceService.getPracticeStats(currentUser.getId()));
    }
}
