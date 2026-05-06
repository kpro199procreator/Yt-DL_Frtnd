"""
Motor de descarga usando yt-dlp. v0.3
"""

from __future__ import annotations
from pathlib import Path
from typing import Callable, Optional

import yt_dlp

from ytmusicdl import cache, metadata, lyrics as lyr_mod, cover as cover_mod
from ytmusicdl.utils.config import get as cfg_get
from ytmusicdl.utils.paths import expand


def _output_dir() -> Path:
    raw = cfg_get("download", "output_dir", "~/Music")
    p = expand(raw)
    p.mkdir(parents=True, exist_ok=True)
    return p


def _audio_format() -> str:
    return cfg_get("download", "format", "m4a")


def _make_ydl_opts(output_dir: Path, audio_format: str,
                   on_bytes: Optional[Callable] = None) -> dict:
    def _hook(d: dict):
        if on_bytes is None:
            return
        if d["status"] == "downloading":
            dl  = d.get("downloaded_bytes", 0)
            tot = d.get("total_bytes") or d.get("total_bytes_estimate", 0)
            on_bytes(dl, tot)
        elif d["status"] == "finished":
            on_bytes(d.get("total_bytes", 0), d.get("total_bytes", 0))

    return {
        "format": "140",  # m4a AAC 128kbps — consistente con yt_backend.py
        "outtmpl": str(output_dir / "%(artist)s - %(title)s.%(ext)s"),
        "quiet": True,
        "no_warnings": True,
        "writethumbnail": False,
        "postprocessors": [
            {"key": "FFmpegExtractAudio", "preferredcodec": audio_format, "preferredquality": "0"},
            {"key": "FFmpegMetadata", "add_metadata": True},
        ],
        "retries": 5,
        "fragment_retries": 10,
        "progress_hooks": [_hook],
    }


def download_track(
    track: dict,
    output_dir: Optional[Path] = None,
    audio_format: Optional[str] = None,
    embed_lyrics: bool = True,
    save_lrc: bool = True,
    on_bytes: Optional[Callable[[int, int], None]] = None,
) -> dict:
    """
    Retorna:
      {"status": "ok"|"skipped"|"error", "path": str, "error": str, "cover_data": bytes|None}
    """
    out_dir  = output_dir or _output_dir()
    fmt      = audio_format or _audio_format()
    video_id = track.get("video_id", "")

    # Skip si ya descargado
    if cfg_get("download", "skip_downloaded", True) and video_id:
        existing = cache.is_downloaded(video_id)
        if existing:
            return {"status": "skipped", "path": existing, "error": None, "cover_data": None}

    url = track.get("url")
    if not url and video_id:
        url = f"https://music.youtube.com/watch?v={video_id}"
    if not url:
        return {"status": "error", "path": None, "error": "Sin URL", "cover_data": None}

    # Descargar cover ANTES del audio (para embed y render)
    cover_data = cover_mod.fetch_best(track.get("cover_url", "")) if track.get("cover_url") else None

    # Descarga de audio
    opts = _make_ydl_opts(out_dir, fmt, on_bytes)
    try:
        with yt_dlp.YoutubeDL(opts) as ydl:
            info     = ydl.extract_info(url, download=True)
            raw_path = ydl.prepare_filename(info)
            filepath = _resolve_final_path(raw_path, fmt)
    except yt_dlp.utils.DownloadError as e:
        return {"status": "error", "path": None, "error": str(e), "cover_data": cover_data}
    except Exception as e:
        return {"status": "error", "path": None, "error": str(e), "cover_data": cover_data}

    if not filepath or not Path(filepath).exists():
        return {"status": "error", "path": None,
                "error": "Archivo no encontrado tras descarga", "cover_data": cover_data}

    # Metadata
    meta = {k: track.get(k) for k in
            ("title", "artist", "album", "year", "track_number", "total_tracks", "genre")}

    if cover_data and cfg_get("metadata", "embed_cover", True):
        meta["cover_data"] = cover_data

    if embed_lyrics or save_lrc:
        title  = track.get("title", "")
        artist = track.get("artist", "")
        if title and artist:
            lyrics = lyr_mod.get(title, artist)
            if embed_lyrics and (lyrics.get("lrc") or lyrics.get("plain")):
                meta["lyrics"] = lyrics.get("lrc") or lyrics.get("plain")
            if save_lrc and lyrics.get("lrc") and cfg_get("metadata", "save_lrc", True):
                lyr_mod.save_lrc_file(filepath, lyrics["lrc"])

    metadata.write(filepath, meta)

    if video_id:
        cache.mark_downloaded(video_id, filepath, fmt)

    return {"status": "ok", "path": filepath, "error": None, "cover_data": cover_data}


def _resolve_final_path(raw_path: str, fmt: str) -> str:
    p = Path(raw_path)
    target = p.with_suffix(f".{fmt}")
    if target.exists():
        return str(target)
    if p.exists():
        return str(p)
    for c in p.parent.glob(f"{p.stem}.*"):
        if c.suffix.lstrip(".") in ("m4a", "mp3", "opus", "ogg"):
            return str(c)
    return raw_path
