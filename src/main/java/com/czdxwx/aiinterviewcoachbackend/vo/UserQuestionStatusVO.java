package com.czdxwx.aiinterviewcoachbackend.vo;
import com.czdxwx.aiinterviewcoachbackend.model.enums.ProficiencyStatus;
import lombok.Data;
import java.util.Date;
import java.util.List;
/**
 * 用户刷题历史的视图对象 (VO)
 * 用于向前端展示一个包含题目详情和标签的完整练习记录
 */
@Data
public class UserQuestionStatusVO {

    // 来自 user_question_status 表
    private Long id;
    private Long userId;
    private Long questionId;
    private ProficiencyStatus proficiencyStatus;
    private Date lastPracticedAt;
    private String notes;

    // --- 从其他表 JOIN 查询来的额外信息 ---

    // 来自 questions 表
    private String questionText;
    private String difficulty;

    // 来自 tags 表 (通过 question_tags 关联)
    private List<String> tags;
}