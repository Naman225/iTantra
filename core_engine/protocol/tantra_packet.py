"""
iTantra Binary Neural Radio Access Protocol (iTantra-Packet)
Ultra-low-bitrate framing for transmitting speech metadata over constrained links.
Includes Automatic Language Identification (Auto-LID) across 10 Indian languages.
"""

import struct
from typing import Tuple, Optional

# Magic Byte for iTantra protocol ('T' = 0x54)
MAGIC_BYTE = 0x54

# Flag bitmasks
FLAG_ALERT_EMERGENCY = 0x80  # Bit 7: High Priority Distress / Alert
FLAG_PTT_MODE        = 0x40  # Bit 6: PTT (1) vs Phone VAD Mode (0)
FLAG_RESERVED_5      = 0x20  # Bit 5: Reserved
FLAG_RESERVED_4      = 0x10  # Bit 4: Reserved
LANG_MASK            = 0x0F  # Bits 0-3: Language ID (0 to 15)

# Language ID Mapping (10 Indian Languages)
LANG_IDS = {
    0: ("hi", "Hindi"),
    1: ("en", "English"),
    2: ("gu", "Gujarati"),
    3: ("mr", "Marathi"),
    4: ("kn", "Kannada"),
    5: ("ml", "Malayalam"),
    6: ("ta", "Tamil"),
    7: ("te", "Telugu"),
    8: ("or", "Odia"),
    9: ("bn", "Bengali")
}

LANG_CODE_TO_ID = {v[0]: k for k, v in LANG_IDS.items()}

# Unicode Script Ranges for Automatic Language Detection
SCRIPT_RANGES = {
    "bn": (0x0980, 0x09FF),  # Bengali
    "gu": (0x0A80, 0x0AFF),  # Gujarati
    "or": (0x0B00, 0x0B7F),  # Odia
    "ta": (0x0B80, 0x0BFF),  # Tamil
    "te": (0x0C00, 0x0C7F),  # Telugu
    "kn": (0x0C80, 0x0CFF),  # Kannada
    "ml": (0x0D00, 0x0D7F),  # Malayalam
    "hi": (0x0900, 0x097F),  # Devanagari (Hindi)
}

def detect_language_from_text(text: str) -> str:
    """
    Automatically detects the language code based on unicode character distributions
    across all 10 mandated Indian languages.
    """
    counts = {k: 0 for k in SCRIPT_RANGES}
    latin_count = 0
    has_marathi_char = False

    for ch in text:
        cp = ord(ch)
        if (65 <= cp <= 90) or (97 <= cp <= 122):
            latin_count += 1
            continue
        if cp == 0x0933:  # 'ळ' character specific to Marathi
            has_marathi_char = True
        for lang, (low, high) in SCRIPT_RANGES.items():
            if low <= cp <= high:
                counts[lang] += 1
                break

    # Find predominant Indic script
    max_indic_lang = max(counts, key=counts.get)
    max_indic_count = counts[max_indic_lang]

    if max_indic_count > latin_count and max_indic_count > 0:
        if max_indic_lang == "hi" and has_marathi_char:
            return "mr"
        return max_indic_lang
    elif latin_count > 0:
        return "en"
    return "hi"  # fallback default


def crc16_ccitt(data: bytes) -> int:
    """Computes CRC-16 CCITT (polynomial 0x1021, init 0xFFFF)."""
    crc = 0xFFFF
    for byte in data:
        crc ^= (byte << 8)
        for _ in range(8):
            if crc & 0x8000:
                crc = ((crc << 1) ^ 0x1021) & 0xFFFF
            else:
                crc = (crc << 1) & 0xFFFF
    return crc


