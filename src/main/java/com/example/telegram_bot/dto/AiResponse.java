package com.example.telegram_bot.dto;

import lombok.Data;

@Data
public class AiResponse {
    private Result result;

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }
}

