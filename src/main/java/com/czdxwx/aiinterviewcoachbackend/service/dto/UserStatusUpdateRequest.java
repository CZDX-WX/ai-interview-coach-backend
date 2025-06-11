package com.czdxwx.aiinterviewcoachbackend.service.dto;

import com.czdxwx.aiinterviewcoachbackend.model.enums.ProficiencyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class UserStatusUpdateRequest {
    @NotNull(message = "熟练度状态不能为空")
    private ProficiencyStatus status;
    private String notes;
}