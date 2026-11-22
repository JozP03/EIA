package com.eia.app.providers;

import android.util.Log;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;
import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;

import java.util.Collections;
import java.util.List;

public class GeminiProvider implements AiProvider {
    private static final String TAG = "GeminiProvider";
    private final GeminiService service;
    private final String apiKey;

    public GeminiProvider(String baseUrl, String apiKey) {
        this.apiKey = apiKey;

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        this.service = retrofit.create(GeminiService.class);
    }

    @Override
    public void askAi(String prompt, AiCallback callback) {
        GeminiRequest request = new GeminiRequest(prompt);
        
        service.generateContent(apiKey, request).enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        GeminiResponse body = response.body();
                        if (body.candidates != null && !body.candidates.isEmpty()) {
                            GeminiResponse.Candidate candidate = body.candidates.get(0);
                            if (candidate.content != null && candidate.content.parts != null && !candidate.content.parts.isEmpty()) {
                                String text = candidate.content.parts.get(0).text;
                                callback.onSuccess(text);
                                return;
                            }
                        }
                        callback.onError("AI nie wygenerowało odpowiedzi (możliwa blokada treści).");
                    } catch (Exception e) {
                        Log.e(TAG, "Błąd parsowania: ", e);
                        callback.onError("Błąd przetwarzania odpowiedzi.");
                    }
                } else {
                    callback.onError("Błąd API: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                callback.onError("Błąd sieci: " + t.getMessage());
            }
        });
    }


    interface GeminiService {
        @POST("v1beta/models/gemini-3.6-flash:generateContent")
        Call<GeminiResponse> generateContent(@Query("key") String apiKey, @Body GeminiRequest request);
    }

    static class GeminiRequest {
        List<Content> contents;

        GeminiRequest(String text) {
            this.contents = Collections.singletonList(new Content(text));
        }

        static class Content {
            List<Part> parts;
            Content(String text) {
                this.parts = Collections.singletonList(new Part(text));
            }
        }

        static class Part {
            String text;
            Part(String text) {
                this.text = text;
            }
        }
    }

    static class GeminiResponse {
        List<Candidate> candidates;
        static class Candidate {
            Content content;
        }
        static class Content {
            List<Part> parts;
        }
        static class Part {
            String text;
        }
    }
}
