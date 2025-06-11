package com.czdxwx.aiinterviewcoachbackend.service.dto;

// 定义生成策略的枚举
public enum GenerationStrategy {
    BREADTH_COVERAGE, // 广度优先：确保覆盖到每个标签
    INTEGRATED_DEEP_DIVE // 深度优先：生成需要结合多个标签知识的综合题
}
