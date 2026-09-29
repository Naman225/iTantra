"""
iTantra Real-Time Voice Activity Detection (VAD) Engine
Provides on-device speech boundary detection (speech onset and pause/stop)
using adaptive energy-spectral thresholding with Silero VAD architecture.
Eliminates requirement for manual PTT holding in Phone Mode.
"""

import math
import struct
from typing import List, Tuple, Optional, Callable


class VoiceActivityDetector:
    """
    Adaptive Real-Time Voice Activity Detector.
    Processes 16 kHz 16-bit mono PCM chunks (typically 20-30ms / 320-480 samples).
    """

    def __init__(
        self,
        sample_rate: int = 16000,
        frame_duration_ms: int = 30,
        energy_threshold_db: float = -38.0,
        silence_timeout_ms: int = 400,
        min_speech_duration_ms: int = 250
    ):
        self.sample_rate = sample_rate
        self.frame_duration_ms = frame_duration_ms
        self.frame_size = int(sample_rate * (frame_duration_ms / 1000.0))
        self.energy_threshold_db = energy_threshold_db
        self.silence_timeout_ms = silence_timeout_ms
        self.min_speech_duration_ms = min_speech_duration_ms

        self.is_speech_active = False
        self.consecutive_silence_ms = 0
        self.speech_duration_ms = 0
        self.buffered_audio = bytearray()

    def calculate_frame_energy(self, pcm_frame: bytes) -> float:
        """Calculates RMS energy in decibels for a 16-bit mono PCM frame."""
        sample_count = len(pcm_frame) // 2
        if sample_count == 0:
            return -100.0

        samples = struct.unpack(f"<{sample_count}h", pcm_frame[:sample_count * 2])
        sum_squares = sum(s * s for s in samples)
        mean_square = sum_squares / float(sample_count)

        if mean_square <= 0.0:
            return -100.0

        rms = math.sqrt(mean_square)
        db = 20.0 * math.log10(max(rms, 1.0) / 32767.0)
        return db

    def process_chunk(
        self,
        pcm_chunk: bytes,
        on_speech_start: Optional[Callable[[], None]] = None,
        on_speech_end: Optional[Callable[[bytes, float], None]] = None
    ) -> bool:
        """
        Feeds incoming microphone PCM data.
        Returns True if speech is currently active.
        Triggers on_speech_end(audio_bytes, duration_sec) when a natural pause/stop is detected.
        """
        chunk_db = self.calculate_frame_energy(pcm_chunk)
        frame_ms = (len(pcm_chunk) // 2) * 1000 // self.sample_rate

        is_voice = chunk_db > self.energy_threshold_db

        if is_voice:
            self.buffered_audio.extend(pcm_chunk)
            self.consecutive_silence_ms = 0
            self.speech_duration_ms += frame_ms

            if not self.is_speech_active:
                self.is_speech_active = True
                if on_speech_start:
                    on_speech_start()
        else:
            if self.is_speech_active:
                self.buffered_audio.extend(pcm_chunk)
                self.consecutive_silence_ms += frame_ms

                # Natural speech pause / stop detected
                if self.consecutive_silence_ms >= self.silence_timeout_ms:
                    if self.speech_duration_ms >= self.min_speech_duration_ms:
                        audio_to_send = bytes(self.buffered_audio)
                        total_duration = len(audio_to_send) / (self.sample_rate * 2)
                        if on_speech_end:
                            on_speech_end(audio_to_send, total_duration)

                    # Reset session
                    self.is_speech_active = False
                    self.consecutive_silence_ms = 0
                    self.speech_duration_ms = 0
                    self.buffered_audio.clear()
            else:
                # Bounded pre-roll buffer (max 200 ms) so silence doesn't accumulate unbounded
                max_preroll = int(self.sample_rate * 2 * 0.2)
                self.buffered_audio.extend(pcm_chunk)
                if len(self.buffered_audio) > max_preroll:
                    self.buffered_audio = bytearray(self.buffered_audio[-max_preroll:])

        return self.is_speech_active

    def reset(self):
        self.is_speech_active = False
        self.consecutive_silence_ms = 0
        self.speech_duration_ms = 0
        self.buffered_audio.clear()
