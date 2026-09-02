package com.eia.app.repositories;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.eia.app.models.MqttEvent;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

import java.util.UUID;

public class MqttRepository {
    private static final String TAG = "MqttRepository";
    private static MqttRepository instance;
    
    private final MutableLiveData<MqttEvent> eventStream = new MutableLiveData<>();

    private String host = "";
    private String username = "";
    private String password = "";
    private Mqtt5AsyncClient client;

    private MqttRepository(){
    }

    public static synchronized MqttRepository getInstance(){
        if( instance == null ){
            instance = new MqttRepository();
        }
        return instance;
    }

    public void configure(String host, String username, String password) {
        if (host == null || host.isEmpty()) {
            Log.e(TAG, "Server host cannot be null or empty.");
            return;
        }
        this.host = host;
        this.username = username;
        this.password = password;

        client = MqttClient.builder()
                .useMqttVersion5()
                .identifier("app" + UUID.randomUUID().toString())
                .serverHost(host)
                .serverPort(8883)
                .sslWithDefaultConfig()
                .automaticReconnectWithDefaultConfig()
                .addConnectedListener(context -> {
                    Log.d(TAG, "Połączono (lub połączono ponownie)");
                    subscribeTopics();
                })
                .addDisconnectedListener(context -> Log.w(TAG, "Rozłączono: " + (context.getCause() != null ? context.getCause().getMessage() : "brak powodu")))
                .buildAsync();
    }

    public void connectToBroker(){
        if (client == null) {
            Log.e(TAG, "Klient MQTT nie został skonfigurowany!");
            return;
        }

        if (client.getState().isConnected()) {
            Log.d(TAG, "MQTT już połączone.");
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
                        Log.d(TAG,"Wysłano żądanie połączenia MQTT");
                    }
                }));
    }

    public void disconnectFromBroker(){
        if (client != null) {
            Log.d(TAG,"Definitywne rozłączanie z MQTT...");
            client.disconnect();
            client = null;
        }
        this.host = "";
        this.username = "";
        this.password = "";
        eventStream.postValue(null);
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
        if (client == null) {
            Log.e(TAG, "Cannot subscribe: Client is null");
            return;
        }

        // Format statusu bramki: deviceid/status
        // Format danych sensora: deviceid/sensorid
        client.subscribeWith()
                .topicFilter("#")
                .callback(publish -> {
                    String topic = publish.getTopic().toString();
                    String payload = new String(publish.getPayloadAsBytes());
                    
                    String[] parts = topic.split("/");
                    if (parts.length == 2) {
                        String deviceId = parts[0];
                        String secondPart = parts[1];
                        
                        if (secondPart.equals("status")) {
                            // deviceid/status
                            eventStream.postValue(new MqttEvent(deviceId, null, payload, MqttEvent.Type.STATUS));
                        } else if (secondPart.equals("history")) {
                            // deviceid/history
                            eventStream.postValue(new MqttEvent(deviceId, null, payload, MqttEvent.Type.HISTORY));
                        } else {
                            // deviceid/sensorid
                            eventStream.postValue(new MqttEvent(deviceId, secondPart, payload, MqttEvent.Type.DATA));
                        }
                    } else if (parts.length == 3) {
                        // id_bramki/id_czujnika/cos
                        String deviceId = parts[0];
                        String sensorId = parts[1];
                        String thirdPart = parts[2];
                        
                        if (thirdPart.equals("config")) {
                            eventStream.postValue(new MqttEvent(deviceId, sensorId, payload, MqttEvent.Type.CONFIG));
                        } else {
                            eventStream.postValue(new MqttEvent(deviceId, sensorId, payload, MqttEvent.Type.DATA));
                        }
                    }
                })
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "Błąd subskrypcji: " + throwable.getMessage());
                    } else {
                        Log.d(TAG, "Subskrypcja aktywna");
                    }
                });
    }

    public LiveData<MqttEvent> getEventStream(){
        return eventStream;
    }
}
