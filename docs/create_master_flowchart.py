import math
import os
from PIL import Image, ImageDraw, ImageFont

OUTPUT_PNG = "/home/naman/Desktop/SIH/Backup/docs/iTantra_Architecture_Flowchart.png"
OUTPUT_PDF = "/home/naman/Desktop/SIH/Backup/docs/iTantra_Architecture_Flowchart.pdf"

FONT_BOLD = "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"
FONT_REG = "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"

def get_font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except:
        return ImageFont.load_default()

W, H = 2400, 1550
img = Image.new("RGBA", (W, H), (248, 250, 252, 255))
draw = ImageDraw.Draw(img)

# Typography
f_title = get_font(FONT_BOLD, 42)
f_sub = get_font(FONT_REG, 22)
f_sec = get_font(FONT_BOLD, 20)
f_box_t = get_font(FONT_BOLD, 20)
f_box_d = get_font(FONT_REG, 15)
f_metric = get_font(FONT_BOLD, 16)
f_arr = get_font(FONT_BOLD, 14)
f_badge = get_font(FONT_BOLD, 18)

# 1. Header Section
draw.rectangle([0, 0, W, 125], fill=(15, 23, 42, 255))
draw.text((60, 30), "iTANTRA — END-TO-END SYSTEM ARCHITECTURE & DATA FLOW", fill=(255, 255, 255, 255), font=f_title)
draw.text((60, 80), "100% Offline Multilingual Neural Transceiver • 99.92% Bandwidth Saved • Zero Cloud Dependencies", fill=(56, 189, 248, 255), font=f_sub)

# Right Header Badges
draw.rounded_rectangle([W - 470, 32, W - 60, 92], radius=12, fill=(30, 41, 59, 255), outline=(51, 65, 85, 255), width=2)
draw.text((W - 265, 62), "ISRO PS SIH26173 • APACHE 2.0 / MIT", fill=(245, 158, 11, 255), font=f_badge, anchor="mm")

