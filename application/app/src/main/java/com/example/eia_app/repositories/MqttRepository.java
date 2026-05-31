package com.example.eia_app.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import java.util.UUID;

public class MqttRepository {
    private static MqttRepository instance;
    private final MutableLiveData<String> MessageStream = new MutableLiveData<>();
    Mqtt5AsyncClient client = MqttClient.builder()
            .useMqttVersion5()
            .identifier("app" + UUID.randomUUID().toString())
            .serverHost("") //adress mqtt brokera
            .serverPort(8883) //port tls
            .sslWithDefaultConfig()
            .buildAsync();

    private MqttRepository(){
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

    public void subscribeTopics(){
        client.subscribeWith()
                .topicFilter("#") //narazie testowo # - wszystkie
                .callback(mqtt5Publish -> {
                    String payload = new String(mqtt5Publish.getPayloadAsBytes());

                    MessageStream.postValue(payload);
                })
                .send();
    }

    public LiveData<String> getMessageStream(){
        return MessageStream;
    }
}
