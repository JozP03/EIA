#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEScan.h>
#include <BLEAdvertisedDevice.h>
#include <Preferences.h>

Preferences preferences;

// --- KONFIGURACJA ---
const int WIFI_TIMEOUT_MS = 25000;
String mqtt_server = "";
int mqtt_port = 8883;
String mqtt_user = "";
String mqtt_pass = "";
 
// Certyfikat ISRG Root X1
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
};
#define MAX_SENSORS 10
SensorStatus sensorRegistry[MAX_SENSORS];

struct Message {
    char sensorId[12]; 
    char payloadData[32];
};

// --- PROTOTYPY ---
void updateSensorStatus(const char* id);
void checkOfflineSensors();
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

// --- CALLBACK BLE ---
class MyAdvertisedDeviceCallbacks: public BLEAdvertisedDeviceCallbacks {
  void onResult(BLEAdvertisedDevice advertisedDevice) {
    if (advertisedDevice.haveManufacturerData()) {
      std::string strManufacturerData = advertisedDevice.getManufacturerData();
      String data = String(strManufacturerData.c_str());

      // Oczekujemy np. "ID:ESP_A1B2C3;T:22.5;U:60"
      if (data.startsWith("ESP_")) {
        int semiColonIndex = data.indexOf(';');
        if (semiColonIndex != -1) {
          String idStr = data.substring(0, semiColonIndex);
          
          // Wszystko po pierwszym sredniku
          String restOfData = data.substring(semiColonIndex + 1);

          Message msg;
          memset(&msg, 0, sizeof(Message));
          
          strncpy(msg.sensorId, idStr.c_str(), sizeof(msg.sensorId) - 1);
          strncpy(msg.payloadData, restOfData.c_str(), sizeof(msg.payloadData) - 1);

          updateSensorStatus(msg.sensorId);
          xQueueSend(valueQueue, &msg, 0);
        }
      }
    }
  }
};

void setup() {
  Serial.begin(115200);
  Serial.setTxTimeoutMs(0);
  WiFi.mode(WIFI_STA);
  WiFi.disconnect();

  valueQueue = xQueueCreate(10, sizeof(Message));
  espClient.setCACert(root_ca);
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
  mqttClient.setCallback(mqttCallback);
  
  connectToSavedWifi();

  xTaskCreate(bleTask, "BLE_TASK", 6000, NULL, 1, NULL);
  xTaskCreate(mqttTask, "MQTT_TASK", 8000, NULL, 1, NULL);
}

void loop() {
  if (Serial.available() > 0) {
    String input = Serial.readStringUntil('\n');
    input.trim();
    if (input.equalsIgnoreCase("SCAN")) executeWifiScan();
    else if (input.startsWith("CONN:")) handleConnectionRequest(input);
    else if (input.startsWith("CONN_STATIC:")) handleStaticConnectionRequest(input);
    else if (input.equalsIgnoreCase("RESET")) {
        preferences.begin("wifi", false); preferences.clear(); preferences.end(); ESP.restart();
    }
    else if (input.startsWith("MQTT:")) handleMqttConfig(input);
  }
  delay(100);
}

// --- TASKS ---
void bleTask(void *pvParameters) {
    while (true) {
        pBLEScan->start(3, true);
        pBLEScan->clearResults(); 
        vTaskDelay(pdMS_TO_TICKS(500));
    }
}

void mqttTask(void *pvParameters) {
    Message msg;
    static unsigned long lastReconnectAttempt = 0;
    static unsigned long lastCheckTime = 0;

    while (true) {
        if (WiFi.status() == WL_CONNECTED && mqtt_server.length() > 0) {
            if (!mqttClient.connected()) {
                if (millis() - lastReconnectAttempt > 5000) {
                    lastReconnectAttempt = millis();
                    reconnectMqtt();
                }
            } else {
                mqttClient.loop();
                if (millis() - lastCheckTime > 60000) {
                    checkOfflineSensors();
                    lastCheckTime = millis();
                }
                while (xQueueReceive(valueQueue, &msg, 0) == pdTRUE) {
                    char topic[64];
                    snprintf(topic, sizeof(topic), "%s/%s", gateId.c_str(), msg.sensorId);
                    mqttClient.publish(topic, msg.payloadData);
                    Serial.printf("MQTT_PUB: [%s] -> %s\n", topic, msg.payloadData);
                }
            }
        }
        vTaskDelay(pdMS_TO_TICKS(10));
    }
}

