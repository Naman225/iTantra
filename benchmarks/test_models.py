#!/usr/bin/env python3
"""
iTantra - Model Verification & Benchmark Suite
Measures Word Error Rate (WER), Latency, Real-Time Factor (RTF),
and Bitrate Compression Ratios across Indian Languages.
"""

import sys
import os
import time
import argparse
import glob
import wave
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

from core_engine.protocol.tantra_packet import TantraPacket, LANG_CODE_TO_ID
from core_engine.stt.stt_engine import VoskOfflineSTT
from core_engine.tts.tts_engine import PiperOfflineTTS

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

VOICE_CONFIGS = {
    "hi": {"model": "hi_IN-pratham-medium.onnx"},
    "en": {"model": "en_US-lessac-low.onnx"},
    "bn": {"model": "bn_IN-indic-medium.onnx"},
    "gu": {"model": "gu_IN-indic-medium.onnx"},
    "mr": {"model": "mr_IN-indic-medium.onnx"},
    "kn": {"model": "kn_IN-indic-medium.onnx"},
    "ml": {"model": "ml_IN-indic-medium.onnx"},
    "ta": {"model": "ta_IN-indic-medium.onnx"},
    "te": {"model": "te_IN-indic-medium.onnx"},
    "or": {"model": "or_IN-indic-medium.onnx"},
}

def compute_wer(reference: str, hypothesis: str) -> float:
    """Computes Word Error Rate using Levenshtein distance on words."""
    r = reference.strip().split()
    h = hypothesis.strip().split()
    if not r:
        return 0.0 if not h else 1.0
    if not h:
        return 1.0  # Empty hypothesis is 100% error rate
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

def compute_cer(reference: str, hypothesis: str) -> float:
    """Computes Character Error Rate using Levenshtein distance on characters (ignoring spaces)."""
    r = list(reference.strip().replace(" ", ""))
    h = list(hypothesis.strip().replace(" ", ""))
    if not r:
        return 0.0 if not h else 1.0
    if not h:
        return 1.0
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
    {
        "lang": "en",
        "sample_rate": 16000,
        "is_emergency": True,
        "text": "emergency alert flash flood warning evacuate immediate area"
    },
    {
        "lang": "hi",
        "sample_rate": 22050,
        "is_emergency": True,
        "text": "यह एक आपातकालीन सहायता संदेश है तुरंत बचाव दल भेजें"
    },
    {
        "lang": "bn",
        "sample_rate": 22050,
        "is_emergency": True,
        "text": "জরুরি সতর্কতা বন্যা পরিস্থিতি অবিলম্বে এলাকা খালি করুন"
    },
    {
        "lang": "gu",
        "sample_rate": 22050,
        "is_emergency": True,
        "text": "કટોકટી ચેતવણી પૂરની સ્થિતિ તાત્કાલિક વિસ્તાર ખાલી કરો"
    },
    {
        "lang": "mr",
        "sample_rate": 22050,
        "is_emergency": False,
        "text": "सर्व पथकांना कळविण्यात येत आहे की रस्ता सुरक्षित आहे"
    },
    {
        "lang": "kn",
        "sample_rate": 22050,
        "is_emergency": True,
        "text": "ತುರ್ತು ಎಚ್ಚರಿಕೆ ಪ್ರವಾಹ ಪರಿಸ್ಥಿತಿ ತಕ್ಷಣವೇ ಸ್ಥಳ ಖಾಲಿ ಮಾಡಿ"
    },
    {
        "lang": "ml",
        "sample_rate": 22050,
        "is_emergency": True,
        "text": "അടിയന്തര മുന്നറിയിപ്പ് പ്രളയ മുന്നറിയിപ്പ് ഉടൻ പ്രദേശം ഒഴിയുക"
    },
    {
        "lang": "ta",
        "sample_rate": 22050,
        "is_emergency": True,
        "text": "அவசர எச்சரிக்கை வெள்ள அபாயம் உடனடியாக வெளியேறவும்"
    },
    {
        "lang": "te",
        "sample_rate": 22050,
        "is_emergency": False,
        "text": "అన్ని బృందాలకు మార్గం సురక్షితంగా ఉందని తెలియజేయడమైనది"
    },
    {
        "lang": "or",
        "sample_rate": 22050,
        "is_emergency": True,
        "text": "ଜରୁରୀକାଳୀନ ସତର୍କତା ବନ୍ୟା ପରିସ୍ଥିତି ତୁରନ୍ତ ସ୍ଥାନ ଖାଲି କରନ୍ତୁ"
    }
]

