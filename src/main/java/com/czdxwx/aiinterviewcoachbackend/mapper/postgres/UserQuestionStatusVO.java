package com.czdxwx.aiinterviewcoachbackend.mapper.postgres;

import com.czdxwx.aiinterviewcoachbackend.model.enums.ProficiencyStatus;
import lombok.Data;
import java.time.Instant;

@Data
public class UserQuestionStatusVO {
    private Long id;
    private Long userId;
    private Long questionId;
    private ProficiencyStatus proficiencyStatus;
    private Instant lastPracticedAt;
    private String notes;

    // --- 从 Question 表关联查询来的额外字段 ---
    private String questionText;
    private String questionDifficulty;
}