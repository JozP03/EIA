package com.example.eia_app.repositories;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import java.util.UUID;

public class MqttRepository {
    private static final String TAG = "MqttRepository";
    private static MqttRepository instance;
    private final MutableLiveData<Float> messageStream = new MutableLiveData<>();
    private final MutableLiveData<String> statusStream = new MutableLiveData<>();

    private String username = "";
    private String password = "";
    private Mqtt5AsyncClient client;

    private MqttRepository(){
    }

    public void configure(String host, String username, String password) {
        this.username = username;
        this.password = password;

        client = MqttClient.builder()
                .useMqttVersion5()
                .identifier("app" + UUID.randomUUID().toString())
                .serverHost(host)
                .serverPort(8883)
                .sslWithDefaultConfig()
                .buildAsync();
    }

    public void connectToBroker(){
        if (client == null) {
            Log.e(TAG, "Klient MQTT nie został skonfigurowany!");
            return;
        }

        client.connectWith()
                .simpleAuth()
                .username(username)
                .password(password.getBytes())
                .applySimpleAuth()
                .send()
                .whenComplete(((mqtt5ConnAck, throwable) -> {
                    if(throwable != null){
                        Log.e(TAG,"Błąd połączenia z MQTT: " + throwable.getMessage());
                    }else {
                        Log.e(TAG,"Połączono z MQTT");
                        subscribeTopics();
                    }
                }));
    }

    public void disconnectFromBroker(){
        Log.e(TAG,"Rozłączono z MQTT");
        client.disconnect();
    }

    public static synchronized MqttRepository getInstance(){
        if( instance == null ){
            instance = new MqttRepository();
        }
        return instance;
    }

    public void publishCommand(String topic, String jsonPayload) {
        if(client != null && client.getState().isConnected()) {
            client.publishWith()
                    .topic(topic)
                    .payload(jsonPayload.getBytes())
                    .send();
        }
    }

    public void subscribeTopics() {
        // Subskrypcja na dane (np. dom/czujnik1/temp)
        client.subscribeWith()
                .topicFilter("dom/+/temp")
                .callback(publish -> {
                    String payload = new String(publish.getPayloadAsBytes());
                    try {
                        float value = Float.parseFloat(payload.trim());
                        messageStream.postValue(value);
                    } catch (NumberFormatException e) {
                        Log.e(TAG,"Błąd dekodowania danych: " + e.getMessage());
                    }
                })
                .send();

        // Subskrypcja na statusy (np. dom/brama/status)
        client.subscribeWith()
                .topicFilter("dom/+/status")
                .callback(publish -> {
                    String status = new String(publish.getPayloadAsBytes());
                    String topic = publish.getTopic().toString();
                    Log.d(TAG, "Status z " + topic + ": " + status);
                    statusStream.postValue(status);
                })
                .send();
    }

    public LiveData<Float> getMessageStream(){
        return messageStream;
    }

    public LiveData<String> getStatusStream() {
        return statusStream;
    }
}
