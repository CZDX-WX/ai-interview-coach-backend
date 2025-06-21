package com.czdxwx.aiinterviewcoachbackend.service.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressUpdateDTO {
    private String taskId;
    private int progress; // 进度百分比, e.g., 0-100
    private String message; // 当前步骤的描述
    private boolean finished; // 任务是否已完成
    private Object data; // 任务完成时，携带最终的数据
}