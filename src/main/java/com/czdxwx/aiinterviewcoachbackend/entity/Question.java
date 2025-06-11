package com.czdxwx.aiinterviewcoachbackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.czdxwx.aiinterviewcoachbackend.config.mybatis.PGvectorTypeHandler;
import com.pgvector.PGvector;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Date;

@Data
@TableName("questions")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String questionText;
    private String referenceAnswer;
    private String speechSynthesisContent;
    private String difficulty;
    private String questionHash;
    @TableField(typeHandler = PGvectorTypeHandler.class)
    private PGvector questionVector;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt; // 【修改】类型从 Instant/OffsetDateTime 变为 Date
}