package com.eia.app.viewModels;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.eia.app.models.Device;
import com.eia.app.models.MqttEvent;
import com.eia.app.models.Sensor;
import com.eia.app.repositories.MqttRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DashboardViewModel extends AndroidViewModel {

    private static final String PREFS_NAME = "EIA_DEVICES_PREFS";
    private static final String KEY_DEVICES = "devices_list";
    private static final String TAG = "DashboardViewModel";

    private final MutableLiveData<List<Device>> devices = new MutableLiveData<>(new ArrayList<>());
    private final Gson gson = new Gson();
    private final SharedPreferences prefs;


    public DashboardViewModel(@NonNull Application application) {
        super(application);
        prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadDevices();
        observeMqttEvents();
    }

    private void observeMqttEvents() {
        MqttRepository.getInstance().getEventStream().observeForever(event -> {
            if (event == null || event.getDeviceId() == null) return;
            
            Log.d(TAG, "Nowy event MQTT: " + event.getDeviceId() + " [" + event.getType() + "]");

            List<Device> currentList = devices.getValue();
            if (currentList == null) return;

            List<Device> newList = new ArrayList<>();
            boolean deviceFound = false;
            boolean anyUpdated = false;

            for (Device device : currentList) {
                if (device.getId().equals(event.getDeviceId())) {
                    deviceFound = true;
                    Device updatedDevice = device.copy();
                    if (event.getType() == MqttEvent.Type.STATUS) {
                        boolean isOnline = "ONLINE".equalsIgnoreCase(event.getPayload());
                        updatedDevice.setOnline(isOnline);
                        
                        // Jeśli bramka jest offline
                        if (!isOnline) {
                            for (Sensor s : updatedDevice.getSensorList()) {
                                s.setHasError(true);
                            }
                        }
                        
                        newList.add(updatedDevice);
                        anyUpdated = true;
                    } else if (event.getType() == MqttEvent.Type.DATA) {
                        updateSensorData(updatedDevice, event.getSensorId(), event.getPayload());
                        newList.add(updatedDevice);
                        anyUpdated = true;
                    } else {
                        newList.add(device);
                    }
                } else {
                    newList.add(device);
                }
            }

            //jeśli urządzenia nie ma na liście
            if (!deviceFound && currentList.size() < 5) {
                Log.d(TAG, "new device: " + event.getDeviceId());
                Device newDevice = new Device(event.getDeviceId(), "Bramka " + event.getDeviceId());
                
                if (event.getType() == MqttEvent.Type.STATUS) {
                    boolean isOnline = "ONLINE".equalsIgnoreCase(event.getPayload());
                    newDevice.setOnline(isOnline);
                } else if (event.getType() == MqttEvent.Type.DATA) {
                    updateSensorData(newDevice, event.getSensorId(), event.getPayload());
                }
                
                newList.add(newDevice);
                anyUpdated = true;
            }

            if (anyUpdated) {
                devices.setValue(newList);
                persistDevices(newList);
            }
        });
    }

    private void updateSensorData(Device device, String sensorId, String payload) {
        if (sensorId == null || payload == null) return;
        
        List<Sensor> sensors = device.getSensorList();
        if (sensors == null) {
            sensors = new ArrayList<>();
            device.setSensorList(sensors);
        }

        try {
            String trimmedPayload = payload.trim();

            // Obsługa stanu błędu lub offline
            if ("ERR:NoSensors".equalsIgnoreCase(trimmedPayload) || "offline".equalsIgnoreCase(trimmedPayload)) {
                updateAllSensorsError(sensors, sensorId, true);
                return;
            }

            // Rozdzielamy po średniku (np. T:24.3;H:70)
            String[] parts = trimmedPayload.split(";");
            boolean firstFound = false;

            for (String part : parts) {
                String p = part.trim();
                if (p.contains(":")) {
                    String[] kv = p.split(":", 2);
                    String prefix = kv[0].trim();
                    String valStr = kv[1].trim();
                    
                    if (valStr.isEmpty()) continue;

                    String unit = com.eia.app.models.SensorMetadata.getUnitForPrefix(prefix);
                    float value = Float.parseFloat(valStr);
                    
                    // Unikalne ID dla każdego typu danych z tego czujnika
                    String logicSensorId = sensorId + "_" + prefix;
                    boolean isPrimary = !firstFound; // Pierwszy w stringu jest główny

                    updateSingleSensor(sensors, logicSensorId, prefix, unit, value, isPrimary);
                    firstFound = true;
                }
            }
            
        } catch (NumberFormatException e) {
            Log.e(TAG, "Błąd formatu danych sensora: " + payload);
        }
    }

    private void updateSingleSensor(List<Sensor> sensors, String id, String prefix, String unit, float value, boolean isPrimary) {
        boolean found = false;
        for (Sensor s : sensors) {
            if (s.getId().equals(id)) {
                s.setValue(value);
                s.setUnit(unit);
                s.setPrefix(prefix);
                s.setPrimary(isPrimary);
                s.setHasError(false);
                found = true;
                break;
            }
        }

        if (!found) {
            String name;
            switch (prefix) {
                case "T":
                    name = "Temperatura";
                    break;
                case "H":
                    name = "Wilgotność";
                    break;
                case "P":
                    name = "Ciśnienie";
                    break;
                case "L":
                    name = "Jasność";
                    break;
                default:
                    name = id; // Fallback do ID
                    break;
            }
            sensors.add(new Sensor(id, name, unit, value, false, prefix, isPrimary));
        }
    }

    private void updateAllSensorsError(List<Sensor> sensors, String baseSensorId, boolean hasError) {
        boolean anyFound = false;
        for (Sensor s : sensors) {
            if (s.getId().startsWith(baseSensorId)) {
                s.setHasError(hasError);
                anyFound = true;
            }
        }
        if (!anyFound && hasError) {
            sensors.add(new Sensor(baseSensorId, "Brak sensora", "---", 0, true, "", true));
        }
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
        
        boolean found = false;
        for (int i = 0; i < currentList.size(); i++) {
            if (currentList.get(i).getId().equals(device.getId())) {
                currentList.set(i, device);
                found = true;
                break;
            }
        }
        
        if (!found) {
            if (currentList.size() >= 5) return;
            currentList.add(device);
        }
        
        devices.setValue(new ArrayList<>(currentList));
        persistDevices(currentList);
    }

    public void deleteDevice(String deviceId) {
        List<Device> currentList = devices.getValue();
        if (currentList != null) {
            currentList.removeIf(d -> d.getId().equals(deviceId));
            devices.setValue(new ArrayList<>(currentList));
            persistDevices(currentList);
        }
    }

    public void clearAllDevices() {
        List<Device> emptyList = new ArrayList<>();
        devices.setValue(emptyList);
        persistDevices(emptyList);
    }

    private void persistDevices(List<Device> list) {
        String json = gson.toJson(list);
        prefs.edit().putString(KEY_DEVICES, json).apply();
    }

    public LiveData<List<Device>> getDevices() {
        return devices;
    }

    public void initMqttConnection() {
        MqttRepository.getInstance().connectToBroker();
    }

}
