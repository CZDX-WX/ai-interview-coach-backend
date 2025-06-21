package com.czdxwx.aiinterviewcoachbackend.service.impl;

// 1. 导入 Spring Data 的 Page 和 Pageable
import com.czdxwx.aiinterviewcoachbackend.entity.ForumCategory;
import com.czdxwx.aiinterviewcoachbackend.entity.ForumPost;
import com.czdxwx.aiinterviewcoachbackend.entity.ForumThread;
import com.czdxwx.aiinterviewcoachbackend.entity.User;
import com.czdxwx.aiinterviewcoachbackend.mapper.ForumCategoryMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.ForumPostMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.ForumThreadMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.UserMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
// com.baomidou.mybatisplus.extension.plugins.pagination.Page 将不再导入，而是使用完全限定名
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.czdxwx.aiinterviewcoachbackend.config.security.SecurityUtils;
import com.czdxwx.aiinterviewcoachbackend.service.ForumService;
import com.czdxwx.aiinterviewcoachbackend.service.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class ForumServiceImpl implements ForumService {

    private final Logger log = LoggerFactory.getLogger(ForumServiceImpl.class);

    private final ForumCategoryMapper categoryMapper;
    private final ForumThreadMapper threadMapper;
    private final ForumPostMapper postMapper;
    private final UserMapper userMapper;

    public ForumServiceImpl(ForumCategoryMapper categoryMapper, ForumThreadMapper threadMapper, ForumPostMapper postMapper, UserMapper userMapper) {
        this.categoryMapper = categoryMapper;
        this.threadMapper = threadMapper;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
    }

    private User getCurrentUser() {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(userMapper::findOneByUsernameWithAuthorities)
                .orElseThrow(() -> new IllegalStateException("用户未登录或无法获取用户信息"));
    }
    // --- DTO 转换辅助方法 (核心修改点) ---
    private AuthorInfoDto mapUserToAuthorInfo(User user) { // <-- 返回类型改为 AuthorInfoDto
        if (user == null) {
            // 返回一个代表“已注销用户”的默认对象，避免null
            AuthorInfoDto anonymous = new AuthorInfoDto();
            anonymous.setName("已注销用户");
            return anonymous;
        }
        AuthorInfoDto authorInfo = new AuthorInfoDto();
        authorInfo.setUserId(String.valueOf(user.getId()));
        authorInfo.setName(user.getFullName() != null ? user.getFullName() : user.getUsername());
        authorInfo.setAvatarUrl(user.getAvatarUrl());
        return authorInfo;
    }

    private PostDto mapPostToDto(ForumPost post, Map<Long, User> authorMap) {
        PostDto dto = new PostDto();
        dto.setId(String.valueOf(post.getId()));
        dto.setContent(post.getContent());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setIsOp(post.getIsOp());

        // 现在 mapUserToAuthorInfo 返回的是 AuthorInfoDto，与 PostDto 的 author 字段类型匹配
        if (post.getAuthorId() != null && authorMap.containsKey(post.getAuthorId())) {
            dto.setAuthor(mapUserToAuthorInfo(authorMap.get(post.getAuthorId())));
        } else if (post.getAuthorName() != null) {
            AuthorInfoDto anonymous = new AuthorInfoDto();
            anonymous.setName(post.getAuthorName());
            dto.setAuthor(anonymous);
        }
        return dto;
    }

    private ThreadSummaryDto mapThreadToDto(ForumThread thread, Map<Long, User> authorMap) {
        ThreadSummaryDto dto = new ThreadSummaryDto();
        BeanUtils.copyProperties(thread, dto, "id", "author", "lastReplyAuthor");
        dto.setId(String.valueOf(thread.getId()));
        dto.setCategoryId(String.valueOf(thread.getCategoryId())); // <-- 确保这里有赋值
        // 现在 mapUserToAuthorInfo 返回的是 AuthorInfoDto，与 ThreadSummaryDto 的 author 字段类型匹配
        if (thread.getAuthorId() != null && authorMap.containsKey(thread.getAuthorId())) {
            dto.setAuthor(mapUserToAuthorInfo(authorMap.get(thread.getAuthorId())));
        } else if (thread.getAuthorName() != null) {
            AuthorInfoDto anonymous = new AuthorInfoDto();
            anonymous.setName(thread.getAuthorName());
            dto.setAuthor(anonymous);
        }

        if (thread.getLastReplyAuthorId() != null && authorMap.containsKey(thread.getLastReplyAuthorId())) {
            dto.setLastReplyAuthor(mapUserToAuthorInfo(authorMap.get(thread.getLastReplyAuthorId())));
        } else if (thread.getLastReplyAuthorName() != null) {
            AuthorInfoDto anonymous = new AuthorInfoDto();
            anonymous.setName(thread.getLastReplyAuthorName());
            dto.setLastReplyAuthor(anonymous);
        }
        return dto;
    }


    @Override
    @Transactional(readOnly = true)
    public List<ForumCategoryDto> getAllCategories() {
        List<ForumCategory> categories = categoryMapper.selectList(new QueryWrapper<ForumCategory>().orderByAsc("display_order"));
        // 批量获取最新主题信息以减少查询
        List<Long> lastThreadIds = categories.stream()
                .map(ForumCategory::getLastThreadId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, ForumThread> lastThreadsMap = lastThreadIds.isEmpty() ? Collections.emptyMap() :
                threadMapper.selectBatchIds(lastThreadIds).stream().collect(Collectors.toMap(ForumThread::getId, t -> t));

        return categories.stream().map(cat -> {
            ForumCategoryDto dto = new ForumCategoryDto();
            dto.setId(String.valueOf(cat.getId()));
            dto.setName(cat.getName());
            dto.setDescription(cat.getDescription());
            dto.setThreadCount(cat.getThreadCount());
            dto.setPostCount(cat.getPostCount());

            if (cat.getLastThreadId() != null && lastThreadsMap.containsKey(cat.getLastThreadId())) {
                ForumThread lastThread = lastThreadsMap.get(cat.getLastThreadId());
                ForumCategoryDto.LastThreadInfo lastThreadInfo = new ForumCategoryDto.LastThreadInfo();
                lastThreadInfo.setThreadId(String.valueOf(lastThread.getId()));
                lastThreadInfo.setTitle(lastThread.getTitle());
                lastThreadInfo.setAuthorName(lastThread.getLastReplyAuthorName() != null ? lastThread.getLastReplyAuthorName() : lastThread.getAuthorName());
                lastThreadInfo.setTimestamp(lastThread.getLastReplyAt() != null ? lastThread.getLastReplyAt() : lastThread.getCreatedAt());
                dto.setLastThread(lastThreadInfo);
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ThreadSummaryDto> getThreadsByCategory(Long categoryId, Pageable pageable) {
        log.debug("请求获取分类 {} 下的主题列表", categoryId);

        // **核心修正点**: 实例化 Mybatis-Plus 的 Page 时使用完全限定名
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ForumThread> mybatisPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());

        QueryWrapper<ForumThread> queryWrapper = new QueryWrapper<ForumThread>()
                .eq("category_id", categoryId)
                .orderByDesc("is_pinned", "last_reply_at", "created_at");

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ForumThread> resultPage =
                threadMapper.selectPage(mybatisPage, queryWrapper);

        // ... (批量获取作者信息和 DTO 转换逻辑保持不变) ...
        Set<Long> authorIds = resultPage.getRecords().stream()
                .flatMap(thread -> Stream.of(thread.getAuthorId(), thread.getLastReplyAuthorId()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> authorMap = authorIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectBatchIds(authorIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        List<ThreadSummaryDto> dtoList = resultPage.getRecords().stream()
                .map(thread -> mapThreadToDto(thread, authorMap))
                .collect(Collectors.toList());

        // 使用 PageImpl 将结果封装成 Spring Data 的 Page 对象
        return new PageImpl<>(dtoList, pageable, resultPage.getTotal());
    }

    @Override
    @Transactional
    public ThreadDetailDto getThreadDetails(Long threadId, Pageable pageable) {
        log.debug("请求获取主题 {} 的详情", threadId);
        threadMapper.incrementViewCount(threadId);

        ForumThread thread = threadMapper.selectById(threadId);
        if (thread == null) throw new RuntimeException("主题未找到或已被删除。");

        // **核心修正点**: 实例化 Mybatis-Plus 的 Page 时使用完全限定名
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ForumPost> postMybatisPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());

        QueryWrapper<ForumPost> postQueryWrapper = new QueryWrapper<ForumPost>()
                .eq("thread_id", threadId)
                .orderByAsc("created_at");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ForumPost> resultPostPage =
                postMapper.selectPage(postMybatisPage, postQueryWrapper);

        // ... (批量获取作者信息和 DTO 转换逻辑保持不变) ...
        Set<Long> authorIds = resultPostPage.getRecords().stream()
                .map(ForumPost::getAuthorId)
                .collect(Collectors.toSet());
        authorIds.add(thread.getAuthorId());
        authorIds.add(thread.getLastReplyAuthorId());
        authorIds.remove(null);

        Map<Long, User> authorMap = authorIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectBatchIds(authorIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        ThreadSummaryDto threadSummaryDto = mapThreadToDto(thread, authorMap);
        List<PostDto> postDtoList = resultPostPage.getRecords().stream()
                .map(post -> mapPostToDto(post, authorMap))
                .collect(Collectors.toList());

        Page<PostDto> postDtoPage = new PageImpl<>(postDtoList, pageable, resultPostPage.getTotal());

        ThreadDetailDto threadDetailDto = new ThreadDetailDto();
        threadDetailDto.setThreadInfo(threadSummaryDto);
        threadDetailDto.setPosts(postDtoPage);

        return threadDetailDto;
    }

    @Override
    @Transactional
    public ThreadSummaryDto createThread(Long categoryId, CreateThreadRequestDto dto) {
        User currentUser = getCurrentUser();
        log.info("用户 '{}' 正在分类 '{}' 下创建新主题: '{}'", currentUser.getUsername(), categoryId, dto.getTitle());

        ForumCategory category = categoryMapper.selectById(categoryId);
        if (category == null) throw new IllegalArgumentException("指定的分类不存在。");

        Instant now = Instant.now();
        String authorDisplayName = currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername();

        ForumThread thread = ForumThread.builder()
                .categoryId(categoryId)
                .authorId(currentUser.getId())
                .authorName(authorDisplayName)
                .title(dto.getTitle())
                .createdAt(now)
                .updatedAt(now)
                .lastReplyAt(now)
                .lastReplyAuthorId(currentUser.getId())
                .lastReplyAuthorName(authorDisplayName)
                .replyCount(0)
                .viewCount(0)
                .isPinned(false)
                .isLocked(false)
                .build();
        threadMapper.insert(thread);

        ForumPost post = ForumPost.builder()
                .threadId(thread.getId())
                .authorId(currentUser.getId())
                .authorName(authorDisplayName)
                .content(dto.getContent())
                .createdAt(now)
                .updatedAt(now)
                .isOp(true)
                .build();
        postMapper.insert(post);

        category.setThreadCount(category.getThreadCount() + 1);
        category.setPostCount(category.getPostCount() + 1);
        category.setLastThreadId(thread.getId());
        categoryMapper.updateById(category);

        return mapThreadToDto(thread, Map.of(currentUser.getId(), currentUser));
    }

    @Override
    @Transactional
    public PostDto createPost(Long threadId, CreatePostRequestDto dto) {
        User currentUser = getCurrentUser();
        ForumThread thread = threadMapper.selectById(threadId);
        if (thread == null) throw new IllegalArgumentException("回复的主题不存在。");
        if (thread.getIsLocked()) throw new IllegalArgumentException("该主题已被锁定，无法回复。");

        log.info("用户 '{}' 正在回复主题 '{}'", currentUser.getUsername(), thread.getTitle());
        String authorDisplayName = currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername();

        Instant now = Instant.now();
        ForumPost post = ForumPost.builder()
                .threadId(threadId)
                .authorId(currentUser.getId())
                .authorName(authorDisplayName)
                .content(dto.getContent())
                .createdAt(now)
                .updatedAt(now)
                .isOp(false)
                .build();
        postMapper.insert(post);

        thread.setReplyCount(thread.getReplyCount() + 1);
        thread.setLastReplyAt(post.getCreatedAt());
        thread.setLastReplyAuthorId(currentUser.getId());
        thread.setLastReplyAuthorName(authorDisplayName);
        threadMapper.updateById(thread);

        categoryMapper.incrementPostCount(thread.getCategoryId(), 1);

        ForumCategory category = categoryMapper.selectById(thread.getCategoryId());
        category.setLastThreadId(thread.getId());
        categoryMapper.updateById(category);

        return mapPostToDto(post, Map.of(currentUser.getId(), currentUser));
    }
}