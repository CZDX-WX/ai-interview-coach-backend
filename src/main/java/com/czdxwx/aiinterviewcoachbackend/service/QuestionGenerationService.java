package com.czdxwx.aiinterviewcoachbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.czdxwx.aiinterviewcoachbackend.client.SparkClient;

import com.czdxwx.aiinterviewcoachbackend.entity.Question;
import com.czdxwx.aiinterviewcoachbackend.entity.Role;
import com.czdxwx.aiinterviewcoachbackend.handler.ProgressWebSocketHandler;
import com.czdxwx.aiinterviewcoachbackend.mapper.QuestionMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.UserQuestionStatusMapper;
import com.czdxwx.aiinterviewcoachbackend.service.dto.GeneratedQuestion;
import com.czdxwx.aiinterviewcoachbackend.service.dto.ProgressUpdateDTO;
import com.czdxwx.aiinterviewcoachbackend.service.dto.QuestionGenerationRequest;
import com.czdxwx.aiinterviewcoachbackend.utils.AuthUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 核心业务服务，负责编排面试题的生成、去重和存储全流程。
 * 这是整个应用最核心的“业务大脑”。
 */
@Service
@RequiredArgsConstructor
public class QuestionGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(QuestionGenerationService.class);

    // 依赖注入
    private final SparkClient sparkClient;
    private final EmbeddingService embeddingService;
    private final MilvusService milvusService;
    private final RoleService roleService;
    private final TagService tagService;
    private final TtsService ttsService;
    private final QuestionMapper questionMapper;
    private final UserQuestionStatusMapper userQuestionStatusMapper;
    private final ProgressWebSocketHandler progressWebSocketHandler;
    private final Gson gson = new Gson();
    private final AsyncTaskManager asyncTaskManager; // 【新增】注入任务管理器
    private static final double SEMANTIC_SIMILARITY_THRESHOLD = 0.2;
    private static final long API_CALL_DELAY_MS = 500;

    /**
     * [公共方法] 异步地填充公共题库。
     * @param request  题目生成要求的DTO
     * @param taskId   用于WebSocket进度通知的唯一任务ID
     */
    @Async("taskExecutor")
    public void populatePublicQuestions(QuestionGenerationRequest request, String taskId) {
        logger.info("Starting ASYNC population for PUBLIC questions. Task ID: {}", taskId);
        runGenerationWorkflow(request, null, taskId);
    }

    /**
     * [公共方法] 异步地为用户生成个性化题目。
     * @param request 题目生成要求的DTO
     * @param userId  当前登录用户的ID
     * @param taskId  用于WebSocket进度通知的唯一任务ID
     */
    @Async("taskExecutor")
    public void generatePersonalizedQuestions(QuestionGenerationRequest request, Long userId, String taskId) {
        logger.info("Starting ASYNC generation for PERSONALIZED questions for user: {}. Task ID: {}", userId, taskId);
        runGenerationWorkflow(request, userId, taskId);
    }

    /**
     * [总调度方法] 统一的核心处理流程，被上面的两个异步公共方法调用。
     * @param request 请求参数
     * @param ownerId 题目的拥有者ID，公共题为null
     * @param taskId  本次任务的唯一ID
     */
    @Transactional
    public void runGenerationWorkflow(QuestionGenerationRequest request, Long ownerId, String taskId) {
        try {
            // 【修正】补全所有 sendProgress 的参数
            updateTaskProgress(taskId, 5, "任务已启动，正在解析请求...", false, null);
            Role contextRole = roleService.findByName(request.roleName());
            if (contextRole == null) { /* ... */ }
            final Long roleId = contextRole.getId();

            updateTaskProgress(taskId, 15, "检索现有题库，避免重复...", false, null);
            Optional<List<GeneratedQuestion>> similarQuestions = findAndReuseSimilarQuestion(request, roleId, ownerId);

            if (similarQuestions.isPresent()) {
                // 【修正】补全 sendProgress 的参数
                updateTaskProgress(taskId, 100, "任务完成！为您找到了高度匹配的现有题目。", true, similarQuestions.get());
                return;
            }
            updateTaskProgress(taskId, 30, "连接AI，生成新内容...", false, null);
            List<Object> resultList = createAndPersistNewQuestions(request, ownerId, roleId, taskId);
            updateTaskProgress(taskId, 100, String.format("任务完成！成功生成 %d 道新题目。", resultList.size()), true, resultList);

        } catch (Exception e) {
            logger.error("An error occurred in async generation task {}: {}", taskId, e.getMessage(), e);
            updateTaskProgress(taskId, 100, "任务生成失败：" + e.getMessage(), true, null);
        }
    }

    // ====================================================================
    //              私有辅助方法 (Private Helper Methods)
    // ====================================================================

    /**
     * 执行“先搜”逻辑，查找并复用满足条件的、用户未做过的相似题目。
     * @return 如果找到可复用的题目，返回包含题目信息的Optional；否则返回空。
     */
    private Optional<List<GeneratedQuestion>> findAndReuseSimilarQuestion(QuestionGenerationRequest request, Long roleId, Long ownerId) {
        // 只有为登录用户生成时，才执行“先搜后复用”逻辑，避免公共题库生成时互相复用导致内容不丰富
        if (ownerId == null) {
            return Optional.empty();
        }

        List<List<Float>> tagVectors = tagService.getVectorsByTagNames(request.tags());
        if (CollectionUtils.isEmpty(tagVectors)) return Optional.empty();

        List<Float> searchVector = calculateAverageVector(tagVectors);
        if (CollectionUtils.isEmpty(searchVector)) return Optional.empty();

        List<SearchResp.SearchResult> similarCandidates = milvusService.searchQuestions(searchVector, 5); // 取5个候选
        if (similarCandidates.isEmpty()) return Optional.empty();

        List<Long> candidateIds = similarCandidates.stream()
                .map(res -> Long.parseLong(res.getId().toString()))
                .collect(Collectors.toList());
        Set<Long> practicedIds = userQuestionStatusMapper.findPracticedQuestionIds(ownerId, candidateIds);

        for (SearchResp.SearchResult candidate : similarCandidates) {
            long candidateId = Long.parseLong(candidate.getId().toString());
            if (!practicedIds.contains(candidateId) && candidate.getScore() < SEMANTIC_SIMILARITY_THRESHOLD) {
                Question existingQuestion = questionMapper.selectById(candidateId);
                logger.info("智能复用：为用户 {} 找到未练习过的相似题目 (ID: {})", ownerId, existingQuestion.getId());

                tagService.resolveAndAssociateTags(request.tags(), existingQuestion.getId(), ownerId, roleId);
                List<String> allTags = tagService.getTagsByQuestionId(existingQuestion.getId());
                List<GeneratedQuestion> finalResult = List.of(new GeneratedQuestion(
                        existingQuestion.getQuestionText(), existingQuestion.getReferenceAnswer(),
                        existingQuestion.getSpeechSynthesisContent(), allTags, existingQuestion.getSpeechAudioUrl()
                ));
                return Optional.of(finalResult);
            }
        }

        return Optional.empty();
    }

    /**
     * 执行“后生”逻辑，生成、处理并持久化新题目。
     * @return 成功入库的新题目的ID和文本列表。
     */
    private List<Object> createAndPersistNewQuestions(QuestionGenerationRequest request, Long ownerId, Long roleId, String taskId) throws Exception {
        List<GeneratedQuestion> candidates = fetchCandidatesFromAI(request);
        List<Object> resultList = new ArrayList<>();

        for (int i = 0; i < candidates.size(); i++) {
            GeneratedQuestion candidate = candidates.get(i);
            int currentProgress = 60 + (int)(((i + 1.0) / candidates.size()) * 30.0);
            updateTaskProgress(taskId, currentProgress, String.format("正在处理第 %d/%d 道新题目...", i + 1, candidates.size()), false, null);

            if (candidate == null || !StringUtils.hasText(candidate.questionText())) {
                logger.warn("AI返回了内容为空的题目，已跳过。");
                continue;
            }

            String text = candidate.questionText();
            String hash = AuthUtils.calculateSha256(text);
            if (questionMapper.exists(new QueryWrapper<Question>().eq("question_hash", hash))) {
                logger.warn("精确去重：生成的题目 '{}' 已存在，已丢弃。", text.substring(0, Math.min(20, text.length())));
                continue;
            }

            String audioUrl = synthesizeAudioForHardQuestion(request.difficulty(), candidate.speechSynthesisContent(), i + 1, taskId);

            Question newQuestion = saveQuestionMetadata(candidate, hash, request.difficulty(), ownerId, audioUrl);
            float[] embeddingArray = embeddingService.getEmbedding(text);
            milvusService.insertQuestionVector(newQuestion.getId(), toFloatList(embeddingArray));

            tagService.resolveAndAssociateTags(request.tags(), newQuestion.getId(), ownerId, roleId);

            resultList.add(Map.of("id", newQuestion.getId(), "questionText", newQuestion.getQuestionText()));
            Thread.sleep(API_CALL_DELAY_MS);
        }
        return resultList;
    }

    /**
     * 封装调用 SparkClient 的逻辑。
     */
    private List<GeneratedQuestion> fetchCandidatesFromAI(QuestionGenerationRequest request) throws Exception {
        String rawJsonResponse = sparkClient.generateContent(request);
        String cleanedJson = AuthUtils.extractJsonFromString(rawJsonResponse);
        return gson.fromJson(cleanedJson, new TypeToken<List<GeneratedQuestion>>() {});
    }

    /**
     * 将题目元数据保存到 MySQL。
     */
    private Question saveQuestionMetadata(GeneratedQuestion dto, String hash, String difficulty, Long ownerId, String audioUrl) {
        Question question = new Question();
        question.setQuestionText(dto.questionText());
        question.setReferenceAnswer(dto.referenceAnswer());
        question.setSpeechSynthesisContent(dto.speechSynthesisContent());
        question.setDifficulty(difficulty);
        question.setOwnerId(ownerId);
        question.setQuestionHash(hash);
        question.setSpeechAudioUrl(audioUrl);
        question.setCreatedAt(new Date());
        questionMapper.insert(question);
        return question;
    }

    /**
     * 条件化生成语音的辅助方法。
     */
    private String synthesizeAudioForHardQuestion(String difficulty, String content, int index, String taskId) {
        if (("困难".equalsIgnoreCase(difficulty)) && StringUtils.hasText(content)) {
            try {
                updateTaskProgress(taskId, 75 + (index * 5), String.format("正在为第 %d 道困难题合成语音...", index), false, null);
                return ttsService.textToSpeech(content);
            } catch (Exception e) {
                logger.error("TTS generation failed for a question. Continuing without audio.", e);
            }
        }
        return null;
    }

    /**
     * 【新增】一个统一的进度更新方法
     * 它同时负责更新任务管理器的状态 和 通过WebSocket推送给前端
     */
    private void updateTaskProgress(String taskId, int progress, String message, boolean finished, Object data) {
        ProgressUpdateDTO update = new ProgressUpdateDTO(taskId, progress, message, finished, data);
        // 1. 更新内存中的任务状态
        asyncTaskManager.updateProgress(update);
        // 2. 推送实时状态给已连接的前端
        progressWebSocketHandler.sendProgressUpdate(update);
    }

    /**
     * 计算多个向量的平均向量。
     */
    private List<Float> calculateAverageVector(List<List<Float>> vectors) {
        if (CollectionUtils.isEmpty(vectors)) return null;
        int dimension = vectors.get(0).size();
        float[] averageVector = new float[dimension];
        for (List<Float> vector : vectors) {
            for (int i = 0; i < dimension; i++) averageVector[i] += vector.get(i);
        }
        for (int i = 0; i < dimension; i++) averageVector[i] /= vectors.size();
        float norm = 0.0f;
        for (float v : averageVector) norm += v * v;
        norm = (float) Math.sqrt(norm);
        if (norm == 0.0f) return Collections.emptyList();
        List<Float> normalizedVector = new ArrayList<>(dimension);
        for (float v : averageVector) normalizedVector.add(v / norm);
        return normalizedVector;
    }

    /**
     * 将 float[] 转换为 List<Float> 的辅助方法。
     */
    private List<Float> toFloatList(float[] array) {
        if (array == null) return Collections.emptyList();
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) list.add(f);
        return list;
    }


}