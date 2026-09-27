import os
import sys
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE

OUTPUT_PATH = "/home/naman/Desktop/SIH/Backup/docs/iTantra_SIH_Presentation.pptx"
ASSETS_DIR = "/home/naman/Desktop/SIH/Backup/docs/assets"
SIH_LOGO = "/home/naman/Desktop/SIH/Backup/docs/sih_logo_extracted.png"

# Color Palette
COLOR_NAVY_DARK = RGBColor(15, 23, 42)
COLOR_TEXT_DARK = RGBColor(30, 41, 59)
COLOR_TEXT_MUTED = RGBColor(71, 85, 105)
COLOR_WHITE = RGBColor(255, 255, 255)
COLOR_PRIMARY_BLUE = RGBColor(26, 111, 196)
COLOR_DEEP_BLUE = RGBColor(11, 94, 215)
COLOR_FOOTER_BLUE = RGBColor(19, 81, 168)
COLOR_CARD_BORDER = RGBColor(226, 232, 240)

# Accent badge colors
COLOR_PINK_BG = RGBColor(255, 228, 235)
COLOR_PINK_BORDER = RGBColor(233, 30, 99)
COLOR_PINK_HEADER = RGBColor(216, 27, 96)

COLOR_GREEN_BG = RGBColor(232, 248, 239)
COLOR_GREEN_BORDER = RGBColor(34, 197, 94)
COLOR_GREEN_HEADER = RGBColor(21, 128, 61)

COLOR_BLUE_BG = RGBColor(235, 244, 255)
COLOR_BLUE_BORDER = RGBColor(59, 130, 246)
COLOR_BLUE_HEADER = RGBColor(29, 78, 216)

COLOR_PURPLE_BG = RGBColor(246, 235, 255)
COLOR_PURPLE_BORDER = RGBColor(168, 85, 247)
COLOR_PURPLE_HEADER = RGBColor(126, 34, 206)

COLOR_AMBER_BG = RGBColor(254, 243, 199)
COLOR_AMBER_BORDER = RGBColor(245, 158, 11)

COLOR_RED_BG = RGBColor(254, 226, 226)
COLOR_RED_BORDER = RGBColor(239, 68, 68)

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)
blank_layout = prs.slide_layouts[6]

def add_header_and_footer(slide, slide_num, title_text, subtitle_text=None):
    # Top-Left Oval Badge
    oval = slide.shapes.add_shape(MSO_SHAPE.OVAL, Inches(0.5), Inches(0.2), Inches(1.8), Inches(1.05))
    oval.fill.solid()
    oval.fill.fore_color.rgb = RGBColor(255, 255, 255)
    oval.line.color.rgb = RGBColor(147, 51, 234)
    oval.line.width = Pt(2)
    tf = oval.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = MSO_ANCHOR.MIDDLE
    p1 = tf.paragraphs[0]
    p1.text = "iTantra"
    p1.alignment = PP_ALIGN.CENTER
    p1.font.bold = True
    p1.font.size = Pt(15)
    p1.font.name = "Liberation Sans"
    p1.font.color.rgb = RGBColor(30, 41, 59)
    p2 = tf.add_paragraph()
    p2.text = "Voice Transceiver"
    p2.alignment = PP_ALIGN.CENTER
    p2.font.bold = False
    p2.font.size = Pt(10.5)
    p2.font.name = "Liberation Sans"
    p2.font.color.rgb = COLOR_PRIMARY_BLUE

    # Top Title
    title_box = slide.shapes.add_textbox(Inches(2.5), Inches(0.2), Inches(8.0), Inches(0.6))
    tf_title = title_box.text_frame
    tf_title.word_wrap = True
    p_t = tf_title.paragraphs[0]
    p_t.text = title_text
    p_t.alignment = PP_ALIGN.CENTER
    p_t.font.bold = True
    p_t.font.size = Pt(28)
    p_t.font.name = "Liberation Sans"
    p_t.font.color.rgb = RGBColor(15, 23, 42)

    if subtitle_text:
        sub_box = slide.shapes.add_textbox(Inches(2.4), Inches(0.82), Inches(8.2), Inches(0.45))
        tf_sub = sub_box.text_frame
        tf_sub.word_wrap = True
        p_s = tf_sub.paragraphs[0]
        p_s.text = subtitle_text
        p_s.alignment = PP_ALIGN.CENTER
        p_s.font.bold = True
        p_s.font.size = Pt(12)
        p_s.font.name = "Liberation Sans"
        p_s.font.color.rgb = COLOR_PRIMARY_BLUE

    # Top-Right SIH Logo
    if os.path.exists(SIH_LOGO):
        slide.shapes.add_picture(SIH_LOGO, Inches(10.8), Inches(0.18), width=Inches(2.0))

    # Bottom Dark Blue Bar
    bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(7.1), Inches(13.333), Inches(0.4))
    bar.fill.solid()
    bar.fill.fore_color.rgb = COLOR_FOOTER_BLUE
    bar.line.fill.background()
    tf_bar = bar.text_frame
    tf_bar.vertical_anchor = MSO_ANCHOR.MIDDLE
    p_b = tf_bar.paragraphs[0]
    p_b.text = "@SIH Idea submission"
    p_b.alignment = PP_ALIGN.CENTER
    p_b.font.size = Pt(11)
    p_b.font.name = "Liberation Sans"
    p_b.font.color.rgb = COLOR_WHITE

    # Page number
    p_num_box = slide.shapes.add_textbox(Inches(12.5), Inches(7.08), Inches(0.6), Inches(0.4))
    tf_num = p_num_box.text_frame
    p_n = tf_num.paragraphs[0]
    p_n.text = str(slide_num)
    p_n.alignment = PP_ALIGN.RIGHT
    p_n.font.bold = True
    p_n.font.size = Pt(12)
    p_n.font.name = "Liberation Sans"
    p_n.font.color.rgb = COLOR_WHITE

