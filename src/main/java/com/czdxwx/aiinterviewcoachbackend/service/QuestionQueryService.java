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
import org.mybatis.spring.SqlSessionTemplate;
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

    @Cacheable(value = "questionSearchResults", key = "#request.toString()")
    public IPage<QuestionVO> searchQuestions(QuestionSearchRequest request) {
        IPage<QuestionVO> finalPage;

        // 【核心】根据 searchMode 采用不同的分页策略
        if (request.getSearchMode() == QuestionSearchRequest.SearchMode.ALL_TAGS) {
            // 对于 ALL_TAGS，采用手动分页
            finalPage = executeManualPagination(request);
        } else {
            // 对于 ANY_TAG 和其他情况，使用MyBatis-Plus自动分页
            finalPage = executeAutoPagination(request);
        }

        return finalPage;
    }

    /**
     * [私有] 自动分页逻辑（用于简单查询）
     */
    private IPage<QuestionVO> executeAutoPagination(QuestionSearchRequest request) {
        Page<Long> page = new Page<>(request.getCurrent(), request.getSize());
        IPage<Long> idPage = questionMapper.searchQuestionIds(page, request);
        return fetchDetailsForPage(idPage, request.getUserId());
    }

    /**
     * [私有] 手动分页逻辑（用于ALL_TAGS这个复杂查询）
     */
    private IPage<QuestionVO> executeManualPagination(QuestionSearchRequest request) {
        // 1. 获取所有符合条件的ID
        List<Long> allMatchingIds = questionMapper.findAllQuestionIdsByAllTags(request);

        long total = allMatchingIds.size();
        IPage<QuestionVO> pageResult = new Page<>(request.getCurrent(), request.getSize(), total);

        if (total == 0) {
            pageResult.setRecords(Collections.emptyList());
            return pageResult;
        }

        // 2. 在Java内存中计算分页
        long fromIndex = (long) (request.getCurrent() - 1) * request.getSize();
        if (fromIndex >= total) {
            pageResult.setRecords(Collections.emptyList());
            return pageResult;
        }
        long toIndex = Math.min(fromIndex + request.getSize(), total);
        List<Long> idsForCurrentPage = allMatchingIds.subList((int)fromIndex, (int)toIndex);

        // 3. 查询当前页ID的详情
        if(CollectionUtils.isEmpty(idsForCurrentPage)){
            pageResult.setRecords(Collections.emptyList());
            return pageResult;
        }
        List<QuestionVO> questionVos = questionMapper.findVosByIds(idsForCurrentPage, request.getUserId());
        pageResult.setRecords(questionVos);

        return pageResult;
    }

    /**
     * [私有] 封装了“两步查询法”的第二步
     */
    private IPage<QuestionVO> fetchDetailsForPage(IPage<Long> idPage, Long userId) {
        if (CollectionUtils.isEmpty(idPage.getRecords())) {
            return new Page<>(idPage.getCurrent(), idPage.getSize(), idPage.getTotal());
        }
        List<Long> questionIds = idPage.getRecords();
        List<QuestionVO> questionVos = questionMapper.findVosByIds(questionIds, userId);
        IPage<QuestionVO> finalPage = new Page<>(idPage.getCurrent(), idPage.getSize(), idPage.getTotal());
        finalPage.setRecords(questionVos);
        return finalPage;
    }


}