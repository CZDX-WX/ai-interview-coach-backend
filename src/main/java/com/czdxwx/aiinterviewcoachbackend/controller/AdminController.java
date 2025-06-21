package com.czdxwx.aiinterviewcoachbackend.controller;

import com.czdxwx.aiinterviewcoachbackend.service.TagVectorizationService;
import com.czdxwx.aiinterviewcoachbackend.service.TtsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class); // 新增 logger
    private final TtsService ttsService; // 注入 TtsService
    private final TagVectorizationService tagVectorizationService; // 【新增】注入

    /**
     * 【更新】独立的语音合成测试接口
     * @param payload 包含 "text" 和可选 "voiceName" 字段的JSON对象
     * @return 返回包含文件路径的成功信息
     */
    @PostMapping("/test-tts")
    public ResponseEntity<Map<String, String>> testTextToSpeech(@RequestBody Map<String, String> payload) {
        logger.info("TTS test endpoint reached with payload: {}", payload);
        String textToConvert = payload.get("text");
        // 从请求中获取发音人，如果未提供则使用默认值
        String voiceName = payload.getOrDefault("voiceName", "x4_lingxiaoxuan_oral");

        if (textToConvert == null || textToConvert.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Text cannot be empty."));
        }

        // 调用服务，获取保存后的文件路径
        String filePath = ttsService.textToSpeech(textToConvert);

        if (filePath != null) {
            return ResponseEntity.ok(Map.of("status", "success", "filePath", filePath));
        } else {
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", "Failed to generate or save audio file."));
        }
    }

    /**
     * 【新增】触发一个后台任务，为数据库中所有公共标签创建向量索引
     */
    @PostMapping("/tags/vectorize-backfill")
    public ResponseEntity<Map<String, String>> triggerTagVectorization() {

        // 调用异步服务，立即返回
        tagVectorizationService.backfillTagVectors();
        return ResponseEntity.ok(Map.of("message", "标签向量化回填任务已在后台启动，请关注后台日志查看进度。"));
    }
}