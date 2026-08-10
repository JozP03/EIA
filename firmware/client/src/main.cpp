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

struct Metric {
  String prefix;
  float value;
  int decimals;
};

struct SensorDevice {
  uint8_t address;
  String name;
  bool isActive;
  int metricCount;
  Metric metrics[2]; 
};

SensorDevice mySensors[2] = {
  {
    0x38, "AHT20", false, 2,
    { {"T", 0.0, 1}, {"H", 0.0, 0} }
  },
  {
    0x76, "BMP280", false, 1,
    { {"P", 0.0, 0}}
  }
};

const int numSensors = sizeof(mySensors) / sizeof(mySensors[0]);

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
  uniqueSensorName = "ESP_" + mac.substring(mac.length() - 4);
}

void setup() {
  Serial.begin(115200);
  setId();

  Wire.begin(I2C_SDA, I2C_SCL);

  for (int i = 0; i < numSensors; i++) {
    if (checkI2C(mySensors[i].address)) {
      Serial.println("Wykryto: " + mySensors[i].name);
      
      if (mySensors[i].address == 0x38) {
        mySensors[i].isActive = aht.begin(&Wire);
      } 
      else if (mySensors[i].address == 0x76) {
        mySensors[i].isActive = bmp.begin(0x76);
      }
    } else {
      Serial.println("Brak: " + mySensors[i].name);
    }
  }

  BLEDevice::init("");
  pAdvertising = BLEDevice::getAdvertising();
  
  pAdvertising->setScanResponse(true); 
  pAdvertising->start();
}

void loop() {
  unsigned long currentTime = millis();

  if (currentTime - lastSendTime >= sendInterval) {
    lastSendTime = currentTime;

    for (int i = 0; i < numSensors; i++) {
      if (mySensors[i].isActive) {
        if (mySensors[i].address == 0x38) { // AHT
          sensors_event_t humidity, temp;
          aht.getEvent(&humidity, &temp);
          mySensors[i].metrics[0].value = temp.temperature;
          mySensors[i].metrics[1].value = humidity.relative_humidity;
        } 
        else if (mySensors[i].address == 0x76) { // BMP
          mySensors[i].metrics[0].value = bmp.readPressure() / 100.0F;
        }
      }
    }

    String payload = uniqueSensorName;
    bool anyData = false;
    
    for (int i = 0; i < numSensors; i++) {
      if (mySensors[i].isActive) {
        for (int m = 0; m < mySensors[i].metricCount; m++) {
          payload += ";" + mySensors[i].metrics[m].prefix + ":" + String(mySensors[i].metrics[m].value, mySensors[i].metrics[m].decimals);
          anyData = true;
        }
      }
    }

    if (!anyData) {
      payload += ";ERR:NoSensors";
    }

    pAdvertising->stop();

    BLEAdvertisementData advertisementData;
    advertisementData.setFlags(ESP_BLE_ADV_FLAG_GEN_DISC | ESP_BLE_ADV_FLAG_BREDR_NOT_SPT);
    advertisementData.setManufacturerData(payload.c_str()); 
    
    BLEAdvertisementData scanResponseData;
    scanResponseData.setName(uniqueSensorName.c_str());

    pAdvertising->setAdvertisementData(advertisementData);
    pAdvertising->setScanResponseData(scanResponseData);
    
    pAdvertising->start();
    
    Serial.println("Rozgłoszono: " + payload);
  }

  delay(10); 
}