package com.example.telegram_bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class JapaneseCorrectionService {

    private static final Logger log = LoggerFactory.getLogger(JapaneseCorrectionService.class);

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient webClient;

    public JapaneseCorrectionService(
            @Value("${AI_BASE_URL}") String aiBaseUrl
    ) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60)); // 🔥 IMPORTANT

        this.webClient = WebClient.builder()
                .baseUrl(aiBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String correctJapanese(String input) {

        if (input == null || input.isBlank()) {
            return "入力が空です。テキストを送ってください。";
        }

        // Hard safety limit
        input = input.length() > 200 ? input.substring(0, 200) : input;

        Map<String, Object> body = Map.of("input", input);

        try {
            Map<String, Object> response = webClient.post()
                    .uri("/infer")
                    .bodyValue(body)
                    .exchangeToMono(this::handleResponse)
                    .block(Duration.ofSeconds(70)); // 🔥 must exceed AI latency

            if (response == null || !response.containsKey("result")) {
                log.warn("Invalid AI response: {}", response);
                return "AIの応答が不正です。しばらくしてから再試行してください。";
            }

            Object resultObj = response.get("result");
            if (!(resultObj instanceof Map)) {
                log.warn("Unexpected result type: {}", resultObj);
                return "AIの応答形式が正しくありません。";
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) resultObj;

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

            return reply.toString().trim();

        } catch (Exception e) {
            log.error("AI API call failed", e);
            return "エラーが発生しました。時間をおいて再度お試しください。";
        }
    }

    /**
     * Handles non-2xx responses safely without throwing.
     */
    private Mono<Map<String, Object>> handleResponse(ClientResponse response) {

        if (!response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(String.class)
                    .map(body -> {
                        log.error("AI API error {}: {}", response.statusCode(), body);
                        return Map.of("error", "AI_ERROR");
                    });
        }

        return response.bodyToMono(MAP_TYPE);
    }
}
