package com.czdxwx.aiinterviewcoachbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.czdxwx.aiinterviewcoachbackend.entity.RoleTag;
import com.czdxwx.aiinterviewcoachbackend.entity.QuestionTag;
import com.czdxwx.aiinterviewcoachbackend.entity.Tag;
import com.czdxwx.aiinterviewcoachbackend.mapper.RoleTagMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.QuestionTagMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.TagMapper;
import com.czdxwx.aiinterviewcoachbackend.service.EmbeddingService;
import com.czdxwx.aiinterviewcoachbackend.service.MilvusService;
import com.czdxwx.aiinterviewcoachbackend.service.TagService;
import com.czdxwx.aiinterviewcoachbackend.service.dto.TagCreateRequest;
import com.czdxwx.aiinterviewcoachbackend.service.dto.TagSuggestionResponse;

import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签服务的最终实现类。
 * 负责所有与技术标签相关的业务逻辑，包括：
 * 1. 按条件获取标签列表。
 * 2. 为用户输入的新标签提供智能建议。
 * 3. 创建用户个性化标签。
 * 4. 为后台生成服务提供“查找或创建”的统一接口。
 */
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private static final Logger logger = LoggerFactory.getLogger(TagServiceImpl.class);

    private final TagMapper tagMapper;
    private final RoleTagMapper roleTagMapper;
    private final QuestionTagMapper questionTagMapper;
    private final EmbeddingService embeddingService;
    private final MilvusService milvusService;

    @Value("${milvus.collection.tags}")
    private String tagsCollectionName;

    private static final double TAG_SIMILARITY_THRESHOLD = 0.15;

    /**
     * 获取一个用户可见的标签列表。
     * - 如果提供了 roleId，则只返回与该角色强相关的公共标签。
     * - 否则，返回所有公共标签 + 当前用户的私有标签。
     * @param userId 当前用户ID，可为null（未登录状态只返回公共标签）
     * @param roleId (可选) 角色ID，用于筛选
     * @return 标签实体列表
     */
    @Override
    public List<Tag> getTags(Long userId, Long roleId) {
        if (roleId != null) {
            // 场景1：用户已选择岗位，需要获取与该岗位相关的标签
            return tagMapper.findPublicTagsByRoleId(roleId);
        }

        // 场景2：通用场景，获取所有对用户可见的标签
        QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNull("owner_id") // 所有公共标签
                .or()
                .eq(userId != null, "owner_id", userId); // 以及该用户自己的私有标签
        return tagMapper.selectList(queryWrapper);
    }

    /**
     * 为用户输入的新标签提供智能建议。
     * @param tagName 用户输入的新标签名
     * @param userId 当前用户ID
     * @return 包含建议状态和内容的响应对象
     */
    @Override
    public TagSuggestionResponse suggest(String tagName, Long userId) {
        if (tagName == null || tagName.trim().isEmpty()) {
            throw new IllegalArgumentException("标签名不能为空");
        }

        // 1. 优先进行精确匹配查找
        Tag exactMatch = findByName(tagName, userId);
        if (exactMatch != null) {
            return new TagSuggestionResponse(TagSuggestionResponse.Status.EXACT_MATCH_FOUND, exactMatch, tagName);
        }

        // 2. 如果没有精确匹配，则进行语义相似度搜索
        float[] newTagVectorArray = embeddingService.getEmbedding(tagName);
        List<Float> newTagVector = toFloatList(newTagVectorArray);
        List<SearchResp.SearchResult> similarResults = milvusService.searchTags(newTagVector, 1);

        // 3. 如果找到了足够相似的结果，则返回建议
        if (!similarResults.isEmpty() && similarResults.get(0).getScore() < TAG_SIMILARITY_THRESHOLD) {
            long similarTagId = Long.parseLong(similarResults.get(0).getId().toString());
            Tag similarTag = tagMapper.selectById(similarTagId);
            return new TagSuggestionResponse(TagSuggestionResponse.Status.SIMILAR_TAG_FOUND, similarTag, tagName);
        }

        // 4. 如果既无精确匹配也无相似匹配，则告知前端可以安全创建
        return new TagSuggestionResponse(TagSuggestionResponse.Status.NO_SIMILAR_FOUND, null, tagName);
    }

    /**
     * 为用户创建新的个性化标签，并自动关联到指定岗位。
     * @param request 包含标签名和可选岗位ID的请求
     * @param userId 当前用户ID
     * @return 创建成功后的标签实体
     */
    @Override
    @Transactional
    public Tag create(TagCreateRequest request, Long userId) {
        String tagName = request.getName();
        if (userId == null) {
            throw new SecurityException("只有登录用户才能创建自定义标签");
        }

        Tag existingTag = findByName(tagName, userId);
        if (existingTag != null) {
            throw new IllegalArgumentException(String.format("标签 '%s' 已存在，无需重复创建。", tagName));
        }

        return createNewPrivateTag(tagName, userId, request.getRoleId());
    }

    /**
     * [供后台服务使用] 解析AI生成的标签，找到或创建它，并确保其与角色和问题关联。
     * @param tagNames AI生成的标签名列表
     * @param questionId 新创建的题目ID
     * @param ownerId 题目的拥有者ID (公共题为null)
     * @param roleId 题目所属的岗位ID
     */
    @Override
    @Transactional
    public void resolveAndAssociateTags(List<String> tagNames, Long questionId, Long ownerId, Long roleId) {
        if (tagNames == null || tagNames.isEmpty()) return;

        for (String tagName : tagNames) {
            Tag tag = findOrCreateForGeneration(tagName, ownerId);
            if (tag != null) {
                associateTag(tag.getId(), questionId, roleId);
            }
        }
    }

    /**
     * [供内部使用] 为生成流程查找或创建标签的逻辑
     */
    private Tag findOrCreateForGeneration(String tagName, Long ownerId) {
        if (!StringUtils.hasText(tagName)) return null;

        Tag existingTag = findByName(tagName, ownerId);
        if (existingTag != null) {
            return existingTag;
        }

        // 如果是公共题库生成流程 (ownerId is null)，我们不希望AI随意创建新的公共标签
        if (ownerId == null) {
            logger.warn("AI generated a non-existent public tag '{}', which will be ignored to maintain data quality.", tagName);
            return null;
        }

        // 只有在为特定用户生成私有题目时，才创建新的私有标签
        return createNewPrivateTag(tagName, ownerId, null); // AI生成的标签不与特定roleId强关联
    }


    /**
     * [供内部使用] 根据一批标签名，获取它们对应的向量
     */
    @Override
    public List<List<Float>> getVectorsByTagNames(List<String> tagNames) {
        if (CollectionUtils.isEmpty(tagNames)) {
            return Collections.emptyList();
        }

        List<Tag> tags = tagMapper.selectList(new QueryWrapper<Tag>().in("name", tagNames).isNull("owner_id"));
        if (tags.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = tags.stream().map(Tag::getId).collect(Collectors.toList());

        return milvusService.getVectorsByIds(tagsCollectionName, "tag_id", tagIds);
    }

    /**
     * [供内部使用] 根据名字查找用户可见的标签（自己的或公共的）
     */
    public Tag findByName(String tagName, Long userId) {
        if (!StringUtils.hasText(tagName)) return null;
        QueryWrapper<Tag> qw = new QueryWrapper<Tag>()
                .eq("name", tagName)
                .and(w -> w.eq(userId != null, "owner_id", userId).or().isNull("owner_id"))
                .orderByDesc("owner_id")
                .last("LIMIT 1");
        return tagMapper.selectOne(qw);
    }

    /**
     * [私有] 创建一个新的私有标签的核心实现
     */
    private Tag createNewPrivateTag(String tagName, Long userId, Long roleId) {
        logger.info("Creating new custom tag '{}' for user {}", tagName, userId);
        Tag newTag = new Tag();
        newTag.setName(tagName);
        newTag.setOwnerId(userId);
        newTag.setCreatedAt(new Date());
        tagMapper.insert(newTag);

        float[] newTagVectorArray = embeddingService.getEmbedding(tagName);
        milvusService.insertTagVector(newTag.getId(), toFloatList(newTagVectorArray));

        if (roleId != null) {
            associateTag(newTag.getId(), null, roleId); // 关联到角色
        }

        return newTag;
    }

    /**
     * [私有] 封装关联逻辑
     */
    private void associateTag(Long tagId, Long questionId, Long roleId) {
        if (questionId != null && questionTagMapper.selectCount(new QueryWrapper<QuestionTag>().eq("question_id", questionId).eq("tag_id", tagId)) == 0) {
            questionTagMapper.insert(new QuestionTag(questionId, tagId));
        }
        if (roleId != null && roleTagMapper.selectCount(new QueryWrapper<RoleTag>().eq("role_id", roleId).eq("tag_id", tagId)) == 0) {
            roleTagMapper.insert(new RoleTag(roleId, tagId));
        }
    }

    private List<Float> toFloatList(float[] array) {
        if (array == null) return new ArrayList<>();
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }

    /**
     * 【新增】实现接口中定义的新方法
     */
    @Override
    public List<String> getTagsByQuestionId(Long questionId) {
        if (questionId == null) {
            return Collections.emptyList();
        }
        return tagMapper.findTagsByQuestionId(questionId);
    }
}