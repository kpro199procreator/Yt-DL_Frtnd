import json
import io
import re
from contextlib import redirect_stdout
from yt_dlp import YoutubeDL
from yt_dlp.utils import DownloadError
from ytmusicapi import YTMusic
import requests

_ytmusic = YTMusic()


def _track_from_search_item(item: dict) -> dict:
    artists = item.get("artists") or []
    artist_name = artists[0].get("name", "") if artists else ""
    thumbs = item.get("thumbnails") or []
    return {
        "videoId": item.get("videoId", ""),
        "title": item.get("title", ""),
        "artist": artist_name,
        "album": (item.get("album") or {}).get("name", ""),
        "year": str(item.get("year", "") or ""),
        "coverUrl": thumbs[-1]["url"] if thumbs else "",
        "duration": item.get("duration", ""),
        "streamUrl": "",
    }


def search_tracks(query, limit=8):
    results = _ytmusic.search(query, filter="songs", limit=int(limit or 8))
    tracks = [_track_from_search_item(x) for x in results if x.get("videoId")]
    return json.dumps(tracks)


def extract_audio(video_id, preferred_format_id=None):
    url = f"https://www.youtube.com/watch?v={video_id}"
    opts = {
        "quiet": True,
        "skip_download": True,
        "noplaylist": True,
        "ignoreconfig": True,
        "format": "bestaudio/best",
    }
    try:
        with YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=False)
    except DownloadError as e:
        return json.dumps({
            "error": True,
            "message": f"No se pudo obtener metadata de audio: {str(e)}",
        })

    formats = info.get("formats") or []
    candidates = [f for f in formats if f.get("vcodec") == "none" and f.get("url")]
    if not candidates:
        return json.dumps({
            "error": True,
            "message": "No hay formatos de audio válidos para este video.",
        })

    preferred = str(preferred_format_id or "").strip()
    selected = next((f for f in candidates if str(f.get("format_id")) == preferred), None) if preferred else None

    if not selected:
        preferred_ext_order = {"m4a": 0, "webm": 1, "opus": 2}

        def rank(fmt):
            ext_rank = preferred_ext_order.get((fmt.get("ext") or "").lower(), 99)
            abr = float(fmt.get("abr") or 0)
            tbr = float(fmt.get("tbr") or 0)
            bitrate = abr if abr > 0 else tbr
            return ext_rank, -bitrate, str(fmt.get("format_id") or "")

        selected = sorted(candidates, key=rank)[0]

    return json.dumps({
        "audioUrl": selected.get("url", ""),
        "containerExt": selected.get("ext", "m4a"),
        "bitrate": int(selected.get("abr") or selected.get("tbr") or 0),
        "artist": info.get("uploader", ""),
        "title": info.get("title", ""),
        "coverUrl": info.get("thumbnail", ""),
        "formatId": str(selected.get("format_id") or ""),
    })


def list_audio_formats(video_id):
    url = f"https://www.youtube.com/watch?v={video_id}"
    opts = {
        "quiet": True,
        "skip_download": True,
        "noplaylist": True,
        "ignoreconfig": True,
        "format": "bestaudio/best",
    }
    try:
        with YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=False)
            out = io.StringIO()
            with redirect_stdout(out):
                ydl.list_formats(info)
            list_formats_output = out.getvalue()
    except DownloadError:
        return json.dumps([])

    formats = info.get("formats") or []
    note_by_id = {}
    line_pattern = re.compile(r"^\s*(\S+)\s+(\S+)\s+(.+)$")
    for raw_line in list_formats_output.splitlines():
        line = raw_line.strip()
        if not line or line.lower().startswith("id ") or line.startswith("[info]"):
            continue
        m = line_pattern.match(raw_line)
        if not m:
            continue
        format_id = m.group(1).strip()
        note_by_id[format_id] = m.group(3).strip()

    audio_formats = []
    for fmt in formats:
        if fmt.get("vcodec") != "none":
            continue
        fmt_id = str(fmt.get("format_id") or "")
        audio_formats.append({
            "format_id": fmt_id,
            "ext": fmt.get("ext", ""),
            "abr": int(fmt.get("abr") or 0),
            "acodec": fmt.get("acodec", ""),
            "protocol": fmt.get("protocol", ""),
            "note": note_by_id.get(fmt_id) or fmt.get("format_note", "") or fmt.get("format", ""),
        })
    return json.dumps(audio_formats)


def get_music_metadata(video_id_or_query):
    item = _ytmusic.get_song(video_id_or_query).get("videoDetails") if len(video_id_or_query) <= 20 else None
    if not item:
        rs = _ytmusic.search(video_id_or_query, filter="songs", limit=1)
        if not rs:
            return "null"
        return json.dumps(_track_from_search_item(rs[0]))

    return json.dumps({
        "videoId": item.get("videoId", ""),
        "title": item.get("title", ""),
        "artist": item.get("author", ""),
        "album": "",
        "year": "",
        "coverUrl": (item.get("thumbnail", {}).get("thumbnails") or [{}])[-1].get("url", ""),
        "duration": str(item.get("lengthSeconds", "")),
        "streamUrl": "",
    })


def get_lyrics(title, artist):
    try:
        q = requests.get(
            "https://lrclib.net/api/search",
            params={"track_name": title, "artist_name": artist},
            timeout=8,
        )
        q.raise_for_status()
        data = q.json() or []
        if data:
            return data[0].get("syncedLyrics") or data[0].get("plainLyrics") or ""
    except Exception:
        pass
    return ""


def run_ytdlp_cli(args_line="--help"):
    import shlex
    import contextlib

    argv = shlex.split(args_line or "")
    stdout_buffer = io.StringIO()
    stderr_buffer = io.StringIO()

    try:
        from yt_dlp import __main__ as ytdlp_main
        with contextlib.redirect_stdout(stdout_buffer), contextlib.redirect_stderr(stderr_buffer):
            try:
                ytdlp_main.main(argv)
                exit_code = 0
            except SystemExit as e:
                code = e.code
                exit_code = code if isinstance(code, int) else 1
    except Exception as e:
        return json.dumps({
            "exitCode": 1,
            "stdout": stdout_buffer.getvalue(),
            "stderr": stderr_buffer.getvalue() + f"\n{e}",
        })

    return json.dumps({
        "exitCode": exit_code,
        "stdout": stdout_buffer.getvalue(),
        "stderr": stderr_buffer.getvalue(),
    })
