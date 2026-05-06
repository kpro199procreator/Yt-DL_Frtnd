"""
Letras sincronizadas (LRC) y planas.
Proveedores: lrclib, musixmatch, genius (via syncedlyrics).
Con caché SQLite para evitar re-consultas.
"""

import re
from pathlib import Path

from ytmusicdl import cache
from ytmusicdl.utils.config import get as cfg_get

try:
    import syncedlyrics as _sl
    _AVAILABLE = True
except ImportError:
    _sl = None
    _AVAILABLE = False


def _providers() -> list[str]:
    return cfg_get("lyrics", "providers", ["lrclib", "musixmatch", "genius"])


def _synced_enabled() -> bool:
    return cfg_get("lyrics", "synced", True)


def _fallback_plain() -> bool:
    return cfg_get("lyrics", "fallback_plain", True)


def get(title: str, artist: str) -> dict:
    """
    Retorna un dict con:
      - lrc:    str | None  (letras con timestamps)
      - plain:  str | None  (letras planas, sin timestamps)
      - source: str | None  (proveedor que respondió)
    Usa caché; si no está, consulta proveedores en orden.
    """
    # Caché primero
    cached = cache.get_lyrics(title, artist)
    if cached is not None:
        return cached

    result = {"lrc": None, "plain": None, "source": None}

    if not _AVAILABLE:
        return result

    query = f"{title} {artist}"

    # Intentar LRC sincronizado
    if _synced_enabled():
        for provider in _providers():
            try:
                lrc = _sl.search(query, providers=[provider])
                if lrc and _is_valid_lrc(lrc):
                    result["lrc"] = lrc.strip()
                    result["plain"] = lrc_to_plain(lrc)
                    result["source"] = provider
                    break
            except Exception:
                continue

    # Fallback a letras planas
    if result["lrc"] is None and _fallback_plain():
        try:
            plain = _sl.search(query)
            if plain:
                result["plain"] = plain.strip()
                result["source"] = "plain"
        except Exception:
            pass

    # Cachear si encontramos algo
    if result["lrc"] or result["plain"]:
        cache.set_lyrics(title, artist, result)

    return result


def _is_valid_lrc(text: str) -> bool:
    """Verifica que el LRC tenga al menos un timestamp."""
    return bool(re.search(r"\[\d+:\d+\.\d+\]", text))


def lrc_to_plain(lrc: str) -> str:
    """Convierte LRC con timestamps a texto plano."""
    lines = []
    for line in lrc.splitlines():
        clean = re.sub(r"\[\d+:\d+[.:]\d+\]", "", line).strip()
        if clean:
            lines.append(clean)
    return "\n".join(lines)


def embed_in_m4a(filepath: str, lyrics: dict) -> bool:
    """Embebe letras en un archivo m4a. Retorna True si tuvo éxito."""
    text = lyrics.get("lrc") or lyrics.get("plain")
    if not text:
        return False
    try:
        from mutagen.mp4 import MP4
        audio = MP4(filepath)
        audio["\xa9lyr"] = text
        audio.save()
        return True
    except Exception:
        return False


def embed_in_mp3(filepath: str, lyrics: dict) -> bool:
    """Embebe letras en un archivo mp3."""
    text = lyrics.get("plain") or lrc_to_plain(lyrics.get("lrc", ""))
    if not text:
        return False
    try:
        from mutagen.id3 import ID3, USLT
        tags = ID3(filepath)
        tags["USLT::eng"] = USLT(encoding=3, lang="eng", desc="", text=text)
        tags.save()
        return True
    except Exception:
        return False


def save_lrc_file(audio_path: str, lrc: str) -> Path:
    """Guarda archivo .lrc junto al audio."""
    lrc_path = Path(audio_path).with_suffix(".lrc")
    lrc_path.write_text(lrc, encoding="utf-8")
    return lrc_path
