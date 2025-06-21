package com.czdxwx.aiinterviewcoachbackend.service.dto;

import lombok.Data;
import java.util.List;

@Data
public class QuestionSearchRequest {

    // 分页参数
    private int current = 1;
    private int size = 10;

    // --- 筛选条件 (均为可选) ---

    private Long roleId;
    private List<String> tagNames;
    private SearchMode searchMode = SearchMode.ANY_TAG;
    private String practiceStatus;

    /**
     * 【新增】按难度筛选
     * 可选值: "简单", "中等", "困难"
     */
    private String difficulty;

    /**
     * 【内部使用】由 Controller 设置
     */
    private Long userId;

    public enum SearchMode {
        ANY_TAG, // "或"逻辑
        ALL_TAGS  // "与"逻辑
    }
}