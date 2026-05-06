"""
Gestión de configuración en ~/.config/ytmusicdl/config.toml
Usa tomllib (stdlib Python 3.11+) con fallback a tomli.
"""

import sys
from pathlib import Path
from typing import Any

from ytmusicdl.utils.paths import config_dir, default_music_dir

try:
    import tomllib
except ImportError:
    try:
        import tomli as tomllib  # type: ignore
    except ImportError:
        tomllib = None  # type: ignore

# Intentar tomli_w para escritura; si no, escribir manualmente
try:
    import tomli_w  # type: ignore
    _HAS_TOMLI_W = True
except ImportError:
    _HAS_TOMLI_W = False


DEFAULTS: dict[str, Any] = {
    "download": {
        "format": "m4a",
        "quality": "best",
        "output_dir": str(default_music_dir()),
        "concurrent": 3,
        "skip_downloaded": True,
    },
    "metadata": {
        "embed_lyrics": True,
        "embed_cover": True,
        "save_lrc": True,
        "cover_size": 500,
        "providers": ["ytmusic", "musicbrainz", "lastfm"],
    },
    "lyrics": {
        "synced": True,
        "providers": ["lrclib", "musixmatch", "genius"],
        "fallback_plain": True,
    },
    "cache": {
        "enabled": True,
        "ttl_search": 86400,
        "ttl_metadata": 604800,
        "ttl_lyrics": 2592000,
    },
    "display": {
        "show_cover": True,
        "cover_width": 44,
    },
    "apis": {
        "acoustid_key": "",
        "lastfm_key": "",
    },
}

_CONFIG_PATH = config_dir() / "config.toml"
_loaded: dict[str, Any] = {}


def _deep_merge(base: dict, override: dict) -> dict:
    result = base.copy()
    for k, v in override.items():
        if isinstance(v, dict) and isinstance(result.get(k), dict):
            result[k] = _deep_merge(result[k], v)
        else:
            result[k] = v
    return result


def load() -> dict[str, Any]:
    global _loaded
    if _loaded:
        return _loaded

    data = DEFAULTS.copy()

    if _CONFIG_PATH.exists() and tomllib is not None:
        try:
            with open(_CONFIG_PATH, "rb") as f:
                user = tomllib.load(f)
            data = _deep_merge(data, user)
        except Exception:
            pass  # Si el config está roto, usar defaults

    _loaded = data
    return _loaded


def get(section: str, key: str, fallback: Any = None) -> Any:
    cfg = load()
    return cfg.get(section, {}).get(key, fallback)


def set_value(section: str, key: str, value: Any) -> None:
    """Actualiza un valor y lo persiste en config.toml."""
    global _loaded
    cfg = load()
    if section not in cfg:
        cfg[section] = {}
    cfg[section][key] = value
    _loaded = cfg
    _save(cfg)


def _toml_value(v: Any) -> str:
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, str):
        return f'"{v}"'
    if isinstance(v, list):
        items = ", ".join(f'"{i}"' if isinstance(i, str) else str(i) for i in v)
        return f"[{items}]"
    return str(v)


def _save(cfg: dict) -> None:
    if _HAS_TOMLI_W:
        with open(_CONFIG_PATH, "wb") as f:
            tomli_w.dump(cfg, f)
        return

    # Escritura manual simple si tomli_w no está disponible
    lines = []
    for section, values in cfg.items():
        lines.append(f"\n[{section}]")
        for k, v in values.items():
            lines.append(f"{k} = {_toml_value(v)}")
    _CONFIG_PATH.write_text("\n".join(lines).strip() + "\n", encoding="utf-8")


def ensure_config_exists() -> None:
    if not _CONFIG_PATH.exists():
        _save(DEFAULTS)
