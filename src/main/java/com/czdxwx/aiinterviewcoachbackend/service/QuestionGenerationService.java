package com.czdxwx.aiinterviewcoachbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.czdxwx.aiinterviewcoachbackend.client.SparkClient;
import com.czdxwx.aiinterviewcoachbackend.entity.Question;
import com.czdxwx.aiinterviewcoachbackend.entity.Role;
import com.czdxwx.aiinterviewcoachbackend.entity.UserQuestionStatus;
import com.czdxwx.aiinterviewcoachbackend.handler.ProgressWebSocketHandler;
import com.czdxwx.aiinterviewcoachbackend.mapper.QuestionMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.UserQuestionStatusMapper;
import com.czdxwx.aiinterviewcoachbackend.model.enums.ProficiencyStatus;
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

    // 注入所有需要的服务和 Mapper
    private final SparkClient sparkClient;
    private final EmbeddingService embeddingService;
    private final MilvusService milvusService;
    private final RoleService roleService;
    private final TagService tagService;
    private final TtsService ttsService;
    private final QuestionMapper questionMapper;
    private final UserQuestionStatusMapper userQuestionStatusMapper;
    private final AsyncTaskManager asyncTaskManager; // 确保注入了任务管理器
    private final ProgressWebSocketHandler progressWebSocketHandler;
    private final Gson gson = new Gson();

    // 定义语义相似度的阈值。L2距离越小越相似。
    private static final double SEMANTIC_SIMILARITY_THRESHOLD = 0.2;
    // 定义内部循环的短暂延迟，以避免对外部API调用过于频繁
    private static final long API_CALL_DELAY_MS = 500;

    /**
     * [公共方法] 异步地填充公共题库。
     * 被 AdminController 调用，用于初始化和扩充公共题库。
     * @param request  题目生成要求的DTO
     * @param taskId   用于WebSocket进度通知的唯一任务ID
     */
    @Async("taskExecutor")
    public void populatePublicQuestions(QuestionGenerationRequest request, String taskId) {
        logger.info("Starting ASYNC population for PUBLIC questions. Task ID: {}", taskId);
        // 在任务开始前，先在管理器中注册
        asyncTaskManager.register(taskId);
        runGenerationWorkflow(request, null, taskId);
    }

    /**
     * [公共方法] 异步地为用户生成个性化题目。
     * 被 QuestionController 调用，为登录用户提供服务。
     * @param request 题目生成要求的DTO
     * @param userId  当前登录用户的ID
     * @param taskId  用于WebSocket进度通知的唯一任务ID
     */
    @Async("taskExecutor")
    public void generatePersonalizedQuestions(QuestionGenerationRequest request, Long userId, String taskId) {
        logger.info("Starting ASYNC generation for PERSONALIZED questions for user: {}. Task ID: {}", userId, taskId);
        asyncTaskManager.register(taskId);
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
            // 阶段一：准备与预检查
            updateTaskProgress(taskId, 5, "任务启动，解析请求...", false, null);
            Role contextRole = roleService.findByName(request.roleName());
            if (contextRole == null) {
                throw new NoSuchElementException("请求的岗位不存在: " + request.roleName());
            }
//            // 阶段二：“先搜”，查找是否已有相似题目
//            updateTaskProgress(taskId, 15, "检索现有题库，避免重复...", false, null);
//            Optional<List<GeneratedQuestion>> similarQuestions = findAndReuseSimilarQuestion(request, contextRole.getId(), ownerId);
//
//            if (similarQuestions.isPresent()) {
//                // 如果找到了可复用的题目，直接结束任务
//                updateTaskProgress(taskId, 100, "任务完成！为您找到了高度匹配的现有题目。", true, similarQuestions.get());
//                return;
//            }

            // 阶段三：“后生”，生成并持久化新题目
            updateTaskProgress(taskId, 30, "连接AI，生成新内容...", false, null);
            List<GeneratedQuestion> resultList = createAndPersistNewQuestions(request, ownerId, contextRole.getId(), taskId);

            // 阶段四：任务全部完成
            updateTaskProgress(taskId, 100, String.format("任务完成！成功生成 %d 道新题目。", resultList.size()), true, resultList);

        } catch (Exception e) {
            logger.error("An error occurred in async generation task {}: {}", taskId, e.getMessage(), e);
            updateTaskProgress(taskId, 100, "任务生成失败：" + e.getMessage(), true, null);
        }
    }

