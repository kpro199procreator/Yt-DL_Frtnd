"""
yt_backend.py — Adaptador Chaquopy para ytmusicdl.
Expone las funciones que PythonBridge.kt llama por nombre.
Delega toda la lógica a los módulos en ytmusicdl/ (raíz del repo).
"""
import json
import sys
import os

# yt_backend.py está en app/src/main/python/
# ytmusicdl/ está en la raíz del repo (3 niveles arriba)
_repo_root = os.path.dirname(  # repo root
    os.path.dirname(            # app/
        os.path.dirname(        # app/src/
            os.path.dirname(    # app/src/main/
                os.path.abspath(__file__)  # app/src/main/python/
            )
        )
    )
)
if _repo_root not in sys.path:
    sys.path.insert(0, _repo_root)

from ytmusicdl.apis.ytmusic import (
    search_songs,
    get_album_tracks,
    get_song_info,
)
from ytmusicdl import downloader
from ytmusicdl import lyrics as lyr_mod


def search_tracks(query, limit=8):
    """Búsqueda de canciones. Llamado por SearchScreen vía PythonBridge."""
    tracks = search_songs(str(query), int(limit or 8))
    return json.dumps(tracks)


def extract_audio(video_id):
    """
    Extrae metadata del stream sin descargar.
    Usa formato 140 (m4a AAC 128kbps) — máxima compatibilidad en YouTube.
    Mantiene compatibilidad con PythonBridge.parseAudioResult().
    """
    import yt_dlp
    url = f"https://music.youtube.com/watch?v={video_id}"
    opts = {
        "format": "140",       # m4a AAC 128kbps, universal en YouTube
        "quiet": True,
        "skip_download": True,
        "noplaylist": True,
        "extractor_retries": 3,
    }
    try:
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=False)
        return json.dumps({
            "audioUrl":     info.get("url", ""),
            "containerExt": "m4a",  # 140 siempre es m4a
            "bitrate":      128,
            "artist":       info.get("uploader", ""),
            "title":        info.get("title", ""),
            "coverUrl":     info.get("thumbnail", ""),
        })
    except Exception as e:
        return json.dumps({"error": str(e)})


def download_track_full(video_id, output_path, title="", artist="",
                        album="", year="", cover_url=""):
    """
    Descarga completa: audio + metadata + letras + carátula.
    Llamado por DownloadService cuando delega todo a Python.
    Retorna JSON con {status, path, error}.
    """
    import pathlib
    track = {
        "video_id":  video_id,
        "url":       f"https://music.youtube.com/watch?v={video_id}",
        "title":     title,
        "artist":    artist,
        "album":     album,
        "year":      year,
        "cover_url": cover_url,
    }
    result = downloader.download_track(
        track,
        output_dir=pathlib.Path(output_path),
    )
    return json.dumps(result)


def get_music_metadata(video_id_or_query):
    """Compatibilidad con PythonBridge.parseTrack()."""
    if len(str(video_id_or_query)) <= 20:
        info = get_song_info(str(video_id_or_query))
        if info:
            return json.dumps(info)
    results = search_songs(str(video_id_or_query), limit=1)
    return json.dumps(results[0]) if results else "null"


def get_lyrics(title, artist):
    """Letras vía syncedlyrics (lrclib + musixmatch + genius)."""
    result = lyr_mod.get(str(title), str(artist))
    return result.get("lrc") or result.get("plain") or ""
