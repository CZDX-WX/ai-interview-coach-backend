package com.czdxwx.aiinterviewcoachbackend.utils;

import com.google.gson.JsonSyntaxException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public final class AuthUtils {

    private AuthUtils() {}

    /**
     * 生成讯飞星火的鉴权URL
     */
    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = format.format(new Date());

        String host = url.getHost();
        StringBuilder builder = new StringBuilder("host: ").append(host).append("\n").//
                append("date: ").append(date).append("\n").//
                append("GET ").append(url.getPath()).append(" HTTP/1.1");

        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(builder.toString().getBytes(StandardCharsets.UTF_8));
        String sha = Base64.getEncoder().encodeToString(hexDigits);

        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                apiKey, "hmac-sha256", "host date request-line", sha);
        String authBase = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));

        return String.format("%s?authorization=%s&host=%s&date=%s",
                hostUrl,
                URLEncoder.encode(authBase, StandardCharsets.UTF_8),
                URLEncoder.encode(host, StandardCharsets.UTF_8),
                URLEncoder.encode(date, StandardCharsets.UTF_8)
        );
    }

    /**
     * 【新增】计算字符串的 SHA-256 哈希值
     */
    public static String calculateSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 Algorithm not found", e);
        }
    }

    /**
     * 【新增】从可能包含Markdown标记的字符串中提取出纯净的JSON部分
     */
    public static String extractJsonFromString(String text) {
        int startIndex = text.indexOf('[');
        if (startIndex == -1) {
            startIndex = text.indexOf('{');
        }
        if (startIndex == -1) {
            throw new JsonSyntaxException("Cannot find starting bracket or brace in the response.");
        }
        int endIndex = text.lastIndexOf(']');
        if (endIndex == -1) {
            endIndex = text.lastIndexOf('}');
        }
        if (endIndex == -1) {
            throw new JsonSyntaxException("Cannot find ending bracket or brace in the response.");
        }
        return text.substring(startIndex, endIndex + 1);
    }
}