#pragma once

#include <Arduino.h>
#include <BLEDevice.h>
#include <esp_gap_ble_api.h>

// ESP32-C3 Bluedroid BLE 5 extended advertising/scanning on LE Coded PHY.
namespace Ble5Range {
static constexpr uint8_t kAdvInstance = 0;
static volatile bool advDataReady = false;
static volatile bool advParamsReady = false;
static volatile bool scanParamsReady = false;
static volatile bool scanStarted = false;
static void (*reportHandler)(const uint8_t *, size_t, int8_t) = nullptr;

inline void gapEvent(esp_gap_ble_cb_event_t event, esp_ble_gap_cb_param_t *param) {
  switch (event) {
    case ESP_GAP_BLE_EXT_ADV_SET_PARAMS_COMPLETE_EVT: advParamsReady = param->ext_adv_set_params.status == ESP_BT_STATUS_SUCCESS; break;
    case ESP_GAP_BLE_EXT_ADV_DATA_SET_COMPLETE_EVT: advDataReady = param->ext_adv_data_set.status == ESP_BT_STATUS_SUCCESS; break;
    case ESP_GAP_BLE_SET_EXT_SCAN_PARAMS_COMPLETE_EVT: scanParamsReady = param->set_ext_scan_params.status == ESP_BT_STATUS_SUCCESS; break;
    case ESP_GAP_BLE_EXT_SCAN_START_COMPLETE_EVT: scanStarted = param->ext_scan_start.status == ESP_BT_STATUS_SUCCESS; break;
    case ESP_GAP_BLE_EXT_ADV_REPORT_EVT: {
      auto &r = param->ext_adv_report.params;
      if (reportHandler && r.adv_data_len && r.data_status == ESP_BLE_GAP_EXT_ADV_DATA_COMPLETE)
        reportHandler(r.adv_data, r.adv_data_len, r.rssi);
      break;
    }
    default: break;
  }
}

inline bool waitFlag(volatile bool *flag, uint32_t timeoutMs = 1500) {
  uint32_t start = millis();
  while (!*flag && millis() - start < timeoutMs) delay(1);
  return *flag;
}

inline bool begin(void (*handler)(const uint8_t *, size_t, int8_t)) {
  reportHandler = handler;
  BLEDevice::init("");
  if (esp_ble_gap_register_callback(gapEvent) != ESP_OK) return false;
  esp_ble_gap_ext_adv_params_t adv = {};
  adv.type = ESP_BLE_GAP_SET_EXT_ADV_PROP_NONCONN_NONSCANNABLE_UNDIRECTED;
  adv.interval_min = 160; // 100 ms, units of 0.625 ms
  adv.interval_max = 240; // 150 ms
  adv.channel_map = ADV_CHNL_ALL;
  adv.own_addr_type = BLE_ADDR_TYPE_PUBLIC;
  adv.filter_policy = ADV_FILTER_ALLOW_SCAN_ANY_CON_ANY;
  adv.tx_power = ESP_PWR_LVL_P9;
  adv.primary_phy = ESP_BLE_GAP_PRI_PHY_CODED;
  adv.secondary_phy = ESP_BLE_GAP_PHY_CODED;
  adv.sid = 1;
  if (esp_ble_gap_ext_adv_set_params(kAdvInstance, &adv) != ESP_OK) return false;
  if (!waitFlag(&advParamsReady)) return false;

  esp_ble_ext_scan_params_t scan = {};
  scan.own_addr_type = BLE_ADDR_TYPE_PUBLIC;
  scan.filter_policy = BLE_SCAN_FILTER_ALLOW_ALL;
  scan.scan_duplicate = BLE_SCAN_DUPLICATE_DISABLE;
  scan.cfg_mask = ESP_BLE_GAP_EXT_SCAN_CFG_UNCODE_MASK | ESP_BLE_GAP_EXT_SCAN_CFG_CODE_MASK;
  scan.uncoded_cfg.scan_type = BLE_SCAN_TYPE_PASSIVE;
  scan.uncoded_cfg.scan_interval = 160;
  scan.uncoded_cfg.scan_window = 160;
  scan.coded_cfg.scan_type = BLE_SCAN_TYPE_PASSIVE;
  scan.coded_cfg.scan_interval = 160;
  scan.coded_cfg.scan_window = 160;
  if (esp_ble_gap_set_ext_scan_params(&scan) != ESP_OK) return false;
  return waitFlag(&scanParamsReady);
}

inline bool advertise(const uint8_t *data, uint16_t length) {
  advDataReady = false;
  if (esp_ble_gap_config_ext_adv_data_raw(kAdvInstance, length, data) != ESP_OK || !waitFlag(&advDataReady)) return false;
  esp_ble_gap_ext_adv_t enable = {kAdvInstance, 0, 0};
  return esp_ble_gap_ext_adv_start(1, &enable) == ESP_OK;
}
inline void stopAdvertising() { uint8_t id = kAdvInstance; esp_ble_gap_ext_adv_stop(1, &id); }
inline bool startScan(uint16_t duration10ms) {
  scanStarted = false;
  if (esp_ble_gap_start_ext_scan(duration10ms, 0) != ESP_OK) return false;
  if (!waitFlag(&scanStarted)) return false;
  delay(static_cast<uint32_t>(duration10ms) * 10); // controller duration uses 10 ms units
  return true;
}
inline void stopScan() { esp_ble_gap_stop_ext_scan(); }

// Wrap application bytes in a Manufacturer Specific Data AD structure.
inline size_t makeManufacturerAd(const uint8_t *payload, size_t payloadLength, uint8_t *out, size_t capacity) {
  if (payloadLength + 1 > 255 || payloadLength + 2 > capacity) return 0;
  out[0] = static_cast<uint8_t>(payloadLength + 1);
  out[1] = 0xFF;
  memcpy(out + 2, payload, payloadLength); // preserve the existing application payload byte-for-byte
  return payloadLength + 2;
}
inline bool extractManufacturer(const uint8_t *ad, size_t length, const uint8_t **payload, size_t *payloadLength) {
  size_t i = 0;
  while (i < length) {
    uint8_t fieldLength = ad[i++];
    if (fieldLength == 0 || i + fieldLength > length) break;
    uint8_t type = ad[i];
    if (type == 0xFF && fieldLength >= 1) {
      *payload = ad + i + 1; // the existing firmware uses manufacturer data as a raw application field
      *payloadLength = fieldLength - 1;
      return true;
    }
    i += fieldLength;
  }
  return false;
}
} // namespace Ble5Range
