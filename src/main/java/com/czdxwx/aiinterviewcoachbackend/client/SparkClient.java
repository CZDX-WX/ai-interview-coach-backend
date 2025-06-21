package com.czdxwx.aiinterviewcoachbackend.client;
import com.czdxwx.aiinterviewcoachbackend.service.SparkWebSocketListener;
import com.czdxwx.aiinterviewcoachbackend.service.dto.GenerationStrategy;
import com.czdxwx.aiinterviewcoachbackend.service.dto.QuestionGenerationRequest;
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
@RequiredArgsConstructor
public class SparkClient {

    private static final Logger logger = LoggerFactory.getLogger(SparkClient.class);

    @Value("${spark.chat.appid}")
    private String appid;
    @Value("${spark.chat.apikey}")
    private String apikey;
    @Value("${spark.chat.apisecret}")
    private String apisecret;
    @Value("${spark.chat.url}")
    private String hostUrl;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();

    /**
     * 执行第一步：意图识别与参数提取。
     */
    public JsonObject extractParameters(String userQuery) throws Exception {
        String authUrl = AuthUtils.getAuthUrl(this.hostUrl, this.apikey, this.apisecret, "GET");
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

    /**
     * 【核心修正】执行第二步：根据精确指令生成最终内容。
     * 此方法的签名现在只接收 QuestionGenerationRequest 对象。
     */
    public String generateContent(QuestionGenerationRequest request) throws Exception {
        String authUrl = AuthUtils.getAuthUrl(this.hostUrl, this.apikey, this.apisecret, "GET");
        CompletableFuture<String> futureResponse = new CompletableFuture<>();
        Request wsRequest = new Request.Builder().url(authUrl).build();
        SparkWebSocketListener listener = new SparkWebSocketListener(futureResponse, false);
        WebSocket webSocket = client.newWebSocket(wsRequest, listener);

        String jsonPayload = buildDirectJsonPayload(request);
        logger.debug("Sending payload for direct JSON generation: {}", jsonPayload);
        webSocket.send(jsonPayload);

        return futureResponse.get(60, TimeUnit.SECONDS);
    }

    // --- 私有辅助方法 ---

    private String buildFunctionCallPayload(String userQuery) {
        Map<String, Object> message = Map.of("text", List.of(Map.of("role", "user", "content", userQuery)));
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("header", Map.of("app_id", appid, "uid", "user-1234"));
        payload.put("parameter", Map.of("chat", Map.of("domain", "4.0Ultra", "temperature", 0.5, "max_tokens", 8192)));
        payload.put("payload", Map.of(
                "message", message,
                "functions", Map.of("text", List.of(getInterviewFunctionDefinition()))
        ));
        return gson.toJson(payload);
    }

    /**
     * 【核心修正】此方法的签名也已更新，直接从 request 对象中获取所有需要的信息。
     */
    private String buildDirectJsonPayload(QuestionGenerationRequest request) {
        String roleName = request.roleName();
        String difficulty = request.difficulty();
        int numQuestions = request.numQuestions();
        String tagsAsString = String.join("、", request.tags());
        GenerationStrategy strategy = request.strategy();

        String speechContentInstruction;
        if ("困难".equalsIgnoreCase(difficulty) || "高级".equalsIgnoreCase(difficulty)) {
            speechContentInstruction = "`speech_synthesis_content`: [string] 一段对`reference_answer`的口语化、深入浅出的讲解文稿，就像一位资深面试官在为面试者剖析答案的核心思路和技术要点。内容必须是对答案的讲解，而不是重复问题，可以直接用于TTS语音合成。";
        } else {
            speechContentInstruction = "`speech_synthesis_content`: [string] 对于简单或中等题目，此字段必须返回一个空字符串 `\"\"`。";
        }

        String basePrompt;
        if (strategy == GenerationStrategy.BREADTH_COVERAGE) {
            basePrompt = "你是一位资深技术面试官，需要考察候选人的知识广度。请根据以下要求生成 %d 道面试题。\n\n" +
                    "**要求:**\n" +
                    "- **岗位:** %s\n" +
                    "- **技术领域 (请确保生成的题目能均匀覆盖以下所有领域):** %s\n" +
                    "- **难度:** %s\n\n";
        } else { // INTEGRATED_DEEP_DIVE
            basePrompt = "你是一位资深技术面试官，需要考察候选人融会贯通的能力。请根据以下要求生成 %d 道综合性、有深度的面试题。\n\n" +
                    "**要求:**\n" +
                    "- **岗位:** %s\n" +
                    "- **技术领域 (生成的题目应尽可能地需要结合以下多个领域的知识才能完美回答):** %s\n" +
                    "- **难度:** %s\n\n";
        }

        String userPrompt = String.format(
                basePrompt +
                        "**输出格式要求:**\n" +
                        "你的回答必须是一个纯净的、不包含任何额外说明或markdown标记的JSON数组。数组中的每个对象必须包含 `question_text`, `reference_answer`, 和 `speech_synthesis_content` 三个键（AI不生成tags）。\n" +
                        "- %s",
                numQuestions, roleName, tagsAsString, difficulty, speechContentInstruction
        );

        Map<String, Object> message = Map.of("text", List.of(Map.of("role", "user", "content", userPrompt)));

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("header", Map.of("app_id", appid, "uid", "user-1234"));
        payload.put("parameter", Map.of("chat", Map.of("domain", "4.0Ultra", "temperature", 0.8, "max_tokens", 8192)));
        payload.put("payload", Map.of("message", message));

        return gson.toJson(payload);
    }

    private Map<String, Object> getInterviewFunctionDefinition() {
        return Map.of(
                "name", "generate_interview_questions",
                "description", "根据用户提供的岗位、技术主题、难度和数量等关键词，准备生成面试题。",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "role", Map.of("type", "string", "description", "应聘的岗位，例如'后端工程师'或'前端开发'"),
                                "topic", Map.of("type", "string", "description", "考察的技术点或标签，例如'Java 多并发'或'React Hooks'"),
                                "difficulty", Map.of("type", "string", "description", "面试题的难度，例如'简单'、'中等'或'困难'"),
                                "num_questions", Map.of("type", "number", "description", "需要生成的题目数量")
                        ),
                        "required", List.of("role", "topic", "difficulty", "num_questions")
                )
        );
    }
}