def load_wav(filepath):
    with wave.open(filepath, "rb") as wf:
        sr = wf.getframerate()
        nframes = wf.getnframes()
        pcm_bytes = wf.readframes(nframes)
        duration = nframes / float(sr)
    return pcm_bytes, sr, duration

def run_benchmarks(args):
    print("=" * 80)
    print("  iTantra 10-Language Benchmark: Offline STT & TTS Verification")
    print("=" * 80)
    
    if args.audio_dir:
        print(f"Using real audio from: {args.audio_dir}")
    else:
        print("Using synthesized audio (TTS).")

    tts = PiperOfflineTTS(default_lang="hi")
    stt = VoskOfflineSTT(lang="hi")

    results = []
    out_dir = PROJECT_ROOT / "benchmarks" / "results"
    out_dir.mkdir(parents=True, exist_ok=True)

    tested_languages = 0
    total_languages = len(TEST_DATA)

    for idx, item in enumerate(TEST_DATA, 1):
        lang = item["lang"]
        target_text = item["text"]
        sr = item["sample_rate"]
        is_sos = item["is_emergency"]

        stt_model_name = LANGUAGE_MODEL_MAP.get(lang, "")
        tts_model_name = VOICE_CONFIGS.get(lang, {}).get("model", "")

        stt_model_path = PROJECT_ROOT / "core_engine" / "models" / "stt" / stt_model_name
        tts_model_path = PROJECT_ROOT / "core_engine" / "models" / "tts" / tts_model_name

        has_stt = stt_model_path.exists() and stt_model_path.is_dir()
        has_tts = tts_model_path.exists() and tts_model_path.is_file()

        audio_file_path = None
        if args.audio_dir:
            matches = glob.glob(os.path.join(args.audio_dir, f"{lang}.wav")) + glob.glob(os.path.join(args.audio_dir, f"{lang}_*.wav"))
            if matches:
                audio_file_path = matches[0]

        is_real_audio = bool(audio_file_path)

        can_run = False
        reason = ""
        if is_real_audio:
            if has_stt:
                can_run = True
            else:
                reason = "STT model missing"
        else:
            if has_stt and has_tts:
                can_run = True
            else:
                if not has_stt and not has_tts:
                    reason = "STT & TTS models missing"
                elif not has_stt:
                    reason = "STT model missing"
                else:
                    reason = "TTS model missing"

        print(f"\n[{idx}/{total_languages}] Benchmarking {lang.upper()} ({'EMERGENCY SOS' if is_sos else 'NORMAL'}):")
        
        if not can_run:
            print(f"  -> SKIPPED (no model: {reason})")
            results.append({
                "id": idx,
                "lang": lang,
                "is_emergency": is_sos,
                "target": target_text,
                "recognized": "SKIPPED (no model)",
                "wer": None,
                "cer": None,
                "audio_dur_sec": 0,
                "t_tts_sec": 0,
                "rtf_tts": 0,
                "t_stt_sec": 0,
                "rtf_stt": 0,
                "t_total_sec": 0,
                "packet_bytes": 0,
                "bitrate_bps": 0,
                "savings_pcm": 0,
                "savings_opus": 0,
                "skipped": True,
                "source": "None",
                "stt_model": stt_model_name if has_stt else "Missing",
                "tts_model": tts_model_name if has_tts else "Missing"
            })
            continue

        tested_languages += 1
        print(f"  Input Target:   \"{target_text}\"")
        print(f"  STT Model Loaded: {stt_model_name}")
        
        t_tts = 0.0
        audio_dur = 0.0
        rtf_tts = 0.0
        pcm_bytes = None
        source_label = "Real"

        if is_real_audio:
            pcm_bytes, sr_real, audio_dur = load_wav(audio_file_path)
            sr = sr_real
            print(f"  Audio Source:   Real ({audio_file_path})")
        else:
            print(f"  TTS Model Loaded: {tts_model_name}")
            source_label = "Synthetic"
            wav_file = out_dir / f"bench_audio_{idx}_{lang}.wav"
            tts_res = tts.synthesize(target_text, lang=lang, output_wav=str(wav_file))
            t_tts = tts_res["synthesis_time_sec"]
            audio_dur = tts_res["audio_duration_sec"]
            rtf_tts = tts_res["rtf"]
            pcm_bytes = tts_res["pcm_bytes"]

        if stt.lang != lang:
            stt.load_model(lang)

        stt_res = stt.transcribe_stream(pcm_bytes, sample_rate=sr)
        recognized_text = stt_res["transcript"]
        t_stt = stt_res["inference_time_sec"]
        rtf_stt = stt_res["rtf"]

        wer = compute_wer(target_text, recognized_text)
        cer = None
        if lang != "en":
            cer = compute_cer(target_text, recognized_text)

        lang_id = LANG_CODE_TO_ID.get(lang, 0)
        pkt = TantraPacket(
            text=target_text,
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
            "recognized": recognized_text if recognized_text else "[NO SPEECH RECOGNIZED]",
            "wer": wer,
            "cer": cer,
            "audio_dur_sec": audio_dur,
            "t_tts_sec": t_tts,
            "rtf_tts": rtf_tts,
            "t_stt_sec": t_stt,
            "rtf_stt": rtf_stt,
            "t_total_sec": round(t_tts + t_stt, 3),
            "packet_bytes": packet_bytes,
            "bitrate_bps": telemetry["effective_bps"],
            "savings_pcm": telemetry["savings_vs_pcm"],
            "savings_opus": telemetry["savings_vs_opus"],
            "skipped": False,
            "source": source_label,
            "stt_model": stt_model_name,
            "tts_model": tts_model_name
        }
        results.append(row)

        print(f"  Recognized:     \"{row['recognized']}\"")
        if cer is not None:
            print(f"  Error Rates:    WER: {wer * 100:.1f}% | CER: {cer * 100:.1f}%")
        else:
            print(f"  Error Rates:    WER: {wer * 100:.1f}%")
            
        if not is_real_audio:
            print(f"  TTS Speed:      {audio_dur:.2f}s audio synthesized in {t_tts:.3f}s (RTF: {rtf_tts:.3f})")
        print(f"  STT Speed:      Infer: {t_stt:.3f}s (RTF: {rtf_stt:.3f})")
        print(f"  End-to-End Lat: {row['t_total_sec']}s (TTS + STT Processing)")
        print(f"  iTantra Packet: {packet_bytes} Bytes | Bitrate: {telemetry['effective_bps']} bps | Saved vs PCM: {telemetry['savings_vs_pcm']}%")

    report_path = out_dir / "BENCHMARK_REPORT.md"
    
    valid_results = [r for r in results if not r.get("skipped")]
    
    if valid_results:
        avg_wer = sum(r["wer"] for r in valid_results) / len(valid_results)
        avg_rtf_tts = sum(r["rtf_tts"] for r in valid_results if r["source"] == "Synthetic") / max(1, len([r for r in valid_results if r["source"] == "Synthetic"]))
        avg_rtf_stt = sum(r["rtf_stt"] for r in valid_results) / len(valid_results)
        avg_savings_pcm = sum(r["savings_pcm"] for r in valid_results) / len(valid_results)
        avg_savings_opus = sum(r["savings_opus"] for r in valid_results) / len(valid_results)
        avg_packet_bytes = sum(r["packet_bytes"] for r in valid_results) / len(valid_results)
    else:
        avg_wer = avg_rtf_tts = avg_rtf_stt = avg_savings_pcm = avg_savings_opus = avg_packet_bytes = 0.0

    with open(report_path, "w", encoding="utf-8") as f:
        f.write("# iTantra 10-Language Verification & Benchmark Report\n\n")
        f.write(f"**Status**: {tested_languages}/{total_languages} Languages Verified Offline\n\n")
        f.write("## Executive Summary\n\n")
        f.write(f"- **Languages Evaluated ({tested_languages}/{total_languages})**\n")
        
        if valid_results:
            f.write(f"- **Mean Word Error Rate (WER)**: {avg_wer * 100:.2f}%\n")
            if any(r["source"] == "Synthetic" for r in valid_results):
                f.write(f"- **TTS Real-Time Factor (RTF)**: {avg_rtf_tts:.3f} ({(1/max(avg_rtf_tts, 0.001)):.1f}x faster than real-time)\n")
            f.write(f"- **STT Real-Time Factor (RTF)**: {avg_rtf_stt:.3f} ({(1/max(avg_rtf_stt, 0.001)):.1f}x faster than real-time)\n")
            f.write(f"- **Average Packet Size**: {avg_packet_bytes:.1f} bytes per spoken transmission\n")
            f.write(f"- **Bandwidth Reduction vs Raw Audio (PCM 16kHz)**: **{avg_savings_pcm:.2f}%**\n")
            f.write(f"- **Bandwidth Reduction vs Opus Voice (24 kbps)**: **{avg_savings_opus:.2f}%**\n\n")
        
        f.write("## Detailed 10-Language Test Matrix\n\n")
        f.write("| # | Language | Priority | Source | Audio (s) | STT Lag (s) [RTF] | Total Lag (s) | Packet (B) | Band Saved | WER | CER |\n")
        f.write("|---|---|---|---|---|---|---|---|---|---|---|\n")
        lang_names = {
            "en": "English", "hi": "Hindi", "bn": "Bengali", "gu": "Gujarati", "mr": "Marathi",
            "kn": "Kannada", "ml": "Malayalam", "ta": "Tamil", "te": "Telugu", "or": "Odia"
        }
        for r in results:
            lname = lang_names.get(r["lang"], r["lang"].upper())
            prio = "🚨 SOS" if r["is_emergency"] else "Radio"
            if r.get("skipped"):
                f.write(f"| {r['id']} | **{lname}** (`{r['lang']}`) | {prio} | SKIPPED | - | - | - | - | - | - | - |\n")
            else:
                cer_str = f"{r['cer'] * 100:.1f}%" if r['cer'] is not None else "N/A"
                wer_str = f"{r['wer'] * 100:.1f}%"
                f.write(f"| {r['id']} | **{lname}** (`{r['lang']}`) | {prio} | {r['source']} | {r['audio_dur_sec']:.2f} | {r['t_stt_sec']:.2f} [{r['rtf_stt']:.2f}] | {r['t_total_sec']:.2f} | {r['packet_bytes']} | {r['savings_pcm']:.1f}% | {wer_str} | {cer_str} |\n")
        
        f.write("\n## Model Provenance & Status\n")
        f.write("| Language | STT Model | TTS Model | Status |\n")
        f.write("|---|---|---|---|\n")
        for r in results:
            lname = lang_names.get(r["lang"], r["lang"].upper())
            stt_mod = r["stt_model"]
            tts_mod = r["tts_model"]
            status = "❌ Missing" if r.get("skipped") else "✅ Loaded"
            f.write(f"| {lname} | `{stt_mod}` | `{tts_mod}` | {status} |\n")

    print("\n" + "=" * 80)
    print(f"[SUCCESS] Benchmark report generated at: {report_path}")
    if valid_results:
        print(f"Languages Tested: {tested_languages}/{total_languages} | Mean WER: {avg_wer * 100:.2f}% | Bandwidth Saved vs PCM: {avg_savings_pcm:.2f}%")
    print("=" * 80)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="iTantra Model Benchmark Suite")
    parser.add_argument("--audio-dir", type=str, help="Directory containing real .wav files (e.g. hi.wav)")
    args = parser.parse_args()
    
    run_benchmarks(args)
