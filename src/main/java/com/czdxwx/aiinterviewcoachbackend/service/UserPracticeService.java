package com.czdxwx.aiinterviewcoachbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.czdxwx.aiinterviewcoachbackend.entity.Question;
import com.czdxwx.aiinterviewcoachbackend.entity.UserQuestionStatus;
import com.czdxwx.aiinterviewcoachbackend.mapper.QuestionMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.UserMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.UserQuestionStatusMapper;
import com.czdxwx.aiinterviewcoachbackend.model.enums.ProficiencyStatus;
import com.czdxwx.aiinterviewcoachbackend.service.dto.UserStatusUpdateRequest;
import com.czdxwx.aiinterviewcoachbackend.vo.PracticeStatsVO;
import com.czdxwx.aiinterviewcoachbackend.vo.QuestionVO;
import com.czdxwx.aiinterviewcoachbackend.vo.UserQuestionStatusVO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;


@Service
@RequiredArgsConstructor
public class UserPracticeService {
    private Logger logger = LoggerFactory.getLogger(UserPracticeService.class);
    private final UserMapper userMapper;
    private final UserQuestionStatusMapper userQuestionStatusMapper;
    private final QuestionMapper questionMapper;

    @Transactional
    public UserQuestionStatus updateUserQuestionStatus(Long userId, Long questionId, UserStatusUpdateRequest request) {
        if (userMapper.selectById(userId) == null) {
            throw new NoSuchElementException("用户不存在: " + userId);
        }

        QueryWrapper<UserQuestionStatus> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).eq("question_id", questionId);
        UserQuestionStatus statusRecord = userQuestionStatusMapper.selectOne(qw);

