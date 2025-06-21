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

/**
 * 项目通用工具类
 */
public final class AuthUtils {

    // 私有构造函数，防止该工具类被实例化
    private AuthUtils() {}

    /**
     * 为讯飞星火的 API 生成带有鉴权参数的 URL
     * @param requestUrl API 的基础地址 (例如 "https://..." 或 "wss://...")
     * @param apiKey 您的 APIKey
     * @param apiSecret 您的 APISecret
     * @param method 请求方法，必须是 "GET" (用于WebSocket) 或 "POST" (用于HTTP)
     * @return 包含完整鉴权信息的最终 URL
     */
    public static String getAuthUrl(String requestUrl, String apiKey, String apiSecret, String method) throws Exception {
        if (!"GET".equals(method) && !"POST".equals(method)) {
            throw new IllegalArgumentException("Method must be either GET or POST");
        }

        URL url = new URL(requestUrl);
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

    /**
     * 【已添加】计算字符串的 SHA-256 哈希值
     * 用于为面试题文本生成唯一指纹，实现精确去重。
     * @param text 输入文本
     * @return 64位的十六进制哈希字符串
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
            // 在标准Java环境中，SHA-256 算法总是存在的，所以这里抛出运行时异常
            throw new RuntimeException("SHA-256 Algorithm not found", e);
        }
    }

    /**
     * 【已添加】从可能包含Markdown标记的字符串中提取出纯净的JSON部分
     * @param text 大模型返回的原始文本
     * @return 纯净的 JSON 字符串
     */
    public static String extractJsonFromString(String text) {
        // 寻找第一个 '[' 或 '{'
        int firstBracket = text.indexOf('[');
        int firstBrace = text.indexOf('{');

        if (firstBracket == -1 && firstBrace == -1) {
            throw new JsonSyntaxException("Response from AI does not contain valid JSON.");
        }

        int startIndex = (firstBracket != -1 && firstBrace != -1) ?
                Math.min(firstBracket, firstBrace) :
                Math.max(firstBracket, firstBrace);

        // 寻找最后一个 ']' 或 '}'
        int lastBracket = text.lastIndexOf(']');
        int lastBrace = text.lastIndexOf('}');
        int endIndex = Math.max(lastBracket, lastBrace);

        if (endIndex == -1 || endIndex < startIndex) {
            throw new JsonSyntaxException("Cannot find ending bracket or brace in the response.");
        }

        return text.substring(startIndex, endIndex + 1);
    }
}