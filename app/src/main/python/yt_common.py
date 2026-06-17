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
        "trackNumber": int(item.get("trackNumber") or 1),
    }



def _run_search(query, filter_name, limit):
    try:
        return _ytmusic.search(query, filter=filter_name, limit=int(limit or 8)) or []
    except Exception as exc:
        return [{"_error": str(exc)}]

def album_from_search_item(item: dict) -> dict:
    artists = item.get("artists") or []
    artist_name = artists[0].get("name", "") if artists else ""
    thumbs = item.get("thumbnails") or []
    return {
        "videoId": item.get("browseId", ""),
        "title": item.get("title", ""),
        "artist": artist_name,
        "album": item.get("title", ""),
        "year": str(item.get("year", "") or ""),
        "coverUrl": _best_thumbnail_url(thumbs),
        "duration": item.get("type", "Album"),
        "streamUrl": "",
        "trackNumber": 0,
    }

def playlist_from_search_item(item: dict) -> dict:
    thumbs = item.get("thumbnails") or []
    playlist_id = item.get("playlistId") or item.get("browseId") or item.get("videoId") or ""
    author = item.get("author") or item.get("itemCount") or ""
    return {
        "videoId": playlist_id,
        "title": item.get("title", ""),
        "artist": str(author or ""),
        "album": "Playlist",
        "year": "",
        "coverUrl": _best_thumbnail_url(thumbs),
        "duration": item.get("itemCount", ""),
        "streamUrl": "",
        "trackNumber": 0,
    }

def _first_error(items):
    err = next((x.get("_error") for x in items if isinstance(x, dict) and x.get("_error")), "")
    return err or ""

def search_tracks(query, limit=8):
    results = _run_search(query, "songs", limit)
    tracks = [track_from_search_item(x) for x in results if x.get("videoId")]
    return json.dumps(tracks)

def search_albums(query, limit=8):
    results = _run_search(query, "albums", limit)
    albums = [album_from_search_item(x) for x in results if x.get("browseId")]
    return json.dumps(albums)

def search_playlists(query, limit=8):
    results = _run_search(query, "playlists", limit)
    playlists = [playlist_from_search_item(x) for x in results if (x.get("playlistId") or x.get("browseId") or x.get("videoId"))]
    return json.dumps(playlists)

def search_all(query, limit=24):
    lim = int(limit or 24)
    song_items = _run_search(query, "songs", lim)
    album_items = _run_search(query, "albums", max(8, lim // 2))
    playlist_items = _run_search(query, "playlists", max(8, lim // 2))
    songs = [track_from_search_item(x) for x in song_items if x.get("videoId")]
    albums = [album_from_search_item(x) for x in album_items if x.get("browseId")]
    playlists = [playlist_from_search_item(x) for x in playlist_items if (x.get("playlistId") or x.get("browseId") or x.get("videoId"))]
    errors = [e for e in (_first_error(song_items), _first_error(album_items), _first_error(playlist_items)) if e]
    return json.dumps({"songs": songs, "albums": albums, "playlists": playlists, "errors": errors})


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
        "trackNumber": int(item.get("trackNumber") or 1),
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
