import json
from ytmusicapi import YTMusic

_ytmusic = YTMusic()

def _normalize_playlist_id(playlist_id_or_url: str) -> str:
    raw = (playlist_id_or_url or "").strip()
    if "list=" in raw:
        return raw.split("list=", 1)[1].split("&", 1)[0]
    return raw

def _track_from_playlist_item(item: dict) -> dict:
    thumbs = item.get("thumbnails") or []
    artists = item.get("artists") or []
    artist_name = artists[0].get("name", "") if artists else ""
    return {
        "videoId": item.get("videoId", ""), "title": item.get("title", ""), "artist": artist_name,
        "album": (item.get("album") or {}).get("name", ""), "year": str(item.get("year", "") or ""),
        "coverUrl": thumbs[-1]["url"] if thumbs else "", "duration": item.get("duration", ""), "streamUrl": "",
    }

def get_playlist_tracks(playlist_id_or_url, limit=200):
    playlist_id = _normalize_playlist_id(playlist_id_or_url)
    if not playlist_id:
        return json.dumps({"playlist": {}, "tracks": []})
    data = _ytmusic.get_playlist(playlist_id, limit=int(limit or 200))
    tracks, seen = [], set()
    for item in data.get("tracks") or []:
        video_id = item.get("videoId")
        if not video_id or video_id in seen:
            continue
        seen.add(video_id)
        tracks.append(_track_from_playlist_item(item))
    return json.dumps({"playlist": {"id": playlist_id, "title": data.get("title", ""), "author": data.get("author") or "", "trackCount": len(tracks)}, "tracks": tracks})
