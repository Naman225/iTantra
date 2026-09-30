# 🎙️ iTantra — SIH 2026 Presentation & Live Prototype Demo Script
### Designed for a 6-Member Team | Duration: 5 to 7 Minutes

---

## 👥 Team Roles Breakdown

| Member | Role | Responsibility |
| :--- | :--- | :--- |
| **Speaker 1** | **Team Lead / Pitch Master** | Hook the judges, introduce Problem Statement, high-level impact. |
| **Speaker 2** | **Technical Lead (Architecture)** | Explain the breakthrough concept, bandwidth reduction, pipeline. |
| **Speaker 3** | **Live Prototype Transmitter (Phone A)** | Demonstrates Transmitter PTT, Hindi/English speech input, hazard keywords. |
| **Speaker 4** | **Live Prototype Receiver (Phone B)** | Holds Receiver Phone B up to microphone, shows instant audio playback & VAD. |
| **Speaker 5** | **Mesh & Radio Specialist** | Explains multi-bearer radio (Wi-Fi, Bluetooth RFCOMM, LoRa), 3-tier priority (SOS). |
| **Speaker 6** | **Feasibility, Scalability & Conclusion** | Benchmark metrics, ₹1,200 hardware viability, vision & Q&A handoff. |

---

## ⏱️ Pre-Demo Setup Checklist (2 Minutes Before Pitch)
1. **Device A (Transmitter)** and **Device B (Receiver)** are powered on and connected to the same portable Hotspot (no internet needed!).
2. Ensure both phones have **Media Volume turned to MAX**.
3. Have **Phone A set to Hindi** and **Phone B ready in Call/VAD mode**.
4. Test one quick roger beep before entering the room.

---

## 🎬 Minute-by-Minute Script

---

### [0:00 – 1:00] 📢 Slide 1: The Problem & The Hook
**Speaker 1 (Team Lead)**:
> *"Respected Judges, imagine a severe cyclone strikes coastal Odisha, or a massive landslide hits Uttarakhand.*  
> *Within minutes, power cuts off, telecom towers collapse, and cellular networks go pitch black.*  
> 
> *Traditional emergency radios try to stream human voice, but voice audio demands **256,000 bits per second (256 kbps)**. Over weak emergency channels or low-power LoRa, voice audio chokes and dies.*  
> 
> *Meanwhile, text messages don't work for panicked victims trapped under debris or field personnel wearing gloves. They need to **speak**, and they need to **listen**.*  
> 
> *We are Team **Suraksha Sonic**, and we present **iTantra: Indian Multilingual TTS & STT Aided Neural Transceiver Radio Access**."*

---

### [1:00 – 2:00] 🧠 Slide 2: The Breakthrough & Protocol
**Speaker 2 (Technical Architect)**:
> *"Our core breakthrough is simple yet revolutionary:*  
> ***Send microscopic text tokens across the air, but let humans speak and listen.***  
> 
> *Instead of pushing massive audio files over constrained airwaves:*  
> 1. *Our on-device AI listens to the speaker and converts speech to text locally using quantized Vosk engines.*  
> 2. *We pack this text and priority flags into an ultra-compact binary frame we engineered called **TantraPacket**.*  
> 3. *This reduces bandwidth by **>99%** vs raw PCM — slashing bandwidth from 256 kbps down to **~150 to 350 bits per second**!*  
> 4. *On the other end, the receiver reconstructs natural spoken voice instantly using our embedded neural TTS engine.*  
> 
> *And the best part? It is **100% offline — zero internet, zero cloud, zero telecom dependencies**."*

---

### [2:00 – 3:45] 🔥 LIVE HARDWARE DEMO (The Centerpiece of Your Win!)
*(Speaker 3 and Speaker 4 step forward holding Phone A and Phone B in clear view of the judges)*

**Speaker 3 (Transmitter)**:
> *"Judges, seeing is believing. We have two physical commercial smartphones here, completely in **Airplane Mode with Mobile Data and Internet turned OFF**. They are connected purely via local peer-to-peer radio."*

#### Demo Part 1: Walkie-Talkie (PTT) with Hindi Speech
*(Speaker 3 presses and holds the blue 'Hold to Talk' button on Phone A)*
- **Speaker 3 Speaks into Phone A**:  
  > 🗣️ *"सहायता दल, हम सुरक्षित स्थान पर पहुंच गए हैं।"* *(Rescue team, we have reached safe ground)*
*(Speaker 3 releases the button. Phone B speaks it out loud).*
- **Speaker 4 (Receiver)**:  
  *(Holds Phone B close to the presentation mic so the whole room hears the synthesized Hindi voice loud and clear)*  
  > 🔊 **Phone B speaks**: *"सहायता दल, हम सुरक्षित स्थान पर पहुंच गए हैं।"*  
  > 
  > *"Look at Phone B's screen: The message arrived with sub-second latency, decoded from a **~150-byte packet**, and synthesized into natural Hindi voice with the transcript displayed on the live feed."*

