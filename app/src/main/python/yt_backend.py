import json
from yt_dlp import YoutubeDL
from ytmusicapi import YTMusic
import requests

_ytmusic = YTMusic()


CONTAINER_PRIORITY = {
    "m4a": 3,
    "mp4": 3,
    "aac": 2,
    "webm": 1,
    "opus": 1,
}


BASE_YDL_OPTS = {
    "quiet": True,
    "skip_download": True,
    "noplaylist": True,
    "extractor_retries": 3,
    "retries": 5,
    "fragment_retries": 5,
    "extractor_args": {
        "youtube": {
            "player_client": ["android_vr", "android_tv", "android", "web", "ios"],
        }
    },
}


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


def _score_audio_format(fmt: dict) -> tuple:
    abr = int(fmt.get("abr") or 0)
    ext = (fmt.get("ext") or "").lower()
    container_score = CONTAINER_PRIORITY.get(ext, 0)
    has_size = 1 if (fmt.get("filesize") or fmt.get("filesize_approx")) else 0
    protocol = (fmt.get("protocol") or "").lower()
    protocol_score = 2 if protocol in {"https", "http", "m3u8_native"} else 1 if protocol else 0
    return abr, container_score, has_size, protocol_score


def _extract_info_with_fallback(url: str):
    # Prioriza comportamiento estilo: yt-dlp -f 140 <url>
    format_attempts = [
        "140",
        "251",
        "140/bestaudio[ext=m4a]/bestaudio[ext=webm]/bestaudio/best",
        "bestaudio[ext=m4a]/bestaudio[ext=webm]/bestaudio/best",
    ]
    last_error = None
    for fmt in format_attempts:
        opts = dict(BASE_YDL_OPTS)
        opts["format"] = fmt
        try:
            with YoutubeDL(opts) as ydl:
                info = ydl.extract_info(url, download=False)
            if info:
                return info, fmt
        except Exception as e:
            last_error = str(e)
    raise RuntimeError(last_error or "No se pudo extraer información del video")


def list_audio_formats(video_id):
    url = f"https://music.youtube.com/watch?v={video_id}"
    info, used_fmt = _extract_info_with_fallback(url)

    available = []
    for fmt in info.get("formats") or []:
        if fmt.get("acodec") in (None, "none"):
            continue
        audio_url = fmt.get("url")
        if not audio_url:
            continue
        normalized = {
            "format_id": str(fmt.get("format_id") or ""),
            "ext": (fmt.get("ext") or "").lower(),
            "acodec": fmt.get("acodec") or "",
            "abr": int(fmt.get("abr") or 0),
            "asr": int(fmt.get("asr") or 0),
            "filesize": int(fmt.get("filesize") or fmt.get("filesize_approx") or 0),
            "protocol": fmt.get("protocol") or "",
            "format_note": fmt.get("format_note") or "",
            "url": audio_url,
        }
        normalized["_score"] = _score_audio_format(normalized)
        available.append(normalized)

    if not available:
        direct_url = info.get("url")
        if direct_url:
            selected = {
                "format_id": str(info.get("format_id") or ""),
                "ext": (info.get("ext") or "m4a").lower(),
                "acodec": info.get("acodec") or "",
                "abr": int(info.get("abr") or 0),
                "asr": int(info.get("asr") or 0),
                "filesize": int(info.get("filesize") or info.get("filesize_approx") or 0),
                "protocol": info.get("protocol") or "",
                "format_note": info.get("format_note") or "",
                "url": direct_url,
            }
            return {
                "formats": [selected],
                "selected": selected,
                "reason": f"Used direct extracted format with strategy '{used_fmt}'",
                "info": info,
            }
        return {
            "formats": [],
            "selected": None,
            "reason": "No audio formats with URL were found",
            "info": info,
        }

    available.sort(key=lambda x: x["_score"], reverse=True)
    selected = dict(available[0])
    for f in available:
        f.pop("_score", None)
    selected.pop("_score", None)

    reason = (
        f"Selected format_id={selected.get('format_id')} ext={selected.get('ext')} "
        f"abr={selected.get('abr')}kbps using strategy '{used_fmt}' and score "
        "(abr > container compatibility > filesize/protocol)"
    )

    return {
        "formats": available,
        "selected": selected,
        "reason": reason,
        "info": info,
    }


def search_tracks(query, limit=8):
    results = _ytmusic.search(query, filter="songs", limit=int(limit or 8))
    tracks = [_track_from_search_item(x) for x in results if x.get("videoId")]
    return json.dumps(tracks)


def get_audio_formats(video_id):
    decision = list_audio_formats(video_id)
    payload = {
        "formats": decision.get("formats", []),
        "selected": decision.get("selected"),
        "reason": decision.get("reason", ""),
    }
    return json.dumps(payload)


def extract_audio(video_id, preferred_format_id="140"):
    decision = list_audio_formats(video_id)
    selected = decision.get("selected") or {}
    info = decision.get("info") or {}

    if preferred_format_id:
        pref = str(preferred_format_id)
        preferred = next((f for f in decision.get("formats", []) if f.get("format_id") == pref), None)
        if not preferred and pref == "140":
            preferred = next((f for f in decision.get("formats", []) if str(f.get("format_id", "")).startswith("140")), None)
        if preferred:
            selected = preferred
            decision["reason"] = f"Preferred format matched: {selected.get('format_id')}"
        else:
            decision["reason"] = f"Preferred format {pref} unavailable, fallback to best scored format"

    return json.dumps({
        "audioUrl": selected.get("url", ""),
        "containerExt": selected.get("ext", "m4a"),
        "bitrate": int(selected.get("abr") or 0),
        "artist": info.get("uploader", ""),
        "title": info.get("title", ""),
        "coverUrl": info.get("thumbnail", ""),
        "selectedFormatId": selected.get("format_id", ""),
        "selectedAudioCodec": selected.get("acodec", ""),
        "selectedSampleRate": int(selected.get("asr") or 0),
        "selectedProtocol": selected.get("protocol", ""),
        "selectedFormatNote": selected.get("format_note", ""),
        "selectedFileSize": int(selected.get("filesize") or 0),
        "selectionReason": decision.get("reason", ""),
        "availableFormats": decision.get("formats", []),
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
