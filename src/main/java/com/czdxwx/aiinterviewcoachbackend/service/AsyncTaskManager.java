package com.czdxwx.aiinterviewcoachbackend.service;
import com.czdxwx.aiinterviewcoachbackend.service.dto.ProgressUpdateDTO;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异步任务状态管理器
 * 在内存中跟踪所有长时间运行任务的进度。
 */
@Component
public class AsyncTaskManager {

    private final Map<String, ProgressUpdateDTO> tasks = new ConcurrentHashMap<>();

    /**
     * 当一个新任务开始时，进行注册
     * @param taskId 唯一的任务ID
     */
    public void register(String taskId) {
        // 初始化状态：进度0%，已开始但未完成
        ProgressUpdateDTO initialStatus = new ProgressUpdateDTO(taskId, 0, "任务已创建，正在排队等待执行...", false, null);
        tasks.put(taskId, initialStatus);
    }

    /**
     * 更新任务的进度和状态信息
     * @param update 包含最新进度的 DTO
     */
    public void updateProgress(ProgressUpdateDTO update) {
        tasks.put(update.getTaskId(), update);
    }

    /**
     * 根据任务ID获取当前状态
     * @param taskId 任务ID
     * @return 包含任务状态的 Optional
     */
    public Optional<ProgressUpdateDTO> getTaskStatus(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    /**
     * （可选）可以在这里添加一个定时任务，用于清理已完成很久的任务，防止内存泄漏
     */
}