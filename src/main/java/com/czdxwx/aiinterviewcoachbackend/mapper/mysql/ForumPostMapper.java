package com.czdxwx.aiinterviewcoachbackend.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.czdxwx.aiinterviewcoachbackend.entity.ForumPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ForumPostMapper extends BaseMapper<ForumPost> {
    @Update("UPDATE forum_post SET author_id = NULL, author_name = #{anonymousName} WHERE author_id = #{userId}")
    int anonymizePostsByUserId(@Param("userId") Long userId, @Param("anonymousName") String anonymousName);
}