#!/usr/bin/env python3
"""Render the newspaper / E Ink Kaleido variant of BitLauncher.

Run from the repository root after installing Pillow:
    python3 tools/render_folio_previews.py

The layout mirrors FolioHomeScreen.kt and FolioDrawerScreen.kt. Lora is used for
editorial type; Roboto is reserved for small letterspaced metadata.
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageOps

from render_previews import (
    APPS,
    H,
    S,
    W,
    app_tile,
    chevron_right,
    color,
    font,
    framed,
    grid_icon,
    px,
    rounded_rect,
    save,
    search_icon,
    sliders_icon,
    status_bar,
)

SERIF_FILE = Path(__file__).resolve().parent.parent / "preview" / "assets" / "fonts" / "Lora.ttf"
NEWS_PHOTO = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res" / "drawable" / "news_hero.jpg"

KALEIDO = {
    "bg": "#e3dfd5",
    "surface": "#eae6dd",
    "surface_variant": "#d6d1c6",
    "on_surface": "#1d1c19",
    "on_surface_variant": "#666159",
    "outline": "#979185",
    "outline_variant": "#c7c1b6",
    "primary": "#3e433d",
    "secondary": "#7b584d",
    "accents": ["#625d55", "#746d61", "#585b56"],
}

KALEIDO_NIGHT = {
    "bg": "#191816",
    "surface": "#201f1c",
    "surface_variant": "#2b2925",
    "on_surface": "#e2ded2",
    "on_surface_variant": "#a29c90",
    "outline": "#6b665d",
    "outline_variant": "#37342e",
    "primary": "#b9b09c",
    "secondary": "#b58c7d",
    "accents": ["#a29c90", "#b0a795", "#93958c"],
}


def paper_hex(value: str) -> str:
    """Reduce a tile to 8% of its original chroma, leaving luminance intact."""
    r, g, b, _ = color(value)
    luma = 0.2126 * r + 0.7152 * g + 0.0722 * b
    channels = [round(luma + (channel - luma) * 0.08) for channel in (r, g, b)]
    return "#" + "".join(f"{channel:02x}" for channel in channels)


def paper_tile(img, box, radius, c1, c2, glyph="") -> None:
    app_tile(img, box, radius, paper_hex(c1), paper_hex(c2), glyph)


def serif(size: float, weight: str = "Regular") -> ImageFont.FreeTypeFont:
    path = str(SERIF_FILE) if SERIF_FILE.exists() else "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf"
    result = ImageFont.truetype(path, px(size))
    if SERIF_FILE.exists():
        try:
            result.set_variation_by_name(weight)
        except Exception:
            pass
    return result


def tracked_width(draw: ImageDraw.ImageDraw, text: str, face, tracking: float) -> float:
    if not text:
        return 0
    return sum(draw.textlength(char, font=face) / S for char in text) + tracking * (len(text) - 1)


def tracked_text(
    draw: ImageDraw.ImageDraw,
    x: float,
    baseline: float,
    text: str,
    face,
    fill,
    tracking: float = 1.6,
    align: str = "left",
) -> None:
    width = tracked_width(draw, text, face, tracking)
    if align == "right":
        x -= width
    elif align == "center":
        x -= width / 2
    for char in text:
        draw.text((px(x), px(baseline)), char, font=face, fill=fill, anchor="ls")
        x += draw.textlength(char, font=face) / S + tracking


def rule(img: Image.Image, y: float, palette: dict, thick: bool = False) -> None:
    draw = ImageDraw.Draw(img)
    ink = color(palette["on_surface"] if thick else palette["outline_variant"])
    if thick:
        draw.rectangle([px(20), px(y), px(373), px(y + 2.5)], fill=ink)
        draw.rectangle([px(20), px(y + 4.5), px(373), px(y + 5.3)], fill=ink)
    else:
        draw.line([(px(20), px(y)), (px(373), px(y))], fill=ink, width=max(1, px(0.7)))


def dotted_leader(draw: ImageDraw.ImageDraw, x0: float, x1: float, y: float, tint) -> None:
    x = x0
    while x < x1:
        draw.line([(px(x), px(y)), (px(min(x + 1.4, x1)), px(y))], fill=tint, width=max(1, px(0.7)))
        x += 5.8


def editorial_row(
    img: Image.Image,
    app: tuple,
    number: int,
    y: float,
    palette: dict,
    large: bool = False,
) -> None:
    name, c1, c2, glyph, work = app
    draw = ImageDraw.Draw(img)
    accent = color(palette["accents"][(number - 1) % len(palette["accents"])])
    meta = font(9.5 if large else 9, "SemiBold")
    draw.text((px(20), px(y + 33)), f"{number:02d}", font=meta, fill=accent, anchor="ls")

    icon_size = 26 if large else 28
    icon_y = y + (10 if large else 9)
    paper_tile(img, (48, icon_y, 48 + icon_size, icon_y + icon_size), 6, c1, c2, glyph)
    draw = ImageDraw.Draw(img)

    label_face = serif(23 if large else 18)
    label_x = 86
    baseline = y + (36 if large else 32)
    draw.text((px(label_x), px(baseline)), name, font=label_face, fill=color(palette["on_surface"]), anchor="ls")
    label_end = label_x + draw.textlength(name, font=label_face) / S
    if work:
        tracked_text(draw, label_end + 8, baseline - 1, "KERJA", font(8, "Medium"), color(palette["on_surface_variant"]), 1.2)
        label_end += 46

    dotted_leader(draw, label_end + 12, 356, y + (29 if large else 25), color(palette["outline"]))
    chevron_right(draw, 366, y + (28 if large else 24), 11, color(palette["on_surface_variant"]), 1.4)


def draw_article_line(
    draw: ImageDraw.ImageDraw,
    x: float,
    baseline: float,
    segments: list[tuple[str, str]],
    palette: dict,
) -> None:
    """Draw one article line; link segments are bold and underlined like the live text."""
    for text, role in segments:
        face = serif(16, "Regular" if role == "regular" else "SemiBold")
        tint = color(palette["on_surface"] if role == "regular" else palette["primary"])
        draw.text((px(x), px(baseline)), text, font=face, fill=tint, anchor="ls")
        width = draw.textlength(text, font=face) / S
        if role == "link":
            draw.line([(px(x), px(baseline + 2)), (px(x + width), px(baseline + 2))], fill=tint, width=max(1, px(0.7)))
        x += width


def render_folio_home(palette: dict = KALEIDO) -> Image.Image:
    img = Image.new("RGBA", (W, H), color(palette["bg"]))
    draw = ImageDraw.Draw(img)
    ink = color(palette["on_surface"])
    soft = color(palette["on_surface_variant"])

    tracked_text(draw, 20, 67, "BITLAUNCHER", font(11, "Bold"), ink, 3.4)
    tracked_text(draw, 373, 67, "EDISI 297", font(10, "Medium"), soft, 1.6, "right")
    rule(img, 76, palette, thick=True)
    tracked_text(draw, 196.5, 99, "SABTU, 24 OKTOBER 2026", font(10, "Medium"), soft, 2.2, "center")
    rule(img, 107, palette)

    tracked_text(draw, 20, 132, "KABAR UTAMA", font(10, "SemiBold"), ink, 1.6)
    tracked_text(draw, 373, 132, "HLM. 1", font(10, "Medium"), soft, 1.6, "right")
    draw.text((px(20), px(172)), "3 pesan menanti di", font=serif(34), fill=ink, anchor="ls")
    draw.text((px(20), px(210)), "WhatsApp", font=serif(34), fill=ink, anchor="ls")

    if NEWS_PHOTO.exists():
        photo = Image.open(NEWS_PHOTO).convert("RGB")
        photo = ImageOps.fit(photo, (px(353), px(176)), method=Image.Resampling.LANCZOS)
        img.paste(photo, (px(20), px(222)))
    draw = ImageDraw.Draw(img)
    draw.line([(px(20), px(398)), (px(373), px(398))], fill=ink, width=max(1, px(1)))
    tracked_text(draw, 20, 416, "ILUSTRASI · SUASANA PAGI", font(8, "Medium"), soft, 1.4)

    draw.text(
        (px(20), px(446)),
        "Rembang, Sabtu, 24 Oktober 2026 —",
        font=serif(15, "SemiBold"),
        fill=ink,
        anchor="ls",
    )
    article = [
        [("Pada pukul 09.41 terjadi sebuah fenomena", "regular")],
        [("aneh: ada ", "regular"), ("3 pesan belum dibaca di WhatsApp.", "link")],
        [("Kabar lain datang dari ", "regular"), ("Gmail, dengan 2 pesan", "link")],
        [("yang menanti. ", "link"), ("Untuk kabar berikutnya, buka", "regular")],
        [("Spotify", "link"), (" dan ", "regular"), ("Kamera", "link"), (".", "regular")],
    ]
    for index, line in enumerate(article):
        draw_article_line(draw, 20, 472 + index * 26, line, palette)

    rule(img, 622, palette)
    tracked_text(draw, 20, 642, "INDEKS APLIKASI", font(10, "SemiBold"), ink, 1.6)
    tracked_text(draw, 373, 642, "05 ENTRI", font(10, "Medium"), soft, 1.6, "right")
    rule(img, 650, palette)
    editorial_row(img, APPS[0], 1, 658, palette, large=True)
    editorial_row(img, APPS[1], 2, 720, palette, large=True)

    # Fixed archive actions, matching the Compose footer.
    draw = ImageDraw.Draw(img)
    draw.line([(px(20), px(766)), (px(373), px(766))], fill=ink, width=max(1, px(1)))
    rounded_rect(img, (20, 778, 265, 824), 2, outline=ink)
    rounded_rect(img, (273, 778, 319, 824), 2, outline=ink)
    rounded_rect(img, (327, 778, 373, 824), 2, outline=ink)
    draw = ImageDraw.Draw(img)
    search_icon(draw, 81, 801, 16, ink, 1.6)
    tracked_text(draw, 101, 805, "CARI DI ARSIP", font(10, "SemiBold"), ink, 1.4)
    grid_icon(draw, 296, 801, 16, ink, 1.5)
    sliders_icon(draw, 350, 801, 17, ink, 1.5)
    tracked_text(draw, 196.5, 836, "BITLAUNCHER · LAUNCHER EDITORIAL · HLM. 1", font(8, "Medium"), soft, 1.2, "center")

    status_bar(img, tint=palette["on_surface"])
    return img


def archive_header(img: Image.Image, palette: dict, count: int = 24) -> None:
    draw = ImageDraw.Draw(img)
    ink = color(palette["on_surface"])
    soft = color(palette["on_surface_variant"])
    draw.rounded_rectangle([W // 2 - px(20), px(56), W // 2 + px(20), px(59)], radius=px(1.5), fill=color(palette["outline"]))
    draw.text((px(20), px(105)), "ARSIP", font=serif(34), fill=ink, anchor="ls")
    tracked_text(draw, 142, 101, f"{count} JUDUL", font(9.5, "Medium"), soft, 1.4)
    tracked_text(draw, 373, 101, "TUTUP", font(10, "SemiBold"), soft, 1.6, "right")
    rule(img, 114, palette, thick=True)
    rounded_rect(img, (20, 128, 373, 174), 2, outline=color(palette["outline"]))
    draw = ImageDraw.Draw(img)
    search_icon(draw, 38, 151, 16, soft, 1.6)
    draw.text((px(58), px(151)), "Cari di arsip", font=serif(16), fill=soft, anchor="lm")
    tracked_text(draw, 20, 199, "TAMPILAN: DAFTAR", font(9.5, "Medium"), soft, 1.4)
    grid_icon(draw, 337, 193, 16, soft, 1.5)
    sliders_icon(draw, 365, 193, 16, soft, 1.5)
    rule(img, 214, palette)


def section_heading(draw: ImageDraw.ImageDraw, letter: str, y: float, palette: dict) -> None:
    draw.text((px(20), px(y + 29)), letter, font=serif(28), fill=color(palette["on_surface"]), anchor="ls")
    draw.line([(px(48), px(y + 20)), (px(367), px(y + 20))], fill=color(palette["outline"]), width=max(1, px(0.8)))


def alphabet_rail(draw: ImageDraw.ImageDraw, letters: list[str], palette: dict) -> None:
    if not letters:
        return
    top, bottom = 230, 810
    step = (bottom - top) / len(letters)
    for index, letter in enumerate(letters):
        draw.text((px(382), px(top + step * (index + 0.5))), letter, font=font(8, "Medium"), fill=color(palette["on_surface_variant"]), anchor="mm")


def render_folio_archive(palette: dict = KALEIDO) -> Image.Image:
    img = Image.new("RGBA", (W, H), color(palette["bg"]))
    archive_header(img, palette)
    draw = ImageDraw.Draw(img)

    sections: dict[str, list[tuple]] = {}
    for app in sorted(APPS, key=lambda item: item[0].lower()):
        sections.setdefault(app[0][0].upper(), []).append(app)
    number_by_name = {app[0]: index for index, app in enumerate(sorted(APPS, key=lambda item: item[0].lower()), 1)}

    y = 222
    for letter, group in sorted(sections.items()):
        section_heading(draw, letter, y, palette)
        y += 40
        for app in group:
            if y > 786:
                break
            editorial_row(img, app, number_by_name[app[0]], y, palette)
            y += 50
        if y > 786:
            break

    draw = ImageDraw.Draw(img)
    alphabet_rail(draw, sorted(sections), palette)
    status_bar(img, tint=palette["on_surface"])
    return img


def render_folio_grid(palette: dict = KALEIDO) -> Image.Image:
    img = Image.new("RGBA", (W, H), color(palette["bg"]))
    archive_header(img, palette)
    draw = ImageDraw.Draw(img)
    # Change the metadata to reflect the active grid layout.
    draw.rectangle([px(18), px(181), px(180), px(205)], fill=color(palette["bg"]))
    tracked_text(draw, 20, 199, "TAMPILAN: KISI", font(9.5, "Medium"), color(palette["on_surface_variant"]), 1.4)

    sections: dict[str, list[tuple]] = {}
    for app in sorted(APPS, key=lambda item: item[0].lower()):
        sections.setdefault(app[0][0].upper(), []).append(app)

    y = 224
    cell_w, cell_h, gap = 83.75, 82, 6
    for letter, group in sorted(sections.items()):
        section_heading(draw, letter, y, palette)
        y += 40
        for start in range(0, len(group), 4):
            if y > 742:
                break
            for column, app in enumerate(group[start : start + 4]):
                x = 20 + column * (cell_w + gap)
                rounded_rect(img, (x, y, x + cell_w, y + cell_h), 2, outline=color(palette["outline_variant"]))
                paper_tile(img, (x + cell_w / 2 - 20, y + 10, x + cell_w / 2 + 20, y + 50), 8, app[1], app[2], app[3])
                draw = ImageDraw.Draw(img)
                draw.text((px(x + cell_w / 2), px(y + 68)), app[0], font=serif(11), fill=color(palette["on_surface"]), anchor="mm")
            y += cell_h + gap
        if y > 742:
            break

    status_bar(img, tint=palette["on_surface"])
    return img


def main() -> None:
    save(framed(render_folio_home()), "folio-home")
    save(framed(render_folio_archive()), "folio-archive")
    save(framed(render_folio_grid()), "folio-grid")
    save(framed(render_folio_archive(KALEIDO_NIGHT)), "folio-night")


if __name__ == "__main__":
    main()
