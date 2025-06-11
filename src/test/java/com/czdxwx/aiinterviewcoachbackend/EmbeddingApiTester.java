package com.czdxwx.aiinterviewcoachbackend;
import com.google.gson.Gson;
import okhttp3.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;


public class EmbeddingApiTester {

    // ▼▼▼▼▼▼▼▼ 请确认您的密钥信息 ▼▼▼▼▼▼▼▼
    private static final String APP_ID = "d6422ddf";      // 替换为您的真实 AppID
    private static final String API_KEY = "e235e3a86546316eab30a8398b764260";   // 替换为您的真实 APIKey
    private static final String API_SECRET = "NmIwZmM0ZTJlMWQ3MjQ4NWIxY2YzM2M2"; // 替换为您的真实 APISecret
    // ▲▲▲▲▲▲▲▲ 请确认您的密钥信息 ▲▲▲▲▲▲▲▲

    private static final String BASE_URL = "https://emb-cn-huabei-1.xf-yun.com/";
    private static final String TEXT_TO_EMBED = "你好，讯飞星火！";

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public static void main(String[] args) {
        System.out.println("--- Starting Standalone Embedding API Test (Date Format Fixed) ---");

        try {
            String finalUrl = getAuthUrl(BASE_URL, API_KEY, API_SECRET, "POST");
            System.out.println("\nGenerated Authenticated URL: " + finalUrl);

            String requestBodyJson = buildRequestBody();
            System.out.println("\nRequest Body JSON: " + requestBodyJson);

            RequestBody body = RequestBody.create(requestBodyJson, JSON);
            Request request = new Request.Builder()
                    .url(finalUrl)
                    .post(body)
                    .build();

            System.out.println("\nSending request...");
            try (Response response = client.newCall(request).execute()) {
                System.out.println("Response received!");
                System.out.println("Status Code: " + response.code());
                String responseBody = response.body() != null ? response.body().string() : "null";
                System.out.println("\nResponse Body:\n" + responseBody);

                if (response.isSuccessful()) {
                    System.out.println("\n--- TEST SUCCEEDED! ---");
                } else {
                    System.err.println("\n--- TEST FAILED: HTTP request was not successful. ---");
                }
            }

        } catch (Exception e) {
            System.err.println("\n--- TEST FAILED: An exception occurred. ---");
            e.printStackTrace();
        }
    }

    private static String buildRequestBody() {
        // ... buildRequestBody 方法保持不变 ...
        Map<String, Object> innerTextPayload = Map.of(
                "messages", List.of(Map.of("role", "user", "content", TEXT_TO_EMBED))
        );
        String innerTextJson = new Gson().toJson(innerTextPayload);
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
                "header", Map.of("app_id", APP_ID, "uid", "user-1234", "status", 3),
                "parameter", Map.of("emb", embObject),
                "payload", Map.of("messages", messagesObject)
        );
        return new Gson().toJson(requestMap);
    }

    public static String getAuthUrl(String requestUrl, String apiKey, String apiSecret, String method) throws Exception {
        URL url = new URL(requestUrl);
        // 【核心修正】使用正确的日期格式 "yyyy"
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = format.format(new Date());

        String host = url.getHost();
        String path = url.getPath().isEmpty() ? "/" : url.getPath();

        StringBuilder builder = new StringBuilder("host: ").append(host).append("\n").//
                append("date: ").append(date).append("\n").//
                append(method).append(" ").append(path).append(" HTTP/1.1");

        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(builder.toString().getBytes(StandardCharsets.UTF_8));
        String sha = Base64.getEncoder().encodeToString(hexDigits);

        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                apiKey, "hmac-sha256", "host date request-line", sha);
        String authBase = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));

        return String.format("%s?authorization=%s&host=%s&date=%s",
                requestUrl,
                URLEncoder.encode(authBase, StandardCharsets.UTF_8),
                URLEncoder.encode(host, StandardCharsets.UTF_8),
                URLEncoder.encode(date, StandardCharsets.UTF_8)
        );
    }
}