def create_card_with_badge(slide, x, y, w, h, badge_text, badge_color_bg, badge_color_border, badge_text_color, bullets):
    badge = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y, Inches(2.2), Inches(0.4))
    badge.fill.solid()
    badge.fill.fore_color.rgb = badge_color_bg
    badge.line.color.rgb = badge_color_border
    badge.line.width = Pt(1.5)
    tf_b = badge.text_frame
    tf_b.vertical_anchor = MSO_ANCHOR.MIDDLE
    p = tf_b.paragraphs[0]
    p.text = badge_text
    p.alignment = PP_ALIGN.CENTER
    p.font.bold = True
    p.font.size = Pt(13)
    p.font.name = "Liberation Sans"
    p.font.color.rgb = badge_text_color

    tb = slide.shapes.add_textbox(x, y + Inches(0.42), w, h - Inches(0.42))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.margin_left = Inches(0.04)
    tf.margin_right = Inches(0.04)
    tf.margin_top = Inches(0.04)

    for i, bullet in enumerate(bullets):
        p_item = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p_item.text = "• " + bullet
        p_item.font.size = Pt(9.5)
        p_item.font.name = "Liberation Sans"
        p_item.font.color.rgb = COLOR_TEXT_DARK
        p_item.space_after = Pt(2.5)

# ==========================================
# SLIDE 1: OVERVIEW & PROPOSAL (PAGE 2)
# ==========================================
slide1 = prs.slides.add_slide(blank_layout)
add_header_and_footer(slide1, 2, "iTANTRA", 
                      "AI-Powered Real-Time Multilingual Neural Transceiver for Low-Bitrate Disaster & Tactical Radios")

phone_hub_path = os.path.join(ASSETS_DIR, "slide1_phone_hub.png")
if os.path.exists(phone_hub_path):
    slide1.shapes.add_picture(phone_hub_path, Inches(4.7), Inches(1.5), width=Inches(3.9), height=Inches(3.9))

create_card_with_badge(
    slide1, Inches(0.5), Inches(1.4), Inches(4.1), Inches(2.4),
    "Challenges", COLOR_PINK_BG, COLOR_PINK_BORDER, COLOR_PINK_HEADER,
    [
        "Total cellular & telecom network blackout during natural disasters (floods, earthquakes, cyclones).",
        "Standard voice codecs (256 kbps PCM / 24 kbps Opus) collapse over narrowband radio links (< 1 kbps).",
        "Illiterate disaster victims and stressed tactical personnel cannot type or navigate mobile keyboards.",
        "Critical multilingual barrier between national relief teams (NDRF) and regional disaster victims."
    ]
)

create_card_with_badge(
    slide1, Inches(0.5), Inches(3.95), Inches(4.1), Inches(2.4),
    "Proposed Solution", COLOR_BLUE_BG, COLOR_BLUE_BORDER, COLOR_BLUE_HEADER,
    [
        "100% Offline Edge STT: On-device Vosk ASR transcribes spoken voice to text tokens with zero internet.",
        "TantraPacket Binary Protocol: Encapsulates language ID, priority, and text into tiny 106.5-byte packets.",
        "Multi-Bearer P2P Radio: Transmits over de-licensed LoRa (865.2 MHz), Bluetooth RFCOMM & Wi-Fi Hotspots.",
        "On-Device Neural TTS: Receiver decodes 106.5 B packet and synthesizes natural voice via Piper ONNX."
    ]
)

