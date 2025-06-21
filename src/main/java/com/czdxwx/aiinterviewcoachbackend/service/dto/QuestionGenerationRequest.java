package com.czdxwx.aiinterviewcoachbackend.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;


public record QuestionGenerationRequest(
        /**
         * 【确认】岗位的ID，由前端在用户选择后传入
         */
        @NotNull(message = "岗位ID不能为空")
        Long roleId,

        /**
         * 【确认】岗位的名称，由前端在用户选择后一并传入，用于构造Prompt
         */
        @NotNull(message = "岗位名称不能为空")
        String roleName,

        @NotEmpty(message = "技术标签不能为空")
        List<String> tags,

        @NotNull(message = "难度不能为空")
        String difficulty,

        @NotNull(message = "题目数量不能为空")
        @Min(1) @Max(5)
        int numQuestions,

        @NotNull(message = "生成策略不能为空")
        GenerationStrategy strategy
) {}