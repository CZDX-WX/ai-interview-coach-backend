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

    private Long questionId;
    private Long tagId;
}