create_card_with_badge(
    slide1, Inches(8.7), Inches(1.4), Inches(4.1), Inches(2.4),
    "Value Proposition", COLOR_GREEN_BG, COLOR_GREEN_BORDER, COLOR_GREEN_HEADER,
    [
        "99.92% Bandwidth Reduction: Slashes data rate from 256,000 bps down to ~132 bps.",
        "100% Offline & Open-Source: Apache-2.0 & MIT stack — strictly zero proprietary cloud APIs.",
        "Sub-Second Latency: 0.69s – 0.96s total voice-in to voice-out turnaround lag.",
        "10–15 km Voice Reach: Ultra-low 42 ms RF airtime penetrates heavy rubble and mountain valleys.",
        "Life-Saving SOS Override: 65-Byte high-priority distress beacon with 100% volume alarm preemption."
    ]
)

create_card_with_badge(
    slide1, Inches(8.7), Inches(3.95), Inches(4.1), Inches(2.4),
    "Key Features", COLOR_PURPLE_BG, COLOR_PURPLE_BORDER, COLOR_PURPLE_HEADER,
    [
        "Ultra-Low-Bitrate Neural Transceiver (106.5 B avg packet size vs 64 kbps standard audio).",
        "3-Tier Preemptive Priority Gate (SOS Distress > Tactical Directives > Normal Chat).",
        "Multilingual Voice Pipeline (Hindi & Indian English verified, 8 scheduled languages modular).",
        "Multi-Bearer Ad-Hoc Mesh Routing over LoRa SX1262 & Bluetooth RFCOMM.",
        "Cryptographic Integrity (AES-256 GCM payload encryption + CRC-32 verification)."
    ]
)

pill = slide1.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(2.3), Inches(6.45), Inches(8.7), Inches(0.52))
pill.fill.solid()
pill.fill.fore_color.rgb = RGBColor(235, 244, 255)
pill.line.color.rgb = COLOR_PRIMARY_BLUE
pill.line.width = Pt(1.5)
tf_pill = pill.text_frame
tf_pill.vertical_anchor = MSO_ANCHOR.MIDDLE
p_p1 = tf_pill.paragraphs[0]
p_p1.text = "iTantra = Offline Voice ➔ 106.5-Byte Packet ➔ 42ms LoRa Airtime ➔ Neural Audio (99.92% Saved)"
p_p1.alignment = PP_ALIGN.CENTER
p_p1.font.bold = True
p_p1.font.size = Pt(11)
p_p1.font.name = "Liberation Sans"
p_p1.font.color.rgb = COLOR_PRIMARY_BLUE

p_p2 = tf_pill.add_paragraph()
p_p2.text = "Lifesaving voice communication when all other networks fail."
p_p2.alignment = PP_ALIGN.CENTER
p_p2.font.size = Pt(9)
p_p2.font.name = "Liberation Sans"
p_p2.font.color.rgb = COLOR_TEXT_MUTED

# ==========================================
# SLIDE 2: TECHNICAL APPROACH (PAGE 3)
# ==========================================
slide2 = prs.slides.add_slide(blank_layout)
add_header_and_footer(slide2, 3, "TECHNICAL APPROACH")

steps = [
    ("Audio Ingestion", "16 kHz Mono PCM\nPTT / Mic Capture"),
    ("Noise Filter & VAD", "Bandpass 300-3400Hz\nSilero VAD Trim"),
    ("Offline STT Engine", "Vosk Edge ASR\nRTF 0.163 (ARM NEON)"),
    ("TantraPacket Enc", "106.5-Byte Binary\nMagic | Seq | CRC32"),
    ("3-Tier Priority Gate", "Preemptive Routing\nChannel Contention"),
    ("Multi-Bearer RF", "LoRa SX1262 (865MHz)\n42ms Airtime / BT"),
    ("Neural TTS Synth", "Piper VITS ONNX\nRTF 0.049 Local Voice")
]

start_x = 0.5
box_w = 1.48
box_h = 1.05
gap = 0.32

for idx, (title, desc) in enumerate(steps):
    bx = Inches(start_x + idx * (box_w + gap))
    by = Inches(1.35)
    
    box = slide2.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, bx, by, Inches(box_w), Inches(box_h))
    box.fill.solid()
    if idx == 4:
        box.fill.fore_color.rgb = RGBColor(219, 234, 254)
        box.line.color.rgb = COLOR_PRIMARY_BLUE
        box.line.width = Pt(2)
    else:
        box.fill.fore_color.rgb = RGBColor(241, 245, 249)
        box.line.color.rgb = RGBColor(203, 213, 225)
        box.line.width = Pt(1)
        
    tf = box.text_frame
    tf.word_wrap = True
    tf.margin_top = Inches(0.08)
    tf.margin_left = Inches(0.04)
    tf.margin_right = Inches(0.04)
    
    p_t = tf.paragraphs[0]
    p_t.text = title
    p_t.alignment = PP_ALIGN.CENTER
    p_t.font.bold = True
    p_t.font.size = Pt(10)
    p_t.font.name = "Liberation Sans"
    p_t.font.color.rgb = COLOR_PRIMARY_BLUE if idx == 4 else COLOR_NAVY_DARK
    
    p_d = tf.add_paragraph()
    p_d.text = desc
    p_d.alignment = PP_ALIGN.CENTER
    p_d.font.size = Pt(8.5)
    p_d.font.name = "Liberation Sans"
    p_d.font.color.rgb = COLOR_TEXT_MUTED
    
    if idx < 6:
        arr_x = bx + Inches(box_w + 0.04)
        arrow = slide2.shapes.add_shape(MSO_SHAPE.RIGHT_ARROW, arr_x, by + Inches(0.38), Inches(0.24), Inches(0.25))
        arrow.fill.solid()
        arrow.fill.fore_color.rgb = COLOR_PRIMARY_BLUE
        arrow.line.fill.background()

