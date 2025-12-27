package com.example.telegram_bot.service;


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

    private final WebClient webClient;
    private final String model;

    public JapaneseCorrectionService(
            @Value("${HF_API_KEY}") String apiKey,
            @Value("${HF_MODEL}") String model) {

        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl("https://api-inference.huggingface.co")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String correctJapanese(String input) {

        input = input.length() > 200 ? input.substring(0, 200) : input;

        String prompt = """
        あなたは日本語教師です。
        次の文を自然で正しい日本語に直してください。

        文：
        %s

        答え：
        """.formatted(input);

        Map<String, Object> body = Map.of(
                "inputs", prompt,
                "parameters", Map.of(
                        "max_new_tokens", 64,
                        "do_sample", false
                )
        );

        try {
            Object raw = webClient.post()
                    .uri("/models/" + model)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block(Duration.ofSeconds(8));

            if (!(raw instanceof List<?> list) || list.isEmpty()) {
                return "少し待ってから、もう一度送ってください。";
            }

            Map<?, ?> item = (Map<?, ?>) list.get(0);

            return item.get("generated_text")
                    .toString()
                    .replace(prompt, "")
                    .trim();

        } catch (Exception e) {
            return "エラーが発生しました。少し後でもう一度試してください。";
        }
    }
}