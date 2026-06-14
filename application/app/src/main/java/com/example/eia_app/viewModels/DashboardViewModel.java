package com.example.eia_app.viewModels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.eia_app.repositories.MqttRepository;

public class DashboardViewModel extends ViewModel {

    //Testowe do zminany na poźniejszym etapie
    public void initMqttConnection() {
        MqttRepository.getInstance().configure("", "", "");
        MqttRepository.getInstance().connectToBroker();
    }

    private final LiveData<Float> temp;
    private final LiveData<String> status;

    public DashboardViewModel() {
        temp = MqttRepository.getInstance().getMessageStream();
        status = MqttRepository.getInstance().getStatusStream();
    }

    public LiveData<Float> getTemperature() {
        return temp;
    }

    public LiveData<String> getStatus() {
        return status;
    }
}
