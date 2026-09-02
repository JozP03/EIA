#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEScan.h>
#include <BLEAdvertisedDevice.h>
#include <Preferences.h>
#include <time.h> 
#include <LittleFS.h> 

Preferences preferences;

// --- KONFIGURACJA ---
const int WIFI_TIMEOUT_MS = 25000;
String mqtt_server = "";
int mqtt_port = 8883;
String mqtt_user = "";
String mqtt_pass = "";
 
// --- CERTYFIKAT ---
const char* root_ca = \
  "-----BEGIN CERTIFICATE-----\n" \
  "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" \
  "-----END CERTIFICATE-----\n";

// --- GLOBALNE ---
String gateId;
WiFiClientSecure espClient;
PubSubClient mqttClient(espClient);
BLEScan* pBLEScan;
QueueHandle_t valueQueue;

struct SensorStatus {
    char id[16];
    unsigned long lastSeen;
    bool isOnline;
    char lastPayload[32];
};
#define MAX_SENSORS 10
SensorStatus sensorRegistry[MAX_SENSORS];

struct Message {
    char sensorId[12]; 
    char payloadData[32];
};

struct HistoryRecord {
    uint32_t timestamp;  
    char sensorId[12];   
    char payloadData[32];
};

#define MAX_HISTORY_RECORDS 250 
HistoryRecord historyBuffer[MAX_HISTORY_RECORDS];
int historyIndex = 0;
bool historyWrapped = false; 
bool triggerHistorySend = false;
bool pendingBleConfig = false;
char configTargetId[16] = {0};
char configPayloadRaw[18] = {0};

// --- PROTOTYPY ---
void reconnectMqtt();
void mqttCallback(char* topic, byte* payload, unsigned int length);
bool connectToSavedWifi();
void executeWifiScan();
void handleConnectionRequest(String cmd);
void handleStaticConnectionRequest(String cmd);
void handleMqttConfig(String cmd);
void bleTask(void *pvParameters);
void mqttTask(void *pvParameters);
void loadMqttConfig();
void loadHistoryFromFlash();
void saveHistoryToFlash();

// --- LITTLEFS ---
void loadHistoryFromFlash() {
    if (!LittleFS.begin(true)) {
        if (Serial) Serial.println("Błąd montowania LittleFS!");
        return;
    }
    
    File file = LittleFS.open("/history.bin", FILE_READ);
    if (!file || file.size() == 0) {
        if (Serial) Serial.println("Brak zapisanej historii");
        return;
    }
    
    file.read((uint8_t*)historyBuffer, sizeof(historyBuffer));
    file.read((uint8_t*)&historyIndex, sizeof(historyIndex));
    file.read((uint8_t*)&historyWrapped, sizeof(historyWrapped));
    file.close();
    
    if (Serial) Serial.println("wczytano historie.");
}

void saveHistoryToFlash() {
    File file = LittleFS.open("/history.bin", FILE_WRITE);
    if (!file) {
        if (Serial) Serial.println("Błąd zapisu historii");
        return;
    }
    file.write((uint8_t*)historyBuffer, sizeof(historyBuffer));
    file.write((uint8_t*)&historyIndex, sizeof(historyIndex));
    file.write((uint8_t*)&historyWrapped, sizeof(historyWrapped));
    file.close();
}

