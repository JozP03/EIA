package com.eia.app.viewModels;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.eia.app.models.Device;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DashboardViewModel extends AndroidViewModel {

    private static final String PREFS_NAME = "EIA_DEVICES_PREFS";
    private static final String KEY_DEVICES = "devices_list";

    private final MutableLiveData<List<Device>> devices = new MutableLiveData<>(new ArrayList<>());
    private final Gson gson = new Gson();
    private final SharedPreferences prefs;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadDevices();
    }

    public void loadDevices() {
        String json = prefs.getString(KEY_DEVICES, null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<Device>>() {}.getType();
            List<Device> loadedDevices = gson.fromJson(json, type);
            devices.setValue(loadedDevices);
        }
    }

    public void saveDevice(Device device) {
        List<Device> currentList = devices.getValue();
        if (currentList == null) currentList = new ArrayList<>();
        
        // Jeśli już istnieje urządzenie o tym ID, podmień je
        boolean found = false;
        for (int i = 0; i < currentList.size(); i++) {
            if (currentList.get(i).getId().equals(device.getId())) {
                currentList.set(i, device);
                found = true;
                break;
            }
        }
        
        if (!found) {
            if (currentList.size() >= 5) {
                // można by rzucić błąd jakis
                return;
            }
            currentList.add(device);
        }
        
        devices.setValue(currentList);
        persistDevices(currentList);
    }

    public void deleteDevice(String deviceId) {
        List<Device> currentList = devices.getValue();
        if (currentList != null) {
            currentList.removeIf(d -> d.getId().equals(deviceId));
            devices.setValue(currentList);
            persistDevices(currentList);
        }
    }

    private void persistDevices(List<Device> list) {
        String json = gson.toJson(list);
        prefs.edit().putString(KEY_DEVICES, json).apply();
    }

    public LiveData<List<Device>> getDevices() {
        return devices;
    }

    public void initMqttConnection() {

    }
}
