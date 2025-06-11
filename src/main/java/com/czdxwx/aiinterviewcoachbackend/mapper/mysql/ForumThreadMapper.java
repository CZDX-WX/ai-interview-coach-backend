package com.czdxwx.aiinterviewcoachbackend.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.czdxwx.aiinterviewcoachbackend.entity.ForumThread;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ForumThreadMapper extends BaseMapper<ForumThread> {
    @Update("UPDATE forum_thread SET reply_count = reply_count + #{count} WHERE id = #{id}")
    int incrementReplyCount(@Param("id") Long id, @Param("count") int count);

    @Update("UPDATE forum_thread SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    @Update("UPDATE forum_thread SET author_id = NULL, author_name = #{anonymousName} WHERE author_id = #{userId}")
    int anonymizeThreadsByUserId(@Param("userId") Long userId, @Param("anonymousName") String anonymousName);
}