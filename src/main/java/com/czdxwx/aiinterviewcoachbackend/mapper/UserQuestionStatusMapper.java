package com.czdxwx.aiinterviewcoachbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.czdxwx.aiinterviewcoachbackend.config.mybatis.StringToListTypeHandler;
import com.czdxwx.aiinterviewcoachbackend.entity.UserQuestionStatus;
import com.czdxwx.aiinterviewcoachbackend.vo.UserQuestionStatusVO;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
public interface UserQuestionStatusMapper extends BaseMapper<UserQuestionStatus> {

    List<UserQuestionStatusVO> findPracticeHistoryForUser(@Param("userId") Long userId);

    /**
     * 【新增】从给定的题目ID列表中，查询出指定用户已经练习过的ID
     * @param userId      用户ID
     * @param questionIds 候选题目ID列表
     * @return 该用户已练习过的题目ID集合
     */
    Set<Long> findPracticedQuestionIds(@Param("userId") Long userId, @Param("questionIds") List<Long> questionIds);

    /**
     * 【新增】按熟练度状态分组，统计每个状态下的题目数量
     * @param userId 用户ID
     * @return 一个Map列表，每个Map包含 "proficiency_status" 和 "count"
     */
    @MapKey("proficiency_status")
    List<Map<String, Object>> countByProficiencyStatus(@Param("userId") Long userId);

    /**
     * 【新增】统计指定用户收藏的题目总数
     * @param userId 用户ID
     * @return 收藏的题目数量
     */
    long countBookmarked(@Param("userId") Long userId);
}




