#!/usr/bin/env python3
"""
iTantra - Virtual Neural Transceiver (Desktop Testbed & Live Bridge)
Demonstrates two nodes communicating over UDP (Loopback or Local Wi-Fi Hotspot)
using the ultra-low-bitrate TantraPacket protocol with offline Vosk STT and Piper TTS.
Includes Automatic Language Identification (Auto-LID) for seamless Hindi/English switching.

Usage:
  python virtual_transceiver.py                  # Runs 2-node simulation on loopback
  python virtual_transceiver.py -i               # Interactive walkie-talkie terminal
  python virtual_transceiver.py -i --network     # Live network mode (talks to Android phone over Wi-Fi!)
"""

import sys
import os
import time
import socket
import argparse
import threading
from pathlib import Path

# Add project root to sys.path
PROJECT_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

from core_engine.protocol.tantra_packet import (
    TantraPacket,
    LANG_IDS,
    LANG_CODE_TO_ID,
    detect_language_from_text
)
from core_engine.stt.stt_engine import VoskOfflineSTT
from core_engine.tts.tts_engine import PiperOfflineTTS

DEFAULT_PORT = 5005

class VirtualReceiverNode:
    def __init__(self, port: int = DEFAULT_PORT, host: str = "0.0.0.0"):
        self.host = host
        self.port = port
        self.running = False
        self.sock = None
        self.tts = PiperOfflineTTS(default_lang="hi")
        self.received_messages = []

    def start(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        self.sock.bind((self.host, self.port))
        self.running = True

        self.thread = threading.Thread(target=self._listen_loop, daemon=True)
        self.thread.start()
        print(f"[RECEIVER NODE] Listening on {self.host}:{self.port} ... Ready.")

    def _listen_loop(self):
        while self.running:
            try:
                data, addr = self.sock.recvfrom(65535)
                recv_time = time.time()
                self._handle_packet(data, addr, recv_time)
            except Exception as e:
                if self.running:
                    print(f"[RECEIVER ERROR] {e}")
                break

    def _handle_packet(self, raw_bytes: bytes, addr, recv_time: float):
        try:
            packet = TantraPacket.decode(raw_bytes)
        except Exception as e:
            # Ignore non-iTantra network noise
            return

        lang_code = packet.lang_code
        is_sos = packet.is_emergency
        text = packet.text

        # Process via TTS (automatic voice selection with script compatibility guard)
        out_wav = PROJECT_ROOT / "benchmarks" / "results" / f"received_seq_{packet.seq_num}_{lang_code}.wav"
        tts_res = self.tts.synthesize(text, lang=lang_code, output_wav=str(out_wav))
        actual_voice_lang = tts_res["lang"]

        record = {
            "packet": packet,
            "tts_res": tts_res,
            "raw_len": len(raw_bytes),
            "out_wav": str(out_wav),
            "recv_time": recv_time
        }
        self.received_messages.append(record)

        print("\n" + "=" * 70)
        if is_sos:
            print("🚨🚨🚨 [HIGH PRIORITY DISTRESS / EMERGENCY ALERT] 🚨🚨🚨")
            print(">> Volume Override: MAX (AudioAttributes.USAGE_ALARM) <<")
            print(">> Mode: NON-INTERRUPTIBLE VOICE BROADCAST <<")
        else:
            print(f"📻 [RADIO MESSAGE RECEIVED] from {addr[0]}:{addr[1]} (Seq #{packet.seq_num})")

        print(f"Language:    {packet.lang_name} ({lang_code.upper()}) | Voice Engine: {actual_voice_lang.upper()}")
        print(f"Mode:        {'PTT Walkie-Talkie' if packet.is_ptt else 'Phone VAD'}")
        print(f"Packet Size: {len(raw_bytes)} Bytes (Ultra-Low Bitrate Neural Frame)")
        print(f"Text:        \"{text}\"")
        print(f"TTS Output:  Generated {tts_res['audio_duration_sec']}s audio in {tts_res['synthesis_time_sec']}s (RTF: {tts_res['rtf']})")
        print(f"Audio Saved: {out_wav.name}")
        print("=" * 70 + "\n")

    def stop(self):
        self.running = False
        if self.sock:
            self.sock.close()


class VirtualTransmitterNode:
    def __init__(self, dest_port: int = DEFAULT_PORT, dest_host: str = "255.255.255.255"):
        self.dest_host = dest_host
        self.dest_port = dest_port
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        self.stt = VoskOfflineSTT(lang="hi")
        self.seq_counter = 0

    def transmit_text(self, text: str, lang: str = "auto", is_emergency: bool = False, is_ptt: bool = True) -> TantraPacket:
        self.seq_counter += 1
        if lang == "auto":
            lang_code = detect_language_from_text(text)
        else:
            lang_code = lang

        lang_id = LANG_CODE_TO_ID.get(lang_code, 0)
        pkt = TantraPacket(
            text=text,
            lang_id=lang_id,
            is_emergency=is_emergency,
            is_ptt=is_ptt,
            seq_num=self.seq_counter
        )
        data = pkt.encode()
        self.sock.sendto(data, (self.dest_host, self.dest_port))
        return pkt

    def transmit_audio(self, pcm_bytes: bytes, sample_rate: int = 16000, lang: str = "hi", is_emergency: bool = False, is_ptt: bool = True) -> dict:
        if self.stt.lang != lang:
            self.stt.load_model(lang)

        stt_res = self.stt.transcribe_stream(pcm_bytes, sample_rate=sample_rate)
        transcript = stt_res["transcript"]

        if not transcript:
            print("[TRANSMITTER] No speech detected in audio stream.")
            return {"status": "no_speech", "stt_res": stt_res}

        t_tx_start = time.time()
        detected_lang = detect_language_from_text(transcript) if lang == "auto" else lang
        pkt = self.transmit_text(
            text=transcript,
            lang=detected_lang,
            is_emergency=is_emergency,
            is_ptt=is_ptt
        )
        t_tx = time.time() - t_tx_start

        telemetry = pkt.telemetry_summary(speech_duration_sec=stt_res["audio_duration_sec"])

        return {
            "status": "transmitted",
            "transcript": transcript,
            "stt_res": stt_res,
            "packet": pkt,
            "tx_time_sec": t_tx,
            "telemetry": telemetry
        }

    def close(self):
        self.sock.close()


def run_demo():
    print("==================================================================")
    print("  iTantra: Neural Transceiver End-to-End Simulation              ")
    print("==================================================================")

    receiver = VirtualReceiverNode(port=DEFAULT_PORT, host="127.0.0.1")
    receiver.start()

    transmitter = VirtualTransmitterNode(dest_port=DEFAULT_PORT, dest_host="127.0.0.1")
    tts_synth = PiperOfflineTTS(default_lang="hi")

    # Demo 1: English Emergency Message
    print("\n--- TEST SCENARIO 1: ENGLISH EMERGENCY SOS ALERT (PTT MODE) ---")
    en_phrase = "emergency alert flash flood warning evacuate immediate area"
    print(f"[Simulating Mic Input]: Generating test audio for: \"{en_phrase}\" ...")
    raw_audio_en = tts_synth.synthesize(en_phrase, lang="en")["pcm_bytes"]

    transmitter.transmit_audio(
        pcm_bytes=raw_audio_en,
        sample_rate=16000,
        lang="en",
        is_emergency=True,
        is_ptt=True
    )
    time.sleep(0.6)

    # Demo 2: Hindi Distress Message
    print("\n--- TEST SCENARIO 2: HINDI TACTICAL RESCUE MESSAGE (PHONE MODE) ---")
    hi_phrase = "यह एक आपातकालीन सहायता संदेश है तुरंत बचाव दल भेजें"
    print(f"[Simulating Mic Input]: Generating test audio for: \"{hi_phrase}\" ...")
    raw_audio_hi = tts_synth.synthesize(hi_phrase, lang="hi")["pcm_bytes"]

    transmitter.transmit_audio(
        pcm_bytes=raw_audio_hi,
        sample_rate=22050,
        lang="hi",
        is_emergency=False,
        is_ptt=False
    )
    time.sleep(0.6)

    receiver.stop()
    transmitter.close()
    print("\n[SUCCESS] Virtual Transceiver Simulation Completed Successfully!")


def run_interactive(is_network: bool = False):
    host_listen = "0.0.0.0"
    dest_ip = "255.255.255.255" if is_network else "127.0.0.1"

    print("==================================================================")
    print("  iTantra: Interactive Walkie-Talkie Neural Transceiver Terminal ")
    print("==================================================================")
    print(f"Network Mode: {'LIVE WI-FI BROADCAST (Talks to Android phones)' if is_network else 'LOCAL LOOPBACK'}")
    print("Features:")
    print("  • Auto-Language Detection: Type in English or Hindi seamlessly!")
    print("  • Emergency Alert: Prefix with 'sos:' or 'alert:' for emergency broadcast")
    print("  • Manual Language Override: 'lang hi', 'lang en', or 'lang auto'")
    print("  • Exit: Type 'exit' or 'quit'\n")

    receiver = VirtualReceiverNode(port=DEFAULT_PORT, host=host_listen)
    receiver.start()
    transmitter = VirtualTransmitterNode(dest_port=DEFAULT_PORT, dest_host=dest_ip)

    current_lang = "auto"

    try:
        while True:
            prompt = f"[{current_lang.upper()} Transceiver] > "
            user_input = input(prompt).strip()
            if not user_input:
                continue
            if user_input.lower() in ["exit", "quit", "q"]:
                break
            if user_input.lower() in ["lang auto"]:
                current_lang = "auto"
                print(f"[SWITCH] Mode set to AUTO language detection.")
                continue
            if user_input.lower() in ["lang hi", "lang hindi"]:
                current_lang = "hi"
                print(f"[SWITCH] Language locked to Hindi ({current_lang})")
                continue
            if user_input.lower() in ["lang en", "lang english"]:
                current_lang = "en"
                print(f"[SWITCH] Language locked to English ({current_lang})")
                continue

            is_sos = False
            msg_text = user_input
            if user_input.lower().startswith("sos:") or user_input.lower().startswith("alert:"):
                is_sos = True
                msg_text = user_input.split(":", 1)[1].strip()

            transmitter.transmit_text(
                text=msg_text,
                lang=current_lang,
                is_emergency=is_sos,
                is_ptt=True
            )
            time.sleep(0.4)
    except KeyboardInterrupt:
        print("\nExiting transceiver...")
    finally:
        receiver.stop()
        transmitter.close()

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="iTantra Virtual Neural Transceiver")
    parser.add_argument("-i", "--interactive", action="store_true", help="Launch interactive walkie-talkie mode")
    parser.add_argument("--network", action="store_true", help="Broadcast to local network/hotspot to talk to Android devices")
    args = parser.parse_args()

    if args.interactive:
        run_interactive(is_network=args.network)
    else:
        run_demo()
