package com.example.eia_app.providers;

public interface AiProvider {
    interface AiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    void askAi(String prompt, AiCallback callback);
}
