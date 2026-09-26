/*
 * iTantra - Embedded LoRa Radio Transceiver Gateway Firmware
 * Platform: ESP32 + Semtech SX1276 / SX1262 LoRa Transceiver
 * Frequency: 865.200 MHz (Govt of India De-licensed ISM Band)
 *
 * How it works:
 * 1. Phone connects to ESP32 via Bluetooth Serial (SPP)
 * 2. When Phone sends 50-byte TantraPacket -> ESP32 broadcasts over LoRa RF (15 km)
 * 3. When LoRa packet arrives over the air -> ESP32 sends to Phone via Bluetooth
 */

#include <SPI.h>
#include <LoRa.h>
#include "BluetoothSerial.h"

// Hardware Pin Definitions (TTGO T-Beam / Heltec LoRa32 / Standard ESP32)
#define SCK_PIN     5
#define MISO_PIN    19
#define MOSI_PIN    27
#define SS_PIN      18
#define RST_PIN     14
#define DIO0_PIN    26

// Govt. of India De-licensed Frequency Band
#define LORA_BAND   865.2E6 

BluetoothSerial SerialBT;

void setup() {
  Serial.begin(115200);
  Serial.println("=== iTantra LoRa Embedded Transceiver Starting ===");

  // 1. Initialize Bluetooth Classic SPP
  SerialBT.begin("iTantra-Radio-Node");
  Serial.println("[BT] Bluetooth Serial Active: 'iTantra-Radio-Node'");

  // 2. Initialize LoRa Radio
  SPI.begin(SCK_PIN, MISO_PIN, MOSI_PIN, SS_PIN);
  LoRa.setPins(SS_PIN, RST_PIN, DIO0_PIN);

  if (!LoRa.begin(LORA_BAND)) {
    Serial.println("[ERROR] Starting LoRa failed! Check wiring.");
    while (1);
  }

  // 3. Optimize LoRa Modulation for Low-Bitrate TantraPackets
  LoRa.setSpreadingFactor(7);           // SF7 (Fast transmission)
  LoRa.setSignalBandwidth(125E3);       // 125 kHz Bandwidth
  LoRa.setCodingRate4(5);               // 4/5 Error Coding
  LoRa.setTxPower(20);                  // 20 dBm (Maximum legal transmit power ~100mW)
  LoRa.enableCrc();                     // Hardware CRC check

  Serial.println("[LORA] Radio Ready on 865.2 MHz (SF7/BW125)");
}

void loop() {
  // A. Phone -> ESP32 (via Bluetooth) -> LoRa Airlink (Tx)
  if (SerialBT.available()) {
    uint8_t txBuffer[256];
    int bytesRead = 0;
    
    // Read complete TantraPacket from phone
    while (SerialBT.available() && bytesRead < 256) {
      txBuffer[bytesRead++] = SerialBT.read();
      delay(2); // Small inter-byte delay
    }

    if (bytesRead > 0 && txBuffer[0] == 0x54) { // Verify iTantra Magic Byte 'T'
      Serial.printf("[TX] Beaming %d bytes over LoRa RF ...\n", bytesRead);
      LoRa.beginPacket();
      LoRa.write(txBuffer, bytesRead);
      LoRa.endPacket();
      Serial.println("[TX] Airlink transmission complete!");
    }
  }

  // B. LoRa Airlink -> ESP32 -> Phone (via Bluetooth) (Rx)
  int packetSize = LoRa.parsePacket();
  if (packetSize) {
    Serial.printf("[RX] Received LoRa packet: %d bytes (RSSI: %d dBm, SNR: %.1f dB)\n", 
                  packetSize, LoRa.packetRssi(), LoRa.packetSnr());

    uint8_t rxBuffer[256];
    int count = 0;
    while (LoRa.available() && count < 256) {
      rxBuffer[count++] = LoRa.read();
    }

    // Forward packet to connected phone via Bluetooth
    if (count > 0 && rxBuffer[0] == 0x54) {
      SerialBT.write(rxBuffer, count);
      Serial.println("[BT] Forwarded packet to Android phone.");
    }
  }
}
