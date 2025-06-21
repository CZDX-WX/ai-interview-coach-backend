package com.czdxwx.aiinterviewcoachbackend;

import com.czdxwx.aiinterviewcoachbackend.service.MilvusService;
import io.milvus.v2.service.vector.response.SearchResp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MilvusService 的集成测试类。
 * @SpringBootTest 注解会自动加载整个 Spring 应用上下文，
 * 使得我们可以直接注入和测试真实的服务 Bean。
 */
@SpringBootTest
public class MilvusServiceIntegrationTest {

    // 【核心】直接从 Spring 容器中注入我们想要测试的 MilvusService Bean
    @Autowired
    private MilvusService milvusService;

    // 从 Spring 容器中获取配置的维度，确保与应用一致
    @Value("${milvus.collection.dimension}")
    private Integer dimension;

    /**
     * 测试核心流程：向 "questions" 集合插入一个向量，然后立即搜索并验证结果。
     */
    @Test
    @DisplayName("测试题目向量的插入与搜索")
    void testInsertAndSearchForQuestions() throws InterruptedException {
        System.out.println("--- Starting Test: Insert and Search Questions Collection ---");

        // 1. 准备测试数据
        long testQuestionId = 99999L;
        List<Float> testVector = generateRandomVector();

        // 2. 调用服务进行插入
        System.out.println("Inserting vector for question_id: " + testQuestionId);
        milvusService.insertQuestionVector(testQuestionId, testVector);

        // 等待 Milvus flush 数据，使其可被搜索
        Thread.sleep(2000);

        // 3. 调用服务进行搜索
        System.out.println("Searching for the inserted vector...");
        List<SearchResp.SearchResult> results = milvusService.searchQuestions(testVector, 1);

        // 4. 使用 JUnit Assertions进行断言验证
        System.out.println("Verifying results...");
        Assertions.assertFalse(results.isEmpty(), "搜索结果不应为空");

        SearchResp.SearchResult topResult = results.get(0);
        long foundId = Long.parseLong(topResult.getId().toString());
        float score = topResult.getScore();

        System.out.printf("Found ID: %d, Score (Cosine Similarity): %f\n", foundId, score);

        Assertions.assertEquals(testQuestionId, foundId, "搜索到的ID应与插入的ID匹配");
        // 对于余弦相似度，向量与自身的匹配得分应为1.0
        Assertions.assertEquals(1.0f, score, 0.0001f, "相似度得分应非常接近1.0");

        System.out.println("✅ Test for 'questions' collection PASSED!");
    }

    /**
     * 测试核心流程：向 "tags" 集合插入一个向量，然后立即搜索并验证结果。
     */
    @Test
    @DisplayName("测试标签向量的插入与搜索")
    void testInsertAndSearchForTags() throws InterruptedException {
        System.out.println("--- Starting Test: Insert and Search Tags Collection ---");

        // 1. 准备测试数据
        long testTagId = 88888L;
        List<Float> testVector = generateRandomVector();

        // 2. 调用服务进行插入
        System.out.println("Inserting vector for tag_id: " + testTagId);
        milvusService.insertTagVector(testTagId, testVector);

        Thread.sleep(2000);

        // 3. 调用服务进行搜索
        System.out.println("Searching for the inserted vector...");
        List<SearchResp.SearchResult> results = milvusService.searchTags(testVector, 1);

        // 4. 使用 JUnit Assertions进行断言验证
        System.out.println("Verifying results...");
        Assertions.assertFalse(results.isEmpty(), "搜索结果不应为空");

        SearchResp.SearchResult topResult = results.get(0);
        long foundId = Long.parseLong(topResult.getId().toString());
        float score = topResult.getScore();

        System.out.printf("Found ID: %d, Score (Cosine Similarity): %f\n", foundId, score);

        Assertions.assertEquals(testTagId, foundId, "搜索到的ID应与插入的ID匹配");
        Assertions.assertEquals(1.0f, score, 0.0001f, "相似度得分应非常接近1.0");

        System.out.println("✅ Test for 'tags' collection PASSED!");
    }


    /**
     * 生成一个指定维度的随机浮点数向量的辅助方法
     */
    private List<Float> generateRandomVector() {
        Random rand = new Random();
        List<Float> vector = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            vector.add(rand.nextFloat());
        }
        return vector;
    }
}