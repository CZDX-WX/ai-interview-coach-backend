package com.czdxwx.aiinterviewcoachbackend.mapper.postgres;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.czdxwx.aiinterviewcoachbackend.entity.Question;
import com.pgvector.PGvector;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    // language=PostgreSQL
    @Select("SELECT * FROM questions ORDER BY question_vector <=> #{embedding} LIMIT #{limit}")
    List<Question> findNearestNeighbors(@Param("embedding") PGvector embedding, @Param("limit") int limit);
}