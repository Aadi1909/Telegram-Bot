package com.example.telegram_bot.dto;

import lombok.Data;

import java.util.List;


@Data
public class Result {
    private String kanji;
    private String hiragana;
    private String explanation;
    private List<String> warnings;

    public String getKanji() {
        return kanji;
    }

    public String getHiragana() {
        return hiragana;
    }

    public String getExplanation() {
        return explanation;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}