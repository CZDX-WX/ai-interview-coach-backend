package com.czdxwx.aiinterviewcoachbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.czdxwx.aiinterviewcoachbackend.client.SparkClient;
import com.czdxwx.aiinterviewcoachbackend.entity.Question;
import com.czdxwx.aiinterviewcoachbackend.entity.QuestionTag;
import com.czdxwx.aiinterviewcoachbackend.entity.Tag;
import com.czdxwx.aiinterviewcoachbackend.mapper.postgres.QuestionMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.postgres.QuestionTagMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.postgres.TagMapper;
import com.czdxwx.aiinterviewcoachbackend.service.dto.GeneratedQuestion;
import com.czdxwx.aiinterviewcoachbackend.service.dto.QuestionGenerationRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.czdxwx.aiinterviewcoachbackend.utils.AuthUtils;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionGenerationService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionGenerationService.class);

    private final SparkClient sparkClient;
    private final EmbeddingService embeddingService;
    private final QuestionMapper questionMapper;
    private final TagMapper tagMapper;
    private final QuestionTagMapper questionTagMapper;
    private final Gson gson = new Gson();

    @Transactional("postgresTransactionManager")
    public List<GeneratedQuestion> generateAndSaveUniqueQuestions(QuestionGenerationRequest request) throws Exception {

        String tagsAsString = String.join(", ", request.tags());
        String keywordQuery = String.format("请帮我为'%s'岗位，生成%d道'%s'难度的面试题，技术主题涉及：%s。",
                request.role(), request.numQuestions(), request.difficulty(), tagsAsString);

        JsonObject extractedArgs = sparkClient.extractParameters(keywordQuery);

        String rawJsonResponse = sparkClient.generateContent(extractedArgs, request.strategy());
        String cleanedJson = AuthUtils.extractJsonFromString(rawJsonResponse);
        List<GeneratedQuestion> candidates = gson.fromJson(cleanedJson, new TypeToken<List<GeneratedQuestion>>() {}.getType());

        List<GeneratedQuestion> uniqueNewQuestions = new ArrayList<>();
        for (GeneratedQuestion candidate : candidates) {
            String text = candidate.questionText();
            String hash = AuthUtils.calculateSha256(text);

            if (questionMapper.exists(new QueryWrapper<Question>().eq("question_hash", hash))) {
                logger.warn("精确去重：题目已存在，已丢弃: {}", text);
                continue;
            }

            float[] embeddingArray = embeddingService.getEmbedding(text);
            PGvector embedding = new PGvector(embeddingArray);
            List<Question> similarQuestions = questionMapper.findNearestNeighbors(embedding, 1);
            if (!similarQuestions.isEmpty()) {
                logger.warn("语义去重：发现潜在相似题目，已丢弃: {}", text);
                continue;
            }

            Question newQuestion = saveNewQuestion(candidate, hash, embedding, request.difficulty());

            if (candidate.tags() != null && !candidate.tags().isEmpty()) {
                linkTagsToQuestion(candidate.tags(), newQuestion.getId());
            }

            uniqueNewQuestions.add(candidate);
        }
        return uniqueNewQuestions;
    }

    private Question saveNewQuestion(GeneratedQuestion dto, String hash, PGvector embedding, String difficulty) {
        Question question = new Question();
        question.setQuestionText(dto.questionText());
        question.setReferenceAnswer(dto.referenceAnswer());
        question.setSpeechSynthesisContent(dto.speechSynthesisContent());
        question.setDifficulty(difficulty);
        question.setQuestionHash(hash);
        question.setQuestionVector(embedding);
        questionMapper.insert(question);
        return question;
    }

    private void linkTagsToQuestion(List<String> tagNames, Long questionId) {
        for (String tagName : tagNames) {
            Tag tag = tagMapper.selectOne(new QueryWrapper<Tag>().eq("name", tagName));
            if (tag == null) {
                tag = new Tag();
                tag.setName(tagName);
                tagMapper.insert(tag);
            }
            if (questionTagMapper.selectCount(new QueryWrapper<QuestionTag>().eq("question_id", questionId).eq("tag_id", tag.getId())) == 0) {
                questionTagMapper.insert(new QuestionTag(questionId, tag.getId()));
            }
        }
    }
}