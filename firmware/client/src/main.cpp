#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEAdvertising.h>
#include <WiFi.h>

BLEAdvertising *pAdvertising;
float mockTemperature = 22.0;

String uniqueSensorName = "";
unsigned long lastSendTime = 0; 
const unsigned long sendInterval = 30000; // 30 sekund

void setup() {
  Serial.begin(9600);
  
  String mac = WiFi.macAddress();
  mac.replace(":", "");
  
  // Tworzymy unikalną nazwę w formacie ESP_C3_A1B2C3
  uniqueSensorName = "ESP_C3_" + mac.substring(mac.length() - 6);
  
  Serial.print("Unikalna nazwa tego sensora: ");
  Serial.println(uniqueSensorName);

  //Inicjalizacja stosu BLE
  BLEDevice::init(uniqueSensorName.c_str());
  pAdvertising = BLEDevice::getAdvertising();
  
  // Podstawowa konfiguracja rozgłaszania
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->start();
  
  Serial.println("Start rozgłaszania BLE...");
}

void loop() {
  unsigned long currentTime = millis();

  // Sprawdzamy, czy minęło 30 sekund
  if (currentTime - lastSendTime >= sendInterval) {
    lastSendTime = currentTime;

    // Symulacja zmiany temperatury
    mockTemperature += 0.1;
    if(mockTemperature > 28.0) mockTemperature = 22.0;

    // format: "ID:A1B2C3;T:22.5"
    String payload = "ID:" + uniqueSensorName.substring(7) + ";T:" + String(mockTemperature, 1);
    
    // Aktualizacja pakietu BLE
    pAdvertising->stop();

    BLEAdvertisementData oAdvertisementData;
    // Ustawiamy unikalną nazwę urządzenia widoczną przy skanowaniu
    oAdvertisementData.setName(uniqueSensorName.c_str());
    // Wrzucamy payload do pola Manufacturer Data
    oAdvertisementData.setManufacturerData(payload.c_str()); 
    
    pAdvertising->setAdvertisementData(oAdvertisementData);
    pAdvertising->start();
    
    Serial.println("Rozgłaszam pakiet: " + payload);
  }

  delay(10); 
}