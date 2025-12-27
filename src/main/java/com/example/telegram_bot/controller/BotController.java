package com.example.telegram_bot.controller;


import com.example.telegram_bot.service.JapaneseCorrectionService;
import com.example.telegram_bot.service.TelegramSenderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
public class BotController {

    private final TelegramSenderService telegramSenderService;
    private final JapaneseCorrectionService japaneseCorrectionService;

    public BotController(
            TelegramSenderService telegramSenderService,
            JapaneseCorrectionService japaneseCorrectionService
    ) {
        this.telegramSenderService = telegramSenderService;
        this.japaneseCorrectionService = japaneseCorrectionService;
    }

    @PostMapping("/telegram/webhook")
    public ResponseEntity<Void> onUpdate(@RequestBody Update update) {

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return ResponseEntity.ok().build();
        }

        Long chatId = update.getMessage().getChatId();
        String userText = update.getMessage().getText();

        String corrected = japaneseCorrectionService.correctJapanese(userText);

        telegramSenderService.sendMessage(chatId, corrected);

        return ResponseEntity.ok().build();
    }
}
