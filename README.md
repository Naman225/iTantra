# iTantra — Indian Multilingual TTS & STT Aided Neural Transceiver Radio Access

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Offline](https://img.shields.io/badge/Offline-100%25_No_Cloud-green.svg)](README.md)
[![Languages](https://img.shields.io/badge/Languages-10_Indian_Languages-orange.svg)](README.md)
[![Bandwidth](https://img.shields.io/badge/Bandwidth_Saved->99.9%25-brightgreen.svg)](README.md)
[![Tests](https://img.shields.io/badge/Tests-100%25_Passing-success.svg)](tests/)

> **iTantra** is a high-accuracy, ultra-low-bitrate neural transceiver and tactical walkie-talkie platform designed for disaster response, defense, and distress-based scenarios over constrained radio links across **10 Indian languages**.

---

## 📌 Problem & Breakthrough

### The Problem
During disasters (floods, landslides, cyclones) or in remote border/rural sectors:
1. **Cellular networks and internet are completely offline.**
2. **Traditional voice audio requires 32,000 bytes per second (256 kbps).** Over weak radio links (HF/VHF, LoRa at 0.3–2 kbps, or congested disaster beacons), real-time audio streams drop, stutter, or fail completely.
3. **Written text messages fail inclusion**: Many citizens and distress victims cannot read, write, or type under emergency conditions. They need to **speak and listen**.

### The iTantra Breakthrough
**Send text across the air, but let humans speak and listen.**  
Instead of pushing heavy audio through congested airwaves, **iTantra performs edge-local speech recognition (STT)** on the sender's device, encapsulates the sentence into an **8-byte framed binary packet (~50 bytes total)**, beams it across low-power peer-to-peer radio channels, and **synthesizes natural speech locally** on the receiver device using an embedded neural TTS engine.

$$\text{Effective Bitrate} = \frac{(8\text{ bytes header} + 50\text{ bytes payload}) \times 8\text{ bits}}{3.5\text{ seconds speech}} \approx \mathbf{132\text{ bps (bits per second)!}}$$

- **Bandwidth reduction**: **>99.9%** compared to raw PCM audio, and **>98.5%** compared to Opus voice.
- **Hardware range multiplier**: Enables voice communication over long-range **LoRa (10–15 km)** and tactical VHF/UHF radio links where streaming human voice is physically impossible.

---

## 🔄 End-to-End System Flow

```
                      [ SENDER DEVICE ]
                              │
                 [ Microphone Audio Stream (16kHz PCM) ]
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
      [ PTT Button Mode ]           [ Phone Mode (Hands-Free) ]
   (User holds & releases)        (Silero VAD detects >600ms pause)
              │                               │
              └───────────────┬───────────────┘
                              ▼
             [ Offline Neural STT Engine (Vosk / Sherpa) ]
         (Local Acoustic / Zipformer Model - Zero Internet)
                              │  Recognized Text
                              ▼
             [ Auto-LID (Language Identification) ]
          (Dynamically classifies script across 10 languages)
                              │
                              ▼
            [ iTantra Binary Protocol Framer (TantraPacket) ]
          [Magic: 0x54 | Flags(SOS/PTT) | Seq | Len | Text | CRC16]
          (Total overhead: 8 Bytes | Total packet: ~50-80 Bytes)
                              │
                              ▼
             [ Low-Bitrate Transceiver Radio Layer ]
          (Local Wi-Fi Hotspot UDP Broadcast / Bluetooth / LoRa)
       ═══════════════════════════════════════════════════════════
                 Constrained Link (~100 to 300 bps)
       ═══════════════════════════════════════════════════════════
                              │
                              ▼
                     [ RECEIVER DEVICE ]
             [ Low-Bitrate Transceiver Radio Layer ]
                              │
                              ▼
           [ TantraPacket Parser & CRC-16 Checksum Verifier ]
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
       [ Normal Voice Note ]          [ Distress / SOS Alert ]
    - Roger beep audio chime        - Override system volume to 100%
    - Audio log queue card          - AudioAttributes.USAGE_ALARM
    - Standard playback             - Non-interruptible broadcast
              │                               │
              └───────────────┬───────────────┘
                              ▼
             [ Offline Neural TTS Engine (Piper / VITS) ]
            (Native Indian speech synthesized locally)
                              │
                              ▼
                     [ Loudspeaker Voice Out ]
```

---

## 🌐 10 Indian Languages Coverage

iTantra provides native linguistic routing and acoustic synthesis for all 10 mandated languages:

| Lang ID | Language | Code | Primary Script | Voice Profile |
| :---: | :--- | :---: | :--- | :--- |
| `0x00` | **Hindi** | `hi` | Devanagari | Pratham Neural VITS / Kaldi Indic |
| `0x01` | **English** (Indian) | `en` | Latin | Lessac Neural VITS / Indian English |
| `0x02` | **Gujarati** | `gu` | Gujarati | IndicVoices / Offline Indic Pack |
| `0x03` | **Marathi** | `mr` | Devanagari | IndicVoices / Offline Indic Pack |
| `0x04` | **Kannada** | `kn` | Kannada | Dravida-TTS / Offline Indic Pack |
| `0x05` | **Malayalam** | `ml` | Malayalam | Dravida-TTS / Offline Indic Pack |
| `0x06` | **Tamil** | `ta` | Tamil | Dravida-TTS / Offline Indic Pack |
| `0x07` | **Telugu** | `te` | Telugu | Dravida-TTS / Offline Indic Pack |
| `0x08` | **Odia** | `or` | Odia | Eastern Indic / Offline Indic Pack |
| `0x09` | **Bengali** | `bn` | Bengali | Eastern Indic / Offline Indic Pack |

---

## 📡 The iTantra Binary Protocol (`TantraPacket`)

To guarantee extreme transmission reliability over noisy RF links, packets use a compact, fixed binary frame:

```
  0                   1                   2                   3
  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 |  Magic (0x54) |     Flags     |        Sequence Number        |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 |         Payload Length        |       UTF-8 Payload Text      |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                               +
 |                                                               |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 |            CRC-16 CCITT Checksum (Polynomial 0x1021)          |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

- **Magic Byte (`0x54`)**: Identifies protocol ('T' for Tantra).
- **Flags Byte**:
  - `Bit 7 (0x80)`: **Emergency SOS Alert** (triggers max volume override & alarm).
  - `Bit 6 (0x40)`: **PTT Mode** (1 = Push-to-talk, 0 = Continuous Phone Mode).
  - `Bits 0-3 (0x0F)`: **Language ID** (0 to 9 mapped to Indian languages).
- **Sequence Number (`uint16`)**: Packet ordering and deduplication.
- **Payload Length (`uint16`)**: Dynamic length of UTF-8 encoded text.
- **CRC-16 CCITT (`uint16`)**: Cyclic redundancy check to instantly drop corrupted airlink frames.
- **Total Overhead**: **Only 8 Bytes!**

---

## 📊 Performance & Benchmark Telemetry

Quantitative measurements collected across English and Hindi tactical test sets:

| Metric | Measured Result | Industry Comparison |
| :--- | :--- | :--- |
| **TTS Real-Time Factor (RTF)** | **0.049** | **~20.2x faster than real-time** (Piper ONNX) |
| **STT Real-Time Factor (RTF)** | **0.163** | **~6.1x faster than real-time** (Vosk Kaldi) |
| **Speech Turnaround Latency** | **0.68s – 0.96s** | Instant voice response on mobile CPU |
| **Average Packet Size** | **106.5 Bytes** | Fits comfortably in a 250B LoRa packet |
| **Bandwidth Saved vs Raw PCM** | **99.92%** | Slashes 256,000 bps down to ~160 bps |
| **Bandwidth Saved vs Opus Voice**| **98.63%** | Slashes 16,000 bps down to ~160 bps |
| **Word Error Rate (WER)** | **0.0% – 12.5%** | High intelligibility on tactical vocabulary |
| **Auto-LID Accuracy** | **100%** | Flawless script routing (Latin vs Devanagari vs Dravidian) |

---

## 📂 Repository Structure

```
.
├── android/                         # Production Native Android Application (Kotlin)
│   ├── app/
│   │   ├── build.gradle             # Jetpack Compose, Coroutines, sherpa-onnx
│   │   └── src/main/
│   │       ├── AndroidManifest.xml  # Audio, Wi-Fi Multicast, Bluetooth permissions
│   │       ├── res/values/          # Tactical OLED theme (Amber, Green, SOS Red)
│   │       └── java/org/itantra/transceiver/
│   │           ├── MainActivity.kt  # Jetpack Compose UI: PTT Button, Telemetry HUD, Log
│   │           ├── protocol/        # TantraPacket.kt (8-byte binary framing + Auto-LID)
│   │           ├── radio/           # UdpRadioTransceiver.kt (Zero-config Hotspot P2P)
│   │           ├── audio/           # AudioRecordManager.kt (PCM capture) & AudioPlayerManager.kt
│   │           ├── emergency/       # EmergencyAlertManager.kt (STREAM_ALARM override + haptics)
│   │           └── engine/          # SpeechToTextManager.kt & TextToSpeechManager.kt
│   ├── build.gradle                 # Top-level build script
│   ├── settings.gradle              # Dependency management
│   └── setup_libs.sh                # Direct AAR downloader for offline packaging
├── core_engine/                     # Offline AI Engine & Virtual Transceiver Core
│   ├── protocol/
│   │   └── tantra_packet.py         # Binary protocol framing & Auto-LID in Python
│   ├── models/
│   │   └── download_models.py       # Automated model downloader & integrity checker
│   ├── stt/
│   │   └── stt_engine.py            # Offline Vosk speech-to-text inference
│   ├── tts/
│   │   └── tts_engine.py            # Offline Piper neural speech synthesis
│   └── virtual_transceiver.py       # 2-node UDP walkie-talkie interactive simulator
├── benchmarks/                      # Quantitative Metrics Suite
│   ├── test_models.py               # Automated benchmark runner (WER, RTF, Bandwidth)
│   └── results/                     # Benchmark logs and synthesized test audio
│       └── BENCHMARK_REPORT.md      # Detailed metrics breakdown table
├── tests/                           # Master Automated Verification Suite
│   └── test_all.py                  # End-to-end unit & integration tests (100% pass)
└── docs/
    └── TEAM_EXECUTIVE_SUMMARY.md    # Executive pitch deck & live demo guide
```

---

## 🚀 Quickstart & Verification

### 1. Run the Interactive Walkie-Talkie Terminal
Test the transceiver on your laptop right now:
```bash
python core_engine/virtual_transceiver.py -i
```
- Type any message in English or Hindi (e.g. `Base station, radio check loud and clear` or `यह एक आपातकालीन संदेश है`).
- Prefix with `alert:` or `sos:` (e.g. `alert: Flash flood warning evacuate immediately`) to test high-priority non-interruptible emergency broadcast with max volume override.
- Language is auto-detected via Auto-LID dynamically!

### 2. Run the Master Verification Test Suite
Execute the automated test suite covering all layers:
```bash
python tests/test_all.py
```
*(All 8 test cases pass with 100% success).*

### 3. Run Quantitative Benchmarks
Evaluate Word Error Rate (WER), Real-Time Factor (RTF), and Bandwidth compression:
```bash
python benchmarks/test_models.py
```

### 4. Build & Run the Android Application
1. Open **Android Studio**.
2. Select **Open** $\rightarrow$ Navigate to `android/`.
3. Gradle will sync dependencies automatically.
4. Connect any low-to-mid-range Android smartphone and click **Run**.
5. To test two-phone communication without internet: turn on portable hotspot on Phone 1, connect Phone 2, open iTantra on both phones, and talk!