branch_y = Inches(2.55)
priorities = [
    ("Priority 3: Normal Chat", COLOR_GREEN_BG, COLOR_GREEN_BORDER, COLOR_GREEN_HEADER, 
     "• Best-effort LoRa mesh queue\n• Routine logistics & status reports\n• Non-preemptive delivery"),
    ("Priority 2: Tactical Alert", COLOR_AMBER_BG, COLOR_AMBER_BORDER, RGBColor(180, 83, 9),
     "• Expedited RF channel queue\n• Visual highlight & haptic prompt\n• Positional directives"),
    ("Priority 1: Emergency SOS", COLOR_RED_BG, COLOR_RED_BORDER, RGBColor(220, 38, 38),
     "• Preempts all active transmissions\n• Forces 100% volume alarm\n• 65-Byte repeating beacon")
]

card_w = 2.05
card_h = 1.05
card_gap = 0.2
for p_idx, (p_title, p_bg, p_border, p_header, p_text) in enumerate(priorities):
    cx = Inches(4.3 + p_idx * (card_w + card_gap))
    p_box = slide2.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, cx, branch_y, Inches(card_w), Inches(card_h))
    p_box.fill.solid()
    p_box.fill.fore_color.rgb = p_bg
    p_box.line.color.rgb = p_border
    p_box.line.width = Pt(1.5)
    
    tf_p = p_box.text_frame
    tf_p.word_wrap = True
    tf_p.margin_top = Inches(0.06)
    tf_p.margin_left = Inches(0.06)
    tf_p.margin_right = Inches(0.06)
    
    p1 = tf_p.paragraphs[0]
    p1.text = p_title
    p1.alignment = PP_ALIGN.CENTER
    p1.font.bold = True
    p1.font.size = Pt(9.5)
    p1.font.name = "Liberation Sans"
    p1.font.color.rgb = p_header
    
    p2 = tf_p.add_paragraph()
    p2.text = p_text
    p2.font.size = Pt(8)
    p2.font.name = "Liberation Sans"
    p2.font.color.rgb = COLOR_TEXT_DARK

# Bottom-Left: Technology Stack
tech_card = slide2.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.5), Inches(3.75), Inches(5.6), Inches(3.15))
tech_card.fill.solid()
tech_card.fill.fore_color.rgb = COLOR_WHITE
tech_card.line.color.rgb = COLOR_PRIMARY_BLUE
tech_card.line.width = Pt(2)

tech_badge = slide2.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.7), Inches(3.62), Inches(2.2), Inches(0.35))
tech_badge.fill.solid()
tech_badge.fill.fore_color.rgb = COLOR_PRIMARY_BLUE
tech_badge.line.fill.background()
tf_tb = tech_badge.text_frame
tf_tb.vertical_anchor = MSO_ANCHOR.MIDDLE
p_tb = tf_tb.paragraphs[0]
p_tb.text = "Technology Stack"
p_tb.alignment = PP_ALIGN.CENTER
p_tb.font.bold = True
p_tb.font.size = Pt(11)
p_tb.font.name = "Liberation Sans"
p_tb.font.color.rgb = COLOR_WHITE

tech_cols = [
    ("Speech AI / ML", ["Vosk Edge ASR", "Piper Neural VITS", "Silero VAD", "Apache-2.0 / MIT"]),
    ("Embedded & RF", ["SX1262 LoRa PHY", "ESP32 C++ Core", "RadioLib Mesh", "BT RFCOMM SPP"]),
    ("Mobile Platform", ["Android 14/15", "Jetpack Compose", "Kotlin Coroutines", "Material 3 Light"]),
    ("Security & Proto", ["TantraPacket Binary", "AES-256 GCM", "CRC-32 Checksum", "Ephemeral RAM"])
]

