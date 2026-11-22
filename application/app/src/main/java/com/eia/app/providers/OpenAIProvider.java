package com.eia.app.providers;

import android.util.Log;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class OpenAIProvider implements AiProvider {
    private static final String TAG = "OpenAIProvider";
    private final OpenAIService service;

    public OpenAIProvider(String baseUrl, String apiKey) {
        String formattedUrl = (baseUrl == null || baseUrl.trim().isEmpty()) ? "https://api.openai.com/" : baseUrl.trim();
        if (!formattedUrl.endsWith("/")) {
            formattedUrl += "/";
        }
        if (formattedUrl.endsWith("/v1/")) {
            formattedUrl = formattedUrl.substring(0, formattedUrl.length() - 3);
        }

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS);

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            clientBuilder.addInterceptor(chain -> {
                Request request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .build();
                return chain.proceed(request);
            });
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(formattedUrl)
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.service = retrofit.create(OpenAIService.class);
    }

    @Override
    public void askAi(String prompt, AiCallback callback) {
        OpenAIChatRequest chatRequest = new OpenAIChatRequest(prompt);

        service.chatCompletions(chatRequest).enqueue(new Callback<OpenAIChatResponse>() {
            @Override
            public void onResponse(Call<OpenAIChatResponse> call, Response<OpenAIChatResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    OpenAIChatResponse body = response.body();
                    if (body.choices != null && !body.choices.isEmpty()) {
                        OpenAIChatResponse.Choice choice = body.choices.get(0);
                        if (choice.message != null && choice.message.content != null) {
                            callback.onSuccess(choice.message.content);
                            return;
                        } else if (choice.text != null) {
                            callback.onSuccess(choice.text);
                            return;
                        }
                    }
                    callback.onError("Brak treści w odpowiedzi AI.");
                } else {
                    tryKoboldNative(prompt, callback, "Błąd API: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<OpenAIChatResponse> call, Throwable t) {
                tryKoboldNative(prompt, callback, "Błąd sieci: " + t.getMessage());
            }
        });
    }

    private void tryKoboldNative(String prompt, AiCallback callback, String originalError) {
        KoboldRequest koboldRequest = new KoboldRequest(prompt);

        service.koboldGenerate(koboldRequest).enqueue(new Callback<KoboldResponse>() {
            @Override
            public void onResponse(Call<KoboldResponse> call, Response<KoboldResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().results != null && !response.body().results.isEmpty()) {
                    String text = response.body().results.get(0).text;
                    if (text != null && !text.trim().isEmpty()) {
                        callback.onSuccess(text);
                        return;
                    }
                }
                callback.onError(originalError);
            }

            @Override
            public void onFailure(Call<KoboldResponse> call, Throwable t) {
                callback.onError(originalError);
            }
        });
    }

    interface OpenAIService {
        @POST("v1/chat/completions")
        Call<OpenAIChatResponse> chatCompletions(@Body OpenAIChatRequest request);

        @POST("api/v1/generate")
        Call<KoboldResponse> koboldGenerate(@Body KoboldRequest request);
    }

    static class OpenAIChatRequest {
        String model = "koboldcpp";
        List<Message> messages;

        OpenAIChatRequest(String prompt) {
            this.messages = Collections.singletonList(new Message("user", prompt));
        }

        static class Message {
            String role;
            String content;

            Message(String role, String content) {
                this.role = role;
                this.content = content;
            }
        }
    }

    static class OpenAIChatResponse {
        List<Choice> choices;

        static class Choice {
            Message message;
            String text;
        }

        static class Message {
            String role;
            String content;
        }
    }

    static class KoboldRequest {
        String prompt;
        int max_length = 512;

        KoboldRequest(String prompt) {
            this.prompt = prompt;
        }
    }

    static class KoboldResponse {
        List<Result> results;

        static class Result {
            String text;
        }
    }
}
