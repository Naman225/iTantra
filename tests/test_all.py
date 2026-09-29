#!/usr/bin/env python3
"""
iTantra Master Verification & Test Suite
Tests all layers: Protocol, Auto-LID, STT, TTS, and UDP Transceiver networking.
"""

import sys
import time
import socket
import unittest
from pathlib import Path

# Add project root to sys.path
PROJECT_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

from core_engine.protocol.tantra_packet import (
    TantraPacket,
    crc16_ccitt,
    detect_language_from_text,
    MAGIC_BYTE,
    FLAG_ALERT_EMERGENCY,
    FLAG_PTT_MODE
)
from core_engine.stt.stt_engine import VoskOfflineSTT
from core_engine.tts.tts_engine import PiperOfflineTTS
from core_engine.virtual_transceiver import VirtualReceiverNode, VirtualTransmitterNode


class TestTantraProtocol(unittest.TestCase):
    """Unit tests for the iTantra-Packet binary protocol."""

    def test_encode_decode_english(self):
        text = "Base station this is unit 4 radio check"
        pkt = TantraPacket(text=text, lang_id=1, is_emergency=False, is_ptt=True, seq_num=101)
        encoded = pkt.encode()

        # Header check: 8 bytes overhead (Magic, Flags, Seq, Len, CRC)
        self.assertEqual(encoded[0], MAGIC_BYTE)
        self.assertEqual(len(encoded), len(text.encode('utf-8')) + 8)

        decoded = TantraPacket.decode(encoded)
        self.assertEqual(decoded.text, text)
        self.assertEqual(decoded.lang_id, 1)
        self.assertEqual(decoded.lang_code, "en")
        self.assertFalse(decoded.is_emergency)
        self.assertTrue(decoded.is_ptt)
        self.assertEqual(decoded.seq_num, 101)

    def test_encode_decode_hindi(self):
        text = "यह एक आपातकालीन बचाव संदेश है"
        pkt = TantraPacket(text=text, lang_id=0, is_emergency=True, is_ptt=False, seq_num=202)
        encoded = pkt.encode()

        decoded = TantraPacket.decode(encoded)
        self.assertEqual(decoded.text, text)
        self.assertEqual(decoded.lang_id, 0)
        self.assertEqual(decoded.lang_code, "hi")
        self.assertTrue(decoded.is_emergency)
        self.assertFalse(decoded.is_ptt)
        self.assertEqual(decoded.seq_num, 202)

    def test_crc16_integrity_and_corruption_rejection(self):
        pkt = TantraPacket(text="Integrity test payload", lang_id=1)
        encoded = bytearray(pkt.encode())

        # Valid packet passes
        decoded = TantraPacket.decode(bytes(encoded))
        self.assertEqual(decoded.text, "Integrity test payload")

        # Corrupt 1 byte in payload -> CRC failure expected
        encoded[8] = (encoded[8] ^ 0xFF)
        with self.assertRaises(ValueError):
            TantraPacket.decode(bytes(encoded))

    def test_hmac_authentication_and_tamper_rejection(self):
        text = "Confidential NDRF evacuation order"
        pkt = TantraPacket(text=text, lang_id=1, is_emergency=True, seq_num=505, node_id=1042)
        auth_bytes = pkt.encode(enable_auth=True)

        # Authenticated packet decodes cleanly
        decoded = TantraPacket.decode(auth_bytes)
        self.assertEqual(decoded.text, text)
        self.assertEqual(decoded.node_id, 1042)
        self.assertEqual(decoded.seq_num, 505)
        self.assertTrue(decoded.is_emergency)

        # Tamper test: Flip 1 bit in payload
        corrupted = bytearray(auth_bytes)
        corrupted[10] ^= 0x01
        with self.assertRaises((PermissionError, ValueError)):
            TantraPacket.decode(bytes(corrupted))

        # Spoofing test: Wrong secret key rejects
        with self.assertRaises((PermissionError, ValueError)):
            TantraPacket.decode(auth_bytes, key=b"Wrong-Attack-Key-99999999999999")

    def test_auto_lid_script_detection(self):
        self.assertEqual(detect_language_from_text("Flash flood alert evacuate immediately"), "en")
        self.assertEqual(detect_language_from_text("बाढ़ का पानी पुल तक आ गया है"), "hi")
        self.assertEqual(detect_language_from_text("இது ஒரு அவசர எச்சரிக்கை"), "ta")  # Tamil
        self.assertEqual(detect_language_from_text("ఇది అత్యవసర హెచ్చరిక"), "te")       # Telugu
        self.assertEqual(detect_language_from_text("এটি একটি জরুরি সতর্কতা"), "bn")      # Bengali

    def test_bandwidth_savings_telemetry(self):
        pkt = TantraPacket(text="River level rising quickly evacuate now", lang_id=1)
        telemetry = pkt.telemetry_summary(speech_duration_sec=3.0)
        self.assertGreater(telemetry["savings_vs_pcm"], 99.0)
        self.assertGreater(telemetry["savings_vs_opus"], 90.0)
        self.assertLess(telemetry["effective_bps"], 300.0)


