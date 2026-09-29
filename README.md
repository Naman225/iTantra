# iTantra — Indian Multilingual TTS & STT Aided Neural Transceiver Radio Access

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Offline](https://img.shields.io/badge/Offline-100%25_No_Cloud-green.svg)](README.md)
[![Languages](https://img.shields.io/badge/Languages-10_Indian_Regional-orange.svg)](README.md)
[![Bandwidth](https://img.shields.io/badge/Bandwidth_Saved->99.9%25-brightgreen.svg)](README.md)
[![Platform](https://img.shields.io/badge/Platform-Android_Native_Compose-purple.svg)](android/)

> **iTantra** is an ultra-low-bitrate, 100% offline tactical neural voice transceiver platform built for disaster response, field teams, and defense scenarios over constrained radio links across **10 Indian languages**.

**[Download APK v1.0.0](https://github.com/Naman225/iTantra/releases/download/v1.0.0/iTantra-v1.0-release.apk)** | **[Website](https://naman225.github.io/itantra-site/)**

---

## Problem & Breakthrough

### The Problem

During disasters (floods, landslides, cyclones) or in tactical border zones:

1. Cellular and telecom networks completely fail.
2. Standard voice codecs require 32,000 bytes/sec (256 kbps). Over low-power radio links (LoRa at 0.3–2 kbps, tactical VHF/UHF, or congested mesh), live audio stalls or fails entirely.
3. Victims and field personnel cannot rely on text-only chat — emergency responders and civilians under distress need natural, hands-free voice-in and voice-out without typing.

### The Breakthrough

**Send microscopic text tokens across the air, but let humans speak and listen.**

Instead of pushing heavy analog or PCM audio through constrained airwaves, iTantra performs on-device speech-to-text (Vosk/Kaldi ASR), encapsulates the sentence and priority metadata into a tiny ~48-byte binary frame (`TantraPacket`), broadcasts it over ad-hoc peer-to-peer radio channels (Wi-Fi Direct, Bluetooth RFCOMM, or LoRa), and synthesizes natural spoken voice locally on the receiver device via an on-device Piper neural TTS engine.

$$\text{Effective Bitrate} = \frac{(8\text{ bytes header} + 40\text{ bytes payload}) \times 8\text{ bits}}{3.5\text{ seconds speech}} \approx \mathbf{132\text{ bps}}$$

- **Bandwidth reduction**: 99.9% savings compared to raw PCM audio (256 kbps to ~132 bps).
- **Sub-second latency**: 0.69s – 0.96s end-to-end turnaround.
- **Extreme reach**: Voice communication over long-range LoRa (10–15 km) and tactical radios where streaming raw audio is physically impossible.

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
     (Instant push-to-talk)          (Silero / Energy VAD)
               │                               │
               └───────────────┬───────────────┘
                               ▼
            [ Offline Edge STT Engine (Vosk / Kaldi) ]
           100% On-Device Neural Acoustic Models
                               │  Recognized Text
                               ▼
                 [ 3-Tier Priority Triage & LID ]
            SOS Red  >  Alert Yellow  >  Normal Green
                               │
                               ▼
        [ iTantra Binary Protocol Framer (TantraPacket) ]
        [Magic: 0x54 | Flags(SOS/Alert) | Seq | Len | Payload | CRC16]
        (Header: 8 Bytes | Total: ~48 Bytes)
                               │
                               ▼
             [ Multi-Bearer Ad-Hoc Radio Layer ]
     ┌─────────────────────────┼─────────────────────────┐
     ▼                         ▼                         ▼
[ Wi-Fi Hotspot/Direct ]  [ Bluetooth RFCOMM ]    [ Sub-GHz LoRa ]
(UDP Broadcast: 8888)     (Serial SPP Socket)     (SX1262 10-15 km)
     ═════════════════════════════════════════════════════════
             Zero-Internet Ad-Hoc Mesh Link (~132 bps)
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
   - Haptic feedback                - Continuous vibration alarm
               │                               │
               └───────────────┬───────────────┘
                               ▼
           [ On-Device Neural TTS Engine (Piper ONNX) ]
          Reconstructs natural spoken voice in target language
                               │
                               ▼
                  [ Loudspeaker Spoken Voice ]
```

---

## Mobile Client Features (Android)

The production Android client is built with Jetpack Compose and Material 3:

- **Dual Interaction Modes**:
  - **Walkie-Talkie (PTT)**: Tactical press-and-hold dial with instant mic engagement and haptic feedback.
  - **Phone Call (VAD)**: Hands-free calling that auto-transmits when speech is detected, with mute and end-call controls.
- **3-Tier Priority Classification**:
  - **SOS (Red)**: Instant distress beacon with continuous vibration until acknowledged.
  - **Alert (Yellow)**: Triggered by danger keywords (khatra, danger, emergency).
  - **Normal (Green)**: Standard routine tactical communication.
- **Multilingual Language Switcher**: Offline models for Hindi, English, Tamil, Telugu, Bengali, Marathi, Gujarati, Kannada, Malayalam, Punjabi, and Odia.
- **LoRa Mesh Broadcast**: Scan, pair, and broadcast over LoRa SX1262/SX1278 radio modules.
- **Zero-Cloud P2P Mesh**: Wi-Fi Hotspot UDP broadcast (port 8888) and Bluetooth RFCOMM for off-grid phone-to-phone links.
- **Activity & Transcript Feed**: Real-time incoming/outgoing messages with instant replay TTS.

---

## TantraPacket Binary Protocol

Compact fixed binary frame for extreme transmission reliability over constrained RF links:

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

| Field | Size | Description |
| :--- | :--- | :--- |
| **Magic Byte** | 1 Byte | `0x54` ('T' for Tantra) |
| **Flags** | 1 Byte | Bit 7: SOS Distress, Bit 6: Alert/Hazard, Bits 0-3: Language ID |
| **Sequence Number** | 2 Bytes | uint16 packet counter for deduplication |
| **Payload Length** | 2 Bytes | uint16 byte count of UTF-8 payload |
| **Payload** | Dynamic | UTF-8 encoded text (~30-80 bytes) |
| **CRC-16 CCITT** | 2 Bytes | Polynomial `0x1021` for corruption detection |
| **Total Overhead** | **8 Bytes** | Ultra-lightweight binary envelope |

---

## Measured Benchmarks

| Metric | Result | Baseline | Advantage |
| :--- | :--- | :--- | :--- |
| **TTS Real-Time Factor** | **0.049** | 1.0 (real-time) | 20x faster than real-time |
| **STT Real-Time Factor** | **0.163** | 1.0 (real-time) | 6x faster than real-time |
| **End-to-End Latency** | **0.69s – 0.96s** | 2.5s – 5.0s (VoLTE) | Sub-second tactical voice |
| **Packet Size** | **~48 Bytes** | 64,000 B/sec (PCM) | 99.9% bandwidth saved |
| **RAM Footprint** | **< 30 MB** | 300+ MB (cloud APIs) | Zero thermal throttling |
| **LoRa Range** | **10–15 km** | 300m (Bluetooth) | Long-range disaster comms |

---

## Repository Structure

```
.
├── android/                     # Production Android Application (Kotlin + Compose)
│   ├── app/
│   │   ├── build.gradle         # Dependencies: Compose, Coroutines, Vosk AAR
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       ├── assets/          # Offline Vosk acoustic models
│   │       └── java/org/itantra/transceiver/
│   │           ├── MainActivity.kt
│   │           ├── audio/       # AudioRecordManager, AudioPlayer, SpectralFilter
│   │           ├── emergency/   # EmergencyAlertManager, KeywordClassifier, NotificationHelper
│   │           ├── engine/      # STT, TTS, VAD, ModelStore, LatencyTracker
│   │           ├── protocol/    # TantraPacket, PhraseCodebook
│   │           ├── radio/       # UdpTransceiver, BluetoothTransceiver, LoRa, RadioBus
│   │           └── ui/screens/  # HomeScreen, Dashboard, ConnectDevice, LoRaBroadcast, About
│   ├── build.gradle
│   └── settings.gradle
├── core_engine/                 # Python Offline Engine & Simulator
│   ├── protocol/tantra_packet.py
│   ├── stt/stt_engine.py
│   ├── tts/tts_engine.py
│   └── virtual_transceiver.py
├── embedded/                    # LoRa Hardware Firmware
│   └── iTantra_LoRa_Node.ino
├── benchmarks/                  # Quantitative Metrics
│   ├── test_models.py
│   └── results/
├── tests/                       # Automated Test Suite
│   └── test_all.py
└── docs/                        # Architecture diagrams, pitch assets, website
    ├── assets/
    └── site/                    # Production landing page
```

---

## Quickstart

### 1. Python Transceiver Simulator

```bash
python core_engine/virtual_transceiver.py -i
```

Type a message in English or Hindi. Prefix with `alert:` or `sos:` to trigger emergency triage.

### 2. Automated Tests

```bash
python tests/test_all.py
```

### 3. Android Application

1. Open the `android/` directory in Android Studio.
2. Allow Gradle to sync.
3. Connect an Android device (USB debugging enabled) and run.
4. **Two-Phone Test**: Turn on hotspot on Phone 1, connect Phone 2, launch iTantra on both, speak.

---

## Contributors

- **Developer**: Naman Tiwari
- **Project**: Smart India Hackathon (SIH 2026)
- **Organization**: Indian Space Research Organisation (ISRO)
- **Problem Statement**: PS 26104 — Indian Multilingual TTS & STT Aided Neural Transceiver Radio Access
- **Repository**: [github.com/Naman225/iTantra](https://github.com/Naman225/iTantra)
- **Website**: [naman225.github.io/itantra-site](https://naman225.github.io/itantra-site/)
