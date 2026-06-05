package com.example.eia_app.viewModels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.eia_app.repositories.MqttRepository;

public class DashboardViewModel extends ViewModel {

    //Testowe do zminany na poźniejszym etapie
    public void initMqttConnection() {
        MqttRepository.getInstance().connectToBroker();
        MqttRepository.getInstance().subscribeTopics();
    }

    private final LiveData<Float> temp;

    public DashboardViewModel() {
        temp = MqttRepository.getInstance().getMessageStream();
    }

    public LiveData<Float> getTemperature() {
        return temp;
    }
}
