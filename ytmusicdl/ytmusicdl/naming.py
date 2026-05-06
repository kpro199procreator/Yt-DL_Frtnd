"""
Sistema de plantillas para nombres de archivo de salida.
Inspirado en spotdl: {title}, {artist}, {album}, {year}, {track}, {playlist}, etc.

Variables disponibles:
  {title}         Título de la canción
  {artist}        Artista principal
  {album}         Nombre del álbum
  {year}          Año de lanzamiento
  {track}         Número de pista (ej: 03)
  {track_number}  Número de pista sin padding (ej: 3)
  {total_tracks}  Total de pistas del álbum
  {playlist}      Nombre de la playlist (si aplica)
  {video_id}      ID de YouTube Music
  {ext}           Extensión del archivo (sin punto)
"""
from __future__ import annotations


import re
from pathlib import Path
from ytmusicdl.utils.config import get as cfg_get

# Plantillas predefinidas (igual que spotdl)
PRESETS = {
    "default":          "{artist} - {title}",
    "title-only":       "{title}",
    "artist-title":     "{artist} - {title}",
    "album-track":      "{album}/{track} - {title}",
    "artist-album":     "{artist}/{album}/{track} - {title}",
    "full":             "{artist}/{album}/{track} - {artist} - {title}",
    "playlist":         "{playlist}/{artist} - {title}",
    "no-folder":        "{artist} - {album} - {track} - {title}",
}

# Caracteres inválidos en nombres de archivo (Windows + Android safe)
_INVALID = re.compile(r'[\\/:*?"<>|]')
_MULTI_SPACE = re.compile(r'\s{2,}')


def _sanitize(text: str) -> str:
    """Elimina caracteres inválidos y normaliza espacios."""
    text = _INVALID.sub("_", text or "")
    text = _MULTI_SPACE.sub(" ", text)
    return text.strip(" .")  # Sin espacios ni puntos al inicio/fin


def _pad_track(n) -> str:
    """Devuelve número de pista con zero-padding (01, 02 ... 99)."""
    try:
        return f"{int(n):02d}"
    except (TypeError, ValueError):
        return "00"


def resolve(track: dict, template: str | None = None, ext: str | None = None) -> str:
    """
    Aplica la plantilla al track y devuelve el nombre de archivo resultante
    (sin extensión si ext=None, con extensión si ext se pasa).

    Ejemplo:
        resolve(track, "{artist}/{album}/{track} - {title}", "m4a")
        → "Rick Astley/Whenever You Need Somebody/01 - Never Gonna Give You Up.m4a"
    """
    if template is None:
        # Intentar preset por nombre, luego tratar como plantilla literal
        raw = cfg_get("download", "output_template", "default")
        template = PRESETS.get(raw, raw)

    variables = {
        "title":        _sanitize(track.get("title", "Unknown")),
        "artist":       _sanitize(track.get("artist", "Unknown Artist")),
        "album":        _sanitize(track.get("album", "Unknown Album")),
        "year":         _sanitize(str(track.get("year", ""))),
        "track":        _pad_track(track.get("track_number")),
        "track_number": str(track.get("track_number", 0)),
        "total_tracks": str(track.get("total_tracks", 0)),
        "playlist":     _sanitize(track.get("playlist", "Playlist")),
        "video_id":     track.get("video_id", ""),
        "ext":          ext or "",
    }

    # Reemplazar variables en la plantilla
    result = template
    for key, value in variables.items():
        result = result.replace(f"{{{key}}}", value)

    # Si quedó alguna variable sin reemplazar, eliminarla
    result = re.sub(r"\{[^}]+\}", "", result)

    # Agregar extensión
    if ext and not result.endswith(f".{ext}"):
        result = f"{result}.{ext}"

    return result


def build_path(output_dir: Path, track: dict, fmt: str, template: str | None = None) -> Path:
    """
    Construye el Path completo de salida para un track.
    Crea subdirectorios intermedios si la plantilla los incluye (ej: artist/album/).
    """
    relative = resolve(track, template, ext=fmt)
    full = output_dir / relative
    full.parent.mkdir(parents=True, exist_ok=True)
    return full


def list_presets() -> dict[str, str]:
    """Retorna todos los presets disponibles."""
    return PRESETS.copy()
