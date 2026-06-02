package com.example.eia_app.viewModels;

import androidx.lifecycle.ViewModel;
import com.example.eia_app.repositories.MqttRepository;

public class DashboardViewModel extends ViewModel {
    public void initMqttConnection() {
        MqttRepository.getInstance().connectToBroker();
    }
}
