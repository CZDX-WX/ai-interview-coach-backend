package com.czdxwx.aiinterviewcoachbackend.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuestionGenerationRequest(
        @NotNull String role,
        @NotEmpty List<String> tags,
        @NotNull String difficulty,
        @NotNull @Min(1) @Max(5) int numQuestions,
        @NotNull GenerationStrategy strategy
) {}