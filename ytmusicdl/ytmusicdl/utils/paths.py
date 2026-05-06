"""
Resolución de paths compatible con Termux (Android) y Linux/macOS.
"""

import os
from pathlib import Path


def is_termux() -> bool:
    return "com.termux" in os.environ.get("PREFIX", "")


def home() -> Path:
    return Path.home()


def config_dir() -> Path:
    p = home() / ".config" / "ytmusicdl"
    p.mkdir(parents=True, exist_ok=True)
    return p


def cache_dir() -> Path:
    p = home() / ".cache" / "ytmusicdl"
    p.mkdir(parents=True, exist_ok=True)
    return p


def default_music_dir() -> Path:
    if is_termux():
        termux_music = home() / "storage" / "music"
        if termux_music.exists():
            return termux_music
    # Fallback estándar
    return home() / "Music"


def expand(path: str) -> Path:
    """Expande ~ y variables de entorno en un path."""
    return Path(os.path.expandvars(os.path.expanduser(path)))
