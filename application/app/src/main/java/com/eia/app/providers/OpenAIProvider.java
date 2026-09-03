package com.eia.app.providers;

public class OpenAIProvider implements AiProvider {

    public OpenAIProvider(String baseUrl, String apiKey) {
        // na pozniej albo nigdy
    }

    @Override
    public void askAi(String prompt, AiCallback callback) {
        callback.onSuccess("Odpowiedź makiety OpenAI");
    }
}