# Helper: Draw smooth rounded box
def draw_card(x, y, w, h, bg_col, border_col, title, desc_lines, metric_badge=None, badge_col=None):
    draw.rounded_rectangle([x + 3, y + 4, x + w + 3, y + h + 4], radius=14, fill=(0, 0, 0, 15))
    draw.rounded_rectangle([x, y, x + w, y + h], radius=14, fill=bg_col, outline=border_col, width=2)
    draw.text((x + w // 2, y + 24), title, fill=(15, 23, 42, 255), font=f_box_t, anchor="mm")
    draw.line([x + 18, y + 44, x + w - 18, y + 44], fill=border_col, width=1)
    for i, line in enumerate(desc_lines):
        draw.text((x + w // 2, y + 66 + i * 22), line, fill=(71, 85, 105, 255), font=f_box_d, anchor="mm")
    if metric_badge:
        bw, bh = 155, 28
        bx = x + (w - bw) // 2
        by = y + h - bh - 10
        draw.rounded_rectangle([bx, by, bx + bw, by + bh], radius=8, fill=badge_col[0])
        draw.text((bx + bw // 2, by + bh // 2), metric_badge, fill=badge_col[1], font=f_metric, anchor="mm")

# Helper: Draw clean arrow
def draw_arrow(x1, y1, x2, y2, label=None, col=(26, 111, 196, 255), width=4, label_y_offset=-16):
    draw.line([x1, y1, x2, y2], fill=col, width=width)
    angle = math.atan2(y2 - y1, x2 - x1)
    hl = 14
    hw = 8
    p1 = (x2 - hl * math.cos(angle) + hw * math.sin(angle), y2 - hl * math.sin(angle) - hw * math.cos(angle))
    p2 = (x2 - hl * math.cos(angle) - hw * math.sin(angle), y2 - hl * math.sin(angle) + hw * math.cos(angle))
    draw.polygon([(x2, y2), p1, p2], fill=col)
    if label:
        mx = (x1 + x2) // 2
        my = (y1 + y2) // 2 + label_y_offset
        bbox = draw.textbbox((mx, my), label, font=f_arr, anchor="mm")
        pad_x, pad_y = 6, 3
        draw.rounded_rectangle([bbox[0] - pad_x, bbox[1] - pad_y, bbox[2] + pad_x, bbox[3] + pad_y], radius=5, fill=(255, 255, 255, 240))
        draw.text((mx, my), label, fill=col, font=f_arr, anchor="mm")

bw, bh = 360, 205
gap = 95
x_coords = [75 + i * (bw + gap) for i in range(5)]

# ----------------- LANE 1: TRANSMITTER (Top) -----------------
y_lane1 = 160
h_lane1 = 295
draw.rounded_rectangle([50, y_lane1, W - 50, y_lane1 + h_lane1], radius=18, fill=(255, 255, 255, 255), outline=(203, 213, 225, 255), width=2)
# Lane 1 Header Pill (Completely inside box)
draw.rounded_rectangle([75, y_lane1 + 14, 520, y_lane1 + 50], radius=10, fill=(26, 111, 196, 255))
draw.text((297, y_lane1 + 32), "STAGE 1: TRANSMITTER (VOICE -> PACKET)", fill=(255, 255, 255, 255), font=f_sec, anchor="mm")

by1 = y_lane1 + 68

# Box 1
draw_card(x_coords[0], by1, bw, bh, (238, 242, 255, 255), (99, 102, 241, 255),
          "Audio Ingestion (PTT)", 
          ["• Push-To-Talk (PTT) Mic Capture", "• 16 kHz Mono PCM Sampling", "• Dynamic Audio Visualizer Flow", "• Zero Cloud / 100% Local"],
          "16 kHz PCM", ((224, 231, 255, 255), (67, 56, 202, 255)))

draw_arrow(x_coords[0] + bw, by1 + bh // 2, x_coords[1], by1 + bh // 2, "Raw PCM")

# Box 2
draw_card(x_coords[1], by1, bw, bh, (254, 243, 199, 255), (245, 158, 11, 255),
          "Noise Filter & Energy VAD", 
          ["• 300Hz–3.4kHz Bandpass Filter", "• RMS Energy Thresholding", "• Cuts 95% Siren/Flood Noise", "• Trims Silence & Saves Compute"],
          "< 1ms Latency", ((254, 243, 199, 255), (180, 83, 9, 255)))

draw_arrow(x_coords[1] + bw, by1 + bh // 2, x_coords[2], by1 + bh // 2, "Filtered Audio")

# Box 3
draw_card(x_coords[2], by1, bw, bh, (220, 252, 231, 255), (34, 197, 94, 255),
          "Vosk Edge ASR Engine", 
          ["• On-Device Kaldi Acoustic Model", "• WFST Graph Decoding on ARM", "• Hindi (hi-IN) & English (en-IN)", "• 0.0% WER on Tactical Directives"],
          "RTF: 0.163 (6.1x)", ((220, 252, 231, 255), (21, 128, 61, 255)))

draw_arrow(x_coords[2] + bw, by1 + bh // 2, x_coords[3], by1 + bh // 2, "Decoded Text")

# Box 4
draw_card(x_coords[3], by1, bw, bh, (243, 232, 255, 255), (168, 85, 247, 255),
          "Language & Intent Parser", 
          ["• Automatic Script Identification", "• Emergency Keyword Parsing", "• Flags SOS / Distress Intents", "• Attaches GPS Coordinates"],
          "Auto-LID Enabled", ((243, 232, 255, 255), (126, 34, 206, 255)))

draw_arrow(x_coords[3] + bw, by1 + bh // 2, x_coords[4], by1 + bh // 2, "Tagged Meta")

# Box 5
draw_card(x_coords[4], by1, bw + 60, bh, (219, 234, 254, 255), (37, 99, 235, 255),
          "TantraPacket Encoder", 
          ["• Ultra-Dense Binary Serialization", "• AES-256 GCM Payload Encryption", "• CRC-32 Frame Integrity Checksum", "• Mean Size: 106.5 Bytes (65B SOS)"],
          "99.92% Saved", ((220, 252, 231, 255), (21, 128, 61, 255)))

# ----------------- LANE 2: RF CHANNEL & ROUTING (Middle) -----------------
y_lane2 = 490
h_lane2 = 410
draw.rounded_rectangle([50, y_lane2, W - 50, y_lane2 + h_lane2], radius=18, fill=(255, 255, 255, 255), outline=(203, 213, 225, 255), width=2)
# Lane 2 Header Pill
draw.rounded_rectangle([75, y_lane2 + 14, 580, y_lane2 + 50], radius=10, fill=(217, 119, 6, 255))
draw.text((327, y_lane2 + 32), "STAGE 2: 3-TIER PRIORITY ROUTING & RF MESH", fill=(255, 255, 255, 255), font=f_sec, anchor="mm")

# Connect Box 5 down to Lane 2 Priority Gate
enc_cx = x_coords[4] + (bw + 60) // 2
draw.line([enc_cx, by1 + bh, enc_cx, y_lane2 + 65], fill=(37, 99, 235, 255), width=4)
draw_arrow(enc_cx, y_lane2 + 65, 1720, y_lane2 + 65, "106.5-Byte Packet", (37, 99, 235, 255), 4)

# Decision Diamond: Priority Gate
dia_cx, dia_cy = 1580, y_lane2 + 215
dia_w, dia_h = 120, 75
draw.polygon([(dia_cx, dia_cy - dia_h), (dia_cx + dia_w, dia_cy), (dia_cx, dia_cy + dia_h), (dia_cx - dia_w, dia_cy)], 
             fill=(254, 243, 199, 255), outline=(245, 158, 11, 255), width=2)
draw.text((dia_cx, dia_cy - 12), "3-Tier Priority", fill=(180, 83, 9, 255), font=f_box_t, anchor="mm")
draw.text((dia_cx, dia_cy + 14), "Channel Gate", fill=(180, 83, 9, 255), font=f_box_t, anchor="mm")

# Connect from 1720 to top of diamond
draw.line([1720, y_lane2 + 65, dia_cx, y_lane2 + 65], fill=(37, 99, 235, 255), width=4)
draw_arrow(dia_cx, y_lane2 + 65, dia_cx, dia_cy - dia_h, "", (37, 99, 235, 255), 4)

# 3 Priority Cards:
p_card_w = 480
p_card_h = 95
# Card 1: SOS
draw_card(910, y_lane2 + 55, p_card_w, p_card_h, (254, 226, 226, 255), (239, 68, 68, 255),
          "Priority 1: Emergency SOS",
          ["• Preempts all active RF traffic", "• Forces 100% volume alarm override (65B Beacon)"])
draw_arrow(dia_cx - dia_w, dia_cy - 40, 910 + p_card_w, y_lane2 + 102, "SOS Flag", (220, 38, 38, 255), 4)

# Card 2: Tactical
draw_card(910, y_lane2 + 170, p_card_w, p_card_h, (254, 243, 199, 255), (245, 158, 11, 255),
          "Priority 2: Tactical Directives",
          ["• Expedited RF channel queue", "• Visual amber flashing + tactile haptic alert"])
draw_arrow(dia_cx - dia_w, dia_cy, 910 + p_card_w, y_lane2 + 217, "Tactical", (217, 119, 6, 255), 4)

# Card 3: Normal
draw_card(910, y_lane2 + 285, p_card_w, p_card_h, (220, 252, 231, 255), (34, 197, 94, 255),
          "Priority 3: Normal Status",
          ["• Best-effort LoRa mesh queue", "• Routine status reports & non-preemptive delivery"])
draw_arrow(dia_cx - 45, dia_cy + dia_h, 910 + p_card_w, y_lane2 + 332, "Normal", (22, 163, 74, 255), 4)

# Multi-Bearer RF Box on Left of Lane 2
rf_w, rf_h = 500, 260
draw_card(75, y_lane2 + 80, rf_w, rf_h, (238, 242, 255, 255), (79, 70, 229, 255),
          "Multi-Bearer RF Physical Layer (PHY)",
          ["• Semtech SX1262 LoRa @ 865.2 MHz (GoI ISM Band)",
           "• 42 ms RF Airtime per spoken sentence (10–15 km Reach)",
           "• Bluetooth RFCOMM SPP Bridge to Android Phone",
           "• Ad-Hoc Wi-Fi Hotspot UDP Broadcast (Port 5005)"],
          "42 ms Airtime • 10-15 km", ((224, 231, 255, 255), (67, 56, 202, 255)))

# Connect 3 Priority cards to RF box
bus_x = 680
draw.line([910, y_lane2 + 102, bus_x, y_lane2 + 102], fill=(220, 38, 38, 255), width=3)
draw.line([910, y_lane2 + 217, bus_x, y_lane2 + 217], fill=(217, 119, 6, 255), width=3)
draw.line([910, y_lane2 + 332, bus_x, y_lane2 + 332], fill=(22, 163, 74, 255), width=3)

draw.line([bus_x, y_lane2 + 102, bus_x, y_lane2 + 332], fill=(79, 70, 229, 255), width=4)
draw_arrow(bus_x, y_lane2 + 217, 75 + rf_w, y_lane2 + 217, "Scheduled Frame", (79, 70, 229, 255), 4)

# ----------------- LANE 3: RECEIVER NODE (Bottom) -----------------
y_lane3 = 935
h_lane3 = 295
draw.rounded_rectangle([50, y_lane3, W - 50, y_lane3 + h_lane3], radius=18, fill=(255, 255, 255, 255), outline=(203, 213, 225, 255), width=2)
# Lane 3 Header Pill
draw.rounded_rectangle([75, y_lane3 + 14, 520, y_lane3 + 50], radius=10, fill=(22, 163, 74, 255))
draw.text((297, y_lane3 + 32), "STAGE 3: RECEIVER (PACKET -> VOICE)", fill=(255, 255, 255, 255), font=f_sec, anchor="mm")

# Connect Lane 2 Multi-Bearer down to Lane 3 Box 1
draw_arrow(75 + rf_w // 2, y_lane2 + 80 + rf_h, 75 + bw // 2, y_lane3 + 68, "Over-The-Air RF Broadcast", (79, 70, 229, 255), 5)

by3 = y_lane3 + 68

# Recv Box 1
draw_card(x_coords[0], by3, bw, bh, (238, 242, 255, 255), (99, 102, 241, 255),
          "RF Demodulation & Decrypt",
          ["• LoRa / BT RFCOMM Ingestion", "• CRC-32 Frame Integrity Check", "• AES-256 GCM Authenticated Decrypt", "• Drops Corrupted RF Frames"],
          "CRC-32 Validated", ((224, 231, 255, 255), (67, 56, 202, 255)))

draw_arrow(x_coords[0] + bw, by3 + bh // 2, x_coords[1], by3 + bh // 2, "Verified Packet")

# Recv Box 2
draw_card(x_coords[1], by3, bw, bh, (219, 234, 254, 255), (37, 99, 235, 255),
          "TantraPacket Decoder",
          ["• Unpacks Binary Header & Payload", "• Extracts Language Code & Priority", "• Decompresses UTF-8 Text Token", "• Telemetry HUD Logging"],
          "106.5 B Unpacked", ((219, 234, 254, 255), (30, 64, 175, 255)))

draw_arrow(x_coords[1] + bw, by3 + bh // 2, x_coords[2], by3 + bh // 2, "Priority Route")

# Recv Box 3
draw_card(x_coords[2], by3, bw, bh, (254, 226, 226, 255), (239, 68, 68, 255),
          "Dispatch & Alarm Guard",
          ["• Checks Priority 1 SOS Distress Flag", "• Forces STREAM_ALARM to 100%", "• Triggers Continuous Haptic Vibration", "• Screen Flashes High-Contrast Red"],
          "100% Audio Volume", ((254, 226, 226, 255), (185, 28, 28, 255)))

draw_arrow(x_coords[2] + bw, by3 + bh // 2, x_coords[3], by3 + bh // 2, "Clean Text")

# Recv Box 4
draw_card(x_coords[3], by3, bw, bh, (243, 232, 255, 255), (168, 85, 247, 255),
          "Piper Neural VITS TTS",
          ["• Fast On-Device VITS Neural Model", "• Multilingual Indian Voice Synthesis", "• Zero Cloud API Latency", "• 20.2x Faster than Real-Time"],
          "RTF: 0.049 (20.2x)", ((243, 232, 255, 255), (126, 34, 206, 255)))

draw_arrow(x_coords[3] + bw, by3 + bh // 2, x_coords[4], by3 + bh // 2, "Synthesized Audio")

# Recv Box 5
draw_card(x_coords[4], by3, bw + 60, bh, (220, 252, 231, 255), (34, 197, 94, 255),
          "Loudspeaker Voice Replay",
          ["• Instant Natural Audio Playback", "• Total E2E Latency: 0.69s – 0.96s", "• Clear Tactical Intelligibility", "• No Cell Towers. Zero Internet."],
          "Total Lag: < 0.96s", ((220, 252, 231, 255), (21, 128, 61, 255)))

# ----------------- FOOTER TELEMETRY BAR -----------------
draw.rectangle([0, H - 95, W, H], fill=(15, 23, 42, 255))

footer_metrics = [
    ("MEAN WER", "14.28%"),
    ("BANDWIDTH SAVED", "99.92% (Raw) / 98.63% (Opus)"),
    ("TTS SPEED FACTOR", "0.049 RTF (20.2x Faster)"),
    ("STT SPEED FACTOR", "0.163 RTF (6.1x Faster)"),
    ("LORA RF AIRTIME", "42 ms @ 865.2 MHz"),
    ("END-TO-END TURNAROUND", "0.69s – 0.96s")
]

for idx, (label, val) in enumerate(footer_metrics):
    mx = 70 + idx * 385
    draw.text((mx, H - 68), label, fill=(148, 163, 184, 255), font=get_font(FONT_BOLD, 15))
    draw.text((mx, H - 36), val, fill=(56, 189, 248, 255), font=get_font(FONT_BOLD, 20))

img.save(OUTPUT_PNG)
print(f"Master flowchart successfully saved to {OUTPUT_PNG}")

img_rgb = img.convert("RGB")
img_rgb.save(OUTPUT_PDF)
print(f"Master flowchart PDF successfully saved to {OUTPUT_PDF}")
