#!/usr/bin/env python3
"""
Render static screenshots of the BitLauncher UI for the README.

The numbers below mirror the Compose implementation (Theme.kt, HomeScreen.kt,
AppDrawerScreen.kt): same palette, spacing, type sizes and radii, just painted
with Pillow so the repository can show the design without an emulator.

Usage:
    pip install pillow
    python3 tools/render_previews.py

The shared Roboto font lives at preview/assets/fonts/Roboto.ttf and is also
used by the interactive preview. DejaVu Sans is the fallback when it is missing.
"""
from __future__ import annotations

import os
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "preview" / "assets"
OUT = ROOT / "docs"
FONT_FILE = ROOT / "preview" / "assets" / "fonts" / "Roboto.ttf"

S = 2  # render at 2x for crisp output
W, H = 393 * S, 852 * S

DARK = {
    "bg": "#101014",
    "surface": "#15151a",
    "surface_variant": "#202027",
    "on_surface": "#f3f3f6",
    "on_surface_variant": "#a9a9b3",
    "outline": "#3b3b44",
    "outline_variant": "#2a2a32",
}
AMOLED = {
    "bg": "#000000",
    "surface": "#0b0b0f",
    "surface_variant": "#1b1b21",
    "on_surface": "#f3f3f6",
    "on_surface_variant": "#a9a9b3",
    "outline": "#3b3b44",
    "outline_variant": "#26262d",
}
LIGHT = {
    "bg": "#f7f7fa",
    "surface": "#ffffff",
    "surface_variant": "#ececf1",
    "on_surface": "#141418",
    "on_surface_variant": "#5b5b66",
    "outline": "#c8c8d0",
    "outline_variant": "#e0e0e6",
}


def px(value: float) -> int:
    return int(round(value * S))


def color(hex_value: str, alpha: int = 255) -> tuple[int, int, int, int]:
    hex_value = hex_value.lstrip("#")
    r, g, b = (int(hex_value[i : i + 2], 16) for i in (0, 2, 4))
    return r, g, b, alpha


def font(size: float, weight: str = "Regular") -> ImageFont.FreeTypeFont:
    path = str(FONT_FILE) if FONT_FILE.exists() else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
    f = ImageFont.truetype(path, px(size))
    if FONT_FILE.exists():
        try:
            f.set_variation_by_name(weight)
        except Exception:
            pass
    return f


def vertical_gradient(size: tuple[int, int], top: tuple[int, int, int], bottom: tuple[int, int, int]) -> Image.Image:
    w, h = size
    img = Image.new("RGB", (w, h))
    draw = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(h - 1, 1)
        draw.line([(0, y), (w, y)], fill=tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return img


def rounded_mask(size: tuple[int, int], radius: int) -> Image.Image:
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius=radius, fill=255)
    return mask


def rounded_rect(
    base: Image.Image,
    box: tuple[float, float, float, float],
    radius: float,
    fill: tuple[int, int, int, int] | None = None,
    outline: tuple[int, int, int, int] | None = None,
    width: int = 1,
) -> None:
    x0, y0, x1, y1 = (px(v) for v in box)
    layer = Image.new("RGBA", base.size, (0, 0, 0, 0))
    ImageDraw.Draw(layer).rounded_rectangle(
        [x0, y0, x1, y1],
        radius=px(radius),
        fill=fill,
        outline=outline,
        width=max(1, px(width)) if outline else 1,
    )
    base.alpha_composite(layer)


def app_tile(base: Image.Image, box: tuple[float, float, float, float], radius: float, c1: str, c2: str, glyph: str = "", glyph_color: str = "#ffffff") -> None:
    """Gradient app tile with a centered initial, mirroring the preview prototype."""
    x0, y0, x1, y1 = (px(v) for v in box)
    w, h = max(1, x1 - x0), max(1, y1 - y0)
    grad = vertical_gradient((w, h), color(c1)[:3], color(c2)[:3]).convert("RGBA")
    grad.putalpha(rounded_mask((w, h), px(radius)))
    base.alpha_composite(grad, (x0, y0))
    if glyph:
        draw = ImageDraw.Draw(base)
        f = font((y1 - y0) / S * 0.46, "SemiBold")
        draw.text(((x0 + x1) / 2, (y0 + y1) / 2), glyph, font=f, fill=color(glyph_color), anchor="mm")


