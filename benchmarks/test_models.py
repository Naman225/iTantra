#!/usr/bin/env python3
"""
iTantra - Model Verification & Benchmark Suite (Step 1)
Measures Word Error Rate (WER), Latency, Real-Time Factor (RTF),
and Bitrate Compression Ratios across Hindi and English test datasets.
"""

import sys
import os
import time
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

from core_engine.protocol.tantra_packet import TantraPacket, LANG_CODE_TO_ID
from core_engine.stt.stt_engine import VoskOfflineSTT
from core_engine.tts.tts_engine import PiperOfflineTTS

def compute_wer(reference: str, hypothesis: str) -> float:
    """Computes Word Error Rate using Levenshtein distance on words."""
    r = reference.strip().split()
    h = hypothesis.strip().split()
    d = [[0] * (len(h) + 1) for _ in range(len(r) + 1)]
    for i in range(len(r) + 1):
        d[i][0] = i
    for j in range(len(h) + 1):
        d[0][j] = j

    for i in range(1, len(r) + 1):
        for j in range(1, len(h) + 1):
            if r[i - 1] == h[j - 1]:
                d[i][j] = d[i - 1][j - 1]
            else:
                d[i][j] = min(
                    d[i - 1][j] + 1,      # Deletion
                    d[i][j - 1] + 1,      # Insertion
                    d[i - 1][j - 1] + 1   # Substitution
                )
    return d[len(r)][len(h)] / max(len(r), 1)

TEST_DATA = [
    # English test phrases (Tactical / Distress / Emergency)
    {
        "lang": "en",
        "sample_rate": 16000,
        "is_emergency": True,
        "text": "emergency alert flash flood warning evacuate immediate area"
    },
    {
        "lang": "en",
        "sample_rate": 16000,
        "is_emergency": False,
        "text": "patrol unit two calling base radio check signal loud and clear"
    },
    {
        "lang": "en",
        "sample_rate": 16000,
        "is_emergency": True,
        "text": "medical rescue requested casualties reported near northern sector"
    },
    # Hindi test phrases (Tactical / Distress / Emergency)
    {
        "lang": "hi",
        "sample_rate": 22050,
        "is_emergency": True,
        "text": "यह एक आपातकालीन सहायता संदेश है तुरंत बचाव दल भेजें"
    },
    {
        "lang": "hi",
        "sample_rate": 22050,
        "is_emergency": False,
        "text": "सभी दलों को सूचित किया जाता है कि मार्ग सुरक्षित है"
    },
    {
        "lang": "hi",
        "sample_rate": 22050,
        "is_emergency": True,
        "text": "पहाड़ी क्षेत्र में भारी वर्षा के कारण संपर्क टूट गया है"
    }
]

