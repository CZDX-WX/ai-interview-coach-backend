package com.czdxwx.aiinterviewcoachbackend.service.dto;

import com.czdxwx.aiinterviewcoachbackend.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagSuggestionResponse {

    private Status status;
    private Tag suggestedTag; // 当找到相似标签时，这里会包含建议的标签对象
    private String originalTagName; // 用户原始输入的标签名

    public enum Status {
        NO_SIMILAR_TAG_FOUND, // 未找到相似标签，可以直接创建
        SIMILAR_TAG_FOUND,    // 找到了一个相似的标签，建议用户使用
        NO_SIMILAR_FOUND, EXACT_MATCH_FOUND     // 找到了完全同名的标签
    }
}
