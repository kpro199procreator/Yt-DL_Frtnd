import json
import requests
from ytmusicapi import YTMusic

_ytmusic = YTMusic()


def _best_thumbnail_url(thumbs) -> str:
    if not thumbs:
        return ""
    best = max(thumbs, key=lambda t: (int(t.get("width") or 0) * int(t.get("height") or 0)))
    raw = best.get("url", "")
    return upgrade_thumbnail_url(raw)


def upgrade_thumbnail_url(url: str) -> str:
    raw = (url or "").strip()
    if not raw:
        return ""
    # YouTube/Google thumbnails usually accept larger sizes via =w{W}-h{H}
    if "=w" in raw and "-h" in raw:
        try:
            base = raw.split("=w", 1)[0]
            return f"{base}=w1200-h1200-l90-rj"
        except Exception:
            return raw
    return raw


def track_from_search_item(item: dict) -> dict:
    artists = item.get("artists") or []
    artist_name = artists[0].get("name", "") if artists else ""
    thumbs = item.get("thumbnails") or []
    return {
        "videoId": item.get("videoId", ""),
        "title": item.get("title", ""),
        "artist": artist_name,
        "album": (item.get("album") or {}).get("name", ""),
        "year": str(item.get("year", "") or ""),
        "coverUrl": _best_thumbnail_url(thumbs),
        "duration": item.get("duration", ""),
        "streamUrl": "",
    }


def search_tracks(query, limit=8):
    results = _ytmusic.search(query, filter="songs", limit=int(limit or 8))
    tracks = [track_from_search_item(x) for x in results if x.get("videoId")]
    return json.dumps(tracks)


def search_all(query, limit=24):
    lim = int(limit or 24)
    songs = [track_from_search_item(x) for x in _ytmusic.search(query, filter="songs", limit=lim) if x.get("videoId")]
    albums = [track_from_search_item(x) for x in _ytmusic.search(query, filter="albums", limit=max(8, lim // 2)) if x.get("videoId")]
    playlists = [track_from_search_item(x) for x in _ytmusic.search(query, filter="playlists", limit=max(8, lim // 2)) if x.get("videoId")]
    return json.dumps({"songs": songs, "albums": albums, "playlists": playlists})


def get_music_metadata(video_id_or_query):
    item = _ytmusic.get_song(video_id_or_query).get("videoDetails") if len(video_id_or_query) <= 20 else None
    if not item:
        rs = _ytmusic.search(video_id_or_query, filter="songs", limit=1)
        if not rs:
            return "null"
        return json.dumps(track_from_search_item(rs[0]))

    thumbs = item.get("thumbnail", {}).get("thumbnails") or [{}]
    return json.dumps({
        "videoId": item.get("videoId", ""),
        "title": item.get("title", ""),
        "artist": item.get("author", ""),
        "album": "",
        "year": "",
        "coverUrl": _best_thumbnail_url(thumbs),
        "duration": str(item.get("lengthSeconds", "")),
        "streamUrl": "",
    })


def get_lyrics(title, artist):
    try:
        q = requests.get("https://lrclib.net/api/search", params={"track_name": title, "artist_name": artist}, timeout=8)
        q.raise_for_status()
        data = q.json() or []
        if data:
            return data[0].get("syncedLyrics") or data[0].get("plainLyrics") or ""
    except Exception:
        pass
    return ""
