package com.czdxwx.aiinterviewcoachbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.czdxwx.aiinterviewcoachbackend.entity.UserQuestionStatus;
import com.czdxwx.aiinterviewcoachbackend.mapper.mysql.UserMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.postgres.UserQuestionStatusMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.postgres.UserQuestionStatusVO;
import com.czdxwx.aiinterviewcoachbackend.service.dto.UserStatusUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserPracticeService {

    private final UserMapper userMapper;
    private final UserQuestionStatusMapper userQuestionStatusMapper;

    @Transactional("postgresTransactionManager")
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

    public List<UserQuestionStatusVO> findPracticeHistoryForUser(Long userId) {
        if (userMapper.selectById(userId) == null) {
            throw new NoSuchElementException("查询失败：用户ID " + userId + " 不存在！");
        }
        return userQuestionStatusMapper.findUserPracticeHistory(userId);
    }
}