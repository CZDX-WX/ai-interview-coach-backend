package com.czdxwx.aiinterviewcoachbackend.service.dto;

import java.util.List;

public record QuestionGenerationResponse(
        List<GeneratedQuestion> questions
) {}
