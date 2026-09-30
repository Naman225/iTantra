"""
iTantra Offline Text-To-Speech (TTS) Engine
Powered by Piper neural VITS ONNX models across 10 Indian Languages:
Hindi (hi), English (en), Bengali (bn), Gujarati (gu), Marathi (mr),
Kannada (kn), Malayalam (ml), Tamil (ta), Telugu (te), Odia (or).
Ultra-fast, low-latency, offline synthesis with automatic script routing.
"""

import time
import wave
import logging
from pathlib import Path
from typing import Tuple, Dict, Any, Optional
import piper

from core_engine.protocol.tantra_packet import detect_language_from_text

logger = logging.getLogger("iTantra.TTS")
MODELS_DIR = Path(__file__).resolve().parent.parent / "models" / "tts"

VOICE_CONFIGS = {
    "hi": {
        "model": "hi_IN-pratham-medium.onnx",
        "name": "Pratham (Hindi Medium)"
    },
    "en": {
        "model": "en_US-lessac-low.onnx",
        "name": "Lessac (English Low/Fast)"
    },
    "bn": {
        "model": "bn_IN-indic-medium.onnx",
        "name": "Indic Bengali (Medium)"
    },
    "gu": {
        "model": "gu_IN-indic-medium.onnx",
        "name": "Indic Gujarati (Medium)"
    },
    "mr": {
        "model": "mr_IN-indic-medium.onnx",
        "name": "Indic Marathi (Medium)"
    },
    "kn": {
        "model": "kn_IN-indic-medium.onnx",
        "name": "Indic Kannada (Medium)"
    },
    "ml": {
        "model": "ml_IN-indic-medium.onnx",
        "name": "Indic Malayalam (Medium)"
    },
    "ta": {
        "model": "ta_IN-indic-medium.onnx",
        "name": "Indic Tamil (Medium)"
    },
    "te": {
        "model": "te_IN-indic-medium.onnx",
        "name": "Indic Telugu (Medium)"
    },
    "or": {
        "model": "or_IN-indic-medium.onnx",
        "name": "Indic Odia (Medium)"
    }
}


class PiperOfflineTTS:
    def __init__(self, default_lang: str = "hi"):
        self.voices = {}
        self.current_lang = default_lang
        self.fallback_langs = set()
        self.voice_model_files = {}
        self.load_voice("en")
        self.load_voice("hi")

    def load_voice(self, lang: str):
        normalized_lang = lang.lower().strip()
        if normalized_lang not in VOICE_CONFIGS:
            normalized_lang = "hi"
        
        if normalized_lang in self.voices:
            self.current_lang = normalized_lang
            return self.voices[normalized_lang]

        model_file = MODELS_DIR / VOICE_CONFIGS[normalized_lang]["model"]
        if not model_file.exists():
            # Graceful fallback to Hindi or English if specific language model pack not installed yet
            fallback_file = MODELS_DIR / "hi_IN-pratham-medium.onnx"
            if not fallback_file.exists():
                fallback_file = MODELS_DIR / "en_US-lessac-low.onnx"
            if not fallback_file.exists():
                raise FileNotFoundError(f"No Piper TTS models found in {MODELS_DIR}")
            
            logger.warning(
                f"TTS Model for '{normalized_lang}' ({model_file.name}) not found. "
                f"Falling back to base neural voice: {fallback_file.name}"
            )
            model_file = fallback_file
            self.fallback_langs.add(normalized_lang)

        voice = piper.PiperVoice.load(str(model_file))
        self.voices[normalized_lang] = voice
        self.current_lang = normalized_lang
        self.voice_model_files[normalized_lang] = model_file
        return voice

    def synthesize(self, text: str, lang: Optional[str] = None, output_wav: Optional[str] = None) -> Dict[str, Any]:
        """
        Synthesizes text into raw 16-bit PCM audio.
        Automatically resolves language if set to 'auto' or checks script compatibility.
        """
        if lang is None or lang == "auto":
            target_lang = detect_language_from_text(text)
        else:
            # Script compatibility check: If text is predominantly Latin and lang was Indic, route to 'en'
            auto_detected = detect_language_from_text(text)
            if auto_detected == "en" and lang in ["hi", "bn", "gu", "mr", "kn", "ml", "ta", "te", "or"]:
                target_lang = "en"
            else:
                target_lang = lang

        if target_lang not in self.voices:
            self.load_voice(target_lang)

        voice = self.voices.get(target_lang, self.voices.get("hi", self.voices.get("en")))

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
            "pcm_bytes": pcm_bytes,
            "is_fallback": target_lang in self.fallback_langs,
            "actual_model": self.voice_model_files.get(target_lang, Path("unknown")).name
        }


if __name__ == "__main__":
    tts = PiperOfflineTTS(default_lang="hi")
    print(f"PiperOfflineTTS loaded with voices: {list(tts.voices.keys())}")