// --- CALLBACK BLE---
class MyAdvertisedDeviceCallbacks: public BLEAdvertisedDeviceCallbacks {
  void onResult(BLEAdvertisedDevice advertisedDevice) {
    if (advertisedDevice.haveManufacturerData()) {
      std::string strManufacturerData = advertisedDevice.getManufacturerData();
      
      const char* dataPtr = strManufacturerData.c_str();

      if (strncmp(dataPtr, "ESP_", 4) == 0) { 
        const char* semiColonIndex = strchr(dataPtr, ';');
        
        if (semiColonIndex != nullptr) {
          Message msg;
          memset(&msg, 0, sizeof(Message));
          
          int idLen = semiColonIndex - dataPtr;
          if (idLen > 11) idLen = 11;
          strncpy(msg.sensorId, dataPtr, idLen);
          
          strncpy(msg.payloadData, semiColonIndex + 1, sizeof(msg.payloadData) - 1);

          bool isNewData = false;
          bool found = false;

          for (int i = 0; i < MAX_SENSORS; i++) {
              if (strcmp(sensorRegistry[i].id, msg.sensorId) == 0) {
                  found = true;
                  sensorRegistry[i].lastSeen = millis(); 
                  
                  if (strcmp(sensorRegistry[i].lastPayload, msg.payloadData) != 0) {
                      snprintf(sensorRegistry[i].lastPayload, sizeof(sensorRegistry[i].lastPayload), "%s", msg.payloadData);
                      isNewData = true;
                  }
                  break;
              }
          }

          if (!found) {
              for (int i = 0; i < MAX_SENSORS; i++) {
                  if (sensorRegistry[i].id[0] == '\0') {
                      snprintf(sensorRegistry[i].id, sizeof(sensorRegistry[i].id), "%s", msg.sensorId);
                      snprintf(sensorRegistry[i].lastPayload, sizeof(sensorRegistry[i].lastPayload), "%s", msg.payloadData);
                      sensorRegistry[i].lastSeen = millis();
                      isNewData = true;
                      break;
                  }
              }
          }

          if (isNewData) {
            xQueueSend(valueQueue, &msg, 0);
          }
        }
      }
    }
  }
};

void setup() {
  Serial.begin(115200);
  Serial.setTxTimeoutMs(0);
  
  loadHistoryFromFlash();
  
  WiFi.mode(WIFI_STA);
  WiFi.setAutoReconnect(true); 
  WiFi.disconnect();

  valueQueue = xQueueCreate(15, sizeof(Message));
  espClient.setCACert(root_ca);
  
  mqttClient.setBufferSize(512); 
  mqttClient.setKeepAlive(60); 
  
  mqttClient.setServer(mqtt_server.c_str(), mqtt_port);
  mqttClient.setCallback(mqttCallback);

  BLEDevice::init("");
  pBLEScan = BLEDevice::getScan();
  pBLEScan->setAdvertisedDeviceCallbacks(new MyAdvertisedDeviceCallbacks());
  pBLEScan->setActiveScan(true);
  pBLEScan->setInterval(1349);
  pBLEScan->setWindow(449);

  delay(1000);
  gateId = "gate_" + WiFi.macAddress();
  gateId.replace(":", "");

  loadMqttConfig();
  if (mqtt_server.length() > 0) {
      mqttClient.setServer(mqtt_server.c_str(), mqtt_port);
  }
  
  if (connectToSavedWifi()) {
      configTzTime("CET-1CEST,M3.5.0,M10.5.0/3", "pool.ntp.org", "time.nist.gov");
  }

  xTaskCreate(bleTask, "BLE_TASK", 6000, NULL, 1, NULL);
  xTaskCreate(mqttTask, "MQTT_TASK", 10240, NULL, 1, NULL);
}

void loop() {
  if (Serial.available() > 0) {
    String input = Serial.readStringUntil('\n');
    input.trim();
    if (input.equalsIgnoreCase("SCAN")) executeWifiScan();
    else if (input.startsWith("CONN:")) handleConnectionRequest(input);
    else if (input.startsWith("CONN_STATIC:")) handleStaticConnectionRequest(input);
    else if (input.equalsIgnoreCase("CLEAR_HISTORY")) {
        memset(historyBuffer, 0, sizeof(historyBuffer));
        historyIndex = 0;
        historyWrapped = false;
        LittleFS.remove("/history.bin");
        if (Serial) Serial.println("STATUS:HISTORY_CLEARED");
    }
    else if (input.equalsIgnoreCase("RESET")) {
        preferences.begin("wifi", false); preferences.clear(); preferences.end(); 
        LittleFS.remove("/history.bin"); 
        ESP.restart();
    }
    else if (input.startsWith("MQTT:")) handleMqttConfig(input);
  }

  static unsigned long offlineTime = 0;
  if (WiFi.status() != WL_CONNECTED) {
    if (offlineTime == 0) offlineTime = millis();
    
    if (millis() - offlineTime > 180000) {
        if (Serial) Serial.println("Brak Wi-Fi przez 3 min. Twardy restart...");
        ESP.restart(); 
    }
  } else {
    offlineTime = 0;
  }
  
  delay(100);
}

