#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEScan.h>
#include <BLEAdvertisedDevice.h>
#include <Preferences.h>

Preferences preferences;

// WiFi mode (0 - Static, 1 - DHCP)
int mode;
const int WIFI_TIMEOUT_MS = 25000;

// MQTT zmienne
const char* mqtt_server = "";
const int mqtt_port = 8883;
const char* mqtt_user = "";
const char* mqtt_pass = "";

// Certyfikat ISRG Root X1
const char* root_ca = \
  "-----BEGIN CERTIFICATE-----\n" \
  "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" \
  "-----END CERTIFICATE-----\n";

WiFiClientSecure espClient;
PubSubClient mqttClient(espClient);

BLEScan* pBLEScan;

// Prototypy funkcji
void executeWifiScan();
void handleConnectionRequest(String cmd);
void handleStaticConnectionRequest(String cmd);
void mqttCallback(char* topic, byte* payload, unsigned int length);
bool connectToSavedWifi();
void reconnectMqtt();
void bleTask(void *pvParameters);
void mqttTask(void *pvParameters);

// Struktura wiadomości
struct Message {
    char sensorId[12]; 
    float floatValue;
};

QueueHandle_t valueQueue;

// Klasa callbacku BLE parsująca format "ID:A1B2C3;T:22.5"
class MyAdvertisedDeviceCallbacks: public BLEAdvertisedDeviceCallbacks {
  void onResult(BLEAdvertisedDevice advertisedDevice) {
    String deviceName = String(advertisedDevice.getName().c_str());

    if (deviceName.startsWith("ESP_")) {
      String data = advertisedDevice.getManufacturerData().c_str();

      if (data.startsWith("ID:")) {
        int semiColonIndex = data.indexOf(';');
        if (semiColonIndex != -1) {
          String idStr = data.substring(3, semiColonIndex); // Wyciąga "A1B2C3"
          int tIndex = data.indexOf("T:", semiColonIndex);
          
          if (tIndex != -1) {
            String tempStr = data.substring(tIndex + 2); // Wyciąga temperaturę
            float temp = tempStr.toFloat();

            Message msg;
            memset(msg.sensorId, 0, sizeof(msg.sensorId));
            strncpy(msg.sensorId, idStr.c_str(), sizeof(msg.sensorId) - 1);
            msg.floatValue = temp;

            if (xQueueSend(valueQueue, &msg, 0) != pdPASS) {
                Serial.println("QUEUE FULL");
            }

            Serial.print("BLE_RCV [Sensor: ");
            Serial.print(msg.sensorId);
            Serial.print("]: ");
            Serial.println(temp);
          }
        }
      }
    }
  }
};

void setup() {
  Serial.begin(115200);
  
  WiFi.mode(WIFI_STA);
  WiFi.disconnect();

  valueQueue = xQueueCreate(10, sizeof(Message));

  if (valueQueue == NULL) {
      Serial.println("QUEUE CREATE FAILED");
  }
  
  espClient.setCACert(root_ca);
  mqttClient.setServer(mqtt_server, mqtt_port);
  mqttClient.setCallback(mqttCallback);

  BLEDevice::init("");
  pBLEScan = BLEDevice::getScan();
  pBLEScan->setAdvertisedDeviceCallbacks(new MyAdvertisedDeviceCallbacks());
  pBLEScan->setActiveScan(true);
  pBLEScan->setInterval(100);
  pBLEScan->setWindow(99);

  delay(1000);

  Serial.println("STATUS:WIFI_INIT_AUTO_CONNECT...");
  if (!connectToSavedWifi()) {
    Serial.println("STATUS:NO_SAVED_WIFI_OR_CONNECT_FAIL");
  }

  Serial.println("STATUS:GATEWAY_READY");
  
  xTaskCreate(bleTask, "BLE_TASK", 6000, NULL, 1, NULL);
  xTaskCreate(mqttTask, "MQTT_TASK", 8000, NULL, 1, NULL);
}

void executeFactoryReset() {
  Serial.println("STATUS:CLEARING_PREFERENCES...");
  preferences.begin("wifi", false);
  preferences.clear(); 
  preferences.end();
  Serial.println("STATUS:PREFERENCES_CLEARED. REBOOTING IN 2 SECONDS...");
  delay(2000);
  ESP.restart();
}

void loop() {
  if (Serial.available() > 0) {
    String input = Serial.readStringUntil('\n');
    input.trim();

    if (input.equalsIgnoreCase("SCAN")) {
      executeWifiScan();
    }
    else if (input.startsWith("CONN:")) {
      handleConnectionRequest(input);
    }
    else if (input.startsWith("CONN_STATIC:")) {
      handleStaticConnectionRequest(input);
    }
    else if (input.startsWith("RESET")) {
      executeFactoryReset();
    }
  }
}

//-- Tasks --
void bleTask(void *pvParameters) {
    while (true) {
        pBLEScan->start(1, false);
        pBLEScan->clearResults();
        vTaskDelay(pdMS_TO_TICKS(5000));
    }
}