def text_shadowed(base: Image.Image, xy: tuple[float, float], text: str, f, fill, anchor="ls", blur: float = 6, shadow_alpha: int = 150) -> None:
    """Android-style drop shadow so home text stays readable on any wallpaper."""
    layer = Image.new("RGBA", base.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.text((px(xy[0]), px(xy[1]) + px(2)), text, font=f, fill=(0, 0, 0, shadow_alpha), anchor=anchor)
    layer = layer.filter(ImageFilter.GaussianBlur(px(blur)))
    base.alpha_composite(layer)
    ImageDraw.Draw(base).text((px(xy[0]), px(xy[1])), text, font=f, fill=fill, anchor=anchor)


def wallpaper_layer() -> Image.Image:
    wall = Image.open(ASSETS / "wallpaper.jpg").convert("RGB")
    target_ratio = W / H
    src_ratio = wall.width / wall.height
    if src_ratio > target_ratio:
        new_w = int(wall.height * target_ratio)
        wall = wall.crop(((wall.width - new_w) // 2, 0, (wall.width + new_w) // 2, wall.height))
    else:
        new_h = int(wall.width / target_ratio)
        wall = wall.crop((0, (wall.height - new_h) // 2, wall.width, (wall.height + new_h) // 2))
    return wall.resize((W, H), Image.LANCZOS).convert("RGBA")


def scrim() -> Image.Image:
    stops = [(0.0, 0.38), (0.30, 0.10), (0.68, 0.0), (1.0, 0.58)]
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    for y in range(H):
        t = y / max(H - 1, 1)
        for i in range(len(stops) - 1):
            a, av = stops[i]
            b, bv = stops[i + 1]
            if a <= t <= b:
                alpha = int(255 * (av + (bv - av) * ((t - a) / (b - a))))
                draw.line([(0, y), (W, y)], fill=(0, 0, 0, alpha))
                break
    return layer


def status_bar(img: Image.Image, tint: str = "#ffffff", time_text: str = "09:41") -> None:
    draw = ImageDraw.Draw(img)
    f = font(13, "Medium")
    draw.text((px(30), px(24)), time_text, font=f, fill=color(tint), anchor="ls")

    right = px(393 - 24)
    # battery
    bx = right - px(24)
    draw.rounded_rectangle([bx, px(15), bx + px(20), px(25)], radius=px(3), outline=color(tint, 130), width=max(1, px(0.8)))
    draw.rounded_rectangle([bx + px(1.6), px(16.6), bx + px(16.5), px(23.4)], radius=px(1.6), fill=color(tint))
    draw.rounded_rectangle([bx + px(21), px(18), bx + px(22.5), px(22)], radius=px(1), fill=color(tint, 120))
    # wifi
    wx = bx - px(21)
    draw.pieslice([wx - px(3), px(17), wx + px(14), px(33)], 200, 340, fill=color(tint))
    draw.pieslice([wx + px(0.5), px(20.5), wx + px(10.5), px(30.5)], 200, 340, fill=color(tint, 0))
    draw.arc([wx - px(3), px(17), wx + px(14), px(33)], 200, 340, fill=color(tint), width=max(1, px(1.4)))
    draw.ellipse([wx + px(4), px(23), wx + px(6.4), px(25.4)], fill=color(tint))
    # signal bars
    sx = wx - px(20)
    for i in range(4):
        bh = (4 + i * 2.2)
        draw.rounded_rectangle(
            [sx + px(i * 4.4), px(25 - bh), sx + px(i * 4.4 + 2.6), px(25)],
            radius=px(0.8),
            fill=color(tint, 255 if i < 3 else 90),
        )

    # home indicator
    draw.rounded_rectangle([W // 2 - px(59), px(846), W // 2 + px(59), px(850)], radius=px(2), fill=color(tint, 140))


# --------------------------------------------------------------------------- data
APPS = [
    ("WhatsApp", "#2fbf6a", "#128c4a", "W", False),
    ("Spotify", "#3ee07a", "#0b6b34", "S", False),
    ("Kamera", "#6b7788", "#2b3138", "K", False),
    ("Catatan", "#f6c85f", "#c9962e", "C", False),
    ("Chrome", "#5aa9f7", "#2762b8", "C", False),
    ("Gmail", "#f2685f", "#b4332c", "M", False),
    ("Maps", "#5fc98a", "#22795c", "M", False),
    ("YouTube", "#f2554f", "#a51d1a", "Y", False),
    ("Instagram", "#e26ba0", "#8b3a86", "I", False),
    ("Telegram", "#59b8ee", "#2a7ab5", "T", False),
    ("Kalender", "#eef1f7", "#c3c9d6", "K", False),
    ("Kalkulator", "#40444d", "#22252b", "=", False),
    ("Drive", "#4ea9f3", "#2e7a4f", "D", False),
    ("Foto", "#8e7ff0", "#4b3aa8", "P", False),
    ("Play Store", "#58c3f0", "#2b7bb3", "▶", False),
    ("Pengaturan", "#8d95a3", "#4c525c", "G", False),
    ("Dompet", "#6fd0b8", "#24796a", "D", False),
    ("Discord", "#7b86f0", "#3d43a8", "D", False),
    ("Files", "#6ea8ee", "#2c5c9e", "F", False),
    ("Netflix", "#dd4b3f", "#7e1a14", "N", False),
    ("Slack", "#8a63c9", "#4c2f7c", "S", True),
]


def search_icon(draw: ImageDraw.ImageDraw, cx: float, cy: float, size: float, tint, width: float = 1.7) -> None:
    r = size * 0.32
    draw.ellipse([px(cx - r), px(cy - r), px(cx + r), px(cy + r)], outline=tint, width=max(1, px(width)))
    draw.line([px(cx + r * 0.72), px(cy + r * 0.72), px(cx + r * 1.45), px(cy + r * 1.45)], fill=tint, width=max(1, px(width)))


def sliders_icon(draw: ImageDraw.ImageDraw, cx: float, cy: float, size: float, tint, width: float = 1.7) -> None:
    half = size / 2
    for i, knob in enumerate((0.68, 0.32, 0.5)):
        y = cy - half + size * (0.22 + i * 0.28)
        draw.line([px(cx - half), px(y), px(cx + half), px(y)], fill=tint, width=max(1, px(width)))
        kx = cx - half + size * knob
        draw.ellipse([px(kx - size * 0.12), px(y - size * 0.12), px(kx + size * 0.12), px(y + size * 0.12)], fill=tint)


def grid_icon(draw: ImageDraw.ImageDraw, cx: float, cy: float, size: float, tint, width: float = 1.7) -> None:
    half = size / 2
    cell = size * 0.34
    for dx, dy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        x = cx + dx * (half - cell / 2)
        y = cy + dy * (half - cell / 2)
        draw.rounded_rectangle(
            [px(x - cell / 2), px(y - cell / 2), px(x + cell / 2), px(y + cell / 2)],
            radius=px(1.5),
            outline=tint,
            width=max(1, px(width)),
        )


def chevron_up(draw: ImageDraw.ImageDraw, cx: float, cy: float, size: float, tint, width: float = 1.9) -> None:
    draw.line([px(cx - size / 2), px(cy + size * 0.22), px(cx), px(cy - size * 0.24), px(cx + size / 2), px(cy + size * 0.22)], fill=tint, width=max(1, px(width)), joint="curve")


def chevron_right(draw: ImageDraw.ImageDraw, cx: float, cy: float, size: float, tint, width: float = 1.8) -> None:
    draw.line([px(cx - size * 0.18), px(cy - size / 2), px(cx + size * 0.24), px(cy), px(cx - size * 0.18), px(cy + size / 2)], fill=tint, width=max(1, px(width)), joint="curve")


# ----------------------------------------------------------------- screens
def render_home() -> Image.Image:
    img = wallpaper_layer()
    img.alpha_composite(scrim())

    pal = DARK
    draw = ImageDraw.Draw(img)

    # clock + date
    text_shadowed(img, (24, 118), "09:41", font(76, "Light"), color("#ffffff"), blur=7, shadow_alpha=160)
    text_shadowed(img, (24, 148), "Sabtu, 24 Oktober", font(16, "Regular"), color("#ffffff", 210), blur=5, shadow_alpha=120)

    # favorites
    y = 186
    for name, c1, c2, glyph, _work in APPS[:5]:
        app_tile(img, (24, y + 4, 54, y + 34), 9, c1, c2, glyph, "#22252b" if name == "Kalender" else "#ffffff")
        text_shadowed(img, (72, y + 28), name, font(26, "Light"), color("#ffffff"), blur=6, shadow_alpha=150)
        y += 52

    # bottom bar
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle([px(20), px(776), px(315), px(824)], radius=px(28), fill=(0, 0, 0, 107))
    draw.rounded_rectangle([px(325), px(776), px(373), px(824)], radius=px(24), fill=(0, 0, 0, 107))
    search_icon(draw, 40, 800, 18, color("#ffffff", 205))
    draw.text((px(58), px(800)), "Cari aplikasi", font=font(14, "Regular"), fill=color("#ffffff", 205), anchor="lm")
    chevron_up(draw, 291, 800, 16, color("#ffffff", 205))
    sliders_icon(draw, 349, 800, 20, color("#ffffff", 235))

    # grabber
    draw.rounded_rectangle([W // 2 - px(20), px(761), W // 2 + px(20), px(765)], radius=px(2), fill=color("#ffffff", 115))

    status_bar(img)
    return img


def render_drawer(palette: dict, with_sheet: bool = False) -> Image.Image:
    img = Image.new("RGBA", (W, H), color(palette["bg"]))
    draw = ImageDraw.Draw(img)

    # grabber
    draw.rounded_rectangle([W // 2 - px(20), px(56), W // 2 + px(20), px(60)], radius=px(2), fill=color(palette["outline"]))

    # search field + tool icons
    rounded_rect(img, (20, 76, 289, 124), 24, fill=color(palette["surface_variant"], 140))
    draw = ImageDraw.Draw(img)
    search_icon(draw, 40, 100, 18, color(palette["on_surface_variant"]))
    draw.text((px(58), px(100)), "Cari aplikasi", font=font(17, "Regular"), fill=color(palette["on_surface_variant"]), anchor="lm")
    grid_icon(draw, 313, 100, 20, color(palette["on_surface_variant"]))
    sliders_icon(draw, 361, 100, 20, color(palette["on_surface_variant"]))

    draw.line([(0, px(134)), (W, px(134))], fill=color(palette["outline_variant"]))

    # list
    y = 144
    letter = APPS[0][0][0]
    sections = {}
    for app in APPS:
        sections.setdefault(app[0][0].upper(), []).append(app)

    for index, (letter, group) in enumerate(sorted(sections.items())):
        group = sorted(group, key=lambda a: a[0].lower())
        draw = ImageDraw.Draw(img)
        draw.text((px(24), px(y + 14)), letter, font=font(11, "SemiBold"), fill=color(palette["on_surface_variant"]), anchor="ls")
        y += 26
        for name, c1, c2, glyph, work in group:
            app_tile(img, (24, y + 10, 58, y + 44), 10, c1, c2, glyph, "#22252b" if name == "Kalender" else "#ffffff")
            draw = ImageDraw.Draw(img)
            draw.text((px(74), px(y + 27)), name, font=font(16, "Medium"), fill=color(palette["on_surface"]), anchor="lm")
            if work:
                tw = draw.textlength("kerja", font=font(9, "SemiBold"))
                draw.rounded_rectangle([px(300), px(y + 20), px(300) + tw + px(10), px(y + 34)], radius=px(4), fill=color(palette["surface_variant"]))
                draw.text((px(305), px(y + 27)), "kerja", font=font(9, "SemiBold"), fill=color(palette["on_surface_variant"]), anchor="lm")
            y += 54
            if y > 790:
                break
        if y > 790:
            break

    # alphabet rail
    letters = sorted(sections.keys())
    rail_top, rail_bottom = 150, 806
    step = (rail_bottom - rail_top) / max(len(letters), 1)
    for i, letter in enumerate(letters):
        draw.text((px(378), px(rail_top + step * i + step / 2)), letter, font=font(9, "Regular"), fill=color(palette["on_surface_variant"]), anchor="mm")

    status_bar(img, tint=palette["on_surface"])
    return img


PRIMARY = "#8fd8c8"
ON_PRIMARY = "#08211c"


def chip(draw: ImageDraw.ImageDraw, base: Image.Image, x: float, y: float, label: str, selected: bool, palette: dict) -> float:
    tw = draw.textlength(label, font=font(13, "Medium")) / S
    rounded_rect(
        base,
        (x, y, x + tw + 28, y + 32),
        50,
        fill=color(PRIMARY) if selected else None,
        outline=None if selected else color(palette["outline"]),
    )
    ImageDraw.Draw(base).text(
        (px(x + 14), px(y + 16)),
        label,
        font=font(13, "Medium"),
        fill=color(ON_PRIMARY) if selected else color(palette["on_surface_variant"]),
        anchor="lm",
    )
    return x + tw + 36


def render_settings(palette: dict) -> Image.Image:
    img = Image.new("RGBA", (W, H), color(palette["bg"]))
    draw = ImageDraw.Draw(img)

    # header
    draw.line([px(22), px(54), px(42), px(74)], fill=color(palette["on_surface"]), width=max(1, px(1.8)))
    draw.line([px(42), px(54), px(22), px(74)], fill=color(palette["on_surface"]), width=max(1, px(1.8)))
    draw.text((px(52), px(66)), "Pengaturan", font=font(24, "Medium"), fill=color(palette["on_surface"]), anchor="lm")

    y = 96
    card_bg = color(palette["surface_variant"], 128)

    def card_title(label: str, top: float) -> float:
        draw.text((px(24), px(top + 14)), label.upper(), font=font(11, "Medium"), fill=color(palette["on_surface_variant"]), anchor="ls")
        return top + 24

    # Tampilan card
    y = card_title("Tampilan", y)
    rounded_rect(img, (16, y, 377, y + 214), 22, fill=card_bg)
    draw = ImageDraw.Draw(img)
    draw.text((px(34), px(y + 20)), "Tema", font=font(12, "Regular"), fill=color(palette["on_surface_variant"]), anchor="ls")
    chip_y = y + 30
    cx = 34
    for i, label in enumerate(("Terang", "Gelap", "AMOLED")):
        cx = chip(draw, img, cx, chip_y, label, i == 1, palette)
        draw = ImageDraw.Draw(img)

    # switches
    rows = [("Baris ringkas", False), ("Jam besar", True), ("Tampilkan tanggal", True)]
    ry = chip_y + 48
    for label, on in rows:
        draw.text((px(34), px(ry + 16)), label, font=font(16, "Medium"), fill=color(palette["on_surface"]), anchor="lm")
        tx0, ty0 = 307, ry + 3
        rounded_rect(img, (tx0, ty0, tx0 + 44, ty0 + 26), 26, fill=color(PRIMARY) if on else color(palette["surface_variant"]), outline=None if on else color(palette["outline"]))
        draw = ImageDraw.Draw(img)
        knob_x = tx0 + (26 if on else 4)
        draw.ellipse([px(knob_x), px(ty0 + 5), px(knob_x + 16), px(ty0 + 21)], fill=color(ON_PRIMARY) if on else color(palette["on_surface_variant"]))
        ry += 44

    # Laci aplikasi card
    y = ry + 18
    y = card_title("Laci aplikasi", y)
    rounded_rect(img, (16, y, 377, y + 128), 22, fill=card_bg)
    draw = ImageDraw.Draw(img)
    draw.text((px(34), px(y + 20)), "Tata letak", font=font(12, "Regular"), fill=color(palette["on_surface_variant"]), anchor="ls")
    # chips: Daftar selected, Kisi
    cx = 34
    for i, label in enumerate(("Daftar", "Kisi")):
        cx = chip(draw, img, cx, y + 30, label, i == 0, palette)
        draw = ImageDraw.Draw(img)
    draw.text((px(34), px(y + 88)), "Rel alfabet", font=font(16, "Medium"), fill=color(palette["on_surface"]), anchor="lm")
    rounded_rect(img, (307, y + 75, 351, y + 101), 26, fill=color(PRIMARY))
    draw = ImageDraw.Draw(img)
    draw.ellipse([px(333), px(y + 80), px(349), px(y + 96)], fill=color(ON_PRIMARY))

    # Aplikasi card
    y += 146
    y = card_title("Aplikasi", y)
    rounded_rect(img, (16, y, 377, y + 56), 22, fill=card_bg)
    draw = ImageDraw.Draw(img)
    draw.text((px(34), px(y + 28)), "Aplikasi tersembunyi", font=font(16, "Medium"), fill=color(palette["on_surface"]), anchor="lm")
    draw.text((px(330), px(y + 28)), "3", font=font(12, "Regular"), fill=color(palette["on_surface_variant"]), anchor="rm")
    chevron_right(draw, 362, y + 28, 14, color(palette["on_surface_variant"]))

    # Tentang
    y += 74
    y = card_title("Tentang", y)
    rounded_rect(img, (16, y, 377, y + 74), 22, fill=card_bg)
    draw = ImageDraw.Draw(img)
    draw.text((px(34), px(y + 26)), "BitLauncher 0.1.0", font=font(16, "Medium"), fill=color(palette["on_surface"]), anchor="lm")
    draw.text((px(34), px(y + 48)), "Minimalis ala Niagara + Minima, lengkap ala Nova.", font=font(12, "Regular"), fill=color(palette["on_surface_variant"]), anchor="lm")

    status_bar(img, tint=palette["on_surface"])
    return img


def render_sheet(palette: dict) -> Image.Image:
    base = render_drawer(palette)
    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 128))
    base.alpha_composite(overlay)

    sheet_top = 424
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle([px(0), px(sheet_top - 8), px(393), px(852)], radius=px(28), fill=(0, 0, 0, 90))
    base.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(px(8))))

    rounded_rect(base, (0, sheet_top, 393, 852), 28, fill=color(palette["surface"]))
    draw = ImageDraw.Draw(base)
    draw.rounded_rectangle([W // 2 - px(16), px(sheet_top + 10), W // 2 + px(16), px(sheet_top + 14)], radius=px(2), fill=color(palette["outline"]))

    app = APPS[0]
    app_tile(base, (24, sheet_top + 26, 68, sheet_top + 70), 12, app[1], app[2], app[3])
    draw = ImageDraw.Draw(base)
    draw.text((px(84), px(sheet_top + 40)), app[0], font=font(20, "Medium"), fill=color(palette["on_surface"]), anchor="lm")
    draw.text((px(84), px(sheet_top + 60)), "Ketuk untuk buka", font=font(12, "Regular"), fill=color(palette["on_surface_variant"]), anchor="lm")

    draw.line([(0, px(sheet_top + 84)), (W, px(sheet_top + 84))], fill=color(palette["outline_variant"]))

    for i, label in enumerate(("Chat baru", "Kamera", "Status")):
        cx0 = 20 + i * 84
        rounded_rect(base, (cx0, sheet_top + 96, cx0 + 76, sheet_top + 152), 16, fill=color(palette["surface_variant"]))
        glyph_x, glyph_y = cx0 + 38, sheet_top + 122
        draw = ImageDraw.Draw(base)
        tint = color(palette["on_surface"])
        if label == "Chat baru":
            draw.rounded_rectangle([px(glyph_x - 10), px(glyph_y - 8), px(glyph_x + 10), px(glyph_y + 6)], radius=px(5), outline=tint, width=max(1, px(1.5)))
            draw.polygon([(px(glyph_x - 4), px(glyph_y + 6)), (px(glyph_x - 8), px(glyph_y + 11)), (px(glyph_x + 2), px(glyph_y + 6))], fill=tint)
        elif label == "Kamera":
            draw.rounded_rectangle([px(glyph_x - 11), px(glyph_y - 6), px(glyph_x + 11), px(glyph_y + 8)], radius=px(3), outline=tint, width=max(1, px(1.5)))
            draw.rectangle([px(glyph_x - 4), px(glyph_y - 9), px(glyph_x + 4), px(glyph_y - 6)], outline=tint, width=max(1, px(1.5)))
            draw.ellipse([px(glyph_x - 4), px(glyph_y - 2), px(glyph_x + 4), px(glyph_y + 6)], outline=tint, width=max(1, px(1.5)))
        else:
            draw.ellipse([px(glyph_x - 9), px(glyph_y - 9), px(glyph_x + 9), px(glyph_y + 9)], outline=tint, width=max(1, px(1.5)))
            draw.ellipse([px(glyph_x - 3), px(glyph_y - 3), px(glyph_x + 3), px(glyph_y + 3)], fill=tint)
        draw.text((px(cx0 + 38), px(sheet_top + 146)), label, font=font(11, "Regular"), fill=color(palette["on_surface"]), anchor="mm")

    draw.line([(0, px(sheet_top + 166)), (W, px(sheet_top + 166))], fill=color(palette["outline_variant"]))

    actions = [("Pin ke beranda", False), ("Sembunyikan aplikasi", False), ("Info aplikasi", False), ("Copot pemasangan", True)]
    ay = sheet_top + 182
    for label, danger in actions:
        tint = color("#ffb4ab") if danger else color(palette["on_surface"])
        draw = ImageDraw.Draw(base)
        if "Pin" in label:
            draw.polygon([(px(28), px(ay + 4)), (px(32), px(ay + 14)), (px(44), px(ay + 15)), (px(35), px(ay + 23)), (px(37), px(ay + 35)), (px(28), px(ay + 29)), (px(19), px(ay + 35)), (px(21), px(ay + 23)), (px(12), px(ay + 15)), (px(24), px(ay + 14))], outline=tint)
        elif "Sembunyikan" in label:
            draw.ellipse([px(14), px(ay + 12), px(38), px(ay + 30)], outline=tint, width=max(1, px(1.6)))
            draw.line([px(14), px(ay + 30), px(38), px(ay + 12)], fill=tint, width=max(1, px(1.6)))
        elif "Info" in label:
            draw.ellipse([px(15), px(ay + 12), px(39), px(ay + 36)], outline=tint, width=max(1, px(1.6)))
            draw.text((px(27), px(ay + 23)), "i", font=font(14, "SemiBold"), fill=tint, anchor="mm")
        else:
            draw.rounded_rectangle([px(18), px(ay + 16), px(36), px(ay + 19)], radius=px(1.5), fill=tint)
            draw.rectangle([px(22), px(ay + 12), px(32), px(ay + 16)], outline=tint, width=max(1, px(1.4)))
            draw.line([px(20), px(ay + 20), px(22), px(ay + 34), px(32), px(ay + 34), px(34), px(ay + 20)], fill=tint, width=max(1, px(1.4)))
        draw.text((px(56), px(ay + 24)), label, font=font(16, "Medium"), fill=tint, anchor="lm")
        ay += 50

    return base


def framed(img: Image.Image) -> Image.Image:
    """Wrap a phone screen in a subtle device frame for the README."""
    pad = px(12)
    canvas = Image.new("RGBA", (W + pad * 2, H + pad * 2), (0, 0, 0, 0))
    frame = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(frame).rounded_rectangle(
        [0, 0, canvas.width - 1, canvas.height - 1],
        radius=px(52),
        fill=(28, 28, 32, 255),
        outline=(70, 70, 78, 255),
        width=max(1, px(1)),
    )
    canvas.alpha_composite(frame)
    mask = rounded_mask((W, H), px(42))
    canvas.paste(img, (pad, pad), mask)
    return canvas


def save(img: Image.Image, name: str) -> None:
    path = OUT / f"preview-{name}.jpg"
    img.convert("RGB").save(path, quality=90, optimize=True, progressive=True)
    print(f"wrote {path.relative_to(ROOT)} ({os.path.getsize(path) // 1024} KB)")


def main() -> None:
    OUT.mkdir(exist_ok=True)
    save(framed(render_home()), "home")
    save(framed(render_drawer(DARK)), "drawer")
    save(framed(render_settings(DARK)), "settings")
    save(framed(render_sheet(DARK)), "app-actions")


if __name__ == "__main__":
    main()
