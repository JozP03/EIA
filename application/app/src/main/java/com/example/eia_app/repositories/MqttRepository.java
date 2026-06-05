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

    //testowe zmienne todo: Wpisywanie powinno być w ustawieniach. Przypisywanie do zmiennych
    String username = "";
    String password = "";
    String host = "";
    // usunac pozniej
    Mqtt5AsyncClient client = MqttClient.builder()
            .useMqttVersion5()
            .identifier("app" + UUID.randomUUID().toString())
            .serverHost(host) //adress mqtt brokera
            .serverPort(8883) //port tls
            .sslWithDefaultConfig()
            .buildAsync();


    private MqttRepository(){
    }

    public void connectToBroker(){
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

    public void subscribeTopics(){
        client.subscribeWith()
                .topicFilter("#") //narazie testowo # - wszystkie
                .callback(mqtt5Publish -> {
                    String payload = new String(mqtt5Publish.getPayloadAsBytes());

                    try{
                        float value = Float.parseFloat(payload.trim());
                        messageStream.postValue(value);
                    }catch (NumberFormatException e){
                        Log.e(TAG,"Błąd dekodowania: " + e.getMessage());
                    }
                })
                .send();
    }

    public LiveData<Float> getMessageStream(){
        return messageStream;
    }
}
