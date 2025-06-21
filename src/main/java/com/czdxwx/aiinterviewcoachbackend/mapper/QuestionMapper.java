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
     * 【新增】分页查询某个用户还未做过的公共题目
     */
    IPage<QuestionVO> findUnpracticedPublicQuestions(IPage<QuestionVO> page, @Param("userId") Long userId);

    IPage<Long> searchQuestionIds(IPage<Long> page, @Param("request") QuestionSearchRequest request);

    /**
     * 【核心修改】方法签名增加 userId 参数
     */
    List<QuestionVO> findVosByIds(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    /**
     * 【新增】统计对一个用户可见的题目总数
     */
    long countVisibleQuestions(@Param("userId") Long userId);
}
