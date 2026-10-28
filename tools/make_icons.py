"""Draw the Avrora TV launcher icon and TV banner (sunrise: disc, rays, horizon)."""
import math
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'app', 'src', 'main', 'res')
FONT_SEMI = os.path.join(RES, 'font', 'montserrat_semibold.ttf')
FONT_LIGHT = os.path.join(RES, 'font', 'montserrat_light.ttf')

TOP = (26, 14, 8)
MID = (122, 59, 18)
LOW = (255, 176, 74)
SUN_CORE = (255, 243, 208)
SUN_EDGE = (255, 165, 60)
RAY = (255, 208, 138)
GROUND = (24, 11, 5)


def gradient(size, top, mid, low):
    width, height = size
    image = Image.new('RGB', size)
    draw = ImageDraw.Draw(image)
    for y in range(height):
        t = y / max(1, height - 1)
        if t < 0.55:
            k = t / 0.55
            color = tuple(int(top[i] + (mid[i] - top[i]) * k) for i in range(3))
        else:
            k = (t - 0.55) / 0.45
            color = tuple(int(mid[i] + (low[i] - mid[i]) * k) for i in range(3))
        draw.line([(0, y), (width, y)], fill=color)
    return image


def paint_sun(image, cx, cy, r, rays=True):
    """Sun disc, glow and rays drawn straight onto the canvas (no square seam)."""
    size = image.size
    glow = Image.new('RGBA', size, (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse(
        [cx - r * 2.0, cy - r * 2.0, cx + r * 2.0, cy + r * 2.0],
        fill=(255, 190, 110, 95),
    )
    image = Image.alpha_composite(image, glow.filter(ImageFilter.GaussianBlur(r * 0.5)))

    if rays:
        ray_layer = Image.new('RGBA', size, (0, 0, 0, 0))
        rdraw = ImageDraw.Draw(ray_layer)
        for index in range(15):
            angle = math.radians(188 + index * (164 / 14))
            inner = r * 1.2
            outer = r * (1.62 if index % 2 == 0 else 1.4)
            rdraw.line(
                [(cx + math.cos(angle) * inner, cy + math.sin(angle) * inner),
                 (cx + math.cos(angle) * outer, cy + math.sin(angle) * outer)],
                fill=RAY + (105,),
                width=max(2, int(r * 0.06)),
            )
        image = Image.alpha_composite(image, ray_layer)

    disc = Image.new('RGBA', size, (0, 0, 0, 0))
    ddraw = ImageDraw.Draw(disc)
    steps = 40
    for step in range(steps, 0, -1):
        k = step / steps
        radius = r * k
        color = tuple(int(SUN_CORE[i] + (SUN_EDGE[i] - SUN_CORE[i]) * (1 - k)) for i in range(3))
        ddraw.ellipse([cx - radius, cy - radius, cx + radius, cy + radius], fill=color + (255,))
    return Image.alpha_composite(image, disc)


def paint_ground(image, horizon, curve=0.55):
    size = image.size
    layer = Image.new('RGBA', size, (0, 0, 0, 0))
    width, height = size
    ImageDraw.Draw(layer).ellipse(
        [-width * 0.3, horizon, width * 1.3, horizon + height * curve * 2],
        fill=GROUND + (255,),
    )
    return Image.alpha_composite(image, layer)


def icon(size=512):
    image = gradient((size, size), TOP, MID, LOW).convert('RGBA')
    image = paint_sun(image, size * 0.5, size * 0.58, size * 0.215)
    image = paint_ground(image, size * 0.58 + size * 0.215 * 0.60)
    mask = Image.new('L', (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * 0.24), fill=255)
    out = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    out.paste(image, (0, 0), mask)
    return out


def banner():
    width, height = 320, 180
    image = gradient((width, height), TOP, MID, LOW).convert('RGBA')
    image = paint_sun(image, 78, 118, 40)
    image = paint_ground(image, 152, 0.35)

    draw = ImageDraw.Draw(image)
    title_font = ImageFont.truetype(FONT_SEMI, 40)
    sub_font = ImageFont.truetype(FONT_LIGHT, 15)

    title = 'AVRORA'
    while draw.textlength(title, font=title_font) > width - 150 and title_font.size > 18:
        title_font = ImageFont.truetype(FONT_SEMI, title_font.size - 1)
    draw.text((136, 52), title, font=title_font, fill=(255, 255, 255, 255))

    sub = 'Т Е Л Е В И Д Е Н И Е'
    sub_font = ImageFont.truetype(FONT_LIGHT, 11)
    while draw.textlength(sub, font=sub_font) > width - 150 and sub_font.size > 7:
        sub_font = ImageFont.truetype(FONT_LIGHT, sub_font.size - 1)
    draw.text((139, 104), sub, font=sub_font, fill=(255, 208, 150, 235))

    veil = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    ImageDraw.Draw(veil).rectangle([0, 0, width, height], fill=(0, 0, 0, 30))
    return Image.alpha_composite(image, veil)


def foreground(size=432):
    image = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    sun = paint_sun(Image.new('RGBA', (size, size), (0, 0, 0, 0)),
                    size * 0.5, size * 0.52, size * 0.155)
    mask = Image.new('L', (size, size), 0)
    ImageDraw.Draw(mask).ellipse([size * 0.12, size * 0.12, size * 0.88, size * 0.88], fill=255)
    image.paste(sun, (0, 0), mask)
    return image


def main():
    icon().save(os.path.join(RES, 'drawable', 'ic_launcher.png'))
    foreground().save(os.path.join(RES, 'drawable', 'ic_launcher_foreground.png'))
    banner().save(os.path.join(RES, 'drawable', 'banner.png'))

    mipmap = os.path.join(RES, 'mipmap-anydpi-v26')
    os.makedirs(mipmap, exist_ok=True)
    with open(os.path.join(mipmap, 'ic_launcher.xml'), 'w', encoding='utf-8') as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n'
                '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
                '    <background android:drawable="@color/ic_launcher_background" />\n'
                '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
                '</adaptive-icon>\n')
    print('icons written')


if __name__ == '__main__':
    main()
