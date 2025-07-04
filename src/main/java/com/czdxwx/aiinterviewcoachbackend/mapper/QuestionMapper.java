package com.czdxwx.aiinterviewcoachbackend.mapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.czdxwx.aiinterviewcoachbackend.entity.Question;
import com.czdxwx.aiinterviewcoachbackend.service.dto.QuestionSearchRequest;
import com.czdxwx.aiinterviewcoachbackend.vo.QuestionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    IPage<QuestionVO> findPageWithTags(IPage<QuestionVO> page, @Param(Constants.WRAPPER) Wrapper<Question> queryWrapper);

    IPage<QuestionVO> findPageByTagNameWithTags(IPage<QuestionVO> page, @Param("tagName") String tagName);




    /**
     * [自动分页] 用于处理除 ALL_TAGS 外的所有查询场景。
     * MyBatis-Plus分页插件会拦截此方法。
     */
    IPage<Long> searchQuestionIds(IPage<Long> page, @Param("request") QuestionSearchRequest request);

    /**
     * [手动分页] 专门用于 ALL_TAGS 场景，一次性返回所有匹配的ID。
     * 此方法不会被分页插件拦截，因此可以避免报错。
     */
    List<Long> findAllQuestionIdsByAllTags(@Param("request") QuestionSearchRequest request);

    /**
     * 根据ID列表获取题目详情。
     */
    List<QuestionVO> findVosByIds(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    /**
     * 【新增】统计对一个用户可见的题目总数
     */
    long countVisibleQuestions(@Param("userId") Long userId);
    /**
     * 【新增】分页查询某个用户还未做过的公共题目
     */
    IPage<QuestionVO> findUnpracticedPublicQuestions(IPage<QuestionVO> page, @Param("userId") Long userId);
}