sub_w = 1.3
for c_idx, (col_title, items) in enumerate(tech_cols):
    col_x = Inches(0.65 + c_idx * 1.33)
    tb_c = slide2.shapes.add_textbox(col_x, Inches(4.05), Inches(sub_w), Inches(2.7))
    tf_c = tb_c.text_frame
    tf_c.word_wrap = True
    tf_c.margin_left = Inches(0.02)
    tf_c.margin_right = Inches(0.02)
    
    p_ct = tf_c.paragraphs[0]
    p_ct.text = col_title
    p_ct.font.bold = True
    p_ct.font.size = Pt(10)
    p_ct.font.name = "Liberation Sans"
    p_ct.font.color.rgb = COLOR_PRIMARY_BLUE
    p_ct.space_after = Pt(4)
    
    for item in items:
        p_item = tf_c.add_paragraph()
        p_item.text = "• " + item
        p_item.font.size = Pt(8.5)
        p_item.font.name = "Liberation Sans"
        p_item.font.color.rgb = COLOR_TEXT_DARK
        p_item.space_after = Pt(2)

proto_path = os.path.join(ASSETS_DIR, "slide2_prototype_card.png")
if os.path.exists(proto_path):
    slide2.shapes.add_picture(proto_path, Inches(6.3), Inches(3.72), width=Inches(6.5), height=Inches(3.2))

# ==========================================
# SLIDE 3: FEASIBILITY AND VIABILITY (PAGE 4)
# ==========================================
slide3 = prs.slides.add_slide(blank_layout)
add_header_and_footer(slide3, 4, "FEASIBILITY AND VIABILITY")

feas_box = slide3.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.5), Inches(1.35), Inches(4.3), Inches(5.55))
feas_box.fill.solid()
feas_box.fill.fore_color.rgb = RGBColor(240, 247, 255)
feas_box.line.color.rgb = COLOR_PRIMARY_BLUE
feas_box.line.width = Pt(1.5)

f_badge = slide3.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.7), Inches(1.5), Inches(3.9), Inches(0.48))
f_badge.fill.solid()
f_badge.fill.fore_color.rgb = COLOR_PRIMARY_BLUE
f_badge.line.fill.background()
tf_fb = f_badge.text_frame
tf_fb.vertical_anchor = MSO_ANCHOR.MIDDLE
p_fb = tf_fb.paragraphs[0]
p_fb.text = "Analysis of Feasibility"
p_fb.alignment = PP_ALIGN.CENTER
p_fb.font.bold = True
p_fb.font.size = Pt(13)
p_fb.font.name = "Liberation Sans"
p_fb.font.color.rgb = COLOR_WHITE

p_fb_sub = tf_fb.add_paragraph()
p_fb_sub.text = "Technically feasible, cost-effective and deployment ready"
p_fb_sub.alignment = PP_ALIGN.CENTER
p_fb_sub.font.size = Pt(8.5)
p_fb_sub.font.name = "Liberation Sans"
p_fb_sub.font.color.rgb = RGBColor(224, 238, 255)

feas_cards = [
    ("Fast TTS Inference (RTF 0.049)", 
     "• Piper ONNX runs 20.2x faster than real-time on CPU\n• High-fidelity regional speech generated in < 0.19s"),
    ("Fast STT Inference (RTF 0.163)", 
     "• Vosk Edge ASR transcribes speech 6.1x faster than real-time\n• Quantized model uses < 30 MB RAM on ARM Cortex"),
    ("Sub-Second Turnaround Latency", 
     "• 0.69s – 0.96s total voice-in to voice-out lag across mesh\n• Preserves natural walkie-talkie conversation rhythm"),
    ("Zero Infrastructure & Low Hardware Cost", 
     "• 100% decentralized P2P; zero servers / cloud API fees\n• Commodity ESP32 + LoRa module under ₹1,200 ($15)")
]

for fc_idx, (fc_title, fc_desc) in enumerate(feas_cards):
    fcy = Inches(2.15 + fc_idx * 1.15)
    fcard = slide3.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.7), fcy, Inches(3.9), Inches(1.02))
    fcard.fill.solid()
    fcard.fill.fore_color.rgb = COLOR_WHITE
    fcard.line.color.rgb = RGBColor(191, 219, 254)
    fcard.line.width = Pt(1)
    
    tf_fc = fcard.text_frame
    tf_fc.word_wrap = True
    tf_fc.margin_top = Inches(0.06)
    tf_fc.margin_left = Inches(0.08)
    tf_fc.margin_right = Inches(0.08)
    
    p1 = tf_fc.paragraphs[0]
    p1.text = fc_title
    p1.font.bold = True
    p1.font.size = Pt(10.5)
    p1.font.name = "Liberation Sans"
    p1.font.color.rgb = COLOR_PRIMARY_BLUE
    p1.space_after = Pt(2)
    
    p2 = tf_fc.add_paragraph()
    p2.text = fc_desc
    p2.font.size = Pt(8.5)
    p2.font.name = "Liberation Sans"
    p2.font.color.rgb = COLOR_TEXT_DARK

badge3_path = os.path.join(ASSETS_DIR, "slide3_center_badge.png")
if os.path.exists(badge3_path):
    slide3.shapes.add_picture(badge3_path, Inches(4.8), Inches(1.6), width=Inches(3.5), height=Inches(3.5))

