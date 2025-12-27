package com.example.telegram_bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class JapaneseCorrectionService {

    private static final Logger log = LoggerFactory.getLogger(JapaneseCorrectionService.class);

    private final WebClient webClient;

    public JapaneseCorrectionService(
            @Value("${AI_BASE_URL}") String aiBaseUrl
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(aiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String correctJapanese(String input) {

        if (input == null || input.isBlank()) {
            return "入力が空です。テキストを送ってください。";
        }

        // Safety limit
        input = input.length() > 200 ? input.substring(0, 200) : input;

        Map<String, Object> body = Map.of("input", input);

        try {
            Map<?, ?> response = webClient.post()
                    .uri("/infer")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(15));

            if (response == null || !response.containsKey("result")) {
                log.warn("Invalid response from AI API: {}", response);
                return "少し待ってから、もう一度送ってください。";
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.get("result");

            String kanji = String.valueOf(result.getOrDefault("kanji", ""));
            String hiragana = String.valueOf(result.getOrDefault("hiragana", ""));
            String explanation = String.valueOf(result.getOrDefault("explanation", ""));
            Object warningsObj = result.get("warnings");

            StringBuilder reply = new StringBuilder();

            if (!kanji.isBlank()) {
                reply.append("✅ 修正結果\n");
                reply.append(kanji);
            }

            if (!hiragana.isBlank()) {
                reply.append("\n（").append(hiragana).append("）");
            }

            if (!explanation.isBlank()) {
                reply.append("\n\n📘 解説\n");
                reply.append(explanation);
            }

            if (warningsObj instanceof List<?> warnings && !warnings.isEmpty()) {
                reply.append("\n\n⚠️ 注意\n");
                for (Object w : warnings) {
                    reply.append("- ").append(w).append("\n");
                }
            }

            return reply.toString();

        } catch (Exception e) {
            log.error("Error calling AI API", e);
            return "エラーが発生しました。少し後でもう一度試してください。";
        }
    }
}