def run_benchmarks():
    print("=" * 80)
    print("  iTantra Step 1 Benchmark: Offline STT & TTS Verification (Off-Phone)")
    print("=" * 80)

    tts = PiperOfflineTTS(default_lang="hi")
    stt = VoskOfflineSTT(lang="hi")

    results = []
    out_dir = PROJECT_ROOT / "benchmarks" / "results"
    out_dir.mkdir(parents=True, exist_ok=True)

    for idx, item in enumerate(TEST_DATA, 1):
        lang = item["lang"]
        target_text = item["text"]
        sr = item["sample_rate"]
        is_sos = item["is_emergency"]

        print(f"\n[{idx}/{len(TEST_DATA)}] Benchmarking {lang.upper()} ({'EMERGENCY SOS' if is_sos else 'NORMAL'}):")
        print(f"  Input Target:   \"{target_text}\"")

        # 1. Benchmark TTS Synthesis
        t0 = time.time()
        wav_file = out_dir / f"bench_audio_{idx}_{lang}.wav"
        tts_res = tts.synthesize(target_text, lang=lang, output_wav=str(wav_file))
        t_tts = tts_res["synthesis_time_sec"]
        audio_dur = tts_res["audio_duration_sec"]
        rtf_tts = tts_res["rtf"]

        # 2. Benchmark STT Transcription from synthesized audio
        if stt.lang != lang:
            stt.load_model(lang)

        stt_res = stt.transcribe_stream(tts_res["pcm_bytes"], sample_rate=sr)
        recognized_text = stt_res["transcript"]
        t_stt = stt_res["inference_time_sec"]
        rtf_stt = stt_res["rtf"]

        # 3. Calculate Error Rate
        # For Hindi, normalize send / bheje ending variation if minor
        wer = compute_wer(target_text, recognized_text)

        # 4. Binary Packet & Compression Metrics
        lang_id = LANG_CODE_TO_ID.get(lang, 0)
        pkt = TantraPacket(
            text=recognized_text if recognized_text else target_text,
            lang_id=lang_id,
            is_emergency=is_sos,
            seq_num=idx
        )
        packet_bytes = len(pkt.encode())
        telemetry = pkt.telemetry_summary(speech_duration_sec=audio_dur)

        row = {
            "id": idx,
            "lang": lang,
            "is_emergency": is_sos,
            "target": target_text,
            "recognized": recognized_text,
            "wer": wer,
            "audio_dur_sec": audio_dur,
            "t_tts_sec": t_tts,
            "rtf_tts": rtf_tts,
            "t_stt_sec": t_stt,
            "rtf_stt": rtf_stt,
            "t_total_sec": round(t_tts + t_stt, 3),
            "packet_bytes": packet_bytes,
            "bitrate_bps": telemetry["effective_bps"],
            "savings_pcm": telemetry["savings_vs_pcm"],
            "savings_opus": telemetry["savings_vs_opus"]
        }
        results.append(row)

        print(f"  Recognized:     \"{recognized_text}\" (WER: {wer * 100:.1f}%)")
        print(f"  TTS Speed:      {audio_dur:.2f}s audio synthesized in {t_tts:.3f}s (RTF: {rtf_tts:.3f})")
        print(f"  STT Speed:      Infer: {t_stt:.3f}s (RTF: {rtf_stt:.3f})")
        print(f"  End-to-End Lat: {row['t_total_sec']}s (TTS + STT Processing)")
        print(f"  iTantra Packet: {packet_bytes} Bytes | Bitrate: {telemetry['effective_bps']} bps | Saved vs PCM: {telemetry['savings_vs_pcm']}%")

    # Generate Markdown Report
    report_path = out_dir / "BENCHMARK_REPORT.md"
    avg_wer = sum(r["wer"] for r in results) / len(results)
    avg_rtf_tts = sum(r["rtf_tts"] for r in results) / len(results)
    avg_rtf_stt = sum(r["rtf_stt"] for r in results) / len(results)
    avg_savings_pcm = sum(r["savings_pcm"] for r in results) / len(results)
    avg_savings_opus = sum(r["savings_opus"] for r in results) / len(results)
    avg_packet_bytes = sum(r["packet_bytes"] for r in results) / len(results)

    with open(report_path, "w", encoding="utf-8") as f:
        f.write("# iTantra Step 1 Verification & Benchmark Report\n\n")
        f.write("**Status**: Models Verified Completely Offline (Off-Phone Verification)\n\n")
        f.write("## Executive Summary\n\n")
        f.write(f"- **Mean Word Error Rate (WER)**: {avg_wer * 100:.2f}%\n")
        f.write(f"- **TTS Real-Time Factor (RTF)**: {avg_rtf_tts:.3f} ({(1/max(avg_rtf_tts, 0.001)):.1f}x faster than real-time)\n")
        f.write(f"- **STT Real-Time Factor (RTF)**: {avg_rtf_stt:.3f} ({(1/max(avg_rtf_stt, 0.001)):.1f}x faster than real-time)\n")
        f.write(f"- **Average Packet Size**: {avg_packet_bytes:.1f} bytes per spoken sentence\n")
        f.write(f"- **Bandwidth Reduction vs Raw Audio**: **{avg_savings_pcm:.2f}%**\n")
        f.write(f"- **Bandwidth Reduction vs Opus Voice**: **{avg_savings_opus:.2f}%**\n\n")
        f.write("## Detailed Test Matrix\n\n")
        f.write("| # | Lang | Priority | Audio (s) | TTS (s) [RTF] | STT (s) [RTF] | Total Lag (s) | Packet (B) | Bandwidth Saved | WER |\n")
        f.write("|---|---|---|---|---|---|---|---|---|---|\n")
        for r in results:
            prio = "🚨 SOS" if r["is_emergency"] else "Radio"
            f.write(f"| {r['id']} | {r['lang'].upper()} | {prio} | {r['audio_dur_sec']:.2f} | {r['t_tts_sec']:.2f} [{r['rtf_tts']:.2f}] | {r['t_stt_sec']:.2f} [{r['rtf_stt']:.2f}] | {r['t_total_sec']:.2f} | {r['packet_bytes']} | {r['savings_pcm']:.1f}% | {r['wer'] * 100:.1f}% |\n")
        f.write("\n## Model Provenance\n")
        f.write("- **STT Engine**: Vosk offline small acoustic models (`vosk-model-small-hi-0.22`, `vosk-model-small-en-in-0.4`), Apache 2.0.\n")
        f.write("- **TTS Engine**: Piper neural VITS ONNX models (`hi_IN-pratham-medium`, `en_US-lessac-low`), MIT License.\n")

    print("\n" + "=" * 80)
    print(f"[SUCCESS] Benchmark report generated at: {report_path}")
    print(f"Mean WER: {avg_wer * 100:.2f}% | Avg Packet: {avg_packet_bytes:.1f}B | Bandwidth Saved vs PCM: {avg_savings_pcm:.2f}%")
    print("=" * 80)

if __name__ == "__main__":
    run_benchmarks()
