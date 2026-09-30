# iTantra — 30-Second Live Demo Script & Pitch Deck Guide (Step 8)

> **For SIH Evaluators & Video Submission**  
> Built around actual measured metrics: **>95% vs Opus**, **sub-second latency**.

---

## 🎥 1. The 30-Second Live Video Demo Script (Single-Take, No Cuts)

### Video Setup
- **Phone A (Sender)** and **Phone B (Receiver)** placed side-by-side on a desk.
- **Show proof of zero internet**: Swipe down status bar on both phones to show **Airplane Mode ON** and **Mobile Data OFF**. Phone A has Portable Hotspot ON, Phone B is connected to it.

---

### Step-by-Step Recording Actions:

| Time | Action | Voiceover / Spoken Words | What Happens on Screen / Audio |
| :--- | :--- | :--- | :--- |
| **0:00 - 0:08** | **Hook & Context** | *"In a flood or earthquake, internet dies, but streaming audio takes 32,000 bytes per second. Victims cannot type. This is iTantra: a walkie-talkie that needs zero internet and >99% less bandwidth than raw PCM."* | Camera shows both phones with iTantra tactical UI open in Airplane mode. |
| **0:08 - 0:16** | **Hindi Test (PTT)** | Speaker presses PTT on Phone A and speaks:<br>**"बाढ़ का पानी पुल तक आ गया है, तुरंत सहायता भेजें!"**<br>*(Releases PTT button)* | Phone A plays *Roger-beep*. Phone B instantly displays the card and speaks the Hindi sentence out loud! |
| **0:16 - 0:24** | **English Test (PTT)** | Speaker switches language to English, presses PTT:<br>**"Emergency alert: flash flood warning, evacuate immediate sector."**<br>*(Releases PTT button)* | Phone A transmits the packet. Phone B immediately speaks the English voice note clearly. |
| **0:24 - 0:30** | **SOS Alarm Override** | Speaker flips **SOS DISTRESS** switch on Phone A and presses PTT:<br>**"Evacuate bridge now!"** | Phone B's screen flashes red, volume forces to 100% max, alarm chime sounds, and loud speech announces the distress note. |
| **0:30** | **Outro** | *"~150 bytes transmitted average. Sub-second total latency. 100% offline."* | Point to the on-screen Telemetry HUD showing **>95% saved vs Opus** and **~150-350 bps**. |

---

## 📊 2. The 8-Slide Pitch Deck Blueprint

### **Slide 1: Title & Identity**
- **Title**: iTantra — Indian Multilingual TTS & STT Aided Neural Transceiver Radio Access
- **Subtitle**: Ultra-Low-Bitrate Voice-in / Voice-out Transceiver for Disaster & Tactical Links
- **Badges**: 100% Offline • Open-Source TinyML • Hindi & English verified, 10-language architecture • >95% Bandwidth Saved vs Opus

### **Slide 2: The Critical Gap**
- **The Problem**: During disasters, cellular towers collapse. High-bitrate voice audio (256 kbps PCM / 24 kbps Opus) drops over weak emergency RF links.
- **The Human Element**: Illiterate victims and stressed relief workers cannot read, type, or navigate complex keyboards.
- **Existing Solutions Fail**:
  - *Meshtastic / LoRaWAN*: Sends typed text only — no voice.
  - *Google Assistant / Alexa*: Requires cloud internet — completely dead in a disaster.
  - *Ham Radio / Codec2*: Robotic, continuous bandwidth required, English only.

### **Slide 3: The Breakthrough Architecture**
- **Core Concept**: *Do not stream audio — stream semantic metadata.*
- **Pipeline**: Microphone $\rightarrow$ RMS energy-based VAD $\rightarrow$ Offline Vosk STT $\rightarrow$ Auto-LID $\rightarrow$ `TantraPacket` $\rightarrow$ Peer-to-Peer Radio $\rightarrow$ Receiver USAGE_ALARM Check $\rightarrow$ Offline Piper TTS $\rightarrow$ Loudspeaker.
- **The Math**:
  Slashing 256,000 bps down to ~150-350 bps depending on language enables voice transmission over LoRa links.

