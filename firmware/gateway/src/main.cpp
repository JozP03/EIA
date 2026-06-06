#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEScan.h>
#include <BLEAdvertisedDevice.h>
#include <Preferences.h>

Preferences preferences;

//zmienna określajaca tryb wifi
// 0 - połączenie statyczne (Static)
// 1 - połączenie dynamiczne (DHCP)
// WiFi mode
int mode;
const int WIFI_TIMEOUT_MS = 25000;

// ===== SENSOR DATA =====
struct SensorData {
  float temp;
  bool available;
};

// mutex (CRITICAL SECTION)
portMUX_TYPE tempMux = portMUX_INITIALIZER_UNLOCKED;

// global data
SensorData sensorData = {0.0, false};

//mqtt zmienne
const char* mqtt_server = "";
const int mqtt_port = 8883;
const char* mqtt_user = "";
const char* mqtt_pass = "";

//certyfikat ISRG Root X1 https://letsencrypt.org/certificates/
const char* root_ca = \
  "-----BEGIN CERTIFICATE-----\n" \
  "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" \
  "-----END CERTIFICATE-----\n";

WiFiClientSecure espClient;
PubSubClient mqttClient(espClient); // definicja mqtt

BLEScan* pBLEScan; //definicja BLE
unsigned long lastBleScanTime = 0;
const unsigned long BLE_SCAN_INTERVAL = 5000;

volatile bool newTemperatureAvailable = false; // Flaga informująca o nowych danych
float latestTemperature = 0.0;

void executeWifiScan();
void handleConnectionRequest(String cmd);
void handleStaticConnectionRequest(String cmd);
void testMqttConnection();
void mqttCallback(char* topic, byte* payload, unsigned int length);
bool connectToSavedWifi();

// Klasa callbacku wywoływana, gdy radio BLE coś znajdzie w powietrzu
class MyAdvertisedDeviceCallbacks: public BLEAdvertisedDeviceCallbacks {
  void onResult(BLEAdvertisedDevice advertisedDevice) {

    if (advertisedDevice.getName() == "ESP_C3_TEMP") {

      String data = advertisedDevice.getManufacturerData().c_str();

      if (data.startsWith("T:")) {
        String tempStr = data.substring(2);
        float temp = tempStr.toFloat();

        portENTER_CRITICAL(&tempMux);

        sensorData.temp = temp;
        sensorData.available = true;

        portEXIT_CRITICAL(&tempMux);

        Serial.print("BLE_RCV: ");
        Serial.println(temp);
      }
    }
  }
};

void setup() {
  Serial.begin(115200);
  
  WiFi.mode(WIFI_STA);
  WiFi.disconnect();
  
  espClient.setCACert(root_ca);
  mqttClient.setServer(mqtt_server, mqtt_port);
  mqttClient.setCallback(mqttCallback);

  // Inicjalizacja BLE w trybie skanera
  BLEDevice::init("");
  pBLEScan = BLEDevice::getScan();
  pBLEScan->setAdvertisedDeviceCallbacks(new MyAdvertisedDeviceCallbacks());
  pBLEScan->setActiveScan(true);
  pBLEScan->setInterval(100);
  pBLEScan->setWindow(99);

  delay(1000);

  // Próba automatycznego połączenia z zapisaną siecią
  Serial.println("STATUS:WIFI_INIT_AUTO_CONNECT...");
  if (connectToSavedWifi()) {
    testMqttConnection();
  } else {
    Serial.println("STATUS:NO_SAVED_WIFI_OR_CONNECT_FAIL");
  }

  Serial.println("STATUS:GATEWAY_READY");
}

void reconnectMqtt() {
  // Pętla wykona się tylko jeśli Wi-Fi działa, ale MQTT padło
  if (WiFi.status() == WL_CONNECTED && !mqttClient.connected()) {
    Serial.println("STATUS:MQTT_RECONNECTING...");
    
    if (mqttClient.connect("Gateway_Test", mqtt_user, mqtt_pass)) {
      Serial.println("STATUS:MQTT_OK_RECONNECTED");
      mqttClient.subscribe("#");
    } else {
      Serial.print("STATUS:MQTT_RECONNECT_FAILED, RC=");
      Serial.println(mqttClient.state());
    }
  }
}

void loop() {

  if (WiFi.status() == WL_CONNECTED) {
    if (!mqttClient.connected()) reconnectMqtt();
    mqttClient.loop();
  }

  float tempCopy = 0;
  bool hasData = false;

  portENTER_CRITICAL(&tempMux);

  if (sensorData.available) {
    tempCopy = sensorData.temp;
    sensorData.available = false;
    hasData = true;
  }

  portEXIT_CRITICAL(&tempMux);

  if (hasData && WiFi.status() == WL_CONNECTED && mqttClient.connected()) {

    String topic = "dom/czujnik1/temp";
    String payload = String(tempCopy, 1);

    Serial.print("MQTT SEND: ");
    Serial.println(payload);

    mqttClient.publish(topic.c_str(), payload.c_str());
  }

  // BLE scan
  if (millis() - lastBleScanTime >= BLE_SCAN_INTERVAL) {
    lastBleScanTime = millis();

    pBLEScan->start(1, false);
    pBLEScan->clearResults();
  }

  // serial commands
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
  }
}

