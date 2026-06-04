#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEAdvertising.h>

#define SENSOR_NAME "ESP_C3_TEMP"

BLEAdvertising *pAdvertising;
float mockTemperature = 22.0;

unsigned long lastSendTime = 0; 
const unsigned long sendInterval = 30000; // 30 sekund

void setup() {
  Serial.begin(9600);
  
  // Inicjalizacja raz w setupie
  BLEDevice::init(SENSOR_NAME);
  pAdvertising = BLEDevice::getAdvertising();
  
  // Podstawowa konfiguracja
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);  
  pAdvertising->start();
  Serial.println("Start rozgłaszania...");
}

void loop() {
  // Pobieramy aktualny czas procesora
  unsigned long currentTime = millis();

  // Sprawdzamy, czy od ostatniego wysłania minęło już 30 sekund
  if (currentTime - lastSendTime >= sendInterval) {
    // Zapisujemy czas obecnego wysłania jako "ostatni"
    lastSendTime = currentTime;

    // Symulacja zmiany temperatury
    mockTemperature += 0.1;
    if(mockTemperature > 28.0) mockTemperature = 22.0;

    // Przygotowanie danych "T:24.5"
    String payload = "T:" + String(mockTemperature, 1);
    
    // Aktualizacja pakietu BLE
    pAdvertising->stop();

    BLEAdvertisementData oAdvertisementData;
    oAdvertisementData.setName(SENSOR_NAME);
    oAdvertisementData.setManufacturerData(payload.c_str()); 
    
    pAdvertising->setAdvertisementData(oAdvertisementData);
    pAdvertising->start();
    
    Serial.println("Rozgłaszam nową temperaturę: " + payload);
  }

  delay(10); 
}