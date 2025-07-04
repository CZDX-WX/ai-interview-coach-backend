package com.czdxwx.aiinterviewcoachbackend.controller;

import com.czdxwx.aiinterviewcoachbackend.config.security.CustomUserDetails;
import com.czdxwx.aiinterviewcoachbackend.entity.Tag;
import com.czdxwx.aiinterviewcoachbackend.service.TagService;
import com.czdxwx.aiinterviewcoachbackend.service.dto.TagCreateRequest;
import com.czdxwx.aiinterviewcoachbackend.service.dto.TagSuggestionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 负责所有与技术标签资源相关的HTTP请求处理。
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * 获取对用户可见的标签列表
     */
    @GetMapping("")
    public ResponseEntity<List<Tag>> getVisibleTags(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) Long roleId) {

        List<Tag> tags;
        if (roleId != null) {
            // 如果按岗位筛选，则返回该岗位的公共标签 + 用户自己的私有标签（如果也关联了的话）
            // 这个逻辑可以在Service中进一步细化，这里先合并
            List<Tag> publicTags = tagService.getPublicTagsByRoleId(roleId);
            List<Tag> privateTags = (currentUser != null) ? tagService.getPrivateTagsForUser(currentUser.getId()) : List.of();
            tags = Stream.concat(publicTags.stream(), privateTags.stream()).distinct().collect(Collectors.toList());
        } else {
            // 否则，返回所有公共标签 + 用户自己的私有标签
            List<Tag> publicTags = tagService.getPublicTags();
            List<Tag> privateTags = (currentUser != null) ? tagService.getPrivateTagsForUser(currentUser.getId()) : List.of();
            tags = Stream.concat(publicTags.stream(), privateTags.stream()).distinct().collect(Collectors.toList());
        }

        return ResponseEntity.ok(tags);
    }

    /**
     * 为当前用户创建一个新的私有标签
     */
    @PostMapping("")
    public ResponseEntity<Tag> createPrivateTag(
            @Valid @RequestBody TagCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Tag createdTag = tagService.createPrivateTag(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTag);
    }

    /**
     * 删除当前用户的一个私有标签
     */
    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> deletePrivateTag(
            @PathVariable Long tagId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        tagService.deletePrivateTag(tagId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}