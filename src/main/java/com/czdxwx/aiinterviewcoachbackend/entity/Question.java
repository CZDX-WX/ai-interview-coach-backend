package com.czdxwx.aiinterviewcoachbackend.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("questions")
public class Question implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("question_text")
    private String questionText;

    @TableField("reference_answer")
    private String referenceAnswer;

    @TableField("speech_synthesis_content")
    private String speechSynthesisContent;

    @TableField("difficulty")
    private String difficulty;

    @TableField("owner_id")
    private Long ownerId;

    @TableField("question_hash")
    private String questionHash;

    // 【重要】不再有 PGvector 类型的字段
    @TableField("speech_audio_url")
    private String speechAudioUrl; // 【新增】
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt; // 使用 java.util.Date
}