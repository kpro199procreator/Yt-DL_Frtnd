"""
Wrapper de ytmusicapi para búsqueda en YouTube Music.
Soporta canciones, álbumes y playlists sin autenticación.
"""

from __future__ import annotations
from typing import Optional

try:
    from ytmusicapi import YTMusic
    _YTM = YTMusic()
    _AVAILABLE = True
except Exception:
    _YTM = None
    _AVAILABLE = False


def available() -> bool:
    return _AVAILABLE


def _thumb(thumbnails: list[dict]) -> str:
    """Retorna la URL de la miniatura de mayor resolución."""
    if not thumbnails:
        return ""
    return thumbnails[-1].get("url", "")


def search_songs(query: str, limit: int = 8) -> list[dict]:
    if not _AVAILABLE:
        return []
    try:
        results = _YTM.search(query, filter="songs", limit=limit)
    except Exception:
        results = []

    tracks = []
    for r in results:
        video_id = r.get("videoId")
        if not video_id:
            continue
        tracks.append({
            "title":     r.get("title", ""),
            "artist":    r["artists"][0]["name"] if r.get("artists") else "Unknown",
            "album":     r.get("album", {}).get("name", "") if r.get("album") else "",
            "video_id":  video_id,
            "url":       f"https://music.youtube.com/watch?v={video_id}",
            "duration":  r.get("duration", ""),
            "cover_url": _thumb(r.get("thumbnails", [])),
            "year":      r.get("year", ""),
        })
    return tracks


def search_albums(query: str, limit: int = 5) -> list[dict]:
    if not _AVAILABLE:
        return []
    try:
        results = _YTM.search(query, filter="albums", limit=limit)
    except Exception:
        results = []

    albums = []
    for r in results:
        browse_id = r.get("browseId")
        if not browse_id:
            continue
        albums.append({
            "title":     r.get("title", ""),
            "artist":    r["artists"][0]["name"] if r.get("artists") else "Unknown",
            "year":      r.get("year", ""),
            "browse_id": browse_id,
            "cover_url": _thumb(r.get("thumbnails", [])),
        })
    return albums


def get_album_tracks(browse_id: str) -> list[dict]:
    if not _AVAILABLE:
        return []
    try:
        album = _YTM.get_album(browse_id)
    except Exception:
        return []

    album_title  = album.get("title", "")
    album_artist = album["artists"][0]["name"] if album.get("artists") else "Unknown"
    album_year   = album.get("year", "")
    cover_url    = _thumb(album.get("thumbnails", []))
    total_tracks = len(album.get("tracks", []))

    tracks = []
    for i, t in enumerate(album.get("tracks", []), 1):
        video_id = t.get("videoId")
        if not video_id:
            continue
        tracks.append({
            "title":        t.get("title", ""),
            "artist":       t["artists"][0]["name"] if t.get("artists") else album_artist,
            "album":        album_title,
            "year":         album_year,
            "track_number": i,
            "total_tracks": total_tracks,
            "video_id":     video_id,
            "url":          f"https://music.youtube.com/watch?v={video_id}",
            "duration":     t.get("duration", ""),
            "cover_url":    cover_url,
        })
    return tracks


def get_playlist_tracks(url_or_id: str) -> list[dict]:
    if not _AVAILABLE:
        return []

    # Extraer ID si viene URL completa
    playlist_id = url_or_id
    if "list=" in url_or_id:
        playlist_id = url_or_id.split("list=")[-1].split("&")[0]

    try:
        playlist = _YTM.get_playlist(playlist_id, limit=None)
    except Exception:
        return []

    tracks = []
    for t in playlist.get("tracks", []):
        video_id = t.get("videoId")
        if not video_id:
            continue
        tracks.append({
            "title":    t.get("title", ""),
            "artist":   t["artists"][0]["name"] if t.get("artists") else "Unknown",
            "album":    t.get("album", {}).get("name", "") if t.get("album") else "",
            "video_id": video_id,
            "url":      f"https://music.youtube.com/watch?v={video_id}",
            "duration": t.get("duration", ""),
            "cover_url": _thumb(t.get("thumbnails", [])),
        })
    return tracks


def get_song_info(video_id: str) -> Optional[dict]:
    """Obtiene info detallada de una canción por video_id."""
    if not _AVAILABLE:
        return None
    try:
        info = _YTM.get_song(video_id)
        details = info.get("videoDetails", {})
        micro = info.get("microformat", {}).get("microformatDataRenderer", {})
        return {
            "title":     details.get("title", ""),
            "artist":    details.get("author", ""),
            "video_id":  video_id,
            "url":       f"https://music.youtube.com/watch?v={video_id}",
            "cover_url": _thumb(details.get("thumbnail", {}).get("thumbnails", [])),
            "duration":  details.get("lengthSeconds", ""),
            "description": micro.get("description", ""),
        }
    except Exception:
        return None
