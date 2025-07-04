package com.czdxwx.aiinterviewcoachbackend.service;


import com.czdxwx.aiinterviewcoachbackend.entity.Tag;
import com.czdxwx.aiinterviewcoachbackend.service.dto.TagCreateRequest;
import com.czdxwx.aiinterviewcoachbackend.service.dto.TagSuggestionResponse;
import com.czdxwx.aiinterviewcoachbackend.vo.TagInRoleVO;

import java.util.List;

public interface TagService {

    /**
     * 获取所有公共标签
     */
    List<Tag> getPublicTags();

    /**
     * 获取指定角色关联的所有公共标签
     */
    List<Tag> getPublicTagsByRoleId(Long roleId);

    /**
     * 获取指定用户的私有标签
     */
    List<Tag> getPrivateTagsForUser(Long userId);

    /**
     * 创建一个公共标签 (管理员权限)
     */
    Tag createPublicTag(TagCreateRequest request);

    /**
     * 为指定用户创建一个私有标签
     */
    Tag createPrivateTag(TagCreateRequest request, Long userId);

    /**
     * 将一个标签关联到一个岗位 (管理员权限)
     */
    void associateTagToRole(Long roleId, Long tagId);

    /**
     * 删除一个公共标签 (管理员权限)
     */
    void deletePublicTag(Long tagId);

    /**
     * 删除一个用户的私有标签
     */
    void deletePrivateTag(Long tagId, Long userId);


    /**
     * 生成题目中为题目绑定标签
     */
    void resolveAndAssociateTags(List<String> tagNames, Long questionId, Long ownerId, Long roleId);
}