class TantraPacket:
    def __init__(
        self,
        text: str,
        lang_id: Optional[int] = None,
        is_emergency: bool = False,
        is_ptt: bool = True,
        seq_num: int = 1,
        auto_detect_lang: bool = True
    ):
        self.text = text
        self.is_emergency = is_emergency
        self.is_ptt = is_ptt
        self.seq_num = seq_num & 0xFFFF

        # If lang_id is not explicitly provided, auto-detect it
        if lang_id is None:
            detected_code = detect_language_from_text(text)
            self.lang_id = LANG_CODE_TO_ID.get(detected_code, 0)
        else:
            self.lang_id = lang_id & 0x0F

    @property
    def lang_code(self) -> str:
        return LANG_IDS.get(self.lang_id, ("hi", "Hindi"))[0]

    @property
    def lang_name(self) -> str:
        return LANG_IDS.get(self.lang_id, ("hi", "Hindi"))[1]

    def encode(self) -> bytes:
        payload = self.text.encode('utf-8')
        payload_len = len(payload)
        if payload_len > 65535:
            raise ValueError(f"Payload too large: {payload_len} bytes")

        flags = (self.lang_id & LANG_MASK)
        if self.is_emergency:
            flags |= FLAG_ALERT_EMERGENCY
        if self.is_ptt:
            flags |= FLAG_PTT_MODE

        header = struct.pack(">BBHH", MAGIC_BYTE, flags, self.seq_num, payload_len)
        data_to_checksum = header + payload
        crc = crc16_ccitt(data_to_checksum)
        trailer = struct.pack(">H", crc)

        return data_to_checksum + trailer

    @classmethod
    def decode(cls, raw_bytes: bytes) -> "TantraPacket":
        if len(raw_bytes) < 8:
            raise ValueError(f"Packet too short: {len(raw_bytes)} bytes (min 8 required)")

        magic, flags, seq_num, payload_len = struct.unpack(">BBHH", raw_bytes[:6])
        if magic != MAGIC_BYTE:
            raise ValueError(f"Invalid magic byte: 0x{magic:02X} (expected 0x{MAGIC_BYTE:02X})")

        expected_total_len = 6 + payload_len + 2
        if len(raw_bytes) < expected_total_len:
            raise ValueError(f"Incomplete packet: expected {expected_total_len} bytes, got {len(raw_bytes)}")

        payload_bytes = raw_bytes[6:6 + payload_len]
        received_crc = struct.unpack(">H", raw_bytes[6 + payload_len:6 + payload_len + 2])[0]

        computed_crc = crc16_ccitt(raw_bytes[:6 + payload_len])
        if received_crc != computed_crc:
            raise ValueError(f"CRC Mismatch: computed 0x{computed_crc:04X}, received 0x{received_crc:04X}")

        is_emergency = bool(flags & FLAG_ALERT_EMERGENCY)
        is_ptt = bool(flags & FLAG_PTT_MODE)
        lang_id = flags & LANG_MASK
        text = payload_bytes.decode('utf-8', errors='replace')

        return cls(
            text=text,
            lang_id=lang_id,
            is_emergency=is_emergency,
            is_ptt=is_ptt,
            seq_num=seq_num
        )

    def telemetry_summary(self, speech_duration_sec: float = 3.0) -> dict:
        encoded_bytes = len(self.encode())
        raw_pcm_bytes = int(speech_duration_sec * 16000 * 2)
        opus_16k_bytes = int(speech_duration_sec * (16000 / 8))

        effective_bps = (encoded_bytes * 8) / max(speech_duration_sec, 0.1)
        savings_vs_pcm = (1 - (encoded_bytes / raw_pcm_bytes)) * 100
        savings_vs_opus = (1 - (encoded_bytes / opus_16k_bytes)) * 100

        return {
            "packet_bytes": encoded_bytes,
            "speech_duration_sec": speech_duration_sec,
            "effective_bps": round(effective_bps, 1),
            "savings_vs_pcm": round(savings_vs_pcm, 2),
            "savings_vs_opus": round(savings_vs_opus, 2),
            "is_emergency": self.is_emergency,
            "lang": self.lang_name
        }
