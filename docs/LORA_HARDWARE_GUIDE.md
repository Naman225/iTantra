# iTantra — LoRa & Embedded Hardware Transceiver Integration Guide

This guide explains how **iTantra** interfaces with an external embedded radio (like an ESP32-LoRa module) to achieve **10 to 15 kilometers of voice-in / voice-out range** over low-bitrate links with zero cellular internet.

---

## 1. The Hardware Architecture

```
[ SENDER PHONE ]                                                         [ RECEIVER PHONE ]
┌─────────────────────────┐                                             ┌─────────────────────────┐
│ iTantra Android App     │                                             │ iTantra Android App     │
│ - Mic captures voice    │                                             │ - Parses TantraPacket   │
│ - Offline STT -> Text   │                                             │ - Offline TTS speaks    │
│ - Encodes TantraPacket  │                                             │   in Hindi / English    │
└────────────┬────────────┘                                             └────────────▲────────────┘
             │ Bluetooth SPP                                                         │ Bluetooth SPP
             ▼ (or USB Serial)                                                       │ (or USB Serial)
┌─────────────────────────┐                                             ┌────────────┴────────────┐
│ ESP32 LoRa Transmitter  │                                             │ ESP32 LoRa Receiver     │
│ (LilyGO / Heltec)       │                                             │ (LilyGO / Heltec)       │
│ - Reads Bluetooth byte  │                                             │ - Catches LoRa RF packet│
│ - Modulates to 865 MHz  │                                             │ - Forwards to phone via │
│ - 20 dBm RF output      │                                             │   Bluetooth Serial      │
└────────────┬────────────┘                                             └────────────▲────────────┘
             │                                                                       │
             └═══════════════ LoRa Radio Wave (865 - 867 MHz) ═══════════════════════┘
                                  Range: 10 – 15 Kilometers
                                 (Penetrates Forests & Floods)
```

---

## 2. Why Voice Over LoRa is Impossible, But iTantra Makes It Possible

| Metric | Raw Voice Audio | Opus Compressed Voice | iTantra Neural Packet |
| :--- | :--- | :--- | :--- |
| **Data Rate Needed** | $256,000\text{ bps}$ | $16,000 - 24,000\text{ bps}$ | **$~130\text{ bps}$** |
| **Bytes for 3.5s Speech**| $112,000\text{ Bytes}$ | $7,000\text{ Bytes}$ | **$50 - 65\text{ Bytes}$** |
| **Max LoRa Payload** | $255\text{ Bytes}$ (Exceeded ❌) | $255\text{ Bytes}$ (Exceeded ❌) | **$255\text{ Bytes}$ (Fits in 1 Packet! ✅)**|
| **Can it transmit on LoRa?**| **IMPOSSIBLE** | **IMPOSSIBLE** | **YES (42 ms airtime!)** |

---

## 3. Supported Hardware Modules (Off-The-Shelf)

Any inexpensive ESP32 LoRa development board (~₹1,200 to ₹1,800) works out of the box:
1. **Heltec WiFi LoRa 32 (V2 / V3)**
2. **LilyGO TTGO T-Beam (with GPS & 18650 battery holder)**
3. **Standard ESP32 NodeMCU + Semtech SX1276 / SX1278 breakout module**

### Pin Connection (SPI):
- **SCK**: GPIO 5
- **MISO**: GPIO 19
- **MOSI**: GPIO 27
- **CS / NSS**: GPIO 18
- **RST**: GPIO 14
- **DIO0**: GPIO 26

---

## 4. Regulatory Frequency Compliance (India)

Per the **Government of India National Frequency Allocation Plan (NFAP)**:
- **865.0 to 867.0 MHz** is designated as the **de-licensed ISM band** for short-range and low-power devices (LoRa).
- Maximum allowed EIRP power: **$1\text{ Watt (30 dBm)}$**.
- The provided firmware `embedded/iTantra_LoRa_Node.ino` operates at **$865.200\text{ MHz}$**, complying with Indian telecommunication laws.

---

## 5. How to Flash the ESP32 Firmware

1. Install **Arduino IDE**.
2. Add ESP32 board support via Boards Manager.
3. Install the **`LoRa` by Sandeep Mistry** library (`Sketch -> Include Library -> Manage Libraries`).
4. Open **`embedded/iTantra_LoRa_Node.ino`**.
5. Select your board (e.g. *ESP32 Dev Module* or *Heltec WiFi LoRa 32*), connect via USB, and click **Upload**.
6. When powered, the board advertises Bluetooth as **`iTantra-Radio-Node`**.
7. Pair your phone with `iTantra-Radio-Node`. The phone now has long-range tactical radio capability!