// --- TASKS ---
void bleTask(void *pvParameters) {
    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    
    while (true) {
        if (pendingBleConfig) {
            pBLEScan->stop();
            
            BLEAdvertisementData oAdvertisementData = BLEAdvertisementData();
            std::string strServiceData = "";
            
            char payload[32];
            snprintf(payload, sizeof(payload), "%s;%s", configTargetId, configPayloadRaw);
            strServiceData += payload;
            oAdvertisementData.setManufacturerData(strServiceData);
            
            pAdvertising->setAdvertisementData(oAdvertisementData);
            pAdvertising->start();
            
            if (Serial) Serial.printf("Rozglaszam: %s\n", payload);
            
            vTaskDelay(pdMS_TO_TICKS(5000));
            
            pAdvertising->stop();
            pendingBleConfig = false;
            if (Serial) Serial.println("Koniec nadawania.");
            
        } else {
            pBLEScan->start(3, true);
            pBLEScan->clearResults(); 
            vTaskDelay(pdMS_TO_TICKS(100));
        }
    }
}

void mqttTask(void *pvParameters) {
    Message msg;
    static unsigned long lastReconnectAttempt = 0;
    static unsigned long mqttOfflineTime = 0;

    while (true) {
        if (WiFi.status() == WL_CONNECTED && mqtt_server.length() > 0) {
            if (!mqttClient.connected()) {
                
                if (mqttOfflineTime == 0) mqttOfflineTime = millis();
                if (millis() - mqttOfflineTime > 180000) {
                    if (Serial) Serial.println("Restart");
                    ESP.restart();
                }
                
                if (millis() - lastReconnectAttempt > 5000) {
                    lastReconnectAttempt = millis();
                    reconnectMqtt();
                }
            } else {
                mqttOfflineTime = 0;
                mqttClient.loop();
                
                unsigned long now_ms = millis();
                for (int i = 0; i < MAX_SENSORS; i++) {
                    if (sensorRegistry[i].id[0] != '\0') {
                        bool isRecentlySeen = (now_ms - sensorRegistry[i].lastSeen < 360000); 
                        
                        if (isRecentlySeen && !sensorRegistry[i].isOnline) {
                            sensorRegistry[i].isOnline = true;
                            char topic[64];
                            snprintf(topic, sizeof(topic), "%s/%s/status", gateId.c_str(), sensorRegistry[i].id);
                            mqttClient.publish(topic, "online", true);
                        } else if (!isRecentlySeen && sensorRegistry[i].isOnline) {
                            sensorRegistry[i].isOnline = false;
                            char topic[64];
                            snprintf(topic, sizeof(topic), "%s/%s/status", gateId.c_str(), sensorRegistry[i].id);
                            mqttClient.publish(topic, "offline", true);
                        }
                    }
                }

                if (triggerHistorySend) {
                    triggerHistorySend = false;
                    int count = historyWrapped ? MAX_HISTORY_RECORDS : historyIndex;
                    int startIdx = historyWrapped ? historyIndex : 0;
                    
                    if (Serial) Serial.println("Wysyłam historię...");
                    
                    for (int i = 0; i < count; i++) {
                        int idx = (startIdx + i) % MAX_HISTORY_RECORDS;
                        char histTopic[64];
                        snprintf(histTopic, sizeof(histTopic), "%s/history", gateId.c_str());
                        
                        char histPayload[128];
                        snprintf(histPayload, sizeof(histPayload), "%lu;%s;%s", historyBuffer[idx].timestamp, historyBuffer[idx].sensorId, historyBuffer[idx].payloadData);
                        mqttClient.publish(histTopic, histPayload);
                        vTaskDelay(pdMS_TO_TICKS(10)); 
                    }
                    
                    char eofTopic[64];
                    snprintf(eofTopic, sizeof(eofTopic), "%s/history", gateId.c_str());
                    mqttClient.publish(eofTopic, "EOF"); 
                    if (Serial) Serial.println("Zakończono wysyłanie historii.");
                }

                while (xQueueReceive(valueQueue, &msg, 0) == pdTRUE) {
                    
                    time_t nowTime;
                    time(&nowTime);
                    
                    if (nowTime > 1600000000) {
                        historyBuffer[historyIndex].timestamp = (uint32_t)nowTime;
                        snprintf(historyBuffer[historyIndex].sensorId, sizeof(historyBuffer[historyIndex].sensorId), "%s", msg.sensorId);
                        snprintf(historyBuffer[historyIndex].payloadData, sizeof(historyBuffer[historyIndex].payloadData), "%s", msg.payloadData);
                        
                        historyIndex++;
                        if (historyIndex >= MAX_HISTORY_RECORDS) {
                            historyIndex = 0;
                            historyWrapped = true;
                        }
                        saveHistoryToFlash(); 
                    }

                    char topic[64];
                    snprintf(topic, sizeof(topic), "%s/%s", gateId.c_str(), msg.sensorId);
                    mqttClient.publish(topic, msg.payloadData);
                    
                    if (Serial) { 
                        Serial.printf("MQTT_PUB: [%s] -> %s\n", topic, msg.payloadData);
                    }
                }
            }
        }
        vTaskDelay(pdMS_TO_TICKS(10));
    }
}

