package com.czdxwx.aiinterviewcoachbackend.vo;

import lombok.Data;

@Data
public class PracticeStatsVO {

    /**
     * 对该用户可见的题目总数
     */
    private long totalQuestions;

    /**
     * 已掌握的题目数量
     */
    private long masteredCount;

    /**
     * 待复习的题目数量
     */
    private long needsReviewCount;

    /**
     * 未学习的题目数量
     */
    private long notPracticedCount;

    /**
     * 已收藏的题目数量
     */
    private long bookmarkedCount;
}