package com.czdxwx.aiinterviewcoachbackend.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.czdxwx.aiinterviewcoachbackend.entity.ForumCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ForumCategoryMapper extends BaseMapper<ForumCategory> {
    @Update("UPDATE forum_category SET thread_count = thread_count + #{count} WHERE id = #{id}")
    int incrementThreadCount(@Param("id") Long id, @Param("count") int count);

    @Update("UPDATE forum_category SET post_count = post_count + #{count} WHERE id = #{id}")
    int incrementPostCount(@Param("id") Long id, @Param("count") int count);
}