#### Demo Part 2: Multilingual Support (English)
**Speaker 3**:
> *"Now, let's switch language with one tap to **English**."*  
*(Speaker 3 changes dropdown to English and speaks)*:  
> 🗣️ *"Camp alpha, report situation immediately."*  
*(Phone B instantly receives the packet and synthesizes it out loud).*

#### Demo Part 3: 3-Tier Priority & Emergency SOS Override
**Speaker 3**:
> *"iTantra also features an intelligent **3-Tier Priority System**: Normal, Yellow Alert, and Red SOS.*  
> *If a responder speaks the word **'khatra'** or **'danger'**, our on-device classifier automatically promotes the packet to **Yellow Alert**.*  
> *And if I press this red **SOS Distress** button..."*  
*(Speaker 3 taps the SOS button on Phone A)*  
*(Phone B immediately overrides mute/volume and sounds a high-priority emergency alarm buzzer and displays a glowing Red SOS banner).*  
- **Speaker 4**:  
  > *"Even if the receiving responder's phone was muted or handling another call, the SOS beacon interrupts everything with top-priority tactical override."*

#### Demo Part 4: Hands-Free Phone Call Mode (VAD)
**Speaker 4 (Receiver)**:
> *"For field medics or drivers who cannot hold a button, we built **Phone Call (VAD) Mode**. In this mode, the phone uses RMS energy-based Voice Activity Detection to continuously detect voice activity and automatically transmits whenever they speak — exactly like a phone call, but running over offline mesh!"*

---

### [3:45 – 4:45] 📡 Slide 3: Multi-Bearer Radio Mesh & Security
**Speaker 5 (Mesh & Radio Specialist)**:
> *"How do these packets travel across devastated terrain?*  
> *iTantra is **multi-bearer and hardware-agnostic**:*  
> 1. ***Wi-Fi Direct & Hotspot**: Broadcasts UDP packets on port 5005 for immediate 50–100 meter command post coverage.*  
> 2. ***Bluetooth RFCOMM Serial**: Seamless phone-to-phone short-range tactical pairing.*  
> 3. ***Sub-GHz LoRa (SX1262 at 865 MHz)**: Because our packets are small, they easily fit into LoRa frames. At SF12, they can deliver **10 to 15 kilometers of voice reach** over rugged mountains and rubble, with a ~6s airtime per Indic packet.*  
> 
> *Every frame is protected with **CRC-16 CCITT integrity checks** and **HMAC authentication** to ensure the security of tactical communication."*

---

### [4:45 – 5:45] 📊 Slide 4: Feasibility, Hardware Viability & Costs
**Speaker 6 (Feasibility & Scalability)**:
> *"From a viability standpoint, iTantra is deployment-ready:*  
> - ***Sub-Second Latency**: Our lightweight TTS and STT engines run locally on standard mobile ARM CPUs.*  
> - ***Minimal Memory**: Consumes **less than 30 MB RAM**, meaning it runs smoothly on ₹6,000 budget Android smartphones without heating up.*  
> - ***Zero Infrastructure Cost**: No cloud servers, no recurring API bills, and field LoRa relay nodes cost less than **₹1,200 ($15)** using commodity ESP32 hardware.*  
> 
> *This makes iTantra immediately scalable for the **NDRF, SDRF, Indian Army, Forest Rangers, and fishermen at sea**."*

---

### [5:45 – 6:15] 🏆 Conclusion & Q&A Opening
**Speaker 1 (Team Lead)**:
> *"To conclude:*  
> *In disasters, when communication lines die, people die.*  
> *iTantra restores the voice lifeline when everything else fails. It bridges language barriers, saves >99% bandwidth vs raw PCM, and works 100% offline.*  
> 
> *Our prototype is live, verified, and open-source on GitHub.*  
> *Thank you, Judges. We are now open to your questions!"*

---

## 🛡️ Quick Answers to Likely Judge Questions

1. **Judge: "What if there is heavy background noise (floodwater, rain, wind)?"**
   - **Speaker 2/3**: *"We implement a 300Hz–3.4kHz spectral bandpass filter combined with RMS energy-based VAD gating, eliminating background ambient noise before passing audio to the STT model."*

2. **Judge: "How does this compare to satellite phones?"**
   - **Speaker 5/6**: *"Satellite phones cost ₹40,000 to ₹1,00,000+ per handset, require recurring satellite subscription plans, and don't work indoors or under thick forest canopies. iTantra runs on ordinary existing smartphones and ₹1,200 LoRa modules with zero operational cost."*

3. **Judge: "Can this work across different Indian accents and dialects?"**
   - **Speaker 2**: *"Yes! Vosk Kaldi acoustic models are trained on diverse regional speech corpora, and our architecture supports drop-in language packs."*
