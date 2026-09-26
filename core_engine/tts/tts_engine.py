"""
iTantra Offline Text-To-Speech (TTS) Engine
Powered by Piper neural VITS ONNX models for Hindi and English.
Ultra-fast, low-latency, offline synthesis with automatic script routing.
"""

import time
import wave
from pathlib import Path
from typing import Tuple, Dict, Any, Optional
import piper

from core_engine.protocol.tantra_packet import detect_language_from_text

MODELS_DIR = Path(__file__).resolve().parent.parent / "models" / "tts"

VOICE_CONFIGS = {
    "hi": {
        "model": "hi_IN-pratham-medium.onnx",
        "name": "Pratham (Hindi Medium)"
    },
    "en": {
        "model": "en_US-lessac-low.onnx",
        "name": "Lessac (English Low/Fast)"
    }
}

class PiperOfflineTTS:
    def __init__(self, default_lang: str = "hi"):
        self.voices = {}
        self.current_lang = default_lang
        self.load_voice("en")
        self.load_voice("hi")

    def load_voice(self, lang: str):
        if lang not in VOICE_CONFIGS:
            raise ValueError(f"Language '{lang}' not in supported TTS voices: {list(VOICE_CONFIGS.keys())}")
        
        if lang in self.voices:
            self.current_lang = lang
            return self.voices[lang]

        model_file = MODELS_DIR / VOICE_CONFIGS[lang]["model"]
        if not model_file.exists():
            raise FileNotFoundError(f"Model file not found: {model_file}")

        voice = piper.PiperVoice.load(str(model_file))
        self.voices[lang] = voice
        self.current_lang = lang
        return voice

    def synthesize(self, text: str, lang: Optional[str] = None, output_wav: Optional[str] = None) -> Dict[str, Any]:
        """
        Synthesizes text into raw 16-bit PCM audio.
        Automatically resolves language if set to 'auto' or checks script compatibility.
        """
        if lang is None or lang == "auto":
            target_lang = detect_language_from_text(text)
        else:
            # Script compatibility check: If text is predominantly Latin and lang was 'hi', route to 'en'
            auto_detected = detect_language_from_text(text)
            if auto_detected == "en" and lang == "hi":
                target_lang = "en"
            else:
                target_lang = lang

        if target_lang not in self.voices:
            if target_lang in VOICE_CONFIGS:
                self.load_voice(target_lang)
            else:
                target_lang = "hi"  # fallback

        voice = self.voices[target_lang]

        t0 = time.time()
        pcm_bytes = b""
        for chunk in voice.synthesize(text):
            pcm_bytes += chunk.audio_int16_bytes
        synthesis_time = time.time() - t0

        sample_rate = voice.config.sample_rate
        audio_duration = len(pcm_bytes) / (sample_rate * 2)
        rtf = synthesis_time / max(audio_duration, 0.001)

        if output_wav:
            out_path = Path(output_wav)
            out_path.parent.mkdir(parents=True, exist_ok=True)
            with wave.open(str(out_path), "wb") as wf:
                wf.setnchannels(1)
                wf.setsampwidth(2)
                wf.setframerate(sample_rate)
                wf.writeframes(pcm_bytes)

        return {
            "text": text,
            "lang": target_lang,
            "sample_rate": sample_rate,
            "audio_bytes_len": len(pcm_bytes),
            "audio_duration_sec": round(audio_duration, 3),
            "synthesis_time_sec": round(synthesis_time, 4),
            "rtf": round(rtf, 4),
            "pcm_bytes": pcm_bytes
        }
