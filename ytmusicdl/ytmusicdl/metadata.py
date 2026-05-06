"""
Escritura de tags de audio para m4a (AAC) y mp3.
Soporta: título, artista, álbum, año, número de pista, carátula, letras.
"""

from __future__ import annotations
import requests
from pathlib import Path


def write(filepath: str, meta: dict) -> bool:
    """
    Escribe tags en el archivo de audio detectando el formato automáticamente.
    meta: dict con cualquiera de estas keys:
      title, artist, album, year, track_number, total_tracks, cover_url, lyrics
    """
    path = Path(filepath)
    ext  = path.suffix.lower()

    if ext == ".m4a":
        return _write_m4a(filepath, meta)
    elif ext == ".mp3":
        return _write_mp3(filepath, meta)
    elif ext in (".opus", ".ogg", ".webm"):
        return _write_vorbis(filepath, meta)
    return False


def _fetch_cover(url: str) -> bytes | None:
    try:
        r = requests.get(url, timeout=15)
        r.raise_for_status()
        return r.content
    except Exception:
        return None


def _write_m4a(filepath: str, meta: dict) -> bool:
    try:
        from mutagen.mp4 import MP4, MP4Cover
        audio = MP4(filepath)

        if title := meta.get("title"):
            audio["\xa9nam"] = title
        if artist := meta.get("artist"):
            audio["\xa9ART"] = artist
        if album := meta.get("album"):
            audio["\xa9alb"] = album
        if year := meta.get("year"):
            audio["\xa9day"] = str(year)
        if genre := meta.get("genre"):
            audio["\xa9gen"] = genre

        track = meta.get("track_number")
        total = meta.get("total_tracks", 0)
        if track:
            audio["trkn"] = [(int(track), int(total))]

        if lyrics := meta.get("lyrics"):
            audio["\xa9lyr"] = lyrics

        if cover_url := meta.get("cover_url"):
            data = _fetch_cover(cover_url)
            if data:
                fmt = (MP4Cover.FORMAT_PNG
                       if cover_url.lower().endswith(".png")
                       else MP4Cover.FORMAT_JPEG)
                audio["covr"] = [MP4Cover(data, imageformat=fmt)]

        audio.save()
        return True
    except Exception:
        return False


def _write_mp3(filepath: str, meta: dict) -> bool:
    try:
        from mutagen.id3 import (
            ID3, TIT2, TPE1, TALB, TDRC, TRCK, TCON, USLT, APIC
        )
        try:
            tags = ID3(filepath)
        except Exception:
            from mutagen.id3 import ID3NoHeaderError
            tags = ID3()

        if title := meta.get("title"):
            tags["TIT2"] = TIT2(encoding=3, text=title)
        if artist := meta.get("artist"):
            tags["TPE1"] = TPE1(encoding=3, text=artist)
        if album := meta.get("album"):
            tags["TALB"] = TALB(encoding=3, text=album)
        if year := meta.get("year"):
            tags["TDRC"] = TDRC(encoding=3, text=str(year))
        if genre := meta.get("genre"):
            tags["TCON"] = TCON(encoding=3, text=genre)

        track = meta.get("track_number")
        total = meta.get("total_tracks")
        if track:
            trck = f"{track}/{total}" if total else str(track)
            tags["TRCK"] = TRCK(encoding=3, text=trck)

        if lyrics := meta.get("lyrics"):
            tags["USLT::eng"] = USLT(encoding=3, lang="eng", desc="", text=lyrics)

        if cover_url := meta.get("cover_url"):
            data = _fetch_cover(cover_url)
            if data:
                mime = "image/png" if cover_url.lower().endswith(".png") else "image/jpeg"
                tags["APIC:"] = APIC(encoding=3, mime=mime, type=3, desc="Cover", data=data)

        tags.save(filepath)
        return True
    except Exception:
        return False


def _write_vorbis(filepath: str, meta: dict) -> bool:
    """Tags para opus/ogg via mutagen VorbisComment."""
    try:
        from mutagen import File
        audio = File(filepath)
        if audio is None:
            return False

        if title := meta.get("title"):
            audio["title"] = title
        if artist := meta.get("artist"):
            audio["artist"] = artist
        if album := meta.get("album"):
            audio["album"] = album
        if year := meta.get("year"):
            audio["date"] = str(year)
        if track := meta.get("track_number"):
            audio["tracknumber"] = str(track)
        if lyrics := meta.get("lyrics"):
            audio["lyrics"] = lyrics

        audio.save()
        return True
    except Exception:
        return False
