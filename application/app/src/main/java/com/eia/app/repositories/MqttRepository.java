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
                        Log.d(TAG,"Połączono z MQTT");
                        subscribeTopics();
                    }
                }));
    }

    public void disconnectFromBroker(){
        if (client != null) {
            Log.d(TAG,"Rozłączono z MQTT");
            client.disconnect();
        }
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
        
        // Subskrypcja na wszystko
        // Format statusu bramki: deviceid/status
        // Format danych sensora: deviceid/sensorid/value
        client.subscribeWith()
                .topicFilter("#")
                .callback(publish -> {
                    String topic = publish.getTopic().toString();
                    String payload = new String(publish.getPayloadAsBytes());
                    
                    String[] parts = topic.split("/");
                    if (parts.length == 2) {
                        // deviceid/status
                        String deviceId = parts[0];
                        if (parts[1].equals("status")) {
                            eventStream.postValue(new MqttEvent(deviceId, null, payload, MqttEvent.Type.STATUS));
                        }
                    } else if (parts.length == 3) {
                        // deviceid/sensorid/status lub value
                        String deviceId = parts[0];
                        String sensorId = parts[1];
                        eventStream.postValue(new MqttEvent(deviceId, sensorId, payload, MqttEvent.Type.DATA));
                    }
                })
                .send();
    }

    public LiveData<MqttEvent> getEventStream(){
        return eventStream;
    }
}
