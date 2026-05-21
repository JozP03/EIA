#include <WiFi.h>

// Definicja timeoutu dla połączenia (15 sekund)
const int WIFI_TIMEOUT_MS = 15000;

void executeWifiScan();
void handleConnectionRequest(String cmd);

void setup() {
  Serial.begin(115200);
  
  WiFi.mode(WIFI_STA);
  WiFi.disconnect();
  
  delay(1000);
}

void loop() {
  if (Serial.available() > 0) {
    String input = Serial.readStringUntil('\n');
    input.trim(); 

    if (input == "SCAN") {
      executeWifiScan();
    } 
    else if (input.startsWith("CONN:")) {
      handleConnectionRequest(input);
    }
  }
}

void executeWifiScan() {
  int n = WiFi.scanNetworks();
  
  if (n == 0) {
    Serial.println("NETWORKS:BRAK_SIECI");
  } else {
    String response = "NETWORKS:";
    for (int i = 0; i < n; ++i) {
      response += WiFi.SSID(i);
      if (i < n - 1) {
        response += ",";
      }
    }
    Serial.println(response);
  }
  
  WiFi.scanDelete();
}

void handleConnectionRequest(String cmd) {
  int separatorIndex = cmd.indexOf(';');
  
  if (separatorIndex == -1) {
    Serial.println("STATUS:ERROR_FORMAT");
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
    Serial.print("STATUS:OK;IP:");
    Serial.println(WiFi.localIP().toString());
  } else {
    Serial.println("STATUS:ERROR_TIMEOUT");
    WiFi.disconnect();
  }
}