# iTantra — Android Studio Developer Handoff Guide

> **For Your Teammate / Android Developer**  
> Everything you need to know to open, build, test, and enhance the iTantra Android app.

---

## ⚠️ Critical SIH Hackathon Rule (Read First!)
> **Do NOT use cloud-hosted AI APIs (like Gemini API, OpenAI, or Google Cloud Speech).**  
> The Problem Statement strictly mandates:  
> - **100% Fully Offline**: Must run locally on a low/mid-range phone without internet. In a real flood/earthquake, mobile towers are destroyed.  
> - **Open-Source Only**: TinyML, ONNX Runtime, TFLite, or Sherpa-ONNX. Using proprietary cloud APIs will result in immediate disqualification or zero marks for offline capability.

---

## 1. Quick Setup in Android Studio (2 Minutes)

### Step 1: Clone or Open the Project
If using Git:
```bash
git clone https://github.com/Naman225/iTantra.git
```
In **Android Studio**:
1. Select **File $\rightarrow$ Open**
2. Choose the **`android/`** folder (inside the `iTantra` repository).
3. Wait for Android Studio to sync Gradle automatically.

### Step 2: Ensure Offline Native Libs are Present
From the `android/` directory in terminal (or let Gradle fetch via Maven):
```bash
./setup_libs.sh
```
This ensures the precompiled C++ `sherpa-onnx-1.13.8.aar` native libraries (`arm64-v8a` & `armeabi-v7a`) are available in `app/libs/`.

### Step 3: Run on Device or Emulator
1. Connect an Android smartphone (or start an emulator with API 26+).
2. Click the green **Run ▶** button in Android Studio.
3. Grant **Microphone** and **Nearby Devices** permissions when prompted.

---

## 2. Codebase Map: Where Everything Lives

All source code is inside `android/app/src/main/java/org/itantra/transceiver/`:

| File | What It Does | Where You Might Edit |
| :--- | :--- | :--- |
| **`MainActivity.kt`** | **Tactical Jetpack Compose UI**: PTT button, SOS switch, channel selector, live HUD, and message feed. | Modify layouts, animations, colors, or add new screens. |
| **`protocol/TantraPacket.kt`** | **The 8-byte Binary Protocol**: Serializer, deserializer, CRC-16 CCITT, and Auto-LID script detector. | Add new protocol flags or language IDs. |
| **`radio/UdpRadioTransceiver.kt`** | **Zero-Config P2P Radio Link**: UDP broadcast on port 5005 with `WifiManager.MulticastLock`. | Change ports, add Bluetooth RFCOMM or LoRa serial interface. |
| **`emergency/EmergencyAlertManager.kt`** | **Distress Controller**: Forces device volume to 100% on `STREAM_ALARM` and triggers tactile vibration. | Customize distress ringtone or strobe flash. |
| **`audio/AudioRecordManager.kt`** | **Microphone Capture**: 16kHz mono PCM stream with real-time RMS amplitude tracking. | Adjust audio gain or buffer sizes. |
| **`audio/AudioPlayerManager.kt`** | **Audio Playback**: Low-latency AudioTrack player for speech and walkie-talkie Roger-beep chime. | Add custom squelch sounds or audio effects. |
| **`engine/TextToSpeechManager.kt`** | **Offline Indic TTS**: Uses Android's built-in offline Indic voice packs across 10 languages with `USAGE_ALARM` support. | Switch between native TTS and embedded Piper ONNX models. |
| **`engine/SpeechToTextManager.kt`** | **Offline STT**: Interface for local speech recognition. | Connect sherpa-onnx / Vosk offline model inference. |

---

## 3. How to Test Two Phones (Live Walkie-Talkie Demo)

You do **NOT** need an active internet connection or SIM card to demo this:

1. **Phone A**: Turn on **Portable Wi-Fi Hotspot** (no internet data required).
2. **Phone B**: Connect to Phone A's Wi-Fi hotspot.
3. Install and open the **iTantra** APK on both phones.
4. **Speak**:
   - On Phone A: Press and hold the big circular **HOLD TO TALK** button and speak.
   - Release the button: Phone A plays a *Roger-beep* tone, transmits a ~50-byte packet.
   - **Phone B immediately speaks the sentence out loud!**
5. **Emergency SOS Test**:
   - Turn on the **SOS DISTRESS** switch on Phone A and transmit.
   - Phone B will override its volume to 100%, vibrate urgently, and blast the message non-interruptibly!

---

## 4. Key Gradle & Environment Details
- **Min SDK**: `24` (Android 7.0 - covers >95% of all low/mid-range Android phones).
- **Target SDK**: `34` (Android 14).
- **Java / Kotlin**: Java 17, Kotlin 1.9.22, Jetpack Compose Compiler 1.5.8.