//    /**
//     * [私有] 执行“先搜”逻辑：查找并复用满足条件的、且用户未做过的相似题目。
//     * @return 如果找到可复用的题目，返回包含题目信息的Optional；否则返回空。
//     */
//    private Optional<List<GeneratedQuestion>> findAndReuseSimilarQuestion(QuestionGenerationRequest request, Long roleId, Long ownerId) {
//        // 公共题库生成时，总是创建新题，不走复用逻辑
//        if (ownerId == null) {
//            return Optional.empty();
//        }
//
//        // 1. 动态生成查询向量
//        String topicString = String.join("、", request.tags());
//        String fullTopicDescription = String.format("这是一道关于 %s 岗位的面试题，难度为%s，考察的技术点包括：%s",
//                request.roleName(), request.difficulty(), topicString);
//        float[] requestVector = embeddingService.getEmbedding(fullTopicDescription);
//        List<Float> searchVector = toFloatList(requestVector);
//
//        // 2. 在 Milvus 中搜索最相似的5个候选题目
//        List<SearchResp.SearchResult> similarCandidates = milvusService.searchQuestions(searchVector, 5);
//        if (similarCandidates.isEmpty()) return Optional.empty();
//
//        // 3. 过滤掉用户已练习过的题目
//        List<Long> candidateIds = similarCandidates.stream()
//                .map(res -> Long.parseLong(res.getId().toString()))
//                .collect(Collectors.toList());
//        Set<Long> practicedIds = userQuestionStatusMapper.findPracticedQuestionIds(ownerId, candidateIds);
//
//        // 4. 遍历候选，找出第一个符合条件的
//        for (SearchResp.SearchResult candidate : similarCandidates) {
//            long candidateId = Long.parseLong(candidate.getId().toString());
//            if (!practicedIds.contains(candidateId) && candidate.getScore() < SEMANTIC_SIMILARITY_THRESHOLD) {
//                Question existingQuestion = questionMapper.selectById(candidateId);
//                logger.info("智能复用：为用户 {} 找到未练习过的相似题目 (ID: {})", ownerId, existingQuestion.getId());
//
//                // 为用户与这道复用的题目建立关联
//                tagService.resolveAndAssociateTagsForQuestion(request.tags(), existingQuestion.getId(), ownerId, roleId);
//                createInitialStatusIfNotExists(ownerId, existingQuestion.getId());
//
//                List<String> allTags = tagService.getTagsByQuestionId(existingQuestion.getId());
//                GeneratedQuestion finalResult = new GeneratedQuestion(
//                        existingQuestion.getQuestionText(),
//                        existingQuestion.getReferenceAnswer(),
//                        existingQuestion.getSpeechSynthesisContent(),
//                        allTags,
//                        existingQuestion.getSpeechAudioUrl()
//                );
//                return Optional.of(List.of(finalResult));
//            }
//        }
//
//        return Optional.empty();
//    }

    /**
     * [私有] 执行“后生”逻辑：生成、处理并持久化新题目。
     * @return 成功入库的新题目列表（GeneratedQuestion DTO格式）。
     */
    private List<GeneratedQuestion> createAndPersistNewQuestions(QuestionGenerationRequest request, Long ownerId, Long roleId, String taskId) throws Exception {
        // 1. 从AI获取候选题目
        List<GeneratedQuestion> aiCandidates = fetchCandidatesFromAI(request);
        List<GeneratedQuestion> resultList = new ArrayList<>();

        for (int i = 0; i < aiCandidates.size(); i++) {
            GeneratedQuestion candidate = aiCandidates.get(i);
            int currentProgress = 60 + (int)(((i + 1.0) / aiCandidates.size()) * 30.0);
            updateTaskProgress(taskId, currentProgress, String.format("正在处理第 %d/%d 道新题目...", i + 1, aiCandidates.size()), false, null);

            if (candidate == null || !StringUtils.hasText(candidate.questionText())) {
                logger.warn("AI返回了内容为空的题目，已跳过。");
                continue;
            }

            // 2. 精确去重检查
            String text = candidate.questionText();
            String hash = AuthUtils.calculateSha256(text);
            if (questionMapper.exists(new QueryWrapper<Question>().eq("question_hash", hash))) {
                logger.warn("精确去重：生成的题目 '{}' 已存在，已丢弃。", text.substring(0, Math.min(20, text.length())));
                continue;
            }

            // 3. 条件化语音合成
            String audioUrl = synthesizeAudioForHardQuestion(request.difficulty(), candidate.speechSynthesisContent(), i + 1, taskId);

            // 4. 持久化到数据库
            Question newQuestion = saveQuestionMetadata(candidate, hash, request.difficulty(), ownerId, audioUrl);
            float[] embeddingArray = embeddingService.getEmbedding(text);
            milvusService.insertQuestionVector(newQuestion.getId(), toFloatList(embeddingArray));

            // 5. 关联标签
            tagService.resolveAndAssociateTags(request.tags(), newQuestion.getId(), ownerId, roleId);
            // 6. 创建初始练习状态

            createInitialStatusIfNotExists(ownerId, newQuestion.getId());

            // 7. 准备返回结果
            resultList.add(new GeneratedQuestion(candidate.questionText(), candidate.referenceAnswer(), candidate.speechSynthesisContent(), request.tags(), audioUrl));
            Thread.sleep(API_CALL_DELAY_MS);
        }
        return resultList;
    }


    private List<GeneratedQuestion> fetchCandidatesFromAI(QuestionGenerationRequest request) throws Exception {
        String rawJsonResponse = sparkClient.generateContent(request);
        String cleanedJson = AuthUtils.extractJsonFromString(rawJsonResponse);
        return gson.fromJson(cleanedJson, new TypeToken<List<GeneratedQuestion>>() {});
    }

    /**
     * [私有] 将题目元数据保存到 MySQL。
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
     * [私有] 为一个用户和题目创建“未学习”的初始状态记录。
     */
    private void createInitialStatusIfNotExists(Long userId, Long questionId) {
        if (userId == null) return;
        boolean exists = userQuestionStatusMapper.exists(new QueryWrapper<UserQuestionStatus>().eq("user_id", userId).eq("question_id", questionId));
        if (!exists) {
            UserQuestionStatus initialStatus = new UserQuestionStatus();
            initialStatus.setUserId(userId);
            initialStatus.setQuestionId(questionId);
            initialStatus.setProficiencyStatus(ProficiencyStatus.NOT_PRACTICED);
            initialStatus.setIsBookmarked(false);
            initialStatus.setLastPracticedAt(new Date());
            userQuestionStatusMapper.insert(initialStatus);
            logger.info("Created initial 'NOT_PRACTICED' status for user {} and question {}", userId, questionId);
        }
    }

    /**
     * [私有] 条件化生成语音的辅助方法。
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
     * [私有] 发送进度更新的统一方法。
     */
    private void updateTaskProgress(String taskId, int progress, String message, boolean finished, Object data) {
        ProgressUpdateDTO update = new ProgressUpdateDTO(taskId, progress, message, finished, data);
        // 1. 更新内存中的任务状态，以便API可以查询到
        asyncTaskManager.updateProgress(update);
        // 2. 推送实时状态给已连接的前端
        progressWebSocketHandler.sendProgressUpdate(update);
    }

    /**
     * [私有] 将 float[] 转换为 List<Float> 的辅助方法。
     */
    private List<Float> toFloatList(float[] array) {
        if (array == null) return Collections.emptyList();
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) list.add(f);
        return list;
    }
}