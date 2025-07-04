package com.czdxwx.aiinterviewcoachbackend.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.czdxwx.aiinterviewcoachbackend.entity.QuestionTag;
import com.czdxwx.aiinterviewcoachbackend.entity.RoleTag;
import com.czdxwx.aiinterviewcoachbackend.entity.Tag;
import com.czdxwx.aiinterviewcoachbackend.mapper.QuestionTagMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.RoleTagMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.TagMapper;
import com.czdxwx.aiinterviewcoachbackend.service.TagService;
import com.czdxwx.aiinterviewcoachbackend.service.dto.TagCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final RoleTagMapper roleTagMapper;
    private final QuestionTagMapper questionTagMapper;
    @Override
    public List<Tag> getPublicTags() {
        return tagMapper.selectList(new QueryWrapper<Tag>().isNull("owner_id"));
    }

    @Override
    public List<Tag> getPublicTagsByRoleId(Long roleId) {
        return tagMapper.findPublicTagsByRoleId(roleId);
    }

    @Override
    public List<Tag> getPrivateTagsForUser(Long userId) {
        return tagMapper.selectList(new QueryWrapper<Tag>().eq("owner_id", userId));
    }

    @Override
    @Transactional
    public Tag createPublicTag(TagCreateRequest request) {
        if (tagMapper.exists(new QueryWrapper<Tag>().eq("name", request.getName()).isNull("owner_id"))) {
            throw new IllegalArgumentException(String.format("公共标签 '%s' 已存在。", request.getName()));
        }
        Tag tag = new Tag();
        tag.setName(request.getName());
        tag.setOwnerId(null);
        tag.setCreatedAt(new Date());
        tagMapper.insert(tag);
        return tag;
    }

    @Override
    @Transactional
    public Tag createPrivateTag(TagCreateRequest request, Long userId) {
        if (tagMapper.exists(new QueryWrapper<Tag>().eq("name", request.getName()).eq("owner_id", userId))) {
            throw new IllegalArgumentException(String.format("您已创建过标签 '%s'。", request.getName()));
        }
        Tag tag = new Tag();
        tag.setName(request.getName());
        tag.setOwnerId(userId);
        tag.setCreatedAt(new Date());
        tagMapper.insert(tag);

        // 用户创建私有标签时，也可以选择性地立即关联到某个岗位
        if (request.getRoleId() != null) {
            roleTagMapper.insert(new RoleTag(request.getRoleId(), tag.getId()));
        }
        return tag;
    }

    @Override
    @Transactional
    public void associateTagToRole(Long roleId, Long tagId) {
        if (!roleTagMapper.exists(new QueryWrapper<RoleTag>().eq("role_id", roleId).eq("tag_id", tagId))) {
            roleTagMapper.insert(new RoleTag(roleId, tagId));
        }
    }

    @Override
    @Transactional
    public void deletePublicTag(Long tagId) {
        tagMapper.delete(new QueryWrapper<Tag>().eq("id", tagId).isNull("owner_id"));
    }

    @Override
    @Transactional
    public void deletePrivateTag(Long tagId, Long userId) {
        int deletedRows = tagMapper.delete(new QueryWrapper<Tag>().eq("id", tagId).eq("owner_id", userId));
        if (deletedRows == 0) {
            throw new AccessDeniedException("删除失败：标签不存在或您没有权限。");
        }
    }


    /**
     * [供后台服务使用] 解析AI生成的标签，找到或创建它，并确保其与角色和问题关联。
     *
     * @param tagNames   请求的标签
     * @param questionId 新创建的题目ID
     * @param ownerId    题目的拥有者ID (公共题为null)
     * @param roleId     题目所属的岗位ID
     */
    @Override
    @Transactional
    public void resolveAndAssociateTags(List<String> tagNames, Long questionId, Long ownerId, Long roleId) {
        if (tagNames == null || tagNames.isEmpty()) return;

        for (String tagName : tagNames) {
            associateTag(findByName(tagName,ownerId).getId(), questionId, roleId);
        }
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

}