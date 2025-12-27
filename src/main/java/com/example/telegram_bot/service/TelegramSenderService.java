package com.example.telegram_bot.service;


import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramSenderService {


    private final RestTemplate restTemplate = new RestTemplate();
    private final String botToken;

    public TelegramSenderService(@Value("${telegram.bot.token}") String botToken) {
        this.botToken = botToken;
    }

    public void sendMessage(Long chatId, String text) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);

        restTemplate.postForObject(url, body, String.class);
    }

}
