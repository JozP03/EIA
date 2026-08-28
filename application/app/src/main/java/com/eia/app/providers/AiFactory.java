package com.eia.app.providers;

import android.content.Context;
import android.content.SharedPreferences;

public class AiFactory {
    public static AiProvider getProvider(Context context) {
        Context appContext = context.getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences("EIA_PREFS", Context.MODE_PRIVATE);
        String providerName = prefs.getString("ai_provider", "OpenAI API (ChatGPT)");
        String baseUrl = prefs.getString("ai_base_url", "https://api.openai.com/v1/");
        String apiKey = prefs.getString("ai_api_key", "");

        if ("Gemini API".equals(providerName)) {
            return new GeminiProvider(baseUrl, apiKey);
        } else {
            // Wszystkie inne używają formatu OpenAI
            return new OpenAIProvider(baseUrl, apiKey);
        }
    }
}
