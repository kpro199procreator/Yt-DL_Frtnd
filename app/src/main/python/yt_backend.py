import json
from yt_dlp import YoutubeDL
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
    }
    with YoutubeDL(opts) as ydl:
        info = ydl.extract_info(url, download=False)

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
    }
    with YoutubeDL(opts) as ydl:
        info = ydl.extract_info(url, download=False)

    formats = info.get("formats") or []
    audio_formats = []
    for fmt in formats:
        if fmt.get("vcodec") != "none":
            continue
        audio_formats.append({
            "format_id": str(fmt.get("format_id") or ""),
            "ext": fmt.get("ext", ""),
            "abr": int(fmt.get("abr") or 0),
            "acodec": fmt.get("acodec", ""),
            "protocol": fmt.get("protocol", ""),
            "note": fmt.get("format_note", "") or fmt.get("format", ""),
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
