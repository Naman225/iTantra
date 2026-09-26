# iTantra Step 1 Verification & Benchmark Report

**Status**: Models Verified Completely Offline (Off-Phone Verification)

## Executive Summary

- **Mean Word Error Rate (WER)**: 14.28%
- **TTS Real-Time Factor (RTF)**: 0.049 (20.2x faster than real-time)
- **STT Real-Time Factor (RTF)**: 0.163 (6.1x faster than real-time)
- **Average Packet Size**: 106.5 bytes per spoken sentence
- **Bandwidth Reduction vs Raw Audio**: **99.92%**
- **Bandwidth Reduction vs Opus Voice**: **98.63%**

## Detailed Test Matrix

| # | Lang | Priority | Audio (s) | TTS (s) [RTF] | STT (s) [RTF] | Total Lag (s) | Packet (B) | Bandwidth Saved | WER |
|---|---|---|---|---|---|---|---|---|---|
| 1 | EN | 🚨 SOS | 3.87 | 0.18 [0.05] | 0.68 [0.18] | 0.86 | 65 | 100.0% | 12.5% |
| 2 | EN | Radio | 4.10 | 0.19 [0.05] | 0.77 [0.19] | 0.96 | 72 | 100.0% | 18.2% |
| 3 | EN | 🚨 SOS | 3.95 | 0.18 [0.05] | 0.74 [0.19] | 0.93 | 72 | 99.9% | 25.0% |
| 4 | HI | 🚨 SOS | 4.25 | 0.26 [0.06] | 0.60 [0.14] | 0.87 | 136 | 99.9% | 30.0% |
| 5 | HI | Radio | 3.92 | 0.19 [0.05] | 0.52 [0.13] | 0.71 | 141 | 99.9% | 0.0% |
| 6 | HI | 🚨 SOS | 3.50 | 0.17 [0.05] | 0.52 [0.15] | 0.69 | 153 | 99.9% | 0.0% |

## Model Provenance
- **STT Engine**: Vosk offline small acoustic models (`vosk-model-small-hi-0.22`, `vosk-model-small-en-in-0.4`), Apache 2.0.
- **TTS Engine**: Piper neural VITS ONNX models (`hi_IN-pratham-medium`, `en_US-lessac-low`), MIT License.
