package com.eia.app.repositories;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.eia.app.models.MqttEvent;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MqttRepository {
    private static final String TAG = "MqttRepository";
    private static MqttRepository instance;
    
    private final MutableLiveData<MqttEvent> eventStream = new MutableLiveData<>();

    private String host = "";
    private int port = 0;
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

    public void configure(String host, int port, String username, String password) {
        if (host == null || host.isEmpty()) {
            Log.e(TAG, "Server host cannot be null or empty.");
            return;
        }

        if (this.host.equals(host) && this.port == port && 
            this.username.equals(username) && this.password.equals(password) && 
            client != null) {
            Log.d(TAG, "Konfiguracja MQTT identyczna, pomijam tworzenie klienta.");
            return;
        }

        if (client != null) {
            Log.d(TAG, "Zamykanie starego klienta MQTT przed rekonfiguracją...");
            client.disconnect();
        }

        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;

        Mqtt5ClientBuilder builder = MqttClient.builder()
                .useMqttVersion5()
                .identifier("app-" + UUID.randomUUID().toString().substring(0, 8))
                .serverHost(host)
                .serverPort(port)
                .automaticReconnect()
                    .initialDelay(1, TimeUnit.SECONDS)
                    .maxDelay(30, TimeUnit.SECONDS)
                    .applyAutomaticReconnect()
                .addConnectedListener(context -> {
                    Log.d(TAG, "Połączono (lub połączono ponownie)");
                    subscribeTopics();
                })
                .addDisconnectedListener(context -> {
                    String reason = (context.getCause() != null ? context.getCause().getMessage() : "brak powodu");
                    Log.w(TAG, "Rozłączono: " + reason);
                });

        if (port == 8883) {
            client = builder.sslWithDefaultConfig().buildAsync();
        } else {
            client = builder.buildAsync();
        }
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

        var connectBuilder = client.connectWith();
        
        if (username != null && !username.isEmpty()) {
            connectBuilder.simpleAuth()
                    .username(username)
                    .password(password.getBytes())
                    .applySimpleAuth();
        }

        connectBuilder.send()
                .whenComplete(((mqtt5ConnAck, throwable) -> {
                    if(throwable != null){
                        Log.e(TAG,"Błąd połączenia z MQTT: " + throwable.getMessage());
                    }else {
                        Log.d(TAG,"Wysłano żądanie połączenia MQTT: " + mqtt5ConnAck.getReasonCode());
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
