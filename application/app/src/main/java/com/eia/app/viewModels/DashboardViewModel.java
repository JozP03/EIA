package com.eia.app.viewModels;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.eia.app.R;
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
    private final com.eia.app.db.AppDatabase db;
    private final java.util.Map<String, Long> lastSyncTimes = new java.util.HashMap<>();


    public DashboardViewModel(@NonNull Application application) {
        super(application);
        prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        db = com.eia.app.db.AppDatabase.getDatabase(application);
        loadDevices();
        observeMqttEvents();
        cleanOldData();
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
                    } else if (event.getType() == MqttEvent.Type.HISTORY) {
                        processHistoryMessage(updatedDevice, event.getPayload());
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
                String defaultName = getApplication().getString(R.string.gateway_default_name, event.getDeviceId());
                Device newDevice = new Device(event.getDeviceId(), defaultName);
                
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

    private void processHistoryMessage(Device device, String payload) {
        if (payload == null || payload.equalsIgnoreCase("EOF")) {
            Log.d(TAG, "Koniec przesyłania historii (EOF)");
            return;
        }

        try {
            String[] parts = payload.split(";");
            if (parts.length < 3) return;

            long timestamp = Long.parseLong(parts[0]) * 1000L; // Zamiana sekund na milisekundy
            String physicalId = parts[1];

            List<Sensor> sensors = device.getSensorList();
            if (sensors == null) {
                sensors = new ArrayList<>();
                device.setSensorList(sensors);
            }

            boolean firstInMessage = true;
            for (int i = 2; i < parts.length; i++) {
                String measure = parts[i].trim();
                if (measure.contains(":")) {
                    String[] kv = measure.split(":", 2);
                    String prefix = kv[0].trim();
                    String valStr = kv[1].trim();
                    
                    if (valStr.isEmpty()) continue;

                    float value = Float.parseFloat(valStr);
                    String unit = com.eia.app.models.SensorMetadata.getUnitForPrefix(prefix);
                    
                    String logicSensorId = physicalId + "_" + prefix;
                    boolean isPrimary = false;
                    Sensor existing = null;
                    for (Sensor s : sensors) {
                        if (s.getId().equals(logicSensorId)) {
                            existing = s;
                            break;
                        }
                    }
                    if (existing != null) {
                        isPrimary = existing.isPrimary();
                    } else if (firstInMessage) {
                        isPrimary = true;
                        firstInMessage = false;
                    }

                    updateSingleSensor(sensors, logicSensorId, prefix, unit, value, isPrimary, physicalId, timestamp, false);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Błąd parsowania historii: " + payload + " -> " + e.getMessage());
        }
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

            if ("ERR:NoSensors".equalsIgnoreCase(trimmedPayload) || "offline".equalsIgnoreCase(trimmedPayload)) {
                updateAllSensorsError(sensors, sensorId, true);
                return;
            }

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

                    String logicSensorId = sensorId + "_" + prefix;
                    boolean isPrimary = !firstFound;

                    updateSingleSensor(sensors, logicSensorId, prefix, unit, value, isPrimary, sensorId, System.currentTimeMillis(), true);
                    firstFound = true;
                }
            }
            
        } catch (NumberFormatException e) {
            Log.e(TAG, "Błąd formatu danych sensora: " + payload);
        }
    }

    public String getAiSystemContext() {
        StringBuilder context = new StringBuilder();
        context.append("Jesteś inteligentnym asystentem systemu EIA.AI. ");
        context.append("Pomagasz użytkownikowi monitorować jego dom. ");
        context.append("Oto aktualne dane z systemu:\n\n");

        List<Device> currentList = devices.getValue();
        if (currentList == null || currentList.isEmpty()) {
            context.append("- Brak skonfigurowanych urządzeń.\n");
        } else {
            for (Device d : currentList) {
                context.append("- Urządzenie: ").append(d.getName())
                       .append(" (Status: ").append(d.isOnline() ? "ONLINE" : "OFFLINE").append(")\n");
                
                if (d.getSensorList() != null) {
                    for (com.eia.app.models.Sensor s : d.getSensorList()) {
                        if (s.isHasError()) {
                            context.append("  * ").append(s.getName()).append(": BŁĄD/BRAK DANYCH\n");
                        } else {
                            context.append("  * ").append(s.getName()).append(": ")
                                   .append(s.getValue()).append(" ").append(s.getUnit()).append("\n");
                        }
                    }
                }
            }
        }
        context.append("\nZASADY STEROWANIA:\n");
        context.append("1. Możesz zmieniać częstotliwość raportowania czujników.\n");
        context.append("2. Aby to zrobić, dodaj na końcu odpowiedzi komendę: [CMD:SET_INTERVAL:PHYSICAL_ID:SECONDS].\n");
        context.append("3. PHYSICAL_ID to identyfikator typu ESP_XXXX. SECONDS to liczba sekund (np. 300 dla 5 minut).\n");
        context.append("4. Potwierdź wykonanie akcji jednym krótkim zdaniem.\n");

        context.append("\nINSTRUKCJA ODPOWIADANIA:\n");
        context.append("- Odpowiadaj zawsze w języku, w którym napisał użytkownik.\n");
        context.append("- Odpowiadaj bardzo krótko, konkretnie i wyłącznie na temat.\n");
        context.append("- Nie lej wody, unikaj długich wstępów i zbędnych zdań.\n");
        context.append("- Jeśli użytkownik pyta o dane, podaj je od razu.\n");

        context.append("\nNa podstawie powyższych danych odpowiedz na pytanie użytkownika.");
        return context.toString();
    }

    public String handleAiResponseAndGetCleanText(String deviceId, String response) {
        if (response == null) return "";

        if (response.contains("[CMD:SET_INTERVAL:")) {
            try {
                int start = response.indexOf("[CMD:");
                int end = response.indexOf("]", start);
                String fullCmd = response.substring(start + 5, end);
                String[] parts = fullCmd.split(":");
                
                if (parts.length >= 3) {
                    String physicalId = parts[1];
                    String seconds = parts[2];
                    String targetDeviceId = deviceId;

                    if ("global".equals(deviceId)) {
                        List<Device> currentList = devices.getValue();
                        if (currentList != null) {
                            for (Device d : currentList) {
                                if (d.getSensorList() != null) {
                                    for (Sensor s : d.getSensorList()) {
                                        if (physicalId.equals(s.getPhysicalId())) {
                                            targetDeviceId = d.getId();
                                            break;
                                        }
                                    }
                                }
                                if (!"global".equals(targetDeviceId)) break;
                            }
                        }
                    }

                    if (!"global".equals(targetDeviceId)) {
                        // Format: id_bramki/id_esp/config z treścią INTERVAL:sekundy
                        String topic = targetDeviceId + "/" + physicalId + "/config";
                        String payload = "INTERVAL:" + seconds;
                        com.eia.app.repositories.MqttRepository.getInstance().publishCommand(topic, payload);
                        Log.d(TAG, "AI wysłało komendę MQTT: " + topic + " -> " + payload);
                    } else {
                        Log.w(TAG, "Nie znaleziono bramki dla sensora: " + physicalId);
                    }
                }

                return response.substring(0, start).trim() + response.substring(end + 1).trim();
            } catch (Exception e) {
                Log.e(TAG, "Błąd parsowania komendy AI: " + e.getMessage());
            }
        }
        return response;
    }

    private void updateSingleSensor(List<Sensor> sensors, String id, String prefix, String unit, float value, boolean isPrimary, String physicalId, long timestamp, boolean isLive) {
        boolean found = false;
        for (Sensor s : sensors) {
            if (s.getId().equals(id)) {
                s.setValue(value);
                s.setUnit(unit);
                s.setPrefix(prefix);
                s.setPrimary(isPrimary);
                s.setPhysicalId(physicalId);

                if (isLive) {
                    s.setHasError(false);
                }

                found = true;
                break;
            }
        }

        if (!found) {
            String existingName = null;
            for(Sensor s : sensors) {
                if(physicalId.equals(s.getPhysicalId())) {
                    existingName = s.getName();
                    break;
                }
            }

            String name;
            if (existingName != null) {
                name = existingName;
            } else {
                // Jeśli nie ma nazwy, używamy nazwy przyjaznej dla prefixu lub ID
                switch (prefix) {
                    case "T": name = getApplication().getString(R.string.sensor_name_temp); break;
                    case "H": name = getApplication().getString(R.string.sensor_name_hum); break;
                    case "P": name = getApplication().getString(R.string.sensor_name_pres); break;
                    case "L": name = getApplication().getString(R.string.sensor_name_lux); break;
                    case "V": name = getApplication().getString(R.string.sensor_name_volt); break;
                    default: name = physicalId; break;
                }
            }
            sensors.add(new Sensor(id, name, unit, value, false, prefix, isPrimary, physicalId));
        }

        // zapisanie do bazy danych
        com.eia.app.db.SensorReading reading = new com.eia.app.db.SensorReading(id, value, timestamp);
        com.eia.app.db.AppDatabase.databaseWriteExecutor.execute(() -> {
            db.readingDao().insert(reading);
        });
    }

    private void cleanOldData() {
        // usuwanie danych starszych niż 24 godziny
        long threshold = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        com.eia.app.db.AppDatabase.databaseWriteExecutor.execute(() -> {
            db.readingDao().deleteOldReadings(threshold);
        });
    }

    public LiveData<List<com.eia.app.db.SensorReading>> getReadingsForSensor(String sensorId) {
        return db.readingDao().getReadingsForSensor(sensorId);
    }

    private void updateAllSensorsError(List<Sensor> sensors, String baseSensorId, boolean hasError) {
        boolean anyFound = false;
        for (Sensor s : sensors) {
            if (s.getPhysicalId() != null && s.getPhysicalId().equals(baseSensorId)) {
                s.setHasError(hasError);
                anyFound = true;
            }
        }
        if (!anyFound && hasError) {
            String errorName = getApplication().getString(R.string.sensor_error);
            sensors.add(new Sensor(baseSensorId, errorName, "---", 0, true, "", true, baseSensorId));
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

    public void requestHistorySync(String deviceId) {
        long currentTime = System.currentTimeMillis();
        Long lastSync = lastSyncTimes.get(deviceId);

        if (lastSync == null || (currentTime - lastSync) > 2 * 60 * 1000) {
            String commandTopic = deviceId + "/command";
            MqttRepository.getInstance().publishCommand(commandTopic, "GET_HISTORY");
            lastSyncTimes.put(deviceId, currentTime);
            Log.d(TAG, "Wysłano prośbę o historię dla: " + deviceId);
        } else {
            Log.d(TAG, "Synchronizacja zablokowana (throttle) dla: " + deviceId);
        }
    }

    public void initMqttConnection() {
        MqttRepository.getInstance().connectToBroker();
    }

}
