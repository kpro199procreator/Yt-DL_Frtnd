import json
from typing import Any, Dict, List

import requests
from ytmusicapi import YTMusic
from yt_dlp import YoutubeDL


def _client() -> YTMusic:
    return YTMusic()


def search_tracks(query: str, limit: int = 8) -> str:
    ytm = _client()
    items = ytm.search(query, filter="songs", limit=limit)
    tracks: List[Dict[str, Any]] = []
    for item in items:
        artists = item.get("artists") or []
        artist = artists[0].get("name", "") if artists else ""
        thumbnails = item.get("thumbnails") or []
        tracks.append({
            "videoId": item.get("videoId", ""),
            "title": item.get("title", ""),
            "artist": artist,
            "album": (item.get("album") or {}).get("name", ""),
            "duration": item.get("duration", ""),
            "coverUrl": thumbnails[-1].get("url", "") if thumbnails else "",
        })
    return json.dumps(tracks, ensure_ascii=False)


def extract_audio(video_id: str) -> str:
    url = f"https://www.youtube.com/watch?v={video_id}"
    opts = {"quiet": True, "no_warnings": True, "extract_flat": False}
    with YoutubeDL(opts) as ydl:
        info = ydl.extract_info(url, download=False)
        formats = info.get("formats", [])
        audio_formats = [f for f in formats if f.get("acodec") != "none" and f.get("vcodec") == "none"]
        best = max(audio_formats, key=lambda f: f.get("abr") or 0, default={})
        return json.dumps({"url": best.get("url", ""), "ext": best.get("ext", "m4a")}, ensure_ascii=False)


def get_music_metadata(video_id_or_query: str) -> str:
    ytm = _client()
    vid = video_id_or_query
    if " " in video_id_or_query or len(video_id_or_query) < 8:
        results = ytm.search(video_id_or_query, filter="songs", limit=1)
        vid = results[0].get("videoId", "") if results else ""
    song = ytm.get_song(vid) if vid else {}
    details = song.get("videoDetails", {})
    thumbs = details.get("thumbnail", {}).get("thumbnails", [])
    authors = details.get("author", "")
    return json.dumps({
        "videoId": details.get("videoId", vid),
        "title": details.get("title", ""),
        "artist": authors,
        "album": "",
        "year": "",
        "coverUrl": thumbs[-1].get("url", "") if thumbs else "",
        "duration": details.get("lengthSeconds", ""),
    }, ensure_ascii=False)


def get_lyrics(title: str, artist: str) -> str:
    query = f"{title} {artist}".strip()
    try:
        resp = requests.get("https://lrclib.net/api/search", params={"q": query}, timeout=10)
        resp.raise_for_status()
        data = resp.json() or []
        if data:
            lyrics = data[0].get("syncedLyrics") or data[0].get("plainLyrics") or ""
            return json.dumps({"lyrics": lyrics}, ensure_ascii=False)
    except Exception:
        pass
    return json.dumps({"lyrics": ""}, ensure_ascii=False)
