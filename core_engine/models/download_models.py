#!/usr/bin/env python3
"""
iTantra - Multilingual Offline Model Download Utility
Downloads lightweight offline STT (Vosk) and TTS (Piper ONNX) models
for all 10 Indian languages mandated by SIH & ISRO:
Hindi (hi), English (en), Bengali (bn), Gujarati (gu), Marathi (mr),
Kannada (kn), Malayalam (ml), Tamil (ta), Telugu (te), Odia (or).
"""

import os
import sys
import argparse
import zipfile
import urllib.request
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
STT_DIR = BASE_DIR / "stt"
TTS_DIR = BASE_DIR / "tts"

# All 10 Supported Languages Map
ALL_LANGUAGES = ["hi", "en", "bn", "gu", "mr", "kn", "ml", "ta", "te", "or"]

VOSK_STT_MODELS = {
    "hi": {
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip",
        "extracted_name": "vosk-model-small-hi-0.22",
        "name": "Hindi (small-hi-0.22)"
    },
    "en": {
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip",
        "extracted_name": "vosk-model-small-en-in-0.4",
        "name": "Indian English (small-en-in-0.4)"
    },
    "bn": {
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-bn-0.4.zip",
        "extracted_name": "vosk-model-small-bn-0.4",
        "name": "Bengali (small-bn-0.4)"
    },
    "gu": {
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-gu-0.42.zip",
        "extracted_name": "vosk-model-small-gu-0.42",
        "name": "Gujarati (small-gu-0.42)"
    },
    "mr": {
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-mr-0.4.zip",
        "extracted_name": "vosk-model-small-mr-0.4",
        "name": "Marathi (small-mr-0.4)"
    },
    "kn": {
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-kn-0.4.zip",
        "extracted_name": "vosk-model-small-kn-0.4",
        "name": "Kannada (small-kn-0.4)"
    },
    "ml": {
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-ml-0.4.zip",
        "extracted_name": "vosk-model-small-ml-0.4",
        "name": "Malayalam (small-ml-0.4)"
    },
    "ta": {
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-ta-0.4.zip",
        "extracted_name": "vosk-model-small-ta-0.4",
        "name": "Tamil (small-ta-0.4)"
    },
    "te": {
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-te-0.42.zip",
        "extracted_name": "vosk-model-small-te-0.42",
        "name": "Telugu (small-te-0.42)"
    },
    "or": {
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-or-0.4.zip",
        "extracted_name": "vosk-model-small-or-0.4",
        "name": "Odia (small-or-0.4)"
    }
}

PIPER_TTS_MODELS = {
    "hi": {
        "onnx_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/hi/hi_IN/pratham/medium/hi_IN-pratham-medium.onnx",
        "json_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/hi/hi_IN/pratham/medium/hi_IN-pratham-medium.onnx.json",
        "onnx_file": "hi_IN-pratham-medium.onnx",
        "json_file": "hi_IN-pratham-medium.onnx.json",
        "name": "Hindi Pratham Medium"
    },
    "en": {
        "onnx_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/low/en_US-lessac-low.onnx",
        "json_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/low/en_US-lessac-low.onnx.json",
        "onnx_file": "en_US-lessac-low.onnx",
        "json_file": "en_US-lessac-low.onnx.json",
        "name": "English Lessac Low"
    },
    "bn": {
        "onnx_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/bn/bn_IN/indic/medium/bn_IN-indic-medium.onnx",
        "json_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/bn/bn_IN/indic/medium/bn_IN-indic-medium.onnx.json",
        "onnx_file": "bn_IN-indic-medium.onnx",
        "json_file": "bn_IN-indic-medium.onnx.json",
        "name": "Bengali Indic Medium"
    },
    "gu": {
        "onnx_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/gu/gu_IN/indic/medium/gu_IN-indic-medium.onnx",
        "json_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/gu/gu_IN/indic/medium/gu_IN-indic-medium.onnx.json",
        "onnx_file": "gu_IN-indic-medium.onnx",
        "json_file": "gu_IN-indic-medium.onnx.json",
        "name": "Gujarati Indic Medium"
    },
    "mr": {
        "onnx_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/mr/mr_IN/indic/medium/mr_IN-indic-medium.onnx",
        "json_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/mr/mr_IN/indic/medium/mr_IN-indic-medium.onnx.json",
        "onnx_file": "mr_IN-indic-medium.onnx",
        "json_file": "mr_IN-indic-medium.onnx.json",
        "name": "Marathi Indic Medium"
    },
    "kn": {
        "onnx_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/kn/kn_IN/indic/medium/kn_IN-indic-medium.onnx",
        "json_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/kn/kn_IN/indic/medium/kn_IN-indic-medium.onnx.json",
        "onnx_file": "kn_IN-indic-medium.onnx",
        "json_file": "kn_IN-indic-medium.onnx.json",
        "name": "Kannada Indic Medium"
    },
    "ml": {
        "onnx_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/ml/ml_IN/arjun/medium/ml_IN-arjun-medium.onnx",
        "json_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/ml/ml_IN/arjun/medium/ml_IN-arjun-medium.onnx.json",
        "onnx_file": "ml_IN-arjun-medium.onnx",
        "json_file": "ml_IN-arjun-medium.onnx.json",
        "name": "Malayalam Arjun Medium"
    },
    "ta": {
        "onnx_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/ta/ta_IN/indic/medium/ta_IN-indic-medium.onnx",
        "json_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/ta/ta_IN/indic/medium/ta_IN-indic-medium.onnx.json",
        "onnx_file": "ta_IN-indic-medium.onnx",
        "json_file": "ta_IN-indic-medium.onnx.json",
        "name": "Tamil Indic Medium"
    },
    "te": {
        "onnx_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/te/te_IN/indic/medium/te_IN-indic-medium.onnx",
        "json_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/te/te_IN/indic/medium/te_IN-indic-medium.onnx.json",
        "onnx_file": "te_IN-indic-medium.onnx",
        "json_file": "te_IN-indic-medium.onnx.json",
        "name": "Telugu Indic Medium"
    },
    "or": {
        "onnx_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/or/or_IN/indic/medium/or_IN-indic-medium.onnx",
        "json_url": "https://huggingface.co/rhasspy/piper-voices/resolve/main/or/or_IN/indic/medium/or_IN-indic-medium.onnx.json",
        "onnx_file": "or_IN-indic-medium.onnx",
        "json_file": "or_IN-indic-medium.onnx.json",
        "name": "Odia Indic Medium"
    }
}

