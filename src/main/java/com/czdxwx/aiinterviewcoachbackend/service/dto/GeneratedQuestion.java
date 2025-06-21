package com.czdxwx.aiinterviewcoachbackend.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 代表一个从AI生成、并准备返回给前端的完整面试题信息
 * 这是最终的数据传输对象(DTO)
 */
public record GeneratedQuestion(
        /**
         * 【核心修正】使用 @SerializedName 明确指定JSON字段名
         */
        @SerializedName("question_text")
        String questionText,

        @SerializedName("reference_answer")
        String referenceAnswer,

        @SerializedName("speech_synthesis_content")
        String speechSynthesisContent,

        // 这两个字段是在后端处理时填充的，AI响应中没有，但为了保持DTO统一，我们保留它
        List<String> tags,
        String speechAudioUrl
) {}