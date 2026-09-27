import math
import os
from PIL import Image, ImageDraw, ImageFont

ASSETS_DIR = "/home/naman/Desktop/SIH/Backup/docs/assets"
os.makedirs(ASSETS_DIR, exist_ok=True)

FONT_BOLD = "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"
FONT_REG = "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"

def get_font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except:
        return ImageFont.load_default()

# 1. Slide 1 Center Phone Hub
def create_slide1_phone():
    W, H = 800, 800
    img = Image.new("RGBA", (W, H), (255, 255, 255, 0))
    draw = ImageDraw.Draw(img)

    cx, cy = W // 2, H // 2
    r_bg = 280
    draw.ellipse([cx - r_bg, cy - r_bg, cx + r_bg, cy + r_bg], fill=(225, 242, 254, 180), outline=(180, 220, 250, 255), width=3)

    pw, ph = 200, 380
    px0, py0 = cx - pw // 2, cy - ph // 2 - 20
    px1, py1 = px0 + pw, py0 + ph
    draw.rounded_rectangle([px0, py0, px1, py1], radius=35, fill=(30, 41, 59, 255), outline=(15, 23, 42, 255), width=6)
    
    margin = 12
    draw.rounded_rectangle([px0 + margin, py0 + margin + 15, px1 - margin, py1 - margin - 20], radius=24, fill=(240, 246, 252, 255))
    draw.rounded_rectangle([cx - 30, py0 + 10, cx + 30, py0 + 20], radius=5, fill=(15, 23, 42, 255))

    draw.ellipse([cx - 40, py0 + 60, cx + 40, py0 + 140], fill=(26, 111, 196, 230))
    draw.ellipse([cx - 16, py0 + 78, cx + 16, py0 + 110], fill=(255, 255, 255, 255))
    draw.chord([cx - 30, py0 + 105, cx + 30, py0 + 140], 0, 180, fill=(255, 255, 255, 255))

    wave_bars = [15, 28, 45, 30, 55, 38, 20, 48, 25]
    wx = cx - 60
    wy = py0 + 185
    for i, h in enumerate(wave_bars):
        bx = wx + i * 14
        draw.line([bx, wy - h // 2, bx, wy + h // 2], fill=(26, 111, 196, 255), width=4)

    draw.ellipse([cx - 35, py0 + 245, cx + 35, py0 + 315], fill=(234, 67, 53, 255))
    draw.polygon([(cx - 8, py0 + 270), (cx + 8, py0 + 270), (cx + 12, py0 + 290), (cx - 12, py0 + 290)], fill=(255, 255, 255, 255))
    draw.ellipse([cx - 8, py0 + 262, cx + 8, py0 + 278], fill=(255, 255, 255, 255))

    # Radiating connection dots to 4 corners
    draw.line([cx - 140, cy - 80, cx - 220, cy - 120], fill=(233, 30, 99, 255), width=5)
    draw.ellipse([cx - 235, cy - 135, cx - 215, cy - 115], fill=(233, 30, 99, 255))
    
    draw.line([cx + 140, cy - 80, cx + 220, cy - 120], fill=(46, 125, 50, 255), width=5)
    draw.ellipse([cx + 215, cy - 135, cx + 235, cy - 115], fill=(46, 125, 50, 255))

    draw.line([cx - 140, cy + 100, cx - 220, cy + 150], fill=(26, 111, 196, 255), width=5)
    draw.ellipse([cx - 235, cy + 140, cx - 215, cy + 160], fill=(26, 111, 196, 255))

    draw.line([cx + 140, cy + 100, cx + 220, cy + 150], fill=(142, 36, 170, 255), width=5)
    draw.ellipse([cx + 215, cy + 140, cx + 235, cy + 160], fill=(142, 36, 170, 255))

    font_bold = get_font(FONT_BOLD, 22)
    font_reg = get_font(FONT_REG, 17)
    draw.text((cx, cy + 220), "Resilient Voice Transceiver", fill=(26, 111, 196, 255), font=font_bold, anchor="mm")
    draw.text((cx, cy + 248), "Ultra-Low-Bitrate • 100% Offline Mesh", fill=(71, 85, 105, 255), font=font_reg, anchor="mm")

    img.save(os.path.join(ASSETS_DIR, "slide1_phone_hub.png"))

# 2. Slide 3 Center Ecosystem Badge (800x800)
def create_slide3_badge():
    W, H = 800, 800
    img = Image.new("RGBA", (W, H), (255, 255, 255, 0))
    draw = ImageDraw.Draw(img)

    cx, cy = W // 2, H // 2

    # Outer dashed ring
    for angle in range(0, 360, 10):
        rad = math.radians(angle)
        rad2 = math.radians(angle + 5)
        r = 290
        x1 = cx + r * math.cos(rad)
        y1 = cy + r * math.sin(rad)
        x2 = cx + r * math.cos(rad2)
        y2 = cy + r * math.sin(rad2)
        draw.line([x1, y1, x2, y2], fill=(26, 111, 196, 160), width=4)

    # 4 Orbiting feature circles (further out at r=290)
    orbit_nodes = [
        (cx, cy - 290, (26, 111, 196, 255), "Disaster\nTeams"),
        (cx + 290, cy, (245, 158, 11, 255), "Tactical\nDefense"),
        (cx, cy + 290, (16, 185, 129, 255), "10 Indian\nLanguages"),
        (cx - 290, cy, (16, 185, 129, 255), "Zero Cloud\nResilience")
    ]
    font_node = get_font(FONT_BOLD, 17)
    for nx, ny, col, label in orbit_nodes:
        draw.ellipse([nx - 52, ny - 52, nx + 52, ny + 52], fill=col)
        draw.ellipse([nx - 45, ny - 45, nx + 45, ny + 45], fill=(255, 255, 255, 240))
        lines = label.split("\n")
        draw.text((nx, ny - 11), lines[0], fill=(30, 41, 59, 255), font=font_node, anchor="mm")
        draw.text((nx, ny + 11), lines[1], fill=(30, 41, 59, 255), font=font_node, anchor="mm")

    # Center Shield Emblem
    shield_w, shield_h = 190, 220
    sx, sy = cx - shield_w // 2, cy - shield_h // 2 - 15
    draw.rounded_rectangle([sx, sy, sx + shield_w, sy + shield_h - 45], radius=28, fill=(26, 111, 196, 255))
    draw.polygon([(sx, sy + shield_h - 50), (sx + shield_w, sy + shield_h - 50), (cx, sy + shield_h + 20)], fill=(26, 111, 196, 255))

    # Inner Radio Antenna & Concentric Waves
    draw.line([cx, sy + 40, cx, sy + 140], fill=(255, 255, 255, 255), width=6)
    draw.ellipse([cx - 10, sy + 30, cx + 10, sy + 50], fill=(255, 255, 255, 255))
    # Arcs
    draw.arc([cx - 40, sy + 35, cx + 40, sy + 115], 210, 330, fill=(255, 255, 255, 255), width=5)
    draw.arc([cx - 65, sy + 20, cx + 65, sy + 130], 210, 330, fill=(255, 255, 255, 255), width=5)

    # Title below shield
    font_title = get_font(FONT_BOLD, 30)
    font_sub = get_font(FONT_REG, 20)
    draw.text((cx, cy + 125), "iTantra", fill=(26, 111, 196, 255), font=font_title, anchor="mm")
    draw.text((cx, cy + 160), "Mission-Critical Voice Mesh", fill=(71, 85, 105, 255), font=font_sub, anchor="mm")

    img.save(os.path.join(ASSETS_DIR, "slide3_center_badge.png"))
    print("Slide 3 center badge updated")

# 3. Slide 4 Serpentine Wavy Ribbon
def create_slide4_wavy_roadmap():
    W, H = 1600, 480
    img = Image.new("RGBA", (W, H), (255, 255, 255, 0))
    draw = ImageDraw.Draw(img)

    colors = [
        (239, 68, 68),    # 1. Red
        (249, 115, 22),   # 2. Orange
        (132, 204, 22),   # 3. Light Green
        (16, 185, 129),   # 4. Mint
        (6, 182, 212),    # 5. Cyan
        (59, 130, 246),   # 6. Blue
        (168, 85, 247)    # 7. Purple
    ]

    step_cx = [130 + i * 220 for i in range(7)]
    y_center = 240
    r_outer = 85
    r_inner = 55

    for i in range(6):
        x1 = step_cx[i]
        x2 = step_cx[i+1]
        col1 = colors[i]
        col2 = colors[i+1]
        is_upper = (i % 2 == 0)
        
        for t in range(30):
            frac = t / 30.0
            bx = int(x1 + (x2 - x1) * frac)
            by = int(y_center - 75 * math.sin(math.pi * frac) if is_upper else y_center + 75 * math.sin(math.pi * frac))
            r_col = int(col1[0] * (1 - frac) + col2[0] * frac)
            g_col = int(col1[1] * (1 - frac) + col2[1] * frac)
            b_col = int(col1[2] * (1 - frac) + col2[2] * frac)
            draw.ellipse([bx - 32, by - 32, bx + 32, by + 32], fill=(r_col, g_col, b_col, 220))

    font_num = get_font(FONT_BOLD, 42)
    for i in range(7):
        cx = step_cx[i]
        cy = y_center
        col = colors[i]

        draw.ellipse([cx - r_outer, cy - r_outer + 6, cx + r_outer, cy + r_outer + 6], fill=(0, 0, 0, 30))
        draw.ellipse([cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer], fill=col)
        draw.ellipse([cx - r_inner, cy - r_inner, cx + r_inner, cy + r_inner], fill=(255, 255, 255, 255), outline=(220, 220, 220, 255), width=3)
        draw.text((cx, cy), str(i + 1), fill=(30, 41, 59, 255), font=font_num, anchor="mm")

        if i % 2 == 1:
            draw.line([cx, cy - r_outer, cx, cy - r_outer - 45], fill=(71, 85, 105, 255), width=4)
        else:
            draw.line([cx, cy + r_outer, cx, cy + r_outer + 45], fill=(71, 85, 105, 255), width=4)

    img.save(os.path.join(ASSETS_DIR, "slide4_wavy_roadmap.png"))
    print("Slide 4 wavy roadmap updated")

# 4. Slide 5 Flow Diagrams (High DPI 1400x320)
def create_slide5_diagrams():
    font_title = get_font(FONT_BOLD, 26)
    font_sub = get_font(FONT_REG, 21)

    # 1. Vosk ASR Flow
    W, H = 1400, 300
    img1 = Image.new("RGBA", (W, H), (255, 255, 255, 255))
    d1 = ImageDraw.Draw(img1)
    d1.rounded_rectangle([4, 4, W-5, H-5], radius=24, fill=(248, 250, 252, 255), outline=(203, 213, 225, 255), width=3)
    
    blocks1 = [
        ("Raw Voice Audio", "16 kHz Mono PCM", (238, 242, 255), (99, 102, 241)),
        ("Mel-Spectrogram", "Acoustic Features", (254, 243, 199), (217, 119, 6)),
        ("Vosk Edge Engine", "WFST Decoder", (220, 252, 231), (22, 163, 74)),
        ("Text Token", "Indian Multilingual", (254, 226, 226), (220, 38, 38))
    ]
    box_w = 285
    box_h = 220
    gap = 55
    start_x = 35

    for idx, (b_title, b_sub, b_bg, b_bd) in enumerate(blocks1):
        bx = start_x + idx * (box_w + gap)
        by = (H - box_h) // 2
        d1.rounded_rectangle([bx, by, bx + box_w, by + box_h], radius=18, fill=b_bg, outline=b_bd, width=3)
        d1.text((bx + box_w // 2, by + 80), b_title, fill=(15, 23, 42), font=font_title, anchor="mm")
        d1.text((bx + box_w // 2, by + 140), b_sub, fill=(71, 85, 105), font=font_sub, anchor="mm")
        if idx < 3:
            ax = bx + box_w + 8
            ay = H // 2
            d1.polygon([(ax + 24, ay), (ax + 6, ay - 14), (ax + 6, ay + 14)], fill=(100, 116, 139))
            d1.line([ax, ay, ax + 18, ay], fill=(100, 116, 139), width=5)
    img1.save(os.path.join(ASSETS_DIR, "slide5_flow_asr.png"))

    # 2. TantraPacket Compression Benchmark Flow
    W, H = 1400, 300
    img2 = Image.new("RGBA", (W, H), (255, 255, 255, 255))
    d2 = ImageDraw.Draw(img2)
    d2.rounded_rectangle([4, 4, W-5, H-5], radius=24, fill=(248, 250, 252, 255), outline=(203, 213, 225, 255), width=3)
    
    # 3 big blocks
    # Block 1: Standard Voice
    d2.rounded_rectangle([35, 40, 420, 260], radius=18, fill=(254, 226, 226), outline=(239, 68, 68), width=3)
    d2.text((227, 95), "Standard Voice", fill=(185, 28, 28), font=font_title, anchor="mm")
    d2.text((227, 145), "Opus / AMR: 64,000 bps", fill=(153, 27, 27), font=font_sub, anchor="mm")
    d2.text((227, 185), "Fails on Narrowband Radio", fill=(153, 27, 27), font=font_sub, anchor="mm")

    # Arrow 1
    d2.polygon([(465, 150), (445, 136), (445, 164)], fill=(71, 85, 105))
    d2.line([430, 150, 455, 150], fill=(71, 85, 105), width=5)

    # Block 2: TantraPacket
    d2.rounded_rectangle([485, 40, 895, 260], radius=18, fill=(219, 234, 254), outline=(37, 99, 235), width=3)
    d2.text((690, 95), "TantraPacket Protocol", fill=(29, 78, 216), font=font_title, anchor="mm")
    d2.text((690, 145), "40-Byte Binary Frame", fill=(30, 64, 175), font=font_sub, anchor="mm")
    d2.text((690, 185), "Bitrate: ~80 bps", fill=(30, 64, 175), font=font_sub, anchor="mm")

    # Arrow 2
    d2.polygon([(945, 150), (925, 136), (925, 164)], fill=(71, 85, 105))
    d2.line([905, 150, 935, 150], fill=(71, 85, 105), width=5)

    # Block 3: 99.8% Saved
    d2.rounded_rectangle([965, 40, 1365, 260], radius=18, fill=(220, 252, 231), outline=(22, 163, 74), width=3)
    d2.text((1165, 95), "99.8% SAVED", fill=(21, 128, 61), font=font_title, anchor="mm")
    d2.text((1165, 145), "100x Channel Capacity", fill=(22, 101, 52), font=font_sub, anchor="mm")
    d2.text((1165, 185), "15+ km Range over LoRa", fill=(22, 101, 52), font=font_sub, anchor="mm")

    img2.save(os.path.join(ASSETS_DIR, "slide5_flow_bandwidth.png"))

    # 3. Piper VITS Synthesis Flow
    W, H = 1400, 300
    img3 = Image.new("RGBA", (W, H), (255, 255, 255, 255))
    d3 = ImageDraw.Draw(img3)
    d3.rounded_rectangle([4, 4, W-5, H-5], radius=24, fill=(248, 250, 252, 255), outline=(203, 213, 225, 255), width=3)
    
    blocks3 = [
        ("Received Text", "Decoded 40-Byte Packet", (243, 232, 255), (147, 51, 234)),
        ("G2P Phonemizer", "Indian Language Rules", (254, 243, 199), (217, 119, 6)),
        ("VITS Neural Engine", "On-Device Synthesis", (219, 234, 254), (37, 99, 235)),
        ("Natural Speech", "Local Speaker Audio", (220, 252, 231), (22, 163, 74))
    ]
    for idx, (b_title, b_sub, b_bg, b_bd) in enumerate(blocks3):
        bx = start_x + idx * (box_w + gap)
        by = (H - box_h) // 2
        d3.rounded_rectangle([bx, by, bx + box_w, by + box_h], radius=18, fill=b_bg, outline=b_bd, width=3)
        d3.text((bx + box_w // 2, by + 80), b_title, fill=(15, 23, 42), font=font_title, anchor="mm")
        d3.text((bx + box_w // 2, by + 140), b_sub, fill=(71, 85, 105), font=font_sub, anchor="mm")
        if idx < 3:
            ax = bx + box_w + 8
            ay = H // 2
            d3.polygon([(ax + 24, ay), (ax + 6, ay - 14), (ax + 6, ay + 14)], fill=(100, 116, 139))
            d3.line([ax, ay, ax + 18, ay], fill=(100, 116, 139), width=5)
    img3.save(os.path.join(ASSETS_DIR, "slide5_flow_tts.png"))
    print("Slide 5 flow diagrams updated at high DPI")

create_slide1_phone()
create_slide3_badge()
create_slide4_wavy_roadmap()
create_slide5_diagrams()
print("All diagram assets regenerated successfully")
