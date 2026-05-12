import json
from ytmusicapi import YTMusic

_ytmusic = YTMusic()

def _normalize(v: str) -> str:
    return (v or "").strip().lower()

def _as_track(item: dict, album_title: str, album_year: str, index: int) -> dict:
    artists = item.get("artists") or []
    artist_name = artists[0].get("name", "") if artists else ""
    thumbs = item.get("thumbnails") or []
    return {
        "videoId": item.get("videoId", ""),
        "title": item.get("title", ""),
        "artist": artist_name,
        "album": album_title,
        "year": album_year,
        "coverUrl": thumbs[-1]["url"] if thumbs else "",
        "duration": item.get("duration", ""),
        "streamUrl": "",
        "trackNumber": index,
    }

def get_album_tracks(album_id_or_name, artist):
    query = f"{artist} {album_id_or_name}".strip()
    albums = _ytmusic.search(query, filter="albums", limit=20)
    target = None
    needle = _normalize(album_id_or_name)
    artist_n = _normalize(artist)
    for a in albums:
        bid = a.get("browseId") or ""
        title = a.get("title") or ""
        a_artists = " ".join([(x.get("name") or "") for x in (a.get("artists") or [])]).lower()
        exact_title = _normalize(title) == needle
        exact_id = _normalize(bid) == needle
        artist_ok = (not artist_n) or (artist_n in a_artists)
        if (exact_id or exact_title) and artist_ok:
            target = a
            break
    if not target and albums:
        target = albums[0]

    if not target:
        return json.dumps({"album": {}, "tracks": [], "exactMatch": False})

    album = _ytmusic.get_album(target.get("browseId"))
    title = album.get("title", "")
    year = str(album.get("year", "") or "")
    tracks = []
    seen = set()
    for idx, t in enumerate(album.get("tracks") or [], start=1):
        vid = t.get("videoId")
        if not vid or vid in seen:
            continue
        seen.add(vid)
        tracks.append(_as_track(t, title, year, idx))

    exact_match = (_normalize(target.get("browseId")) == needle) or (_normalize(title) == needle)
    return json.dumps({
        "album": {"id": target.get("browseId", ""), "title": title, "year": year, "trackCount": len(tracks)},
        "tracks": tracks,
        "exactMatch": exact_match,
    })