// --- FUNKCJE LOGIKI ---
void updateSensorStatus(const char* id) {
    bool found = false;
    for (int i = 0; i < MAX_SENSORS; i++) {
        if (strcmp(sensorRegistry[i].id, id) == 0) {
            if (!sensorRegistry[i].isOnline) {
                sensorRegistry[i].isOnline = true;
                char topic[64];
                snprintf(topic, sizeof(topic), "%s/%s/status", gateId.c_str(), id);
                mqttClient.publish(topic, "online", true);
            }
            sensorRegistry[i].lastSeen = millis();
            found = true;
            break;
        }
    }
    if (!found) {
        for (int i = 0; i < MAX_SENSORS; i++) {
            if (sensorRegistry[i].id[0] == '\0') {
                strncpy(sensorRegistry[i].id, id, 15);
                sensorRegistry[i].lastSeen = millis();
                sensorRegistry[i].isOnline = true;
                break;
            }
        }
    }
}

void checkOfflineSensors() {
    unsigned long now = millis();
    const unsigned long TIMEOUT = 60000; // 1 minuta

    for (int i = 0; i < MAX_SENSORS; i++) {
        if (sensorRegistry[i].id[0] != '\0' && sensorRegistry[i].isOnline) {
            if (now - sensorRegistry[i].lastSeen > TIMEOUT) {
                sensorRegistry[i].isOnline = false;
                char topic[64];
                snprintf(topic, sizeof(topic), "%s/%s/status", gateId.c_str(), sensorRegistry[i].id);
                mqttClient.publish(topic, "offline", true);
            }
        }
    }
}

void reconnectMqtt() {
    String statusTopicStr = gateId + "/status";
    if (mqttClient.connect(gateId.c_str(), mqtt_user.c_str(), mqtt_pass.c_str(), statusTopicStr.c_str(), 1, true, "offline")) {
        mqttClient.publish(statusTopicStr.c_str(), "online", true);
    }
}

void mqttCallback(char* topic, byte* payload, unsigned int length) {
  // Obsługa wiadomości przychodzących z MQTT
}

// --- FUNKCJE WIFI
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
    Serial.println("STATUS:ERROR_FORMAT");
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
    Serial.print("STATUS:OK;ID:");
    Serial.println(gateId);
    
    delay(10);
    preferences.putInt("mode", 1);
    preferences.putString("last_ssid", ssid);
    preferences.putString("last_password", password);
    preferences.end();
    delay(1000);
  } else {
    Serial.println("STATUS:ERROR_TIMEOUT");
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
    Serial.println("STATUS:ERROR_FORMAT");
    return;
  }

  String ssid = parts[0];
  String password = parts[1];
  IPAddress local_IP, gateway, subnet;
  
  if (!local_IP.fromString(parts[2]) || !gateway.fromString(parts[3]) || !subnet.fromString(parts[4])) {
    Serial.println("STATUS:ERROR_IP_PARSING");
    return;
  }

  IPAddress dns(8, 8, 8, 8);
  if (!WiFi.config(local_IP, gateway, subnet, dns)) {
    Serial.println("STATUS:ERROR_CONFIG_FAILED");
    return;
  }

  WiFi.begin(ssid.c_str(), password.c_str());
  unsigned long startAttemptTime = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - startAttemptTime < WIFI_TIMEOUT_MS) {
    delay(500);
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("STATUS:OK;ID:");
    Serial.println(gateId);
    
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
    Serial.println("STATUS:ERROR_TIMEOUT");
    WiFi.disconnect();
  }
}

void handleMqttConfig(String cmd) {
  // Oczekiwany formata: "MQTT:server;port;user;pass"
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
  
  Serial.println("STATUS:MQTT_CONFIG_SAVED");  
}

void loadMqttConfig() {
  preferences.begin("mqtt", true);
  mqtt_server = preferences.getString("server", "");
  mqtt_port = preferences.getInt("port", 8883);
  mqtt_user = preferences.getString("user", "");
  mqtt_pass = preferences.getString("pass", "");
  preferences.end();
}