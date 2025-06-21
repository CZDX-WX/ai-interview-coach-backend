package com.czdxwx.aiinterviewcoachbackend.service;


import com.czdxwx.aiinterviewcoachbackend.entity.Tag;
import com.czdxwx.aiinterviewcoachbackend.service.dto.TagCreateRequest;
import com.czdxwx.aiinterviewcoachbackend.service.dto.TagSuggestionResponse;

import java.util.List;

public interface TagService {
    /**
     * 获取用户可见的标签列表
     */
    List<Tag> getTags(Long userId, Long roleId);

    /**
     * 为新标签提供智能建议
     */
    TagSuggestionResponse suggest(String tagName, Long userId);

    /**
     * 创建用户个性化标签
     */
    Tag create(TagCreateRequest request, Long userId);

    /**
     * [供内部使用] 为生成流程解析并关联标签
     */
    void resolveAndAssociateTags(List<String> tagNames, Long questionId, Long ownerId, Long roleId);

    /**
     * [供内部使用] 根据一批标签名，获取它们对应的向量
     */
    List<List<Float>> getVectorsByTagNames(List<String> tagNames);

    /**
     * 【新增】根据题目ID获取其所有标签名
     */
    List<String> getTagsByQuestionId(Long questionId);
}