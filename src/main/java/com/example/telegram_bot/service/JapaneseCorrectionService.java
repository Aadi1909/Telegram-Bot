package com.example.telegram_bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
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

        input = input.length() > 200 ? input.substring(0, 200) : input;

        Map<String, Object> body = Map.of(
                "input", input
        );

        try {
            Map<?, ?> response = webClient.post()
                    .uri("/infer")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(15));

            if (response == null || !response.containsKey("corrected")) {
                log.warn("Invalid response from AI Space: {}", response);
                return "少し待ってから、もう一度送ってください。";
            }

            return response.get("corrected").toString();

        } catch (Exception e) {
            log.error("Error calling AI Space", e);
            return "エラーが発生しました。少し後でもう一度試してください。";
        }
    }
}
