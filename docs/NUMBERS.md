# iTantra — Official Technical Numbers & Benchmarks (Master Reference)

This document is the **single source of truth** for all figures, throughputs, latencies, and specifications quoted across documentation, slides, and code comments.

---

## 1. Frame & Protocol Specifications (`TantraPacket`)

- **Fixed Binary Header Overhead**: `6 Bytes`
  - Magic Byte: `0x54` ('T') (`1 Byte`)
  - Flags: `1 Byte` (Bit 7: SOS Distress, Bit 6: PTT, Bit 5: Alert, Bit 4: HMAC Auth, Bits 0–3: Language ID)
  - Sequence Number: `2 Bytes` (`uint16`)
  - Payload Length: `2 Bytes` (`uint16`)
- **Node ID (v2)**: `2 Bytes` (follows header)
- **Security & Integrity**: 
  - HMAC Checksum: `8 Bytes` (Optional)
  - CRC-16 CCITT Checksum: `2 Bytes` (`uint16`, polynomial `0x1021`)
- **Typical Payload Size**: `30 to 170 Bytes` (UTF-8 encoded text for spoken phrases)
- **Total Packet Size**: 
  - English: **`~38 to 77 Bytes`**
  - Hindi: **`~86 to 153 Bytes`**
  - Indic Average: **`~150 Bytes`**
- **LoRa MTU Limit**: `255 Bytes` (iTantra frames are capped at `200 Bytes` payload to strictly avoid fragmentation)

---

## 2. Bandwidth & Bitrate Reduction

- **Raw Uncompressed Voice Baseline**: `16 kHz, 16-bit Mono PCM = 256,000 bps (32,000 B/s)`
- **Standard Voice Call (Opus / VoLTE)**: `16,000 bps (2,000 B/s)`
- **Codec2 Baseline**: `700 bps`
- **iTantra Effective Bitrate**: **`~150 to 350 bps`** (depending on language and payload length)
- **Bandwidth Reduction Percentage**:
  - vs. Raw Audio (256 kbps): **`>99% Bandwidth Saved`**
  - vs. Compressed Opus (16 kbps): **`>95% Bandwidth Saved`**
  - vs. Codec2 (700 bps): **`~50-70% savings`**

---

## 3. Real-Time Factor (RTF) & Latency Measurements

- **Speech-to-Text (STT - Vosk Kaldi Mobile ARM)**:
  - Real-Time Factor (RTF): **measured on desktop Python, mobile ARM numbers TBD**
- **Text-to-Speech (TTS - Piper ONNX / Fast Neural)**:
  - Real-Time Factor (RTF): **measured on desktop Python, mobile ARM numbers TBD**
- **End-to-End Turnaround Latency (Sentence Spoken to Heard)**:
  - Local Wi-Fi Hotspot / Bluetooth RFCOMM: **sub-second latency**
  - LoRa RF Link: Depends on spreading factor and payload size

---

## 4. Hardware, Radio & Memory Footprints

- **Mobile Client RAM Usage (Heap)**: **`< 30 MB RAM`** on ARM Cortex Android devices
- **Target Android ABI**: `arm64-v8a`, `armeabi-v7a`
- **LoRa Transceiver Frequency**: `865.200 MHz` (Government of India De-licensed ISM Band)
- **LoRa Modulation Parameters**: `SF7` (default firmware), `BW 125 kHz`, `CR 4/5`, `Tx Power 20 dBm (100 mW)`
- **LoRa Burst Airtime**: 
  - At default `SF7`: Fast transmission for close range
  - **Estimated Tactical Line-of-Sight Reach**: `Up to 10–15 km` over open terrain requires `SF12` at the cost of **`~6s airtime per Indic packet`**
- **Commodity Node Hardware Cost**: **`₹1,200 ($15)`** (ESP32 micro-controller + SX1276/SX1262 LoRa module)

---

## 5. Language & Priority Triage

- **Languages Supported**:
  - Native On-Device Acoustic/Language Models (Installed): **Hindi (`hi-IN`)**, **Indian English (`en-IN`)**
  - Publicly available but not installed: Gujarati, Telugu Vosk models; Malayalam Piper voice
  - Planned Regional Expansion Packs: **Tamil, Gujarati, Marathi, Kannada, Malayalam, Telugu, Odia, Bengali**
- **Priority Tiers**:
  - `Tier 1: Red SOS`: Max volume alarm override, emergency buzzer
  - `Tier 2: Yellow Alert`: Automatic hazard keyword triage (*"khatra"*, *"danger"*, *"chetawani"*)
  - `Tier 3: Normal Green`: Standard routine tactical comms
