# iTantra — Indian Multilingual TTS & STT Aided Neural Transceiver Radio Access

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Offline](https://img.shields.io/badge/Offline-100%25_No_Cloud-green.svg)](README.md)
[![Languages](https://img.shields.io/badge/Languages-10_Indian_Regional-orange.svg)](README.md)
[![Bandwidth](https://img.shields.io/badge/Bandwidth_Saved->95%25_vs_Opus-brightgreen.svg)](README.md)
[![Platform](https://img.shields.io/badge/Platform-Android_Native_Compose-purple.svg)](android/)

> **iTantra** is an ultra-low-bitrate, 100% offline tactical neural voice transceiver platform built for disaster response, field teams, and defense scenarios over constrained radio links across Indian languages.

**[Download APK v1.0.0](https://github.com/Naman225/iTantra/releases/download/v1.0.0/iTantra-v1.0-release.apk)** | **[Website](https://naman225.github.io/itantra-site/)**

---

## Problem & Breakthrough

### The Problem

During disasters (floods, landslides, cyclones) or in tactical border zones:

1. **Cellular and telecom backhauls fail.**
2. **Standard voice codecs require significant bandwidth**: Even compressed voice codecs like Opus require 6,000–16,000 bps. Over long-range low-power radio links (LoRa at 0.3–2 kbps, tactical VHF/UHF, or congested mesh), streaming continuous audio packets suffers severe packet loss, latency spikes, or complete collapse.
3. **Emergency personnel and victims cannot rely on text-only chat**: First responders and civilians in crisis need hands-free **voice-in and voice-out** without having to look at or type on a screen.

### The Breakthrough

**Transmit microscopic text tokens over the airwaves, while enabling natural voice interaction for humans.**

Instead of pushing raw or compressed audio through congested or narrow-band links, iTantra performs on-device speech-to-text (Vosk Kaldi edge ASR), encapsulates recognized text and priority metadata into a compact binary frame (`TantraPacket`), broadcasts it over ad-hoc peer-to-peer radio channels (Wi-Fi Direct, Bluetooth RFCOMM, or LoRa), and synthesizes spoken voice locally on the receiving device via offline speech synthesis.

$$\text{Effective Bitrate (English)} = \frac{38\text{ bytes} \times 8\text{ bits}}{2.0\text{ seconds speech}} \approx \mathbf{152\text{ bps}}$$

$$\text{Effective Bitrate (Indic UTF-8)} = \frac{150\text{ bytes} \times 8\text{ bits}}{3.5\text{ seconds speech}} \approx \mathbf{342\text{ bps}}$$

- **Bandwidth reduction**: **~50–70% savings** compared to low-bitrate Codec2 (700 bps), **>95% savings** vs Opus (6 kbps), and **>99% savings** vs uncompressed 16 kHz 16-bit PCM audio (256 kbps).
- **Sub-second latency**: 0.7s – 1.0s turnaround in local edge mode.
- **Extended reach**: Enables intelligible voice communication over long-range LoRa links (up to 10–15 km with directional antennas/SF12) where streaming continuous audio streams is physically impossible.

---

## System Architecture

```
                        [ SENDER NODE ]
                               │
                 Microphone Audio (16 kHz 16-bit PCM)
                               │
               ┌───────────────┴───────────────┐
               ▼                               ▼
       [ Walkie-Talkie (PTT) ]       [ Phone Call Mode (VAD) ]
     (Instant push-to-talk)          (Energy-Spectral VAD)
               │                               │
               └───────────────┬───────────────┘
                               ▼
            [ Offline Edge STT Engine (Vosk / Kaldi) ]
           On-Device Acoustic Models (Hindi / English / Dialects)
                               │  Recognized Text
                               ▼
                 [ 3-Tier Priority Triage & LID ]
            SOS Red  >  Alert Yellow  >  Normal Green
                               │
                               ▼
        [ iTantra Binary Protocol Framer (TantraPacket) ]
    [Magic: 0x54 | Flags | SeqNum | PayloadLen | (NodeID/HMAC) | Payload | CRC16]
        (Standard: 8B Overhead | Authenticated: 18B Overhead)
                               │
                               ▼
             [ Multi-Bearer Ad-Hoc Radio Layer ]
     ┌─────────────────────────┼─────────────────────────┐
     ▼                         ▼                         ▼
[ Wi-Fi Hotspot/Direct ]  [ Bluetooth RFCOMM ]    [ Sub-GHz LoRa ]
 (UDP Broadcast: 5005)    (Serial SPP Socket)    (SX1276 865.2 MHz)
     ═════════════════════════════════════════════════════════
             Zero-Internet Ad-Hoc Mesh Link (~150-350 bps)
     ═════════════════════════════════════════════════════════
                               │
                               ▼
                       [ RECEIVER NODE ]
             [ Multi-Bearer Ad-Hoc Radio Receiver ]
                               │
                               ▼
          [ TantraPacket Parser & CRC-16 CCITT Checksum ]
                               │
               ┌───────────────┴───────────────┐
               ▼                               ▼
     [ Normal Voice Message ]         [ SOS / Hazard Alert ]
   - Transcript feed card           - Override volume to 100%
   - Haptic feedback                - Continuous vibration alert
               │                               │
               └───────────────┬───────────────┘
                               ▼
           [ On-Device Offline TTS Engine (Local Voice) ]
          Reconstructs natural spoken voice in target language
                               │
                               ▼
                  [ Loudspeaker Spoken Voice ]
```

---

## Mobile Client Features (Android)

The production Android client is built with Jetpack Compose and Material 3:

- **Dual Interaction Modes**:
  - **Walkie-Talkie (PTT)**: Half-duplex tactical push-and-hold dial with instant mic engagement and haptic feedback.
  - **Phone Call (VAD)**: Hands-free calling that auto-transmits when speech is detected, with mute and end-call controls.
- **3-Tier Priority Classification**:
  - **SOS (Red)**: Priority distress beacon with continuous vibration alert until acknowledged.
  - **Alert (Yellow)**: Triggered by danger keywords (*khatra*, *danger*, *emergency*).
  - **Normal (Green)**: Standard routine tactical communication.
- **Multilingual Support**:
  - Production offline models: **Hindi (`hi`)** and **Indian English (`en`)**.
  - Script & phonetic codebooks for 10 regional languages (Gujarati, Marathi, Kannada, Malayalam, Tamil, Telugu, Odia, Bengali).
- **LoRa Mesh Broadcast**: Interface to pair and broadcast over Semtech SX1276 LoRa transceivers operating in the Indian 865–867 MHz de-licensed band.
- **Zero-Cloud P2P Mesh**: Wi-Fi Hotspot UDP broadcast (port 5005) and Bluetooth RFCOMM for off-grid phone-to-phone links.
- **Activity & Transcript Feed**: Real-time incoming/outgoing messages with instant replay TTS.

---

## TantraPacket Binary Protocol

Fixed binary framing designed for low overhead and corruption rejection over constrained RF links:

```
  0                   1                   2                   3
  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 |  Magic (0x54) |     Flags     |        Sequence Number        |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 |         Payload Length        |       [Node ID (if Auth)]     |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 |                      UTF-8 Payload Text                       |
 |                              ...                              |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 |               [HMAC-SHA256 8-Byte Tag (if Auth)]              |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 |            CRC-16 CCITT Checksum (Polynomial 0x1021)          |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### Frame Fields

| Field | Size | Description |
| :--- | :--- | :--- |
| **Magic Byte** | 1 Byte | `0x54` ('T' for Tantra) |
| **Flags** | 1 Byte | Bit 7: SOS Distress (`0x80`), Bit 6: PTT Mode (`0x40`), Bit 5: Alert (`0x20`), Bit 4: HMAC Auth (`0x10`), Bits 0–3: Language ID |
| **Sequence Number** | 2 Bytes | uint16 counter for deduplication and ordering |
| **Payload Length** | 2 Bytes | uint16 byte count of UTF-8 payload |
| **Node ID** *(optional)* | 2 Bytes | uint16 transmitter node identifier (present when Flag Bit 4 is set) |
| **Payload** | Dynamic | UTF-8 encoded text (~30–180 bytes) |
| **HMAC Tag** *(optional)* | 8 Bytes | Truncated HMAC-SHA256 for frame authentication and tamper detection |
| **CRC-16 CCITT** | 2 Bytes | Polynomial `0x1021` (init `0xFFFF`) for corruption detection |

---

## Measured Benchmarks & Comparisons

### Realistic Protocol Metrics

| Language / Script | Typical Sentence Size | Effective Bitrate (3.5s speech) | LoRa Airtime (SF7 / BW 125k) |
| :--- | :--- | :--- | :--- |
| **English (Latin)** | 38 – 77 Bytes | ~90 – 176 bps | ~110 – 140 ms |
| **Hindi (Devanagari)** | 86 – 153 Bytes | ~196 – 350 bps | ~180 – 250 ms |
| **Indic Scripts (Tamil, Telugu, etc.)** | 120 – 180 Bytes | ~274 – 411 bps | ~220 – 290 ms |

### Comparison Across Audio Standards

| Audio Format | Nominal Bitrate | iTantra Advantage | Practical Feasibility on LoRa |
| :--- | :--- | :--- | :--- |
| **Raw PCM Audio (16kHz 16-bit)** | 256,000 bps | **> 99.8% bandwidth saved** | Physically impossible |
| **Opus Voice Codec** | 6,000 – 16,000 bps | **> 95.0% bandwidth saved** | Severe packet loss on long links |
| **Codec2 (Tactical Radio Standard)** | 700 – 1,200 bps | **~50 – 70% bandwidth saved** | Marginal / Slow throughput |
| **iTantra Neural Protocol** | **~150 – 350 bps** | **Baseline** | **Reliable transmission across full range** |

---

## Repository Structure

```
.
├── android/                     # Production Android Application (Kotlin + Compose)
│   ├── app/
│   │   ├── build.gradle         # Dependencies: Compose, Coroutines, Vosk AAR
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       ├── assets/          # Acoustic model store
│   │       └── java/org/itantra/transceiver/
│   │           ├── MainActivity.kt
│   │           ├── audio/       # AudioRecordManager, AudioPlayer, SpectralFilter
│   │           ├── emergency/   # EmergencyAlertManager, KeywordClassifier, NotificationHelper
│   │           ├── engine/      # SpeechToTextManager, TextToSpeechManager, ModelStore
│   │           ├── protocol/    # TantraPacket, PhraseCodebook
│   │           ├── radio/       # UdpRadioTransceiver, BluetoothTransceiver, RadioBus
│   │           └── ui/screens/  # HomeScreen, Dashboard, ConnectDevice, LoRaBroadcast, About
│   ├── build.gradle
│   └── settings.gradle
├── core_engine/                 # Python Engine & Simulator
│   ├── protocol/tantra_packet.py
│   ├── stt/stt_engine.py
│   ├── tts/tts_engine.py
│   ├── vad/vad_engine.py
│   └── virtual_transceiver.py
├── embedded/                    # LoRa Hardware Gateway Firmware
│   └── iTantra_LoRa_Node.ino   # ESP32 + Semtech SX1276 SPP/LoRa relay
├── benchmarks/                  # Quantitative Metrics & Test Suites
│   ├── test_models.py
│   └── results/
├── tests/                       # Automated Test Suite (Protocol, CRC, HMAC, Networking)
│   └── test_all.py
└── docs/                        # Architecture diagrams, pitch deck assets, guides
    ├── assets/
    └── site/                    # Landing page deployment
```

---

## Quickstart

### 1. Run Automated Test Suite

```bash
python tests/test_all.py
```

Runs protocol serialization, CRC-16 polynomial checks, HMAC tamper detection, language detection, and end-to-end socket transceiver tests.

### 2. Run Interactive Transceiver Terminal

```bash
python core_engine/virtual_transceiver.py -i
```

- Type in English or Hindi (`Base station unit 4 radio check`).
- Prefix with `sos:` for emergency distress broadcast (`sos: need medical assistance`).
- Prefix with `alert:` for tactical yellow hazard warnings (`alert: water rising rapidly`).

### 3. Android Application

1. Open `android/` in Android Studio.
2. Build and install on an Android device (API 26+).
3. Connect two phones via Portable Wi-Fi Hotspot or Bluetooth to test off-grid communications.

---

## Contributors & Organization

- **Developer**: Naman Tiwari
- **Project**: Smart India Hackathon (SIH 2026)
- **Organization**: Indian Space Research Organisation (ISRO)
- **Problem Statement**: PS 26104 — Indian Multilingual TTS & STT Aided Neural Transceiver Radio Access
- **Website**: [naman225.github.io/itantra-site](https://naman225.github.io/itantra-site/)