c_tag = slide3.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(4.9), Inches(5.35), Inches(3.4), Inches(0.65))
c_tag.fill.solid()
c_tag.fill.fore_color.rgb = RGBColor(235, 244, 255)
c_tag.line.color.rgb = COLOR_PRIMARY_BLUE
c_tag.line.width = Pt(1)
tf_ct = c_tag.text_frame
tf_ct.vertical_anchor = MSO_ANCHOR.MIDDLE
p_ct = tf_ct.paragraphs[0]
p_ct.text = "Mission-Critical Resilient Mesh"
p_ct.alignment = PP_ALIGN.CENTER
p_ct.font.bold = True
p_ct.font.size = Pt(10)
p_ct.font.name = "Liberation Sans"
p_ct.font.color.rgb = COLOR_PRIMARY_BLUE
p_ct2 = tf_ct.add_paragraph()
p_ct2.text = "100% Offline • Zero Foreign Cloud Dependencies"
p_ct2.alignment = PP_ALIGN.CENTER
p_ct2.font.size = Pt(8)
p_ct2.font.name = "Liberation Sans"
p_ct2.font.color.rgb = COLOR_TEXT_MUTED

# Right Side Top: Potential Challenges and Risks
r_top = slide3.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(8.5), Inches(1.35), Inches(4.3), Inches(2.65))
r_top.fill.solid()
r_top.fill.fore_color.rgb = RGBColor(255, 250, 240)
r_top.line.color.rgb = RGBColor(245, 158, 11)
r_top.line.width = Pt(1.5)

ch_badge = slide3.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(8.7), Inches(1.48), Inches(3.9), Inches(0.38))
ch_badge.fill.solid()
ch_badge.fill.fore_color.rgb = RGBColor(245, 158, 11)
ch_badge.line.fill.background()
tf_chb = ch_badge.text_frame
tf_chb.vertical_anchor = MSO_ANCHOR.MIDDLE
p_chb = tf_chb.paragraphs[0]
p_chb.text = "Potential Challenges and Risks"
p_chb.alignment = PP_ALIGN.CENTER
p_chb.font.bold = True
p_chb.font.size = Pt(11)
p_chb.font.name = "Liberation Sans"
p_chb.font.color.rgb = COLOR_WHITE

challenges_items = [
    ("Severe Acoustic Noise", "Storm winds, floodwaters, sirens & engine noise corrupting mic audio."),
    ("RF Packet Loss & Rubble NLOS", "Concrete attenuation and multipath fading causing dropped frames."),
    ("Regional Indian Dialects", "Pronunciation variations and code-mixed speech in rural disaster zones.")
]

for ci_idx, (ci_title, ci_desc) in enumerate(challenges_items):
    tb_ci = slide3.shapes.add_textbox(Inches(8.7), Inches(1.95 + ci_idx * 0.65), Inches(3.9), Inches(0.6))
    tf_ci = tb_ci.text_frame
    tf_ci.word_wrap = True
    tf_ci.margin_left = Inches(0.04)
    tf_ci.margin_right = Inches(0.04)
    tf_ci.margin_top = Inches(0.02)
    p_cit = tf_ci.paragraphs[0]
    p_cit.text = "• " + ci_title + ": "
    p_cit.font.bold = True
    p_cit.font.size = Pt(9.5)
    p_cit.font.name = "Liberation Sans"
    p_cit.font.color.rgb = RGBColor(180, 83, 9)
    run_desc = p_cit.add_run()
    run_desc.text = ci_desc
    run_desc.font.bold = False
    run_desc.font.color.rgb = COLOR_TEXT_DARK

# Right Side Bottom: Strategies For Overcoming Challenges
r_bot = slide3.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(8.5), Inches(4.15), Inches(4.3), Inches(2.75))
r_bot.fill.solid()
r_bot.fill.fore_color.rgb = RGBColor(240, 253, 244)
r_bot.line.color.rgb = RGBColor(34, 197, 94)
r_bot.line.width = Pt(1.5)

st_badge = slide3.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(8.7), Inches(4.28), Inches(3.9), Inches(0.38))
st_badge.fill.solid()
st_badge.fill.fore_color.rgb = RGBColor(34, 197, 94)
st_badge.line.fill.background()
tf_stb = st_badge.text_frame
tf_stb.vertical_anchor = MSO_ANCHOR.MIDDLE
p_stb = tf_stb.paragraphs[0]
p_stb.text = "Strategies For Overcoming Challenges"
p_stb.alignment = PP_ALIGN.CENTER
p_stb.font.bold = True
p_stb.font.size = Pt(11)
p_stb.font.name = "Liberation Sans"
p_stb.font.color.rgb = COLOR_WHITE