void reconnectMqtt() {
    String statusTopicStr = gateId + "/status";
    if (mqttClient.connect(gateId.c_str(), mqtt_user.c_str(), mqtt_pass.c_str(), statusTopicStr.c_str(), 1, true, "offline")) {
        mqttClient.publish(statusTopicStr.c_str(), "online", true);
        
        String cmdTopic = gateId + "/command";
        mqttClient.subscribe(cmdTopic.c_str());

        String configTopic = gateId + "/config";
        mqttClient.subscribe(configTopic.c_str());

        String sensorConfigTopic = gateId + "/+/config";
        mqttClient.subscribe(sensorConfigTopic.c_str());
    }
}

void mqttCallback(char* topic, byte* payload, unsigned int length) {
    String topicStr = String(topic);
    String payloadStr = "";
    for (int i = 0; i < length; i++) {
        payloadStr += (char)payload[i];
    }

    if (Serial) {
        Serial.printf("MQTT_REC: [%s] -> %s\n", topicStr.c_str(), payloadStr.c_str());
    }

    if (topicStr.endsWith("/command") && payloadStr.equalsIgnoreCase("GET_HISTORY")) {
        triggerHistorySend = true;
    }

    else if (topicStr.startsWith(gateId + "/") && topicStr.endsWith("/config") && !topicStr.equals(gateId + "/config")) {
        
        int firstSlash = topicStr.indexOf('/');
        int lastSlash = topicStr.lastIndexOf('/');
        String targetId = topicStr.substring(firstSlash + 1, lastSlash);

        strncpy(configTargetId, targetId.c_str(), sizeof(configTargetId) - 1);
        strncpy(configPayloadRaw, payloadStr.c_str(), sizeof(configPayloadRaw) - 1);
        configPayloadRaw[sizeof(configPayloadRaw) - 1] = '\0';
        
        pendingBleConfig = true;
        
        if (Serial) Serial.printf("przeslanie surowej wiadomosci do %s: %s\n", configTargetId, configPayloadRaw);
    }
}

// --- FUNKCJE WIFI ---
bool connectToSavedWifi() {
  preferences.begin("wifi", false);
  int mode = preferences.getInt("mode", -1);
  if (mode == -1) { preferences.end(); return false; }
  String ssid = preferences.getString("last_ssid", "");
  String pass = preferences.getString("last_password", "");
  
  if (mode == 0) {
      IPAddress ip, gw, sub;
      ip.fromString(preferences.getString("last_ip", ""));
      gw.fromString(preferences.getString("last_gateway", ""));
      sub.fromString(preferences.getString("last_subnet", ""));
      WiFi.config(ip, gw, sub, IPAddress(8,8,8,8));
  }
  WiFi.begin(ssid.c_str(), pass.c_str());
  preferences.end();
  return (WiFi.waitForConnectResult() == WL_CONNECTED);
}

void executeWifiScan() {
    int n = WiFi.scanNetworks();
    for (int i = 0; i < n; ++i) Serial.printf("%s,%d,%d\n", WiFi.SSID(i).c_str(), WiFi.RSSI(i), (WiFi.encryptionType(i) != WIFI_AUTH_OPEN));
    WiFi.scanDelete();
}

