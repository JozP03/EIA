#include <WiFi.h>

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
    input.toUpperCase(); // Konwertujemy na duże litery, aby "scan" i "SCAN" działały tak samo

    if (input == "SCAN") {
      executeWifiScan();
    } 
    else if (input.startsWith("CONN:")) {
      // Przy połączeniu zachowujemy oryginalny input (wielkość liter w haśle ma znaczenie!)
      handleConnectionRequest(input); 
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
      // Przykład: MojeSuperWiFi,-65,1
      Serial.print(ssid);
      Serial.print(",");
      Serial.print(rssi);
      Serial.print(",");
      Serial.println(protectedStr);
      
      delay(10); // Małe opóźnienie, żeby nie zapchać bufora Seriala
    }
  }
  
  // Informujemy Androida, że to już koniec listy
  Serial.println("SCAN_FINISHED");
  
  // Czyszczenie pamięci po skanowaniu
  WiFi.scanDelete();
}

void handleConnectionRequest(String cmd) {
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
  } else {
    Serial.println("STATUS:ERROR_TIMEOUT");
    WiFi.disconnect();
  }
}