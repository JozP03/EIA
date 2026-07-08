package com.eia.app.providers;

public interface AiProvider {
    interface AiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    void askAi(String prompt, AiCallback callback);
}
