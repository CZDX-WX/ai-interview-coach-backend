package com.czdxwx.aiinterviewcoachbackend.client;

import com.czdxwx.aiinterviewcoachbackend.service.SparkWebSocketListener;
import com.czdxwx.aiinterviewcoachbackend.service.dto.GenerationStrategy;
import com.czdxwx.aiinterviewcoachbackend.utils.AuthUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class SparkClient {
    // ... 与上一版完全相同，这里提供完整代码 ...
    private static final Logger logger = LoggerFactory.getLogger(SparkClient.class);

    @Value("${spark.appid}")
    private String appid;
    @Value("${spark.apikey}")
    private String apikey;
    @Value("${spark.apisecret}")
    private String apisecret;

    private final String hostUrl = "https://spark-api.xf-yun.com/v4.0/chat";
    private final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build();
    private final Gson gson = new Gson();

    public JsonObject extractParameters(String userQuery) throws Exception {
        String authUrl = AuthUtils.getAuthUrl(this.hostUrl, this.apikey, this.apisecret);
        CompletableFuture<String> futureResponse = new CompletableFuture<>();
        Request wsRequest = new Request.Builder().url(authUrl).build();
        SparkWebSocketListener listener = new SparkWebSocketListener(futureResponse, true);
        WebSocket webSocket = client.newWebSocket(wsRequest, listener);

        String jsonPayload = buildFunctionCallPayload(userQuery);
        logger.debug("Sending payload for function calling: {}", jsonPayload);
        webSocket.send(jsonPayload);

        String functionCallArgumentsJson = futureResponse.get(30, TimeUnit.SECONDS);
        return gson.fromJson(functionCallArgumentsJson, JsonObject.class);
    }

    public String generateContent(JsonObject args, GenerationStrategy strategy) throws Exception {
        String authUrl = AuthUtils.getAuthUrl(this.hostUrl, this.apikey, this.apisecret);
        CompletableFuture<String> futureResponse = new CompletableFuture<>();
        Request wsRequest = new Request.Builder().url(authUrl).build();
        SparkWebSocketListener listener = new SparkWebSocketListener(futureResponse, false);
        WebSocket webSocket = client.newWebSocket(wsRequest, listener);

        String jsonPayload = buildDirectJsonPayload(args, strategy);
        logger.debug("Sending payload for direct JSON generation with strategy [{}]: {}", strategy, jsonPayload);
        webSocket.send(jsonPayload);

        return futureResponse.get(60, TimeUnit.SECONDS);
    }

    private String buildFunctionCallPayload(String userQuery) {
        Map<String, Object> message = Map.of("text", List.of(Map.of("role", "user", "content", userQuery)));
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("header", Map.of("app_id", appid, "uid", "user-1234"));
        payload.put("parameter", Map.of("chat", Map.of("domain", "4.0Ultra", "temperature", 0.5, "max_tokens", 8192)));
        payload.put("payload", Map.of("message", message, "functions", Map.of("text", List.of(getInterviewFunctionDefinition()))));
        return gson.toJson(payload);
    }

    private String buildDirectJsonPayload(JsonObject args, GenerationStrategy strategy) {
        String role = args.get("role").getAsString();
        String difficulty = args.get("difficulty").getAsString();
        int numQuestions = args.get("num_questions").getAsInt();
        // 注意：topic 现在是从提取出的参数中获取的
        String topic = args.get("topic").getAsString();

        String userPrompt;
        if (strategy == GenerationStrategy.BREADTH_COVERAGE) {
            userPrompt = String.format("你是一位资深技术面试官，需要考察候选人的知识广度。请根据以下要求生成 %d 道面试题。\n\n**要求:**\n- **岗位:** %s\n- **技术领域 (请确保生成的题目能均匀覆盖以下所有领域):** %s\n- **难度:** %s\n\n**输出格式要求:**\n你的回答必须是一个纯净的JSON数组，每个对象包含 `question_text`, `reference_answer`, `speech_synthesis_content`, 和 `tags` 四个键。", numQuestions, role, topic, difficulty);
        } else {
            userPrompt = String.format("你是一位资深技术面试官，需要考察候选人融会贯通的能力。请根据以下要求生成 %d 道综合性、有深度的面试题。\n\n**要求:**\n- **岗位:** %s\n- **技术领域 (生成的题目应尽可能地需要结合以下多个领域的知识才能完美回答):** %s\n- **难度:** %s\n\n**输出格式要求:**\n你的回答必须是一个纯净的JSON数组，每个对象包含 `question_text`, `reference_answer`, `speech_synthesis_content`, 和 `tags` 四个键。", numQuestions, role, topic, difficulty);
        }

        Map<String, Object> message = Map.of("text", List.of(Map.of("role", "user", "content", userPrompt)));
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("header", Map.of("app_id", appid, "uid", "user-1234"));
        payload.put("parameter", Map.of("chat", Map.of("domain", "4.0Ultra", "temperature", 0.8, "max_tokens", 8192)));
        payload.put("payload", Map.of("message", message));
        return gson.toJson(payload);
    }

    private Map<String, Object> getInterviewFunctionDefinition() {
        return Map.of("name", "generate_interview_questions", "description", "根据用户提供的岗位、技术主题、难度和数量等关键词，准备生成面试题。", "parameters", Map.of("type", "object", "properties", Map.of("role", Map.of("type", "string", "description", "应聘的岗位，例如'后端工程师'或'前端开发'"), "topic", Map.of("type", "string", "description", "考察的技术点或标签，例如'Java 多并发'或'React Hooks'"), "difficulty", Map.of("type", "string", "description", "面试题的难度，例如'初级'、'中等'或'困难'"), "num_questions", Map.of("type", "number", "description", "需要生成的题目数量")), "required", List.of("role", "topic", "difficulty", "num_questions")));
    }
}