        if (statusRecord != null) {
            statusRecord.setProficiencyStatus(request.getStatus());
            if(request.getNotes() != null) {
                statusRecord.setNotes(request.getNotes());
            }
            statusRecord.setLastPracticedAt(new Date());
            userQuestionStatusMapper.updateById(statusRecord);
        } else {
            statusRecord = new UserQuestionStatus();
            statusRecord.setUserId(userId);
            statusRecord.setQuestionId(questionId);
            statusRecord.setProficiencyStatus(request.getStatus());
            statusRecord.setNotes(request.getNotes());
            userQuestionStatusMapper.insert(statusRecord);
        }
        return statusRecord;
    }

    /**
     * 【完整实现】获取指定用户的完整刷题历史
     */
    public List<UserQuestionStatusVO> findPracticeHistoryForUser(Long userId) {
        if (userMapper.selectById(userId) == null) {
            throw new NoSuchElementException("查询失败：用户ID " + userId + " 不存在！");
        }
        // 直接调用 Mapper 中的自定义联表查询方法
        return userQuestionStatusMapper.findPracticeHistoryForUser(userId);
    }


    /**
     * 【完整实现】切换题目的收藏状态
     * @param userId      当前用户ID
     * @param questionId  被操作的题目ID
     * @param bookmarked  目标状态 (true为收藏, false为取消收藏)
     * @return 更新或创建后的状态实体
     */
    @Transactional
    public UserQuestionStatus toggleBookmark(Long userId, Long questionId, boolean bookmarked) {
        // 步骤1: 校验用户是否存在
        if (userMapper.selectById(userId) == null) {
            throw new NoSuchElementException("用户不存在: " + userId);
        }

        // 步骤2: 查找是否已存在该用户对该题的状态记录
        QueryWrapper<UserQuestionStatus> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).eq("question_id", questionId);
        UserQuestionStatus statusRecord = userQuestionStatusMapper.selectOne(qw);

        if (statusRecord != null) {
            // 如果记录已存在，则只更新收藏状态
            statusRecord.setIsBookmarked(bookmarked);
            // 同时更新最后操作时间
            statusRecord.setLastPracticedAt(new Date());
            userQuestionStatusMapper.updateById(statusRecord);
        } else {
            // 如果记录不存在，说明用户是第一次与该题交互（直接收藏）
            // 我们需要为他创建一条新的状态记录
            statusRecord = new UserQuestionStatus();
            statusRecord.setUserId(userId);
            statusRecord.setQuestionId(questionId);
            statusRecord.setIsBookmarked(bookmarked);
            statusRecord.setLastPracticedAt(new Date());
            userQuestionStatusMapper.insert(statusRecord);
        }

        return statusRecord;
    }

    /**
     * 【新增】按状态获取用户的题目列表
     */
    public IPage<QuestionVO> getQuestionsByStatusForUser(Long userId, String status, int current, int size) {
        if (userMapper.selectById(userId) == null) {
            throw new NoSuchElementException("用户不存在: " + userId);
        }

        Page<QuestionVO> page = new Page<>(current, size);

        // 使用 QueryWrapper 来构造复杂的查询条件
        QueryWrapper<Question> qw = new QueryWrapper<>();

        switch (status.toUpperCase()) {
            case "NOT_PRACTICED":
                // 调用我们为“未学习”专门创建的 Mapper 方法
                return questionMapper.findUnpracticedPublicQuestions(page, userId);
            case "BOOKMARKED":
                // 查找所有 is_bookmarked 为 true 的题目
                qw.inSql("q.id", "SELECT question_id FROM user_question_status WHERE user_id = " + userId + " AND is_bookmarked = TRUE");
                break;
            case "NEEDS_REVIEW":
            case "MASTERED":
                // 查找所有 proficiency_status 匹配的题目
                qw.inSql("q.id", "SELECT question_id FROM user_question_status WHERE user_id = " + userId + " AND proficiency_status = '" + status.toUpperCase() + "'");
                break;
            default:
                throw new IllegalArgumentException("无效的状态查询参数: " + status);
        }

        // 对于后三种状态，我们使用通用的带标签分页查询方法
        return questionMapper.findPageWithTags(page, qw);
    }

    /**
     * 【核心重构】将题目的熟练度重置为“未学习”，这是一个无损的 UPDATE 操作
     */
    @Transactional
    public void resetStatusToUnpracticed(Long userId, Long questionId) {
        UserQuestionStatus statusRecord = findOrCreateStatusRecord(userId, questionId);

        // 只更新熟练度状态，不触碰收藏状态
        statusRecord.setProficiencyStatus(ProficiencyStatus.NOT_PRACTICED);
        statusRecord.setLastPracticedAt(new Date());
        userQuestionStatusMapper.updateById(statusRecord);

        logger.info("Reset status for user {} and question {} to NOT_PRACTICED.", userId, questionId);
    }

    /**
     * 【新增】一个核心的私有辅助方法，用于查找或创建状态记录
     * @return 一个已存在的或全新的 UserQuestionStatus 实体
     */
    private UserQuestionStatus findOrCreateStatusRecord(Long userId, Long questionId) {
        if (userMapper.selectById(userId) == null) {
            throw new NoSuchElementException("用户不存在: " + userId);
        }
        if (questionMapper.selectById(questionId) == null) {
            throw new NoSuchElementException("题目不存在: " + questionId);
        }

        QueryWrapper<UserQuestionStatus> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).eq("question_id", questionId);
        UserQuestionStatus statusRecord = userQuestionStatusMapper.selectOne(qw);

        if (statusRecord == null) {
            // 如果记录不存在，则创建一条全新的、状态为“未学习”的记录
            statusRecord = new UserQuestionStatus();
            statusRecord.setUserId(userId);
            statusRecord.setQuestionId(questionId);
            statusRecord.setProficiencyStatus(ProficiencyStatus.NOT_PRACTICED); // 默认是未学习
            statusRecord.setIsBookmarked(false); // 默认未收藏
            statusRecord.setLastPracticedAt(new Date());
            userQuestionStatusMapper.insert(statusRecord);
        }

        return statusRecord;
    }


    /**
     * 【核心修正】重构统计逻辑以匹配新的三状态模型
     */
    public PracticeStatsVO getPracticeStats(Long userId) {
        if (userMapper.selectById(userId) == null) {
            throw new NoSuchElementException("用户不存在: " + userId);
        }

        PracticeStatsVO stats = new PracticeStatsVO();

        // 1. 获取该用户可见的题目总数（所有公共题 + 自己的私有题）
        long totalVisibleQuestions = questionMapper.countVisibleQuestions(userId);
        stats.setTotalQuestions(totalVisibleQuestions);

        // 2. 获取收藏总数 (这个逻辑是独立的，保持不变)
        stats.setBookmarkedCount(userQuestionStatusMapper.countBookmarked(userId));

        // 3. 按熟练度状态分组，获取有明确状态的题目数量
        List<Map<String, Object>> countsByStatus = userQuestionStatusMapper.countByProficiencyStatus(userId);

        long masteredCount = 0;
        long needsReviewCount = 0;
        long notPracticedExplicitCount = 0; // 状态明确为 NOT_PRACTICED 的数量

        for (Map<String, Object> row : countsByStatus) {
            String statusStr = (String) row.get("proficiency_status");
            Long count = (Long) row.get("count");

            if (ProficiencyStatus.MASTERED.name().equals(statusStr)) {
                masteredCount = count;
            } else if (ProficiencyStatus.NEEDS_REVIEW.name().equals(statusStr)) {
                needsReviewCount = count;
            } else if (ProficiencyStatus.NOT_PRACTICED.name().equals(statusStr)) {
                notPracticedExplicitCount = count;
            }
        }

        // 4. 计算总的“未学习”数量
        //    它等于 “状态明确为NOT_PRACTICED的数量” + “那些在status表中完全没有记录的题目数量”
        long totalStatusRecords = masteredCount + needsReviewCount + notPracticedExplicitCount;
        long implicitNotPracticedCount = totalVisibleQuestions - totalStatusRecords;

        stats.setMasteredCount(masteredCount);
        stats.setNeedsReviewCount(needsReviewCount);
        // 最终的未学习数 = 显性未学习 + 隐性未学习
        stats.setNotPracticedCount(notPracticedExplicitCount + implicitNotPracticedCount);

        return stats;
    }
}