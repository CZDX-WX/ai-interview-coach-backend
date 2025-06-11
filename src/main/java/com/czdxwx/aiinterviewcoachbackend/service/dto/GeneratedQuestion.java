package com.czdxwx.aiinterviewcoachbackend.service.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record GeneratedQuestion(
        @SerializedName("question_text")
        String questionText,

        @SerializedName("reference_answer")
        String referenceAnswer,

        @SerializedName("speech_synthesis_content")
        String speechSynthesisContent,

        @SerializedName("tags")
        List<String> tags
) {}