strategies_items = [
    ("Spectral Gating & Silero VAD", "Real-time 300Hz-3.4kHz bandpass filter eliminates 95% ambient noise."),
    ("Reed-Solomon FEC & Selective ARQ", "Forward error correction ensures 99.4% packet recovery over lossy links."),
    ("Phonetic Smoothing & 1-Tap SOS", "Phonetic similarity mapping + standardized tactical phrases for 100% accuracy.")
]

for si_idx, (si_title, si_desc) in enumerate(strategies_items):
    tb_si = slide3.shapes.add_textbox(Inches(8.7), Inches(4.75 + si_idx * 0.68), Inches(3.9), Inches(0.65))
    tf_si = tb_si.text_frame
    tf_si.word_wrap = True
    tf_si.margin_left = Inches(0.04)
    tf_si.margin_right = Inches(0.04)
    tf_si.margin_top = Inches(0.02)
    p_sit = tf_si.paragraphs[0]
    p_sit.text = "• " + si_title + ": "
    p_sit.font.bold = True
    p_sit.font.size = Pt(9.5)
    p_sit.font.name = "Liberation Sans"
    p_sit.font.color.rgb = RGBColor(21, 128, 61)
    run_sdesc = p_sit.add_run()
    run_sdesc.text = si_desc
    run_sdesc.font.bold = False
    run_sdesc.font.color.rgb = COLOR_TEXT_DARK

# ==========================================
# SLIDE 4: IMPACT AND BENEFITS (PAGE 5)
# ==========================================
slide4 = prs.slides.add_slide(blank_layout)
add_header_and_footer(slide4, 5, "IMPACT AND BENEFITS")

wave_path = os.path.join(ASSETS_DIR, "slide4_wavy_roadmap.png")
if os.path.exists(wave_path):
    slide4.shapes.add_picture(wave_path, Inches(0.6), Inches(2.7), width=Inches(12.1), height=Inches(2.3))

step_data = [
    (1, False, "Stops Telecom Blackouts", "Zero-Network Voice Link", 
     "Restores instant voice communication in total telecom blackouts (floods, cyclones, earthquakes, defense ops)."),
    (2, True, "99.92% Bandwidth Saved", "100x Channel Capacity",
     "Slashes voice payload from 256 kbps to 132 bps, enabling 100+ concurrent channels where only 1 could fit."),
    (3, False, "Cross-Language Interop", "10 Indian Languages",
     "Unifies NDRF rescue teams, armed forces, and local disaster victims across 10 Indian scheduled languages."),
    (4, True, "Sub-Second SOS Rescue", "Life-Saving Beacon",
     "One-tap emergency broadcast with GPS coordinates, distress sirens, and preemption over all active RF traffic in < 0.86s."),
    (5, False, "15+ km Tactical Mesh", "Ad-Hoc Multi-Hop Routing",
     "Decentralized LoRa repeater nodes blanket entire disaster valleys with 42 ms RF airtime without cell towers."),
    (6, True, "Mass Civil Deployment", "Commodity COTS Hardware",
     "Built on commodity COTS hardware (< ₹1,200/node) and existing Android smartphones for mass adoption."),
    (7, False, "Atmanirbhar Defense", "100% Offline & Encrypted",
     "Zero foreign cloud APIs, offline on-device processing, and AES-256 payload encryption ensuring data security.")
]

card_w_s4 = 1.65
for num, is_top, title, sub, desc in step_data:
    idx = num - 1
    frac = (130 + idx * 220) / 1600.0
    cx_in = 0.6 + frac * 12.1 - card_w_s4 / 2.0
    cy_in = 1.25 if is_top else 5.15
    
    scard = slide4.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(cx_in), Inches(cy_in), Inches(card_w_s4), Inches(1.35))
    scard.fill.solid()
    scard.fill.fore_color.rgb = COLOR_WHITE
    scard.line.color.rgb = COLOR_PRIMARY_BLUE if is_top else RGBColor(147, 51, 234)
    scard.line.width = Pt(1.5)
    
    tf_s = scard.text_frame
    tf_s.word_wrap = True
    tf_s.margin_left = Inches(0.04)
    tf_s.margin_right = Inches(0.04)
    tf_s.margin_top = Inches(0.04)
    
    p1 = tf_s.paragraphs[0]
    p1.text = title
    p1.alignment = PP_ALIGN.CENTER
    p1.font.bold = True
    p1.font.size = Pt(9)
    p1.font.name = "Liberation Sans"
    p1.font.color.rgb = COLOR_NAVY_DARK
    
    p2 = tf_s.add_paragraph()
    p2.text = sub
    p2.alignment = PP_ALIGN.CENTER
    p2.font.bold = True
    p2.font.size = Pt(8)
    p2.font.name = "Liberation Sans"
    p2.font.color.rgb = COLOR_PRIMARY_BLUE
    p2.space_after = Pt(2)
    
    p3 = tf_s.add_paragraph()
    p3.text = desc
    p3.alignment = PP_ALIGN.CENTER
    p3.font.size = Pt(7.5)
    p3.font.name = "Liberation Sans"
    p3.font.color.rgb = COLOR_TEXT_MUTED

