# iTantra 10-Language Verification & Benchmark Report

**Status**: 2/10 Languages Verified Offline

## Executive Summary

- **Languages Evaluated (2/10)**
- **Mean Word Error Rate (WER)**: 5.00%
- **STT Real-Time Factor (RTF)**: 0.206 (4.9x faster than real-time)
- **Average Packet Size**: 105.0 bytes per spoken transmission
- **Bandwidth Reduction vs Raw Audio (PCM 16kHz)**: **99.91%**
- **Bandwidth Reduction vs Opus Voice (24 kbps)**: **98.69%**

## Detailed 10-Language Test Matrix

| # | Language | Priority | Source | Audio (s) | STT Lag (s) [RTF] | Total Lag (s) | Packet (B) | Band Saved | WER | CER |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | **English** (`en`) | 🚨 SOS | Real | 3.79 | 0.81 [0.21] | 0.81 | 67 | 99.9% | 0.0% | N/A |
| 2 | **Hindi** (`hi`) | 🚨 SOS | Real | 4.12 | 0.82 [0.20] | 0.81 | 143 | 99.9% | 10.0% | 2.4% |
| 3 | **Bengali** (`bn`) | 🚨 SOS | SKIPPED | - | - | - | - | - | - | - |
| 4 | **Gujarati** (`gu`) | 🚨 SOS | SKIPPED | - | - | - | - | - | - | - |
| 5 | **Marathi** (`mr`) | Radio | SKIPPED | - | - | - | - | - | - | - |
| 6 | **Kannada** (`kn`) | 🚨 SOS | SKIPPED | - | - | - | - | - | - | - |
| 7 | **Malayalam** (`ml`) | 🚨 SOS | SKIPPED | - | - | - | - | - | - | - |
| 8 | **Tamil** (`ta`) | 🚨 SOS | SKIPPED | - | - | - | - | - | - | - |
| 9 | **Telugu** (`te`) | Radio | SKIPPED | - | - | - | - | - | - | - |
| 10 | **Odia** (`or`) | 🚨 SOS | SKIPPED | - | - | - | - | - | - | - |

## Model Provenance & Status
| Language | STT Model | TTS Model | Status |
|---|---|---|---|
| English | `vosk-model-small-en-in-0.4` | `en_US-lessac-low.onnx` | ✅ Loaded |
| Hindi | `vosk-model-small-hi-0.22` | `hi_IN-pratham-medium.onnx` | ✅ Loaded |
| Bengali | `Missing` | `Missing` | ❌ Missing |
| Gujarati | `Missing` | `Missing` | ❌ Missing |
| Marathi | `Missing` | `Missing` | ❌ Missing |
| Kannada | `Missing` | `Missing` | ❌ Missing |
| Malayalam | `Missing` | `Missing` | ❌ Missing |
| Tamil | `Missing` | `Missing` | ❌ Missing |
| Telugu | `Missing` | `Missing` | ❌ Missing |
| Odia | `Missing` | `Missing` | ❌ Missing |