class TestSpeechEngines(unittest.TestCase):
    """Integration tests for offline STT and TTS engines."""

    @classmethod
    def setUpClass(cls):
        cls.tts = PiperOfflineTTS(default_lang="hi")
        cls.stt = VoskOfflineSTT(lang="hi")

    def test_english_tts_and_stt_loop(self):
        input_phrase = "emergency alert please help"
        
        # 1. Synthesize via Piper
        tts_res = self.tts.synthesize(input_phrase, lang="en")
        self.assertGreater(len(tts_res["pcm_bytes"]), 0)
        self.assertEqual(tts_res["sample_rate"], 16000)
        self.assertLess(tts_res["rtf"], 0.2, "TTS RTF should be faster than real-time")

        # 2. Transcribe via Vosk
        self.stt.load_model("en")
        stt_res = self.stt.transcribe_stream(tts_res["pcm_bytes"], sample_rate=16000)
        self.assertEqual(stt_res["transcript"], input_phrase)
        self.assertLess(stt_res["rtf"], 0.5, "STT RTF should be faster than real-time")

    def test_hindi_tts_and_stt_loop(self):
        input_phrase = "सभी दलों को सूचित किया जाता है कि मार्ग सुरक्षित है"
        
        # 1. Synthesize via Piper
        tts_res = self.tts.synthesize(input_phrase, lang="hi")
        self.assertGreater(len(tts_res["pcm_bytes"]), 0)
        self.assertEqual(tts_res["sample_rate"], 22050)
        self.assertLess(tts_res["rtf"], 0.2)

        # 2. Transcribe via Vosk
        self.stt.load_model("hi")
        stt_res = self.stt.transcribe_stream(tts_res["pcm_bytes"], sample_rate=22050)
        self.assertEqual(stt_res["transcript"], input_phrase)
        self.assertLess(stt_res["rtf"], 0.5)


class TestTransceiverNetworking(unittest.TestCase):
    """End-to-End UDP Transceiver network tests."""

    def test_transceiver_end_to_end_transmission(self):
        test_port = 5099
        receiver = VirtualReceiverNode(port=test_port)
        receiver.start()

        transmitter = VirtualTransmitterNode(dest_port=test_port)

        try:
            # 1. Normal transmission
            pkt1 = transmitter.transmit_text("Radio check from station alpha", lang="en", is_emergency=False)
            time.sleep(0.4)
            self.assertEqual(len(receiver.received_messages), 1)
            rec_msg1 = receiver.received_messages[0]
            self.assertEqual(rec_msg1["packet"].text, "Radio check from station alpha")
            self.assertFalse(rec_msg1["packet"].is_emergency)

            # 2. High priority SOS transmission
            pkt2 = transmitter.transmit_text("आपातकालीन संदेश सहायता भेजें", lang="hi", is_emergency=True)
            time.sleep(0.4)
            self.assertEqual(len(receiver.received_messages), 2)
            rec_msg2 = receiver.received_messages[1]
            self.assertEqual(rec_msg2["packet"].text, "आपातकालीन संदेश सहायता भेजें")
            self.assertTrue(rec_msg2["packet"].is_emergency)

        finally:
            receiver.stop()
            transmitter.close()


if __name__ == "__main__":
    unittest.main(verbosity=2)
