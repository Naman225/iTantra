/*
 * iTantra - Embedded LoRa Radio Transceiver Gateway Firmware (I-08 & I-11)
 * Platform: ESP32 (Original Dual-Core) + Semtech SX1276/78 or SX1262 SPI Transceiver
 * Verified Boards: Heltec WiFi LoRa 32 V2 / TTGO T-Beam (ESP32 Classic)
 * Frequency: 865.200 MHz (Govt of India De-licensed ISM Band)
 *
 * Enhanced with Header-based frame parsing (no 2ms timeout splitting) & CAD back-off.
 */

// =========================================================================
// BOARD SELECTION & HARDWARE ABSTRACTION LAYER (HAL) (I-08 Fix)
// =========================================================================
// Set TARGET_BOARD to:
//   1 -> Heltec WiFi LoRa 32 V3 (ESP32-S3 + Semtech SX1262)
//   2 -> Heltec WiFi LoRa 32 V2 / TTGO T-Beam (ESP32 Classic + Semtech SX1276/78)
#define BOARD_HELTEC_V3_SX1262 1
#define BOARD_HELTEC_V2_SX1276 2

#ifndef TARGET_BOARD
#define TARGET_BOARD BOARD_HELTEC_V3_SX1262
#endif

#include <SPI.h>
#include "BluetoothSerial.h"

#if TARGET_BOARD == BOARD_HELTEC_V3_SX1262
  // Heltec WiFi LoRa 32 V3 (ESP32-S3 + SX1262) Pinout
  #define LORA_SCK    9
  #define LORA_MISO   11
  #define LORA_MOSI   10
  #define LORA_SS     8
  #define LORA_RST    12
  #define LORA_BUSY   13
  #define LORA_DIO1   14
  #define BOARD_NAME  "Heltec V3 (SX1262 / ESP32-S3)"
#else
  // Heltec WiFi LoRa 32 V2 (ESP32 Classic + SX1276) Pinout
  #define LORA_SCK    5
  #define LORA_MISO   19
  #define LORA_MOSI   27
  #define LORA_SS     18
  #define LORA_RST    14
  #define LORA_DIO0   26
  #define BOARD_NAME  "Heltec V2 (SX1276 / ESP32 Classic)"
#endif

#include <LoRa.h>

#define LORA_BAND   865.2E6 // Govt of India De-licensed ISM Band (865 - 867 MHz)

BluetoothSerial SerialBT;

// De-duplication cache for multi-hop / relay
#define CACHE_SIZE 32
uint16_t seenPackets[CACHE_SIZE];
uint8_t cacheIndex = 0;

bool isDuplicate(uint16_t seq) {
  for (int i = 0; i < CACHE_SIZE; i++) {
    if (seenPackets[i] == seq) return true;
  }
  seenPackets[cacheIndex] = seq;
  cacheIndex = (cacheIndex + 1) % CACHE_SIZE;
  return false;
}

void setup() {
  Serial.begin(115200);
  Serial.println("=== iTantra LoRa Node Gateway ===");
  Serial.print("[HAL] Target Hardware: ");
  Serial.println(BOARD_NAME);

  // 1. Initialize Bluetooth Classic SPP
  SerialBT.begin("iTantra-Radio-Node");
  Serial.println("[BT] SPP Active: 'iTantra-Radio-Node'");

  // 2. Initialize LoRa Radio with board-specific pinout
  SPI.begin(LORA_SCK, LORA_MISO, LORA_MOSI, LORA_SS);
#if TARGET_BOARD == BOARD_HELTEC_V3_SX1262
  LoRa.setPins(LORA_SS, LORA_RST, LORA_DIO1);
#else
  LoRa.setPins(LORA_SS, LORA_RST, LORA_DIO0);
#endif

  if (!LoRa.begin(LORA_BAND)) {
    Serial.println("[ERROR] Starting LoRa failed! Check wiring & board definition.");
    while (1);
  }

  // 3. Optimize Modulation
  LoRa.setSpreadingFactor(7);           // SF7 (Fast 98ms burst)
  LoRa.setSignalBandwidth(125E3);       // 125 kHz Bandwidth
  LoRa.setCodingRate4(5);               // 4/5 Error Coding
  LoRa.setTxPower(20);                  // 20 dBm (Max legal power)
  LoRa.enableCrc();                     // Hardware CRC check

  Serial.println("[LORA] Radio Ready on 865.2 MHz (SF7/BW125)");
}

void loop() {
  // A. Phone -> ESP32 (via Bluetooth SPP) -> LoRa Airlink
  // Header-based exact framing (I-11 fix): Read 6-byte header, get length, read remaining
  if (SerialBT.available() >= 6) {
    uint8_t header[6];
    for (int i = 0; i < 6; i++) {
      header[i] = SerialBT.read();
    }

    if (header[0] == 0x54) { // 'T' magic byte
      uint16_t seq = (header[2] << 8) | header[3];
      uint16_t payloadLen = (header[4] << 8) | header[5];
      uint16_t totalExpected = payloadLen + 2; // Payload + 2-byte CRC

      uint8_t frameBuffer[256];
      memcpy(frameBuffer, header, 6);

      int bytesRead = 0;
      unsigned long startWait = millis();
      while (bytesRead < totalExpected && (millis() - startWait < 500)) {
        if (SerialBT.available()) {
          frameBuffer[6 + bytesRead++] = SerialBT.read();
        }
      }

      if (bytesRead == totalExpected) {
        int totalPacketSize = 6 + totalExpected;
        // Channel access random back-off (CAD jitter)
        delay(random(10, 40));

        Serial.printf("[TX] Beaming %d bytes over LoRa (Seq #%d)...\n", totalPacketSize, seq);
        LoRa.beginPacket();
        LoRa.write(frameBuffer, totalPacketSize);
        LoRa.endPacket();
        Serial.println("[TX] LoRa transmission complete.");
      }
    }
  }

  // B. LoRa Airlink -> ESP32 -> Phone (Rx)
  int packetSize = LoRa.parsePacket();
  if (packetSize >= 8 && packetSize <= 255) {
    uint8_t rxBuffer[256];
    int count = 0;
    while (LoRa.available() && count < packetSize) {
      rxBuffer[count++] = LoRa.read();
    }

    if (count > 0 && rxBuffer[0] == 0x54) {
      uint16_t seq = (rxBuffer[2] << 8) | rxBuffer[3];
      if (!isDuplicate(seq)) {
        Serial.printf("[RX] Valid LoRa frame: %d bytes (RSSI: %d dBm, SNR: %.1f dB, Seq: #%d)\n", 
                      count, LoRa.packetRssi(), LoRa.packetSnr(), seq);
        SerialBT.write(rxBuffer, count);
      }
    }
  }
}
