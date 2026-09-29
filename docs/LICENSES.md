# iTantra — Third-Party Licenses & Software Attribution

All components in **iTantra** adhere to strict open-source licensing guidelines to ensure complete independence from proprietary, commercial, or cloud vendor locks.

---

## 1. On-Device Speech & AI Libraries

| Library / Model | Source / Maintainer | License | Usage in iTantra |
| :--- | :--- | :--- | :--- |
| **Vosk ASR** | Alpha Cephei / Kaldi Project | **Apache License 2.0** | On-device speech recognition engine on Android & Python. |
| **JNA (Java Native Access)** | java-native-access team | **Apache License 2.0 / LGPL 2.1** | Native binding for Vosk C library on Android. |
| **Piper Neural TTS** | Michael Hansen (Rhasspy) | **MIT License** | Fast neural voice synthesis engine. |
| **Silero VAD** | Silero Team | **MIT License** | Voice activity detection model for hands-free calling. |
| **espeak-ng** (Data/Phonemes) | espeak-ng Project | **GPL v3+** | Phonemizer lexicon data for Indic text (isolated binary data). |

---

## 2. Android Framework & UI Libraries

| Library | Maintainer | License | Usage in iTantra |
| :--- | :--- | :--- | :--- |
| **Jetpack Compose** | Google / Android Open Source Project | **Apache License 2.0** | Modern declarative UI layer. |
| **KotlinX Coroutines** | JetBrains | **Apache License 2.0** | Asynchronous non-blocking audio & network dispatch. |
| **Coil Compose** | Coil Contributors | **Apache License 2.0** | Lightweight image loading for operator profile photos. |
| **AndroidX Core KTX** | Google / AOSP | **Apache License 2.0** | Core Android runtime extensions. |

---

## 3. Embedded & Radio Firmware

| Library / Tool | Author | License | Usage in iTantra |
| :--- | :--- | :--- | :--- |
| **RadioLib / LoRa** | Jan Gromes / Sandeep Mistry | **MIT License** | Semtech SX1276 / SX1262 SPI driver for ESP32. |
| **ESP32 Arduino Core** | Espressif Systems | **LGPL 2.1 / Apache 2.0** | Micro-controller firmware runtime. |
| **BluetoothSerial** | Espressif Systems | **Apache License 2.0** | Classic Bluetooth SPP serial driver for ESP32 bridge. |

---

## 4. Compliance Verification
- **Commercial Restrictions**: None. All core models and code run 100% offline without cloud API fees or telemetric phone-home locks.
- **Government / Defense Suitability**: Fully air-gapped, open-source stack reproducible without foreign cloud servers.
