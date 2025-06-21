package com.czdxwx.aiinterviewcoachbackend.vo;

import com.czdxwx.aiinterviewcoachbackend.entity.Question;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用于接收 Milvus 向量搜索结果的视图对象。
 * 继承自 Question 实体，并额外增加一个 distance 字段。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuestionWithDistanceVO extends Question {

    private Double distance;
}