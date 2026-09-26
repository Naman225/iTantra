"""
iTantra Offline Speech-To-Text (STT) Engine
Powered by Vosk lightweight acoustic models for Hindi and Indian English.
Completely offline, low-memory footprint, Android-ready.
"""

import json
import time
import wave
from pathlib import Path
from typing import Dict, Any, Optional
import vosk

MODELS_DIR = Path(__file__).resolve().parent.parent / "models" / "stt"

class VoskOfflineSTT:
    def __init__(self, lang: str = "hi"):
        """
        Initializes the offline STT engine for the given language ('hi' or 'en').
        """
        self.lang = lang
        self.model = None
        self.load_model(lang)

    def load_model(self, lang: str):
        self.lang = lang
        if lang in ["hi", "hindi"]:
            model_path = MODELS_DIR / "vosk-model-small-hi-0.22"
        elif lang in ["en", "english", "en-in", "en_in"]:
            model_path = MODELS_DIR / "vosk-model-small-en-in-0.4"
        else:
            raise ValueError(f"Unsupported language: {lang}. Must be 'hi' or 'en'.")

        if not model_path.exists():
            raise FileNotFoundError(f"Vosk model not found at {model_path}. Run download_models.py first.")

        # Suppress verbose Vosk logging
        vosk.SetLogLevel(-1)
        start_time = time.time()
        self.model = vosk.Model(str(model_path))
        load_time = time.time() - start_time
        self.current_model_path = model_path
        return load_time

    def transcribe_wav(self, wav_path: str) -> Dict[str, Any]:
        """
        Transcribes a 16kHz mono WAV file and computes latency & RTF metrics.
        """
        wf = wave.open(str(wav_path), "rb")
        if wf.getnchannels() != 1 or wf.getsampwidth() != 2 or wf.getframerate() != 16000:
            wf.close()
            raise ValueError("Audio must be WAV format mono PCM, 16000Hz, 16-bit.")

        audio_frames = wf.getnframes()
        sample_rate = wf.getframerate()
        audio_duration = audio_frames / float(sample_rate)

        rec = vosk.KaldiRecognizer(self.model, sample_rate)
        rec.SetWords(True)

        start_time = time.time()
        while True:
            data = wf.readframes(4000)
            if len(data) == 0:
                break
            rec.AcceptWaveform(data)

        res = json.loads(rec.FinalResult())
        inference_time = time.time() - start_time
        wf.close()

        transcript = res.get("text", "").strip()
        rtf = inference_time / max(audio_duration, 0.001)

        return {
            "transcript": transcript,
            "lang": self.lang,
            "audio_duration_sec": round(audio_duration, 3),
            "inference_time_sec": round(inference_time, 4),
            "rtf": round(rtf, 4),
            "words": res.get("result", [])
        }

    def transcribe_stream(self, pcm_bytes: bytes, sample_rate: int = 16000) -> Dict[str, Any]:
        """
        Transcribes raw 16kHz 16-bit PCM bytes (from microphone buffer).
        """
        rec = vosk.KaldiRecognizer(self.model, sample_rate)
        audio_duration = len(pcm_bytes) / (sample_rate * 2)

        start_time = time.time()
        rec.AcceptWaveform(pcm_bytes)
        res = json.loads(rec.FinalResult())
        inference_time = time.time() - start_time

        transcript = res.get("text", "").strip()
        rtf = inference_time / max(audio_duration, 0.001)

        return {
            "transcript": transcript,
            "lang": self.lang,
            "audio_duration_sec": round(audio_duration, 3),
            "inference_time_sec": round(inference_time, 4),
            "rtf": round(rtf, 4)
        }

if __name__ == "__main__":
    print("VoskOfflineSTT module loaded successfully.")
