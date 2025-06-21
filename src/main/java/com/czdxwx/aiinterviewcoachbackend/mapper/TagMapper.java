package com.czdxwx.aiinterviewcoachbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.czdxwx.aiinterviewcoachbackend.entity.Tag;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    // language=MySQL
    @Select("SELECT t.name FROM tags t " +
            "JOIN question_tags qt ON t.id = qt.tag_id " +
            "WHERE qt.question_id = #{questionId}")
    List<String> findTagsByQuestionId(@Param("questionId") Long questionId);

    /**
     * 【核心】根据角色ID，查询其关联的所有公共标签
     */
    // language=MySQL
    @Select("SELECT t.* FROM tags t " +
            "JOIN role_tags rt ON t.id = rt.tag_id " +
            "WHERE rt.role_id = #{roleId} AND t.owner_id IS NULL")
    List<Tag> findPublicTagsByRoleId(@Param("roleId") Long roleId);
}



