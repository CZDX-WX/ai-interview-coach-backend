package com.czdxwx.aiinterviewcoachbackend.vo;
import com.czdxwx.aiinterviewcoachbackend.entity.Question;
import com.czdxwx.aiinterviewcoachbackend.model.enums.ProficiencyStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Question 的视图对象，用于向前端展示一个“大而全”的题目信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL) // 序列化为JSON时，不包含值为null的字段
public class QuestionVO extends Question {

    /**
     * 该题目关联的所有标签
     */
    private List<String> tags;

    /**
     * 【新增】当前用户对该题目的熟练度状态
     * 如果用户从未练习过，此字段为 null
     */
    private ProficiencyStatus proficiencyStatus;

    /**
     * 【新增】当前用户是否收藏了该题目
     * 如果用户从未练习过，此字段为 null (或 false，取决于数据库默认值)
     */
    private Boolean isBookmarked;
}