package com.czdxwx.aiinterviewcoachbackend.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("question_tags")
@NoArgsConstructor
@AllArgsConstructor
public class QuestionTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 注意：这里没有任何 @TableId 注解。
     * 字段名通过 MyBatis-Plus 的驼峰转下划线规则，
     * 自动映射到数据库的 question_id 和 tag_id 列。
     */
    private Long questionId;

    private Long tagId;
}