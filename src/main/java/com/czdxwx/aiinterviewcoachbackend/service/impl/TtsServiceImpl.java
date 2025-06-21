package com.czdxwx.aiinterviewcoachbackend.service.impl;

import com.czdxwx.aiinterviewcoachbackend.service.FileStorageService;
import com.czdxwx.aiinterviewcoachbackend.service.TtsService;
import com.czdxwx.aiinterviewcoachbackend.utils.AuthUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class TtsServiceImpl implements TtsService {
    private static final Logger logger = LoggerFactory.getLogger(TtsServiceImpl.class);

    @Value("${spark.tts.url}")
    private String hostUrl;
    @Value("${spark.tts.appid}")
    private String appId;
    @Value("${spark.tts.apikey}")
    private String apiKey;
    @Value("${spark.tts.apisecret}")
    private String apiSecret;
    @Value("${spark.tts.default-voice-name}")
    private String defaultVoiceName;

    private final Gson gson = new Gson();
    private final FileStorageService fileStorageService;

    public TtsServiceImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }
    /**
     * 将文本合成为MP3，并保存到本地文件
     * @param text 要合成的文本
     * @return 保存成功后的文件相对路径
     */
    @Override
    public String textToSpeech(String text) {
        logger.info("textToSpeech start,appid:{},voiceName:{}", appId, this.defaultVoiceName);
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text to be synthesized cannot be empty.");
        }

        try {
            // 1. 【核心修正】调用 AuthUtils 生成带完整鉴权的 URL
            String authUrl = AuthUtils.getAuthUrl(hostUrl, apiKey, apiSecret, "GET");
            String wsUrl = authUrl.replace("https://", "wss://");

            CompletableFuture<byte[]> audioFuture = new CompletableFuture<>();
            URI uri = new URI(wsUrl);

            WebSocketClient client = createWebSocketClient(uri, audioFuture);
            client.connectBlocking(10, TimeUnit.SECONDS);
            sendRequest(client, text, this.defaultVoiceName);

            // 等待音频数据完全接收
            byte[] audioData = audioFuture.get(60, TimeUnit.SECONDS);

            if (!client.isClosed()) {
                client.close();
            }

            // 2. 【核心修正】将音频数据保存到本地文件
            return saveAudioToFile(audioData);

        } catch (Exception e) {
            logger.error("Text-to-Speech process failed.", e);
            throw new RuntimeException("TTS service failed", e);
        }
    }

    private WebSocketClient createWebSocketClient(URI uri, CompletableFuture<byte[]> future) {
        ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
        return new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                logger.info("TTS WebSocket connection opened.");
            }

            @Override
            public void onMessage(String message) {
                JsonObject response = gson.fromJson(message, JsonObject.class);
                int code = response.get("code").getAsInt();
                if (code != 0) {
                    future.completeExceptionally(new RuntimeException("TTS business error: " + response.get("message").getAsString()));
                    return;
                }
                JsonObject data = response.getAsJsonObject("data");
                if (data != null && data.has("audio")) {
                    try {
                        byte[] audioChunk = Base64.getDecoder().decode(data.get("audio").getAsString());
                        audioBuffer.write(audioChunk);
                    } catch (IOException e) {
                        future.completeExceptionally(e);
                    }
                }
                if (data != null && data.get("status").getAsInt() == 2) {
                    future.complete(audioBuffer.toByteArray());
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                logger.info("TTS WebSocket connection closed. Code: {}, Reason: {}", code, reason);
                if (code != 1000 && !future.isDone()) {
                    future.completeExceptionally(new RuntimeException("TTS WebSocket closed unexpectedly. Code: " + code));
                }
            }

            @Override
            public void onError(Exception ex) {
                logger.error("TTS WebSocket encountered an error.", ex);
                if (!future.isDone()) {
                    future.completeExceptionally(ex);
                }
            }
        };
    }

    /**
     * 发送业务请求帧
     */
    private void sendRequest(WebSocketClient client, String text, String voiceName) {
        try {
            JsonObject frame = new JsonObject();
            // common
            JsonObject common = new JsonObject();
            common.addProperty("app_id", this.appId);
            // business
            JsonObject business = new JsonObject();
            business.addProperty("aue", "lame"); // lame: mp3格式, raw: pcm格式
            business.addProperty("tte", "UTF8");
            business.addProperty("vcn", voiceName != null ? voiceName : "x4_lingxiaoxuan_oral"); // 默认或指定的发音人
            business.addProperty("speed", 50);
            business.addProperty("pitch", 50);
            business.addProperty("volume", 50);
            // data
            JsonObject data = new JsonObject();
            data.addProperty("status", 2); // 2: 表示所有文本都一次性发送
            data.addProperty("text", Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)));

            frame.add("common", common);
            frame.add("business", business);
            frame.add("data", data);

            client.send(frame.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to send TTS request frame.", e);
        }
    }

    /**
     * 【核心修改】将音频字节数组保存到文件的辅助方法
     */
    private String saveAudioToFile(byte[] audioData) throws IOException {
        if (audioData == null || audioData.length == 0) {
            logger.warn("接收到空的音频数据，跳过文件保存。");
            return null;
        }

        // 1. 定义存储的子目录
        String subDirectory = "tts_audio";

        // 2. 调用通用的文件存储服务
        String storedRelativePath = fileStorageService.store(audioData, subDirectory, ".mp3");

        // 3. 构建前端可直接访问的完整相对URL
        String accessibleUrl = "/api/files/" + storedRelativePath;

        logger.info("音频文件已保存，可通过URL访问: {}", accessibleUrl);
        return accessibleUrl;
    }
}