package com.czdxwx.aiinterviewcoachbackend.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.czdxwx.aiinterviewcoachbackend.entity.Tag;
import com.czdxwx.aiinterviewcoachbackend.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagVectorizationService {

    private static final Logger logger = LoggerFactory.getLogger(TagVectorizationService.class);

    private final TagMapper tagMapper;
    private final EmbeddingService embeddingService;
    private final MilvusService milvusService;

    // 从 application.yml 注入集合名，确保一致性
    @Value("${milvus.collection.tags}")
    private String tagsCollectionName;

    /**
     * 异步执行公共标签的向量化回填任务
     */
    @Async("taskExecutor") // 在后台线程池中执行
    public void backfillTagVectors() {
        logger.info("======================================================");
        logger.info("====== TAG VECTOR BACKFILL TASK STARTED ======");
        logger.info("======================================================");

        // 1. 从 MySQL 中查询所有公共标签 (owner_id is null)
        List<Tag> publicTags = tagMapper.selectList(new QueryWrapper<Tag>().isNull("owner_id"));

        if (publicTags.isEmpty()) {
            logger.info("No public tags found to vectorize. Task finished.");
            return;
        }

        logger.info("Found {} public tags to process.", publicTags.size());
        int successCount = 0;
        int failCount = 0;

        for (Tag tag : publicTags) {
            try {
                logger.info("Processing tag: '{}' (ID: {})", tag.getName(), tag.getId());

                // 2. 调用 Embedding API 将标签名向量化
                float[] vectorArray = embeddingService.getEmbedding(tag.getName());
                List<Float> vector = toFloatList(vectorArray);

                // 3. 将标签ID和其向量存入 Milvus
                milvusService.insertTagVector(tag.getId(), vector);

                logger.info("-----> SUCCESS: Vector for tag '{}' inserted into Milvus.", tag.getName());
                successCount++;

                // 4. 短暂休眠，避免对 Embedding API 造成太大压力
                Thread.sleep(1000); // 等待1秒

            } catch (Exception e) {
                logger.error("-----> FAILED: Could not process tag '{}'. Error: {}", tag.getName(), e.getMessage());
                failCount++;
            }
        }

        String summary = String.format("Total tags processed. Success: %d, Failed: %d", successCount, failCount);
        logger.info("====================================================");
        logger.info("====== TAG VECTOR BACKFILL TASK FINISHED ======");
        logger.info("====== SUMMARY: {} ======", summary);
        logger.info("====================================================");
    }

    private List<Float> toFloatList(float[] array) {
        if (array == null) return new ArrayList<>();
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }
}