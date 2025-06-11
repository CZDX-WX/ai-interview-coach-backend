package com.czdxwx.aiinterviewcoachbackend.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;

public class SparkWebSocketListener extends WebSocketListener {
    private static final Logger logger = LoggerFactory.getLogger(SparkWebSocketListener.class);

    private final CompletableFuture<String> futureResponse;
    private final StringBuilder fullResponse = new StringBuilder();
    private final boolean expectFunctionCall;

    public SparkWebSocketListener(CompletableFuture<String> futureResponse, boolean expectFunctionCall) {
        this.futureResponse = futureResponse;
        this.expectFunctionCall = expectFunctionCall;
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        logger.debug("Received message from Spark: {}", text);
        JsonObject data = new Gson().fromJson(text, JsonObject.class);
        JsonObject header = data.getAsJsonObject("header");
        int code = header.get("code").getAsInt();

        if (code != 0) {
            String errorMessage = "讯飞 API 错误 - Code: " + code + ", Message: " + header.get("message").getAsString() + ", SID: " + header.get("sid").getAsString();
            logger.error(errorMessage);
            futureResponse.completeExceptionally(new RuntimeException(errorMessage));
            webSocket.close(1000, "API Error");
            return;
        }

        JsonElement textElement = data.getAsJsonObject("payload")
                .getAsJsonObject("choices")
                .getAsJsonArray("text")
                .get(0);

        if (expectFunctionCall) {
            // --- 模式一：寻找 Function Call ---
            // 【核心修正】直接检查 text[0] 中是否存在 function_call 对象
            if (textElement.isJsonObject() && textElement.getAsJsonObject().has("function_call")) {
                JsonObject functionCall = textElement.getAsJsonObject().getAsJsonObject("function_call");
                String funcName = functionCall.get("name").getAsString();
                String funcArgs = functionCall.get("arguments").getAsString();

                // 确认模型成功提取了参数
                if (funcName != null && !funcName.isEmpty() && funcArgs != null && !funcArgs.isEmpty()) {
                    logger.info("Function call successfully detected and parsed: name={}, args={}", funcName, funcArgs);
                    if (!futureResponse.isDone()) {
                        // 我们需要的是参数，所以直接返回 arguments 的 JSON 字符串
                        futureResponse.complete(funcArgs);
                    }
                }
            }
        } else {
            // --- 模式二：拼接普通文本/JSON响应 ---
            String content = textElement.getAsJsonObject().get("content").getAsString();
            fullResponse.append(content);
        }

        if (header.get("status").getAsInt() == 2) {
            if (!futureResponse.isDone()) {
                if (expectFunctionCall) {
                    futureResponse.completeExceptionally(new RuntimeException("讯飞服务已结束，但未返回有效的、包含参数的函数调用。"));
                } else {
                    futureResponse.complete(fullResponse.toString());
                }
            }
            webSocket.close(1000, "Normal closure");
        }
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, Response response) {
        logger.error("WebSocket connection failed.", t);
        if (!futureResponse.isDone()) {
            futureResponse.completeExceptionally(t);
        }
    }
}