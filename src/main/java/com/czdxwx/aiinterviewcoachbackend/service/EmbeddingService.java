package com.czdxwx.aiinterviewcoachbackend.service;

public interface EmbeddingService {
    /**
     * 将给定的文本转换为浮点数数组形式的向量。
     * @param text 需要向量化的文本
     * @return 包含 2560 个维度 的 float 数组
     */
    float[] getEmbedding(String text);
}