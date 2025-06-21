package com.czdxwx.aiinterviewcoachbackend.service.impl;

import com.czdxwx.aiinterviewcoachbackend.service.EmbeddingService;
import com.czdxwx.aiinterviewcoachbackend.service.dto.embedding.SparkEmbeddingRequest;
import com.czdxwx.aiinterviewcoachbackend.service.dto.embedding.SparkEmbeddingResponse;
import com.czdxwx.aiinterviewcoachbackend.utils.AuthUtils;
import com.google.gson.Gson;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SparkEmbeddingServiceImpl implements EmbeddingService {
    private static final Logger logger = LoggerFactory.getLogger(SparkEmbeddingServiceImpl.class);

    @Value("${spark.embedding.appid}") // 从 spark.embedding 读取
    private String appid;
    @Value("${spark.embedding.apikey}") // 从 spark.embedding 读取
    private String apikey;
    @Value("${spark.embedding.apisecret}") // 从 spark.embedding 读取
    private String apisecret;
    @Value("${spark.embedding.url}") // 从 spark.embedding 读取
    private String embeddingUrl;

    // 推荐将 OkHttpClient 和 Gson 作为 Bean 进行管理，这里为保持独立性直接创建
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    public float[] getEmbedding(String text) {
        logger.info("==> EmbeddingService: getEmbedding method called for text: '{}...'", text.substring(0, Math.min(text.length(), 50)));
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text to be embedded cannot be null or empty.");
        }

        try {
            // 1. 使用 AuthUtils 生成带鉴权的完整 URL
            String finalUrl = AuthUtils.getAuthUrl(this.embeddingUrl, this.apikey, this.apisecret,"POST");

            // 2. 构造请求体，这次传入的是动态文本
            String requestBodyJson = buildRequestBody(text);
            RequestBody body = RequestBody.create(requestBodyJson, JSON);

            // 3. 创建请求
            Request request = new Request.Builder()
                    .url(finalUrl)
                    .post(body)
                    .build();

            // 4. 发送请求并处理响应
            logger.info("Sending embedding request for text: '{}...'", text.substring(0, Math.min(text.length(), 50)));
            try (Response response = client.newCall(request).execute()) {
                String responseBody = Objects.requireNonNull(response.body()).string();

                if (!response.isSuccessful()) {
                    logger.error("Embedding API request failed with status {}: {}", response.code(), responseBody);
                    throw new RuntimeException("调用讯飞 Embedding API 失败，状态码: " + response.code());
                }

                logger.debug("Embedding API raw response: {}", responseBody);
                SparkEmbeddingResponse embeddingResponse = gson.fromJson(responseBody, SparkEmbeddingResponse.class);

                if (embeddingResponse == null || embeddingResponse.getHeader() == null || embeddingResponse.getHeader().getCode() != 0) {
                    String errorMessage = (embeddingResponse != null && embeddingResponse.getHeader() != null) ?
                            "讯飞 Embedding API 错误 - Code: " + embeddingResponse.getHeader().getCode() + ", Message: " + embeddingResponse.getHeader().getMessage()
                            : "调用讯飞 Embedding API 时收到格式错误的响应。";
                    logger.error(errorMessage);
                    throw new RuntimeException(errorMessage);
                }

                // 5. 解码并转换向量
                String base64Vector = embeddingResponse.getPayload().getFeature().getText();
                byte[] decodedBytes = Base64.getDecoder().decode(base64Vector);
                return bytesToFloatArray(decodedBytes);
            }
        } catch (Exception e) {
            logger.error("执行向量化时发生严重错误", e);
            throw new RuntimeException("执行向量化时发生严重错误", e);
        }
    }

    /**
     * 根据动态文本，构造请求体
     * @param textToEmbed 需要向量化的文本
     * @return JSON格式的请求体字符串
     */
    private String buildRequestBody(String textToEmbed) {
        Map<String, Object> innerTextPayload = Map.of(
                "messages", List.of(Map.of("role", "user", "content", textToEmbed))
        );
        String innerTextJson = gson.toJson(innerTextPayload);
        String base64EncodedText = Base64.getEncoder().encodeToString(innerTextJson.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> messagesObject = new java.util.HashMap<>();
        messagesObject.put("encoding", "utf8");
        messagesObject.put("compress", "raw");
        messagesObject.put("format", "json");
        messagesObject.put("status", 3);
        messagesObject.put("text", base64EncodedText);

        Map<String, Object> featureObject = Map.of(
                "encoding", "utf8",
                "compress", "raw",
                "format", "plain"
        );
        Map<String, Object> embObject = Map.of(
                "domain", "query",
                "feature", featureObject
        );

        Map<String, Object> requestMap = Map.of(
                "header", Map.of("app_id", appid, "uid", "user-1234", "status", 3),
                "parameter", Map.of("emb", embObject),
                "payload", Map.of("messages", messagesObject)
        );
        return gson.toJson(requestMap);
    }

    /**
     * 将讯飞返回的字节数组转换为 2560 维的浮点数数组。
     * @param bytes Base64解码后的字节数组
     * @return float[] 向量
     */
    private float[] bytesToFloatArray(byte[] bytes) {
        // 每个 float 占 4 个字节
        if (bytes.length % 4 != 0) {
            throw new IllegalArgumentException("字节数组长度必须是4的倍数才能转换为浮点数数组。");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        // 讯飞的向量字节序通常是小端序 (Little Endian)
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        float[] floatArray = new float[bytes.length / 4];
        buffer.asFloatBuffer().get(floatArray);

        return floatArray;
    }
}