void mqttTask(void *pvParameters) {
    Message msg;
    static unsigned long lastReconnectAttempt = 0;
    const unsigned long reconnectInterval = 5000; 

    String mac = WiFi.macAddress();
    mac.replace(":", "");
    String gateId = "gate_" + mac;

    while (true) {
        if (WiFi.status() == WL_CONNECTED) {
            if (!mqttClient.connected()) {
                unsigned long now = millis();
                if (now - lastReconnectAttempt > reconnectInterval) {
                    lastReconnectAttempt = now;
                    reconnectMqtt();
                }
            } else {
                mqttClient.loop();

                while (xQueueReceive(valueQueue, &msg, 0) == pdTRUE) {
                    char topic[64];
                    char payload[16];
                    
                    // Format tematu: gate_24DCF2A1B2C3/A1B2C3
                    snprintf(topic, sizeof(topic), "%s/%s", gateId.c_str(), msg.sensorId);
                    snprintf(payload, sizeof(payload), "%.1f", msg.floatValue);
                    
                    mqttClient.publish(topic, payload);
                    Serial.printf("MQTT_PUB: [%s] -> %s\n", topic, payload);
                }
            }
        }
        vTaskDelay(pdMS_TO_TICKS(10));
    }
}

//-- Functions --
void executeWifiScan() {
  int n = WiFi.scanNetworks(false, false);
  if (n == 0) {
    Serial.println("STATUS:BRAK_SIECI");
  } else {
    for (int i = 0; i < n; ++i) {
      String ssid = WiFi.SSID(i);
      int32_t rssi = WiFi.RSSI(i);
      bool isProtected = (WiFi.encryptionType(i) != WIFI_AUTH_OPEN);
      String protectedStr = isProtected ? "1" : "0";
      Serial.print(ssid);
      Serial.print(",");
      Serial.print(rssi);
      Serial.print(",");
      Serial.println(protectedStr);
      delay(10);
    }
  }
  Serial.println("SCAN_FINISHED");
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
    String mac = WiFi.macAddress();
    mac.replace(":", "");
    Serial.print("STATUS:OK;ID:");
    Serial.println("gate_" + mac);
    
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
    String mac = WiFi.macAddress();
    mac.replace(":", "");
    Serial.print("STATUS:OK;ID:");
    Serial.println("gate_" + mac);
    
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

void reconnectMqtt() {
  if (WiFi.status() == WL_CONNECTED && !mqttClient.connected()) {
    Serial.println("STATUS:MQTT_RECONNECTING...");
    
    String mac = WiFi.macAddress();
    mac.replace(":", "");
    String gateId = "gate_" + mac;
    
    String statusTopicStr = gateId + "/status";
    const char* payloadOnline = "online";
    const char* payloadOffline = "offline";
    
    if (mqttClient.connect(gateId.c_str(), mqtt_user, mqtt_pass, statusTopicStr.c_str(), 1, true, payloadOffline)) {
      Serial.println("STATUS:MQTT_OK_RECONNECTED");
      mqttClient.publish(statusTopicStr.c_str(), payloadOnline, true);
    } else {
      Serial.print("STATUS:MQTT_RECONNECT_FAILED, RC=");
      Serial.println(mqttClient.state());
    }
  }
}

void mqttCallback(char* topic, byte* payload, unsigned int length) {
  Serial.print("MQTT_RCV:[");
  Serial.print(topic);
  Serial.print("]:");
  for (unsigned int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
  }
  Serial.println();
}

bool connectToSavedWifi() {
  preferences.begin("wifi", false);
  mode = preferences.getInt("mode", -1);

  if (mode == -1) {
    Serial.println("STATUS:NO_SAVED_WIFI");
    preferences.end();
    return false;
  }

  String ssid = preferences.getString("last_ssid", "");
  String password = preferences.getString("last_password", "");

  if (ssid == "") {
    Serial.println("STATUS:NO_SAVED_WIFI");
    preferences.end();
    return false;
  }

  if (mode == 0) {
    String ip = preferences.getString("last_ip", "");
    String gateway = preferences.getString("last_gateway", "");
    String subnet = preferences.getString("last_subnet", "");

    if (ip == "" || gateway == "" || subnet == "") {
      Serial.println("STATUS:INCOMPLETE_STATIC_CONFIG");
      preferences.end();
      return false;
    }

    IPAddress local_IP, local_gateway, local_subnet;
    IPAddress dns(8, 8, 8, 8);
    
    if (!local_IP.fromString(ip) || !local_gateway.fromString(gateway) || !local_subnet.fromString(subnet)) {
      Serial.println("STATUS:ERROR_IP_PARSING");
      preferences.end();
      return false;
    }

    WiFi.config(local_IP, local_gateway, local_subnet, dns);
  }

  WiFi.begin(ssid.c_str(), password.c_str());
  unsigned long startAttemptTime = millis();

  while (WiFi.status() != WL_CONNECTED && millis() - startAttemptTime < WIFI_TIMEOUT_MS) {
    delay(500);
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("STATUS:WIFI_AUTO_CONNECTED;IP:");
    Serial.println(WiFi.localIP().toString());
    preferences.end();
    return true;
  } else {
    Serial.println("STATUS:WIFI_AUTO_CONNECT_FAILED");
    WiFi.disconnect();
    preferences.end();
    return false;
  }
}