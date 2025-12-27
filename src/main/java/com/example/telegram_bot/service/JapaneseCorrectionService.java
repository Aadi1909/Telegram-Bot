package com.example.telegram_bot.service;

import com.example.telegram_bot.dto.AiResponse;
import com.example.telegram_bot.dto.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import java.time.Duration;
import java.util.Map;

@Service
public class JapaneseCorrectionService {

    private static final Logger log =
            LoggerFactory.getLogger(JapaneseCorrectionService.class);

    private final WebClient webClient;

    public JapaneseCorrectionService(
            @Value("${AI_BASE_URL}") String aiBaseUrl
    ) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60));

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

        input = input.length() > 200 ? input.substring(0, 200) : input;

        try {
            AiResponse response = webClient.post()
                    .uri("/infer")
                    .bodyValue(Map.of("input", input))
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful(),
                            this::handleError
                    )
                    .bodyToMono(AiResponse.class)
                    .block(Duration.ofSeconds(70));

            if (response == null || response.getResult() == null) {
                return "AIの応答が不正です。";
            }

            Result r = response.getResult();
            StringBuilder reply = new StringBuilder();

            if (notBlank(r.getKanji())) {
                reply.append("<b>✅ 修正結果</b>\n")
                        .append(escapeHtml(r.getKanji()));
            }

            if (notBlank(r.getHiragana())) {
                reply.append("\n（")
                        .append(escapeHtml(r.getHiragana()))
                        .append("）");
            }

            if (notBlank(r.getExplanation())) {
                reply.append("\n\n<b>📘 解説</b>\n")
                        .append(escapeHtml(r.getExplanation()));
            }

            if (r.getWarnings() != null && !r.getWarnings().isEmpty()) {
                reply.append("\n\n<b>⚠️ 注意</b>\n");
                r.getWarnings().forEach(w ->
                        reply.append("- ")
                                .append(escapeHtml(w))
                                .append("\n")
                );
            }

            return reply.toString().trim();

        } catch (Exception e) {
            log.error("AI API call failed", e);
            return "エラーが発生しました。時間をおいて再度お試しください。";
        }
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String escapeHtml(String text) {
        return text == null ? null :
                text.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;");
    }

    private Mono<? extends Throwable> handleError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .flatMap(body -> {
                    log.error("AI API error {}: {}", response.statusCode(), body);
                    return Mono.error(new RuntimeException("AI API Error"));
                });
    }
}
