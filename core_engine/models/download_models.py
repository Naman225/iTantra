#!/usr/bin/env python3
"""
iTantra - Model Download Utility
Downloads lightweight offline STT (Vosk) and TTS (Piper ONNX) models
for Hindi and Indian English.
"""

import os
import sys
import zipfile
import urllib.request
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
STT_DIR = BASE_DIR / "stt"
TTS_DIR = BASE_DIR / "tts"

MODELS = {
    "stt_hindi": {
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip",
        "dest_dir": STT_DIR,
        "extracted_name": "vosk-model-small-hi-0.22",
        "type": "zip"
    },
    "stt_en_in": {
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip",
        "dest_dir": STT_DIR,
        "extracted_name": "vosk-model-small-en-in-0.4",
        "type": "zip"
    },
    "tts_hindi_onnx": {
        "url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/hi/hi_IN/pratham/medium/hi_IN-pratham-medium.onnx",
        "dest_file": TTS_DIR / "hi_IN-pratham-medium.onnx",
        "type": "file"
    },
    "tts_hindi_json": {
        "url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/hi/hi_IN/pratham/medium/hi_IN-pratham-medium.onnx.json",
        "dest_file": TTS_DIR / "hi_IN-pratham-medium.onnx.json",
        "type": "file"
    },
    "tts_english_onnx": {
        "url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/low/en_US-lessac-low.onnx",
        "dest_file": TTS_DIR / "en_US-lessac-low.onnx",
        "type": "file"
    },
    "tts_english_json": {
        "url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/low/en_US-lessac-low.onnx.json",
        "dest_file": TTS_DIR / "en_US-lessac-low.onnx.json",
        "type": "file"
    }
}

def download_file(url: str, target_path: Path):
    if target_path.exists() and target_path.stat().st_size > 0:
        print(f"[OK] File already exists: {target_path.name} ({target_path.stat().st_size / 1024 / 1024:.2f} MB)")
        return
    print(f"[DOWNLOADING] {url} -> {target_path.name} ...")
    target_path.parent.mkdir(parents=True, exist_ok=True)
    
    def reporthook(count, block_size, total_size):
        if total_size > 0:
            percent = min(int(count * block_size * 100 / total_size), 100)
            sys.stdout.write(f"\r  Progress: {percent}% ({count * block_size / 1024 / 1024:.1f}/{total_size / 1024 / 1024:.1f} MB)")
            sys.stdout.flush()

    urllib.request.urlretrieve(url, str(target_path), reporthook=reporthook)
    print(f"\n[DONE] Saved {target_path.name}")

def extract_zip(zip_path: Path, extract_to: Path):
    print(f"[EXTRACTING] {zip_path.name} to {extract_to} ...")
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        zip_ref.extractall(extract_to)
    print(f"[DONE] Extracted {zip_path.name}")

def main():
    STT_DIR.mkdir(parents=True, exist_ok=True)
    TTS_DIR.mkdir(parents=True, exist_ok=True)

    print("=== iTantra Offline Model Setup ===")
    
    # 1. Download Vosk STT models
    for key in ["stt_hindi", "stt_en_in"]:
        cfg = MODELS[key]
        extracted_folder = cfg["dest_dir"] / cfg["extracted_name"]
        if extracted_folder.exists():
            print(f"[OK] Extracted model folder already exists: {extracted_folder.name}")
            continue
        zip_file = cfg["dest_dir"] / Path(cfg["url"]).name
        download_file(cfg["url"], zip_file)
        extract_zip(zip_file, cfg["dest_dir"])
        if zip_file.exists():
            zip_file.unlink()

    # 2. Download Piper TTS models
    for key in ["tts_hindi_onnx", "tts_hindi_json", "tts_english_onnx", "tts_english_json"]:
        cfg = MODELS[key]
        download_file(cfg["url"], cfg["dest_file"])

    print("\n[SUCCESS] All Step 1 models downloaded and verified!")
    print(f"\nSTT Models in: {STT_DIR}")
    for item in sorted(STT_DIR.iterdir()):
        print(f" - {item.name}")
    print(f"\nTTS Models in: {TTS_DIR}")
    for item in sorted(TTS_DIR.iterdir()):
        size_mb = item.stat().st_size / 1024 / 1024
        print(f" - {item.name} ({size_mb:.2f} MB)")

if __name__ == "__main__":
    main()
