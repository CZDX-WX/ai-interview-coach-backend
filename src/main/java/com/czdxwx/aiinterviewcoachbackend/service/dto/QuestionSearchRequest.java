package com.czdxwx.aiinterviewcoachbackend.service.dto;

import lombok.Data;
import java.util.List;

@Data
public class QuestionSearchRequest {

    // 分页参数
    private int current = 1;
    private int size = 10;

    // -- 筛选条件 (均为可选，前端不传或传null则代表“不筛选”) --

    /**
     * 【来自顶部页签】熟练度状态
     * 可选值: "ALL", "NOT_PRACTICED", "NEEDS_REVIEW", "MASTERED"
     */
    private String practiceStatus;

    /**
     * 【来自收藏开关】是否只看收藏的题目
     * 可选值: true (只看收藏), null (不过滤此项)
     */
    private Boolean isBookmarked;

    /**
     * 【来自侧边栏】岗位ID
     */
    private Long roleId;

    /**
     * 【来自侧边栏】标签名称列表
     */
    private List<String> tagNames;

    /**
     * 【来自侧边栏】标签搜索模式
     */
    private SearchMode searchMode = SearchMode.ANY_TAG;

    /**
     * 【来自侧边栏】难度
     */
    private String difficulty;

    /**
     * 【内部使用】由 Controller 设置，前端无需关心
     */
    private Long userId;

    public enum SearchMode {
        ANY_TAG, // 包含任意一个标签即可 (OR)
        ALL_TAGS  // 必须包含所有标签 (AND)
    }
}