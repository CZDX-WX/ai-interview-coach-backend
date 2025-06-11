package com.czdxwx.aiinterviewcoachbackend.service.dto.embedding;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import com.google.gson.Gson;

public record SparkEmbeddingRequest(Header header, Parameter parameter, Payload payload) {

    public record Header(String app_id, int status) {}
    public record Parameter(Emb emb) {}
    public record Emb(String domain) {}
    public record Payload(Messages messages) {}
    public record Messages(String encoding, String compress, String format, int status, String text) {}

    // 这里的 InnerText 是私有的，仅用于构造 text 字段的内部 JSON
    private record InnerText(List<MessageContent> messages) {}
    private record MessageContent(String role, String content) {}

    public static SparkEmbeddingRequest create(String appId, String textToEmbed) {
        InnerText innerTextPayload = new InnerText(List.of(new MessageContent("user", textToEmbed)));
        String innerTextJson = new Gson().toJson(innerTextPayload);
        String base64EncodedText = Base64.getEncoder().encodeToString(innerTextJson.getBytes(StandardCharsets.UTF_8));

        Header header = new Header(appId, 3);
        Emb emb = new Emb("query");
        Parameter parameter = new Parameter(emb);
        Messages messages = new Messages("utf8", "raw", "json", 3, base64EncodedText);
        Payload payload = new Payload(messages);

        return new SparkEmbeddingRequest(header, parameter, payload);
    }
}