void executeWifiScan() {
  // Skanujemy sieci w trybie asynchronicznym = false (blokujący)
  // Trzeci parametr (false) oznacza, że nie pokazujemy sieci ukrytych
  int n = WiFi.scanNetworks(false, false);
  
  if (n == 0) {
    Serial.println("STATUS:BRAK_SIECI");
  } else {
    // Wysyłamy każdą sieć w osobnej linijce
    for (int i = 0; i < n; ++i) {
      String ssid = WiFi.SSID(i);
      int32_t rssi = WiFi.RSSI(i);
      
      // WiFi.encryptionType(i) zwraca typ szyfrowania.
      // Jeśli jest różny od WIFI_AUTH_OPEN (czyli 0), to sieć jest zabezpieczona.
      bool isProtected = (WiFi.encryptionType(i) != WIFI_AUTH_OPEN);
      String protectedStr = isProtected ? "1" : "0";

      // Format: NAZWA_SIECI,RSSI,CZY_ZABEZPIECZONA
      // Przykład: nameWiFi,-65,1
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
    return;
  }

  // Wycinamy SSID i hasło na podstawie formatu CONN:Ssid;Haslo
  String ssid = cmd.substring(5, separatorIndex);
  String password = cmd.substring(separatorIndex + 1);

  WiFi.begin(ssid.c_str(), password.c_str());

  unsigned long startAttemptTime = millis();

  while (WiFi.status() != WL_CONNECTED && millis() - startAttemptTime < WIFI_TIMEOUT_MS) {
    delay(500);
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("STATUS:OK;IP:");
    Serial.println(WiFi.localIP().toString());
    delay(10);
    preferences.putInt("mode", 1);
    preferences.putString("last_ssid", ssid);
    preferences.putString("last_password", password);
    preferences.end();
    delay(1000);
    testMqttConnection();
  } else {
    Serial.println("STATUS:ERROR_TIMEOUT");
    WiFi.disconnect();
  }
}

void handleStaticConnectionRequest(String cmd) {
  preferences.begin("wifi", false);

  // Wycinamy nagłówek "CONN_STATIC:" (12 znaków)
  String data = cmd.substring(12); 
  
  // Tablica na 5 wyciętych stringów: [0]=SSID, [1]=Haslo, [2]=IP, [3]=Brama, [4]=Maska
  String parts[5];
  int partCount = 0;
  
  // Prosty parser dzielący tekst po średnikach
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

  // Sprawdzamy czy otrzymaliśmy komplet danych
  if (partCount < 5) {
    Serial.println("STATUS:ERROR_FORMAT");
    return;
  }

  String ssid = parts[0];
  String password = parts[1];
  
  // Konwersja IP ze Stringów na obiekty IPAddress
  IPAddress local_IP;
  IPAddress gateway;
  IPAddress subnet;
  
  if (!local_IP.fromString(parts[2]) || !gateway.fromString(parts[3]) || !subnet.fromString(parts[4])) {
    Serial.println("STATUS:ERROR_IP_PARSING");
    return;
  }

  // Konfiguracja statycznego IP w ESP32
  if (!WiFi.config(local_IP, gateway, subnet)) {
    Serial.println("STATUS:ERROR_CONFIG_FAILED");
    return;
  }

  // Uruchomienie połączenia
  WiFi.begin(ssid.c_str(), password.c_str());

  unsigned long startAttemptTime = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - startAttemptTime < WIFI_TIMEOUT_MS) {
    delay(500);
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("STATUS:OK;IP:");
    Serial.println(WiFi.localIP().toString());
    delay(10);
    preferences.putInt("mode", 0);
    preferences.putString("last_ssid", ssid);
    preferences.putString("last_password", password);
    preferences.putString("last_ip", local_IP.toString());
    preferences.putString("last_gateway", gateway.toString());
    preferences.putString("last_subnet", subnet.toString());
    preferences.end();
    delay(1000);
    testMqttConnection();
  } else {
    Serial.println("STATUS:ERROR_TIMEOUT");
    WiFi.disconnect();
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

void testMqttConnection() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("STATUS:MQTT_ERROR_NO_WIFI");
    return;
  }

  Serial.println("STATUS:MQTT_CONNECTING");
  
  // Próba połączenia z chmurą
  if (mqttClient.connect("Gateway_Test", mqtt_user, mqtt_pass)) {
    Serial.println("STATUS:MQTT_OK");
    
    // testowa wiadomość
    mqttClient.publish("dom/brama/status", "{\"brama\":\"online\"}");
    
    mqttClient.subscribe("#");
    Serial.println("STATUS:MQTT_SUBSCRIBED");
  } else {
    Serial.print("STATUS:MQTT_ERROR_CODE:");
    Serial.println(mqttClient.state());
  }
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
    // Statyczne połączenie
    String ip = preferences.getString("last_ip", "");
    String gateway = preferences.getString("last_gateway", "");
    String subnet = preferences.getString("last_subnet", "");

    if (ip == "" || gateway == "" || subnet == "") {
      Serial.println("STATUS:INCOMPLETE_STATIC_CONFIG");
      preferences.end();
      return false;
    }

    IPAddress local_IP, local_gateway, local_subnet;
    
    if (!local_IP.fromString(ip) || !local_gateway.fromString(gateway) || !local_subnet.fromString(subnet)) {
      Serial.println("STATUS:ERROR_IP_PARSING");
      preferences.end();
      return false;
    }

    WiFi.config(local_IP, local_gateway, local_subnet);
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