# ==========================================
# SLIDE 5: RESEARCH AND REFERENCES (PAGE 6)
# ==========================================
slide5 = prs.slides.add_slide(blank_layout)
add_header_and_footer(slide5, 6, "RESEARCH AND REFERENCES")

research_items = [
    ("1. Vosk & Kaldi Edge Speech Architecture (ASR)",
     "High-accuracy offline acoustic modeling and Weighted Finite-State Transducers (WFST) optimized for low-power ARM mobile devices. Operates fully offline in under 30 MB RAM with RTF 0.163 (< 150 ms latency) on commodity Android smartphones.",
     "Reference: Povey, D., et al. 'The Kaldi Speech Recognition Toolkit', IEEE ASRU. Apache-2.0 License."),
    
    ("2. TantraPacket Ultra-Dense Binary Framing Protocol",
     "Custom ultra-compact binary protocol designed specifically for severely constrained RF channels (< 1 kbps). Encapsulates preamble, sequence number, 3-tier priority flag, language ID, compressed phonetic payload, and CRC-32 checksum in 106.5 bytes average.",
     "Reference: ITU-R M.1371 / IEEE 802.15.4 Low-Rate Wireless Communication Standards."),
     
    ("3. Piper & VITS Neural Acoustic Synthesis (TTS)",
     "Variational Inference with adversarial learning for end-to-end Text-to-Speech (VITS). Synthesizes natural-sounding regional Indian voice locally on device CPU in real-time (RTF 0.049, 20.2x faster than real-time) with zero cloud API latency.",
     "Reference: Kim, J., et al. 'Conditional Variational Autoencoder with Adversarial Learning for End-to-End TTS', ICML. MIT License.")
]

for r_idx, (r_title, r_desc, r_ref) in enumerate(research_items):
    ry = Inches(1.35 + r_idx * 1.85)
    rcard = slide5.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.5), ry, Inches(5.9), Inches(1.72))
    rcard.fill.solid()
    rcard.fill.fore_color.rgb = RGBColor(248, 250, 252)
    rcard.line.color.rgb = RGBColor(203, 213, 225)
    rcard.line.width = Pt(1.5)
    
    tf_r = rcard.text_frame
    tf_r.word_wrap = True
    tf_r.margin_left = Inches(0.1)
    tf_r.margin_right = Inches(0.1)
    tf_r.margin_top = Inches(0.08)
    
    p1 = tf_r.paragraphs[0]
    p1.text = r_title
    p1.font.bold = True
    p1.font.size = Pt(11)
    p1.font.name = "Liberation Sans"
    p1.font.color.rgb = COLOR_PRIMARY_BLUE
    p1.space_after = Pt(3)
    
    p2 = tf_r.add_paragraph()
    p2.text = r_desc
    p2.font.size = Pt(8.5)
    p2.font.name = "Liberation Sans"
    p2.font.color.rgb = COLOR_TEXT_DARK
    p2.space_after = Pt(3)
    
    p3 = tf_r.add_paragraph()
    p3.text = r_ref
    p3.font.italic = True
    p3.font.size = Pt(8)
    p3.font.name = "Liberation Sans"
    p3.font.color.rgb = COLOR_TEXT_MUTED

diagrams = [
    (os.path.join(ASSETS_DIR, "slide5_flow_asr.png"), Inches(1.35), "Vosk Edge ASR Pipeline (16 kHz Audio ➔ Token)"),
    (os.path.join(ASSETS_DIR, "slide5_flow_bandwidth.png"), Inches(3.20), "TantraPacket Bandwidth Compression Benchmark (99.92% Saved)"),
    (os.path.join(ASSETS_DIR, "slide5_flow_tts.png"), Inches(5.05), "Piper VITS On-Device Neural Synthesis Flow (Token ➔ Speech)")
]

for d_path, dy, d_caption in diagrams:
    if os.path.exists(d_path):
        slide5.shapes.add_picture(d_path, Inches(6.6), dy, width=Inches(6.2), height=Inches(1.55))
        tb_cap = slide5.shapes.add_textbox(Inches(6.6), dy + Inches(1.55), Inches(6.2), Inches(0.25))
        tf_cap = tb_cap.text_frame
        p_c = tf_cap.paragraphs[0]
        p_c.text = d_caption
        p_c.alignment = PP_ALIGN.CENTER
        p_c.font.bold = True
        p_c.font.size = Pt(8.5)
        p_c.font.name = "Liberation Sans"
        p_c.font.color.rgb = COLOR_PRIMARY_BLUE

prs.save(OUTPUT_PATH)
print("Updated presentation saved successfully!")
