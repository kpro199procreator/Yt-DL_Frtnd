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


def extract_audio(video_id):
    url = f"https://www.youtube.com/watch?v={video_id}"
    opts = {
        "quiet": True,
        "skip_download": True,
        "format": "bestaudio/best",
        "noplaylist": True,
    }
    with YoutubeDL(opts) as ydl:
        info = ydl.extract_info(url, download=False)

    return json.dumps({
        "audioUrl": info.get("url", ""),
        "containerExt": info.get("ext", "m4a"),
        "bitrate": int(info.get("abr") or 0),
        "artist": info.get("uploader", ""),
        "title": info.get("title", ""),
        "coverUrl": info.get("thumbnail", ""),
    })


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
