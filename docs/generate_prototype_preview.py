import os
from PIL import Image, ImageDraw, ImageFont

ASSETS_DIR = "/home/naman/Desktop/SIH/Backup/docs/assets"
SCREENSHOT_PATH = "/home/naman/.gemini/antigravity/brain/424dc58a-5efd-4c78-b3a6-0872b28aceb9/itantra_home_screen_1790487096675.jpg"

FONT_BOLD = "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"
FONT_REG = "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"

def get_font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except:
        return ImageFont.load_default()

W, H = 1000, 480
img = Image.new("RGBA", (W, H), (255, 255, 255, 0))
draw = ImageDraw.Draw(img)

# Outer card: soft purple/pink tint with purple border
draw.rounded_rectangle([4, 4, W - 4, H - 4], radius=20, fill=(253, 248, 255, 255), outline=(192, 132, 252, 255), width=3)

# Top badge: Prototype Preview (purple pill)
draw.rounded_rectangle([30, 20, 280, 65], radius=15, fill=(147, 51, 234, 255))
font_badge = get_font(FONT_BOLD, 20)
draw.text((155, 42), "PROTOTYPE PREVIEW", fill=(255, 255, 255, 255), font=font_badge, anchor="mm")

# Inner Dark Hero Card (left side)
hero_x0, hero_y0 = 30, 85
hero_w, hero_h = 630, 365
draw.rounded_rectangle([hero_x0, hero_y0, hero_x0 + hero_w, hero_y0 + hero_h], radius=16, fill=(15, 23, 42, 255))

# Dark hero text
font_hero_tag = get_font(FONT_BOLD, 12)
font_hero_h1 = get_font(FONT_BOLD, 22)
font_hero_desc = get_font(FONT_REG, 13.5)
font_btn = get_font(FONT_BOLD, 13)

# Tag pill
draw.rounded_rectangle([hero_x0 + 25, hero_y0 + 25, hero_x0 + 380, hero_y0 + 52], radius=6, fill=(30, 41, 59, 255))
draw.text((hero_x0 + 202, hero_y0 + 38), "100% OFFLINE MISSION-CRITICAL VOICE", fill=(56, 189, 248, 255), font=font_hero_tag, anchor="mm")

# Title
draw.text((hero_x0 + 25, hero_y0 + 72), "Speak Freely.\nNo Towers. No Satellite.", fill=(255, 255, 255, 255), font=font_hero_h1)

# Description
draw.text((hero_x0 + 25, hero_y0 + 145), 
          "iTantra converts real-time voice into 40-byte\n"
          "neural packets, transmitting across kilometers\n"
          "over LoRa / WiFi hotspot / Bluetooth RFCOMM\n"
          "with multi-language TTS voice replay.",
          fill=(148, 163, 184, 255), font=font_hero_desc)

# Two CTA buttons inside dark card
draw.rounded_rectangle([hero_x0 + 25, hero_y0 + 265, hero_x0 + 185, hero_y0 + 310], radius=8, fill=(26, 111, 196, 255))
draw.text((hero_x0 + 105, hero_y0 + 287), "Hold to Talk (PTT)", fill=(255, 255, 255, 255), font=font_btn, anchor="mm")

draw.rounded_rectangle([hero_x0 + 195, hero_y0 + 265, hero_x0 + 355, hero_y0 + 310], radius=8, fill=(220, 38, 38, 255))
draw.text((hero_x0 + 275, hero_y0 + 287), "Emergency SOS", fill=(255, 255, 255, 255), font=font_btn, anchor="mm")

# Real Phone Screenshot on right of dark hero card
try:
    phone_crop = Image.open(SCREENSHOT_PATH)
    sh_w, sh_h = phone_crop.size
    target_h = 325
    target_w = int(sh_w * (target_h / sh_h))
    phone_resized = phone_crop.resize((target_w, target_h), Image.Resampling.LANCZOS)
    
    px = hero_x0 + hero_w - target_w - 20
    py = hero_y0 + (hero_h - target_h) // 2
    # Bezel
    draw.rounded_rectangle([px - 5, py - 5, px + target_w + 5, py + target_h + 5], radius=16, fill=(30, 41, 59, 255), outline=(94, 234, 212, 200), width=2)
    img.paste(phone_resized, (px, py))
except Exception as e:
    print("Could not paste screenshot:", e)

# Right Side Panel: Access Prototype
panel_x0 = hero_x0 + hero_w + 25
font_acc = get_font(FONT_BOLD, 19)
font_link_btn = get_font(FONT_BOLD, 15)
font_note = get_font(FONT_REG, 13.5)

draw.text((panel_x0 + 135, hero_y0 + 25), "Access Prototype", fill=(147, 51, 234, 255), font=font_acc, anchor="mm")

# Button 1: Open GitHub
btn1_y = hero_y0 + 70
draw.rounded_rectangle([panel_x0, btn1_y, panel_x0 + 270, btn1_y + 52], radius=10, fill=(147, 51, 234, 255))
draw.text((panel_x0 + 135, btn1_y + 26), "Open GitHub Code ->", fill=(255, 255, 255, 255), font=font_link_btn, anchor="mm")

# Button 2: Android APK
btn2_y = hero_y0 + 145
draw.rounded_rectangle([panel_x0, btn2_y, panel_x0 + 270, btn2_y + 52], radius=10, fill=(126, 34, 206, 255))
draw.text((panel_x0 + 135, btn2_y + 26), "Open Android APK ->", fill=(255, 255, 255, 255), font=font_link_btn, anchor="mm")

# Button 3: ISRO PS Info
btn3_y = hero_y0 + 220
draw.rounded_rectangle([panel_x0, btn3_y, panel_x0 + 270, btn3_y + 52], radius=10, fill=(107, 33, 168, 255))
draw.text((panel_x0 + 135, btn3_y + 26), "ISRO PS SIH26173 ->", fill=(255, 255, 255, 255), font=font_link_btn, anchor="mm")

# Note at bottom
draw.text((panel_x0 + 135, hero_y0 + 315), 
          "Click to inspect the verified\n"
          "100% offline codebase & demo.",
          fill=(71, 85, 105, 255), font=font_note, anchor="mm")

img.save(os.path.join(ASSETS_DIR, "slide2_prototype_card.png"))
print("Slide 2 prototype card generated successfully")
