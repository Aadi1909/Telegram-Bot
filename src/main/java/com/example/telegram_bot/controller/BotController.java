package com.example.telegram_bot.controller;


import com.example.telegram_bot.service.TelegramSenderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
public class BotController {

    private final TelegramSenderService telegramSenderService;

    public BotController(TelegramSenderService telegramSenderService) {
        this.telegramSenderService = telegramSenderService;
    }

    @PostMapping("/telegram/webhook")
    public ResponseEntity<Void> onUpdate(@RequestBody Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String text =  update.getMessage().getText();

            String reply = "You said: " + text;
            telegramSenderService.sendMessage(chatId, reply);
        }
        return ResponseEntity.ok().build();
    }
}
