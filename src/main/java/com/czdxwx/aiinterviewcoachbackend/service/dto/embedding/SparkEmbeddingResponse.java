package com.czdxwx.aiinterviewcoachbackend.service.dto.embedding;

import lombok.Data;

// 使用 @Data 注解自动生成 Getter/Setter 等
@Data
public class SparkEmbeddingResponse {
    private Header header;
    private Payload payload;

    @Data
    public static class Header {
        private int code;
        private String message;
        private String sid;
    }

    @Data
    public static class Payload {
        private Feature feature;
    }

    @Data
    public static class Feature {
        private String text; // 这是 Base64 编码后的向量数据
    }
}