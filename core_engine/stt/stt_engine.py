"""
iTantra Offline Speech-To-Text (STT) Engine
Powered by Vosk lightweight acoustic models across 10 Indian Languages:
Hindi (hi), English (en), Bengali (bn), Gujarati (gu), Marathi (mr),
Kannada (kn), Malayalam (ml), Tamil (ta), Telugu (te), Odia (or).
Completely offline, low-memory footprint, Android-ready.
"""

import json
import time
import wave
import logging
from pathlib import Path
from typing import Dict, Any, Optional
import vosk

logger = logging.getLogger("iTantra.STT")
MODELS_DIR = Path(__file__).resolve().parent.parent / "models" / "stt"

LANGUAGE_MODEL_MAP = {
    "hi": "vosk-model-small-hi-0.22",
    "en": "vosk-model-small-en-in-0.4",
    "bn": "vosk-model-small-bn-0.4",
    "gu": "vosk-model-small-gu-0.4",
    "mr": "vosk-model-small-mr-0.4",
    "kn": "vosk-model-small-kn-0.4",
    "ml": "vosk-model-small-ml-0.4",
    "ta": "vosk-model-small-ta-0.4",
    "te": "vosk-model-small-te-0.4",
    "or": "vosk-model-small-or-0.4",
}

# Aliases
LANG_ALIASES = {
    "hindi": "hi",
    "english": "en",
    "en-in": "en",
    "en_in": "en",
    "bengali": "bn",
    "gujarati": "gu",
    "marathi": "mr",
    "kannada": "kn",
    "malayalam": "ml",
    "tamil": "ta",
    "telugu": "te",
    "odia": "or",
    "oriya": "or"
}


class VoskOfflineSTT:
    def __init__(self, lang: str = "hi"):
        """
        Initializes the offline STT engine for any of the 10 supported Indian languages.
        """
        self.lang = lang
        self.model = None
        self.current_model_path = None
        self.load_model(lang)

    def load_model(self, lang: str) -> float:
        normalized_lang = LANG_ALIASES.get(lang.lower().strip(), lang.lower().strip())
        if normalized_lang not in LANGUAGE_MODEL_MAP:
            normalized_lang = "hi"

        model_folder_name = LANGUAGE_MODEL_MAP[normalized_lang]
        model_path = MODELS_DIR / model_folder_name

        if not model_path.exists():
            # Graceful acoustic fallback to Hindi or English if specific language pack not downloaded yet
            fallback_path = MODELS_DIR / "vosk-model-small-hi-0.22"
            if not fallback_path.exists():
                fallback_path = MODELS_DIR / "vosk-model-small-en-in-0.4"
            if not fallback_path.exists():
                raise FileNotFoundError(
                    f"No Vosk STT models found in {MODELS_DIR}. Run download_models.py first."
                )
            logger.warning(
                f"Model for {normalized_lang} ({model_path.name}) not found locally. "
                f"Falling back to shared acoustic model: {fallback_path.name}"
            )
            model_path = fallback_path

        # Suppress verbose Vosk logging
        vosk.SetLogLevel(-1)
        start_time = time.time()
        self.model = vosk.Model(str(model_path))
        load_time = time.time() - start_time
        self.lang = normalized_lang
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
    stt = VoskOfflineSTT(lang="hi")
    print(f"VoskOfflineSTT initialized for '{stt.lang}'. Current model: {stt.current_model_path.name}")
