package com.czdxwx.aiinterviewcoachbackend.mapper.postgres;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.czdxwx.aiinterviewcoachbackend.entity.UserQuestionStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserQuestionStatusMapper extends BaseMapper<UserQuestionStatus> {


    // language=PostgreSQL
    @Select("SELECT " +
            "  s.*, " +
            "  q.question_text, " +
            "  q.difficulty as question_difficulty " +
            "FROM " +
            "  user_question_status s " +
            "JOIN " +
            "  questions q ON s.question_id = q.id " +
            "WHERE " +
            "  s.user_id = #{userId}")
    List<UserQuestionStatusVO> findUserPracticeHistory(@Param("userId") Long userId);
}