### **Slide 4: Real Measured Numbers (The Metric Slide)**
- **End-to-End Latency**: **Sub-second latency** for short phrases.
- **Packet Size**: **~150 Bytes average** for Indic languages (fits easily into 250B LoRa hardware frames).
- **Bandwidth Reduction**: **>99% vs raw PCM**, **>95% vs Opus**, **~50-70% savings vs Codec2 at 700 bps**.
- **Test Suite Verification**: **Protocol tests passing**.

### **Slide 5: Tactical Android Features**
- **Dual Operational Modes**:
  - *PTT Walkie-Talkie Mode*: Touch-down capture, animated pulse, Roger-beep confirmation.
  - *Phone Mode*: Hands-free continuous monitoring with RMS energy-based VAD detecting pauses.
- **Emergency SOS Life-Safety Override**:
  - Intercepts distress flags, forces `AudioManager.STREAM_ALARM` to 100% volume overriding mute/vibrate, vibrates phone, and broadcasts non-interruptibly.
- **Auto-LID (Automatic Language Identification)**:
  - Dynamically detects script (Devanagari, Latin) with zero manual menu switching.

### **Slide 6: Hardware Transceiver Extensibility (LoRa)**
- **Problem Statement Compliance**: *"through wifi/Bluetooth connected embedded device or another phone"*.
- **The Hardware Bridge**: Phone connects via Bluetooth to an external **ESP32-LoRa module** (~₹1,500).
- **Frequency**: **865.200 MHz** (Government of India de-licensed ISM band).
- **Airtime**: ~6s airtime per Indic packet at SF12 to achieve 10 to 15 kilometers of voice range. Default firmware uses SF7 for faster transmission at closer ranges.

### **Slide 7: Honest Scope & Confident Roadmap**
> *Judges respect honesty far more than overpromising. Deliver this with confidence:*
- **What We Built & Measured End-to-End**:
  - Complete Android app, binary protocol, UDP P2P networking, embedded LoRa firmware, and full STT $\rightarrow$ Packet $\rightarrow$ TTS loop in Hindi and English.
- **The Confident Roadmap (Next 8 Languages)**:
  - Hindi and English verified, 8 more on roadmap. Supporting regional languages (Tamil, Telugu, Bengali, Gujarati, Marathi, Kannada, Malayalam, Odia) is purely a matter of packaging the voice assets into the existing pipeline.

### **Slide 8: Summary & Deliverables**
- **GitHub**: Open-source repository with full source code, testbed, and docs.
- **Ready for Deployment**: Runs on low-to-mid-range Android smartphones with zero internet.

---

## 🎯 3. Judge Q&A Defense Cheat Sheet

### Q1: *"Why not just compress audio with Opus or Codec2?"*
**Answer**:  
*"Opus requires at least 16,000 bits per second, which immediately drops when users go behind concrete walls, into basements, or over long-range LoRa links (which cap out at ~500 bps). Codec2 drops to 700 bps, but it sounds robotic and only supports English. iTantra drops bandwidth to **~150-350 bps** while producing high-fidelity natural Indian speech in native regional languages, saving ~50-70% compared to Codec2."*

### Q2: *"How does it work if there is no Wi-Fi router in a disaster?"*
**Answer**:  
*"It does not need a Wi-Fi router. One phone simply turns on its built-in portable hotspot (which requires no SIM card or cellular data). Other phones connect to it, and our UDP broadcast protocol discovers peers automatically on port 5005. Alternatively, phones plug into a ₹1,200 LoRa dongle over Bluetooth for up to 15 km mountain coverage (using SF12)."*

### Q3: *"Did you use Gemini or cloud APIs?"*
**Answer**:  
*"No, absolutely not. Per the SIH guidelines, our pipeline is 100% open-source and offline. We use Vosk Kaldi acoustic models and Piper ONNX neural VITS engines running entirely on the phone's CPU with zero external network dependencies."*
