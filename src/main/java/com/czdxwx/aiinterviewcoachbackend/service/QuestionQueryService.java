package com.czdxwx.aiinterviewcoachbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.czdxwx.aiinterviewcoachbackend.entity.Question;
import com.czdxwx.aiinterviewcoachbackend.mapper.QuestionMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.TagMapper;
import com.czdxwx.aiinterviewcoachbackend.service.dto.QuestionSearchRequest;
import com.czdxwx.aiinterviewcoachbackend.vo.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper; // 使用 Jackson 或 Gson 进行转换
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class QuestionQueryService {

    private final QuestionMapper questionMapper;
    private final TagMapper tagMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 分页获取所有题目（包含标签）
     */
    public IPage<QuestionVO> findAllPaginated(int current, int size) {
        Page<QuestionVO> page = new Page<>(current, size);
        // 【核心修正】传入一个空的 QueryWrapper 作为第二个参数，以匹配 Mapper 方法的签名
        return questionMapper.findPageWithTags(page, new QueryWrapper<>());
    }

    /**
     * 根据ID获取单个题目详情（包含标签）
     */
    public Optional<QuestionVO> findByIdWithTags(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            return Optional.empty();
        }

        QuestionVO vo = new QuestionVO();
        BeanUtils.copyProperties(question, vo);

        List<String> tags = tagMapper.findTagsByQuestionId(id);
        vo.setTags(tags);

        return Optional.of(vo);
    }

    /**
     * 根据标签名称，分页获取题目列表（包含标签）
     */
    @Cacheable(value = "questionsByTag", key = "#tagName + ':' + #current + ':' + #size")
    public IPage<QuestionVO> findPaginatedByTag(String tagName, int current, int size) {
        Page<QuestionVO> page = new Page<>(current, size);

        // 【核心修正】我们之前的版本在这里也存在问题，正确的做法是构造一个 Wrapper
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        // 使用原生 SQL 子查询来筛选出包含特定标签的 question.id
        queryWrapper.inSql("q.id", "SELECT question_id FROM question_tags qt JOIN tags t ON qt.tag_id = t.id WHERE t.name = '" + tagName + "'");

        return questionMapper.findPageWithTags(page, queryWrapper);
    }

    /**
     * 【核心修正】统一的、强大的题目搜索方法，采用两步查询法
     */
    @Cacheable(value = "questionSearchResults", key = "#request.toString()")
    public IPage<QuestionVO> searchQuestions(QuestionSearchRequest request) {
        // 1. 第一步：分页查询出符合条件的题目ID，这部分不变
        Page<Long> page = new Page<>(request.getCurrent(), request.getSize());
        IPage<Long> idPage = questionMapper.searchQuestionIds(page, request);

        if (CollectionUtils.isEmpty(idPage.getRecords())) {
            return new Page<>(idPage.getCurrent(), idPage.getSize(), 0);
        }

        // 2. 第二步：根据ID列表，查询这些题目的完整信息
        List<Long> questionIds = idPage.getRecords();
        // 【核心修改】将 userId 传递给 findVosByIds 方法
        List<QuestionVO> questionVos = questionMapper.findVosByIds(questionIds, request.getUserId());

        // 3. 手动组装最终的分页结果并返回
        IPage<QuestionVO> finalPage = new Page<>(idPage.getCurrent(), idPage.getSize(), idPage.getTotal());
        finalPage.setRecords(questionVos);

        return finalPage;
    }
}