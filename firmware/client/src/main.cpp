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
  Serial.begin(115200);
  
  String mac = WiFi.macAddress();
  mac.replace(":", "");
  uniqueSensorName = "ESP_" + mac.substring(mac.length() - 6);
  
  Serial.println("Unikalna nazwa: " + uniqueSensorName);

  BLEDevice::init("");
  pAdvertising = BLEDevice::getAdvertising();
  
  pAdvertising->setScanResponse(true); 
  pAdvertising->start();
  
  Serial.println("Start rozgłaszania...");
}

void loop() {
  unsigned long currentTime = millis();

  if (currentTime - lastSendTime >= sendInterval) {
    lastSendTime = currentTime;

    mockTemperature += 0.1;
    if(mockTemperature > 28.0) mockTemperature = 22.0;

    String payload = "ID:" + uniqueSensorName + ";T:" + String(mockTemperature, 1);
    
    pAdvertising->stop();

    // 1. Pakiet główny 
    BLEAdvertisementData advertisementData;
    advertisementData.setFlags(ESP_BLE_ADV_FLAG_GEN_DISC | ESP_BLE_ADV_FLAG_BREDR_NOT_SPT);
    advertisementData.setManufacturerData(payload.c_str()); 
    
    // 2. Scan Response
    BLEAdvertisementData scanResponseData;
    scanResponseData.setName(uniqueSensorName.c_str());

    pAdvertising->setAdvertisementData(advertisementData);
    pAdvertising->setScanResponseData(scanResponseData);
    
    pAdvertising->start();
    
    Serial.println("Rozgłoszono: " + payload);
  }

  delay(10); 
}