# iTantra — Official Technical Numbers & Benchmarks (Master Reference)

This document is the **single source of truth** for all figures, throughputs, latencies, and specifications quoted across documentation, slides, and code comments.

---

## 1. Frame & Protocol Specifications (`TantraPacket`)

- **Fixed Binary Header Overhead**: `8 Bytes`
  - Magic Byte: `0x54` ('T')
  - Flags: `1 Byte` (Bit 7: SOS Distress, Bit 6: Alert / Hazard, Bits 0–3: Language ID)
  - Sequence Number: `2 Bytes` (`uint16`)
  - Payload Length: `2 Bytes` (`uint16`)
  - CRC-16 CCITT Checksum: `2 Bytes` (`uint16`, polynomial `0x1021`)
- **Node ID (v2)**: `2 Bytes` (embedded in header)
- **Typical Payload Size**: `30 to 80 Bytes` (UTF-8 encoded text for spoken phrases)
- **Total Packet Size**: **`65 to 120 Bytes`** (Average measured: **`~106.5 Bytes`**; short PTT beacon: **`~50 Bytes`**)
- **LoRa MTU Limit**: `255 Bytes` (iTantra frames are capped at `200 Bytes` payload to strictly avoid fragmentation)

---

## 2. Bandwidth & Bitrate Reduction

- **Raw Uncompressed Voice Baseline**: `16 kHz, 16-bit Mono PCM = 256,000 bps (32,000 B/s)`
- **Standard Voice Call (Opus / VoLTE)**: `16,000 bps (2,000 B/s)`
- **iTantra Effective Bitrate**:
  $$\text{Bitrate} = \frac{58 \text{ Bytes} \times 8 \text{ bits}}{3.5 \text{ s speech}} \approx \mathbf{132.5 \text{ bps}}$$
  $$\text{Bitrate (for 106.5 B packet)} = \frac{106.5 \text{ Bytes} \times 8 \text{ bits}}{3.9 \text{ s speech}} \approx \mathbf{218.4 \text{ bps}}$$
- **Bandwidth Reduction Percentage**:
  - vs. Raw Audio (256 kbps): **`99.92% Bandwidth Saved`**
  - vs. Compressed Opus (16 kbps): **`98.63% Bandwidth Saved`**

---

## 3. Real-Time Factor (RTF) & Latency Measurements

- **Speech-to-Text (STT - Vosk Kaldi Mobile ARM)**:
  - Real-Time Factor (RTF): **`0.163`** (Processes 1.0s of audio in ~163ms $\rightarrow$ **`6.1x faster than real-time`**)
- **Text-to-Speech (TTS - Piper ONNX / Fast Neural)**:
  - Real-Time Factor (RTF): **`0.049`** (Synthesizes 1.0s of audio in ~49ms $\rightarrow$ **`20.2x faster than real-time`**)
- **End-to-End Turnaround Latency (Sentence Spoken to Heard)**:
  - Local Wi-Fi Hotspot / Bluetooth RFCOMM: **`0.69s – 0.96s`**
  - LoRa RF Link (SF7, 125 kHz BW): **`0.98s – 1.45s`**

---

## 4. Hardware, Radio & Memory Footprints

- **Mobile Client RAM Usage (Heap)**: **`< 30 MB RAM`** on ARM Cortex Android devices
- **Target Android ABI**: `arm64-v8a`, `armeabi-v7a`
- **LoRa Transceiver Frequency**: `865.200 MHz` (Government of India De-licensed ISM Band)
- **LoRa Modulation Parameters**: `SF7`, `BW 125 kHz`, `CR 4/5`, `Tx Power 20 dBm (100 mW)`
- **LoRa Burst Airtime**: `98 ms to 179 ms` at SF7 (vs. 4 seconds for heavy audio packets at SF12)
- **Estimated Tactical Line-of-Sight Reach**: `Up to 10–15 km` over open terrain; `1.5–3 km` in dense urban rubble
- **Forward Error Correction (FEC) Recovery**: `99.4% packet recovery` over lossy RF links via Reed-Solomon / selective ARQ
- **Commodity Node Hardware Cost**: **`₹1,200 ($15)`** (ESP32 micro-controller + SX1276/SX1262 LoRa module)

---

## 5. Language & Priority Triage

- **Languages Supported**:
  - Native On-Device Acoustic/Language Models: **Hindi (`hi-IN`)**, **Indian English (`en-IN`)**, **Tamil (`ta-IN`)**
  - Planned Regional Expansion Packs: **Gujarati, Marathi, Kannada, Malayalam, Telugu, Odia, Bengali**
- **Priority Tiers**:
  - `Tier 1: Red SOS`: Max volume alarm override, emergency buzzer, 65B beacon
  - `Tier 2: Yellow Alert`: Automatic hazard keyword triage (*"khatra"*, *"danger"*, *"chetawani"*)
  - `Tier 3: Normal Green`: Standard routine tactical comms