void handleConnectionRequest(String cmd) {
  preferences.begin("wifi", false);
  int separatorIndex = cmd.indexOf(';');
  if (separatorIndex == -1) {
    if (Serial) Serial.println("STATUS:ERROR_FORMAT");
    preferences.end();
    return;
  }
  String ssid = cmd.substring(5, separatorIndex);
  String password = cmd.substring(separatorIndex + 1);
  WiFi.begin(ssid.c_str(), password.c_str());
  unsigned long startAttemptTime = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - startAttemptTime < WIFI_TIMEOUT_MS) {
    delay(500);
  }
  if (WiFi.status() == WL_CONNECTED) {
    if (Serial) {
        Serial.print("STATUS:OK;ID:");
        Serial.println(gateId);
    }
    delay(10);
    preferences.putInt("mode", 1);
    preferences.putString("last_ssid", ssid);
    preferences.putString("last_password", password);
    preferences.end();
    delay(1000);
  } else {
    if (Serial) Serial.println("STATUS:ERROR_TIMEOUT");
    WiFi.disconnect();
  }
}

void handleStaticConnectionRequest(String cmd) {
  preferences.begin("wifi", false);
  String data = cmd.substring(12); 
  String parts[5];
  int partCount = 0;
  
  while (data.length() > 0 && partCount < 5) {
    int idx = data.indexOf(';');
    if (idx == -1) {
      parts[partCount++] = data;
      break;
    } else {
      parts[partCount++] = data.substring(0, idx);
      data = data.substring(idx + 1);
    }
  }

  if (partCount < 5) {
    if (Serial) Serial.println("STATUS:ERROR_FORMAT");
    return;
  }
  String ssid = parts[0];
  String password = parts[1];
  IPAddress local_IP, gateway, subnet;
  
  if (!local_IP.fromString(parts[2]) || !gateway.fromString(parts[3]) || !subnet.fromString(parts[4])) {
    if (Serial) Serial.println("STATUS:ERROR_IP_PARSING");
    return;
  }
  IPAddress dns(8, 8, 8, 8);
  IPAddress dns2(8, 8, 4, 4);
  if (!WiFi.config(local_IP, gateway, subnet, dns, dns2)) {
    if (Serial) Serial.println("STATUS:ERROR_CONFIG_FAILED");
    return;
  }
  WiFi.begin(ssid.c_str(), password.c_str());
  unsigned long startAttemptTime = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - startAttemptTime < WIFI_TIMEOUT_MS) {
    delay(500);
  }
  if (WiFi.status() == WL_CONNECTED) {
    if (Serial) {
        Serial.print("STATUS:OK;ID:");
        Serial.println(gateId);
    }
    delay(10);
    preferences.putInt("mode", 0);
    preferences.putString("last_ssid", ssid);
    preferences.putString("last_password", password);
    preferences.putString("last_ip", local_IP.toString());
    preferences.putString("last_gateway", gateway.toString());
    preferences.putString("last_subnet", subnet.toString());
    preferences.end();
    delay(1000);
  } else {
    if (Serial) Serial.println("STATUS:ERROR_TIMEOUT");
    WiFi.disconnect();
  }
}

void handleMqttConfig(String cmd) {
  String data = cmd.substring(5);
  int s1 = data.indexOf(';');
  int s2 = data.indexOf(';', s1 + 1);
  int s3 = data.indexOf(';', s2 + 1);
  mqtt_server = data.substring(0, s1);
  mqtt_port = data.substring(s1 + 1, s2).toInt();
  mqtt_user = data.substring(s2 + 1, s3);
  mqtt_pass = data.substring(s3 + 1);
  preferences.begin("mqtt", false);
  preferences.putString("server", mqtt_server);
  preferences.putInt("port", mqtt_port);
  preferences.putString("user", mqtt_user);
  preferences.putString("pass", mqtt_pass);
  preferences.end();
  mqttClient.setServer(mqtt_server.c_str(), mqtt_port);
  mqttClient.disconnect();
  if (Serial) Serial.println("STATUS:MQTT_CONFIG_SAVED");  
}

void loadMqttConfig() {
  preferences.begin("mqtt", true);
  mqtt_server = preferences.getString("server", "");
  mqtt_port = preferences.getInt("port", 8883);
  mqtt_user = preferences.getString("user", "");
  mqtt_pass = preferences.getString("pass", "");
  preferences.end();
}