import json
from yt_dlp import YoutubeDL

from yt_common import search_tracks, get_music_metadata, get_lyrics
from yt_albums import get_album_tracks
from yt_playlists import get_playlist_tracks
from yt_tops import get_top_world, get_top_region

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

def _score_audio_format(fmt: dict) -> tuple:
    abr = int(fmt.get("abr") or 0)
    ext = (fmt.get("ext") or "").lower()
    container_score = CONTAINER_PRIORITY.get(ext, 0)
    has_size = 1 if (fmt.get("filesize") or fmt.get("filesize_approx")) else 0
    protocol = (fmt.get("protocol") or "").lower()
    protocol_score = 2 if protocol in {"https", "http", "m3u8_native"} else 1 if protocol else 0
    return abr, container_score, has_size, protocol_score

def _extract_info_with_fallback(url: str):
    format_attempts = ["140","251","140/bestaudio[ext=m4a]/bestaudio[ext=webm]/bestaudio/best","bestaudio[ext=m4a]/bestaudio[ext=webm]/bestaudio/best"]
    last_error = None
    for fmt in format_attempts:
        opts = dict(BASE_YDL_OPTS); opts["format"] = fmt
        try:
            with YoutubeDL(opts) as ydl:
                info = ydl.extract_info(url, download=False)
            if info: return info, fmt
        except Exception as e:
            last_error = str(e)
    raise RuntimeError(last_error or "No se pudo extraer información del video")

def list_audio_formats(video_id):
    url = f"https://music.youtube.com/watch?v={video_id}"
    info, used_fmt = _extract_info_with_fallback(url)
    available = []
    for fmt in info.get("formats") or []:
        if fmt.get("acodec") in (None, "none"): continue
        audio_url = fmt.get("url")
        if not audio_url: continue
        normalized = {"format_id": str(fmt.get("format_id") or ""),"ext": (fmt.get("ext") or "").lower(),"acodec": fmt.get("acodec") or "","abr": int(fmt.get("abr") or 0),"asr": int(fmt.get("asr") or 0),"filesize": int(fmt.get("filesize") or fmt.get("filesize_approx") or 0),"protocol": fmt.get("protocol") or "","format_note": fmt.get("format_note") or "","url": audio_url}
        normalized["_score"] = _score_audio_format(normalized); available.append(normalized)
    if not available:
        return {"formats": [], "selected": None, "reason": "No audio formats with URL were found", "info": info}
    available.sort(key=lambda x: x["_score"], reverse=True)
    selected = dict(available[0])
    for f in available: f.pop("_score", None)
    selected.pop("_score", None)
    return {"formats": available,"selected": selected,"reason": f"Selected format_id={selected.get('format_id')} using strategy '{used_fmt}'","info": info}

def get_audio_formats(video_id):
    decision = list_audio_formats(video_id)
    return json.dumps({"formats": decision.get("formats", []),"selected": decision.get("selected"),"reason": decision.get("reason", "")})

def extract_audio(video_id, preferred_format_id="140"):
    decision = list_audio_formats(video_id); selected = decision.get("selected") or {}; info = decision.get("info") or {}
    if preferred_format_id:
        pref = str(preferred_format_id)
        preferred = next((f for f in decision.get("formats", []) if f.get("format_id") == pref), None)
        if preferred: selected = preferred
    return json.dumps({"audioUrl": selected.get("url", ""),"containerExt": selected.get("ext", "m4a"),"bitrate": int(selected.get("abr") or 0),"artist": info.get("uploader", ""),"title": info.get("title", ""),"coverUrl": info.get("thumbnail", ""),"selectedFormatId": selected.get("format_id", ""),"selectedAudioCodec": selected.get("acodec", ""),"selectedSampleRate": int(selected.get("asr") or 0),"selectedProtocol": selected.get("protocol", ""),"selectedFormatNote": selected.get("format_note", ""),"selectedFileSize": int(selected.get("filesize") or 0),"selectionReason": decision.get("reason", ""),"availableFormats": decision.get("formats", [])})
