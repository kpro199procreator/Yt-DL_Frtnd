"""
Carátula: descarga, embed en audio, render en terminal.
Técnica half-block ▀ con truecolor ANSI (24-bit RGB).

Proporción correcta:
  - Cada char terminal mide aprox 1:2 (ancho:alto)
  - Con half-block ▀ cada char = 1 col × 2 filas de píxeles
  - Para imagen cuadrada: width_px = width_chars, height_px = width_chars
    → height_chars = width_chars / 2  (imagen cuadrada visualmente)
  - cover_width en config = columnas de caracteres
"""

from __future__ import annotations
import io
import requests
from pathlib import Path

try:
    from PIL import Image
    _PIL = True
except ImportError:
    _PIL = False

_RESET = "\x1b[0m"

# ── Descarga ──────────────────────────────────────────────────────────────────

def fetch_best(cover_url: str, size: int = 500) -> bytes | None:
    if not cover_url:
        return None
    base = cover_url.split("=w")[0].split("=h")[0]
    for url in (f"{base}=w{size}-h{size}", base, cover_url):
        try:
            r = requests.get(url, timeout=15)
            if r.ok and r.content:
                return r.content
        except Exception:
            continue
    return None

# ── Embed ─────────────────────────────────────────────────────────────────────

def embed(filepath: str, data: bytes) -> bool:
    ext = Path(filepath).suffix.lower()
    try:
        if ext == ".m4a":
            from mutagen.mp4 import MP4, MP4Cover
            audio = MP4(filepath)
            audio["covr"] = [MP4Cover(data, imageformat=MP4Cover.FORMAT_JPEG)]
            audio.save()
            return True
        elif ext == ".mp3":
            from mutagen.id3 import ID3, APIC
            try:
                tags = ID3(filepath)
            except Exception:
                tags = ID3()
            tags["APIC:"] = APIC(encoding=3, mime="image/jpeg",
                                  type=3, desc="Cover", data=data)
            tags.save(filepath)
            return True
    except Exception:
        pass
    return False

# ── Render terminal ───────────────────────────────────────────────────────────

def render(
    data: bytes | None,
    width: int = 44,
    title: str = "",
    artist: str = "",
    album: str = "",
    year: str = "",
    duration: str = "",
) -> str:
    """
    Renderiza la carátula en terminal usando bloques Unicode ▀.

    width = número de columnas de caracteres de la imagen.
    La imagen se redimensiona a (width × width) píxeles.
    Con half-block, eso produce (width × width/2) caracteres → cuadrado visual.
    """
    if not _PIL or not data:
        return _placeholder(title, artist, album, year, duration, width)

    try:
        img = Image.open(io.BytesIO(data)).convert("RGB")

        # width píxeles de ancho, width píxeles de alto.
        # height_chars = width // 2  → proporción 1:1 visual (cuadrado).
        px_w = width
        px_h = width  # NO multiplicar por 2
        img  = img.resize((px_w, px_h), Image.LANCZOS)
        px   = list(img.getdata())

        height_chars = px_h // 2  # filas de caracteres resultantes
        img_lines = []
        for row in range(height_chars):
            line = ""
            for col in range(px_w):
                tr, tg, tb = px[(row * 2)     * px_w + col]
                br, bg, bb = px[(row * 2 + 1) * px_w + col]
                line += (f"\x1b[38;2;{tr};{tg};{tb}m"
                         f"\x1b[48;2;{br};{bg};{bb}m▀")
            img_lines.append(line + _RESET)

        return _compose(img_lines, _info(title, artist, album, year, duration))

    except Exception:
        return _placeholder(title, artist, album, year, duration, width)


def _info(title, artist, album, year, duration) -> list[str]:
    lines = [""]
    if title:
        lines.append(f"\x1b[1;97m{_trunc(title, 38)}\x1b[0m")
    if artist:
        lines.append(f"\x1b[38;5;213m{_trunc(artist, 38)}\x1b[0m")
    if album:
        lines.append(f"\x1b[2;37m{_trunc(album, 38)}\x1b[0m")
    lines.append("")
    meta_parts = [x for x in (year, duration) if x and str(x) != "None"]
    if meta_parts:
        lines.append(f"\x1b[90m{'  ·  '.join(meta_parts)}\x1b[0m")
    return lines


def _compose(img_lines: list[str], info_lines: list[str], gap: int = 3) -> str:
    total = max(len(img_lines), len(info_lines))
    rows  = []
    for i in range(total):
        left  = img_lines[i]  if i < len(img_lines)  else ""
        right = info_lines[i] if i < len(info_lines) else ""
        rows.append(f"{left}{' ' * gap}{right}")
    return "\n".join(rows)


def _trunc(s: str, n: int) -> str:
    return s if len(s) <= n else s[:n-1] + "…"


def _placeholder(title, artist, album, year, duration, width) -> str:
    initial = (artist or title or "?")[0].upper()
    w     = max(width, 10)
    h     = w // 2
    mid_r = h // 2
    mid_c = (w - 1) // 2
    rows  = []
    rows.append(f"╭{'─' * w}╮")
    for r in range(h):
        if r == mid_r:
            rows.append(f"│{' ' * mid_c}\x1b[1;35m{initial}\x1b[0m{' ' * (w - mid_c - 1)}│")
        else:
            rows.append(f"│{' ' * w}│")
    rows.append(f"╰{'─' * w}╯")
    return _compose(rows, _info(title, artist, album, year, duration), gap=2)
