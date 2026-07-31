#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEAdvertising.h>
#include <WiFi.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_AHTX0.h>
#include <Adafruit_BMP280.h>

#define LED_PIN 8
#define I2C_SDA 2
#define I2C_SCL 3

Adafruit_BMP280 bmp;
Adafruit_AHTX0 aht;

//flagi czunika
bool hasBMP = false;
bool hasAHT = false;

BLEAdvertising *pAdvertising;
float mockTemperature = 22.0;

String uniqueSensorName = "";
unsigned long lastSendTime = 0; 
const unsigned long sendInterval = 30000; // 30 sekund
const char* Defunit = "°C";

bool checkI2C(uint8_t address) {
  Wire.beginTransmission(address);
  return (Wire.endTransmission() == 0);
}

void setId() {
  String mac = WiFi.macAddress();
  mac.replace(":", "");
  uniqueSensorName = "ESP_" + mac.substring(mac.length() - 6);
}

void setup() {
  Serial.begin(115200);
  setId();

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

    String payload = "ID:" + uniqueSensorName + ";T:" + String(mockTemperature, 1) + ";U:" + String(Defunit);
    
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