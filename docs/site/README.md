# iTantra Showcase Website 🌐

### 100% Offline Tactical Multilingual Voice Transceiver
**Smart India Hackathon 2026 · ISRO Problem Statement 26173**

[![GitHub Release](https://img.shields.io/github/v/release/Naman225/iTantra?style=for-the-badge&logo=github)](https://github.com/Naman225/iTantra/releases)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue?style=for-the-badge)](https://opensource.org/licenses/Apache-2.0)
[![Direct APK](https://img.shields.io/badge/Download_APK-v1.0.0_(28_MB)-00C781?style=for-the-badge&logo=android)](https://github.com/Naman225/iTantra/releases/download/v1.0.0/iTantra-v1.0-release.apk)

---

## 📌 Overview

This repository contains the official landing page and technical architecture showcase for **iTantra** — an ultra-low-bitrate, 100% offline tactical neural voice transceiver built for disaster rescue, remote defense posts, and low-bandwidth radio channels (LoRa, UDP, Bluetooth RFCOMM).

### Key Features on the Site:
- 📻 **Interactive Tactical HUD**: Real-time simulation of incoming/outgoing `TantraPacket` frames with animated spectrum equalizer.
- 🧮 **Live Bandwidth Calculator**: Compare uncompressed 16kHz PCM (112 KB) vs. Opus VoIP (10.5 KB) vs. **iTantra (48 Bytes — 99.9% savings)** with dynamic LoRa transmission time estimation.
- 📡 **TantraPacket Protocol Specification**: Visual bit-level framing breakdown (8-byte binary envelope, CRC16 polynomial `0x1021`).
- 🇮🇳 **10 Indian Languages Supported**: Hindi, Tamil, Telugu, Bengali, Marathi, Gujarati, Kannada, Malayalam, Odia, Punjabi + English.
- 📱 **Judge Scan-to-Install Section**: Embedded QR code for instant APK installation onto evaluators' phones.

---

## 🚀 Quick Local Preview

You can preview this site locally using Python's built-in HTTP server:

```bash
cd /home/naman/Desktop/SIH/itantra-site
python3 -m http.server 8080
```

Then open `http://localhost:8080` in your web browser.

---

## 🚢 Deploying to Vercel / GitHub Pages

### Deploy via Vercel CLI (1-command):
```bash
cd /home/naman/Desktop/SIH/itantra-site
npx vercel --prod
```

### Deploy via GitHub Pages:
The site is also mirrored inside `Backup/docs/site/` in the main [iTantra Repository](https://github.com/Naman225/iTantra). You can enable GitHub Pages in repo settings pointing to `/docs/site`.

---

## 👤 Author & Team

- **Developer**: Naman Tiwari
- **GitHub**: [github.com/Naman225](https://github.com/Naman225)
- **Repository**: [github.com/Naman225/iTantra](https://github.com/Naman225/iTantra)
- **Email**: namantiwari2384@gmail.com
- **Event**: Smart India Hackathon 2026
- **Organization**: Indian Space Research Organisation (ISRO)