def download_file(url: str, target_path: Path):
    if target_path.exists() and target_path.stat().st_size > 0:
        print(f"[OK] File already exists: {target_path.name} ({target_path.stat().st_size / 1024 / 1024:.2f} MB)")
        return True
    print(f"[DOWNLOADING] {url} -> {target_path.name} ...")
    target_path.parent.mkdir(parents=True, exist_ok=True)
    
    def reporthook(count, block_size, total_size):
        if total_size > 0:
            percent = min(int(count * block_size * 100 / total_size), 100)
            sys.stdout.write(f"\r  Progress: {percent}% ({count * block_size / 1024 / 1024:.1f}/{total_size / 1024 / 1024:.1f} MB)")
            sys.stdout.flush()

    try:
        urllib.request.urlretrieve(url, str(target_path), reporthook=reporthook)
        print(f"\n[DONE] Saved {target_path.name}")
        return True
    except Exception as e:
        print(f"\n[WARN] Could not download {url}: {e}")
        if target_path.exists():
            target_path.unlink()
        return False

def extract_zip(zip_path: Path, extract_to: Path):
    print(f"[EXTRACTING] {zip_path.name} to {extract_to} ...")
    try:
        with zipfile.ZipFile(zip_path, 'r') as zip_ref:
            zip_ref.extractall(extract_to)
        print(f"[DONE] Extracted {zip_path.name}")
        return True
    except Exception as e:
        print(f"[ERROR] Extraction failed for {zip_path.name}: {e}")
        return False

def setup_languages(languages: list):
    STT_DIR.mkdir(parents=True, exist_ok=True)
    TTS_DIR.mkdir(parents=True, exist_ok=True)

    print("=" * 70)
    print(f"  iTantra 10-Language Model Setup: {', '.join(languages).upper()}")
    print("=" * 70)

    # 1. Download Vosk STT models
    print("\n--- 1. Checking / Downloading Vosk STT Models ---")
    for lang in languages:
        if lang not in VOSK_STT_MODELS:
            continue
        cfg = VOSK_STT_MODELS[lang]
        extracted_folder = STT_DIR / cfg["extracted_name"]
        if extracted_folder.exists():
            print(f"[OK] Vosk STT for {lang.upper()} ({cfg['name']}) already installed at {extracted_folder.name}")
            continue

        zip_file = STT_DIR / Path(cfg["url"]).name
        success = download_file(cfg["url"], zip_file)
        if success and zip_file.exists():
            extract_zip(zip_file, STT_DIR)
            zip_file.unlink()

    # 2. Download Piper TTS models
    print("\n--- 2. Checking / Downloading Piper TTS Models ---")
    for lang in languages:
        if lang not in PIPER_TTS_MODELS:
            continue
        cfg = PIPER_TTS_MODELS[lang]
        onnx_path = TTS_DIR / cfg["onnx_file"]
        json_path = TTS_DIR / cfg["json_file"]
        
        if onnx_path.exists() and json_path.exists():
            print(f"[OK] Piper TTS for {lang.upper()} ({cfg['name']}) already installed.")
            continue

        download_file(cfg["onnx_url"], onnx_path)
        download_file(cfg["json_url"], json_path)

    print("\n[SUCCESS] Model check completed!")
    print(f"\nSTT Models in: {STT_DIR}")
    for item in sorted(STT_DIR.iterdir()):
        print(f" - {item.name}")
    print(f"\nTTS Models in: {TTS_DIR}")
    for item in sorted(TTS_DIR.iterdir()):
        size_mb = item.stat().st_size / 1024 / 1024
        print(f" - {item.name} ({size_mb:.2f} MB)")

def main():
    parser = argparse.ArgumentParser(description="iTantra Multilingual Offline Model Setup")
    parser.add_argument(
        "--lang",
        type=str,
        default="hi,en",
        help="Comma-separated language codes to download (e.g. 'hi,en,ta,te') or 'all' for all 10 languages."
    )
    args = parser.parse_args()

    if args.lang.strip().lower() == "all":
        languages = ALL_LANGUAGES
    else:
        languages = [l.strip().lower() for l in args.lang.split(",") if l.strip().lower() in ALL_LANGUAGES]

    setup_languages(languages)

if __name__ == "__main__":
    main()
