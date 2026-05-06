"""
Caché SQLite offline para búsquedas, metadata y letras.
Evita re-llamadas innecesarias a APIs y re-descargas.
"""

import sqlite3
import json
import hashlib
import time
from pathlib import Path
from contextlib import contextmanager
from typing import Any

from ytmusicdl.utils.paths import cache_dir
from ytmusicdl.utils.config import get as cfg_get

DB_PATH = cache_dir() / "cache.db"


@contextmanager
def _connect():
    con = sqlite3.connect(DB_PATH, timeout=10)
    con.row_factory = sqlite3.Row
    try:
        yield con
        con.commit()
    finally:
        con.close()


def init() -> None:
    with _connect() as con:
        con.executescript("""
            CREATE TABLE IF NOT EXISTS search_cache (
                key     TEXT PRIMARY KEY,
                data    TEXT NOT NULL,
                created INTEGER NOT NULL
            );
            CREATE TABLE IF NOT EXISTS metadata_cache (
                video_id TEXT PRIMARY KEY,
                data     TEXT NOT NULL,
                created  INTEGER NOT NULL
            );
            CREATE TABLE IF NOT EXISTS lyrics_cache (
                key     TEXT PRIMARY KEY,
                lrc     TEXT,
                plain   TEXT,
                source  TEXT,
                created INTEGER NOT NULL
            );
            CREATE TABLE IF NOT EXISTS downloads (
                video_id   TEXT PRIMARY KEY,
                filepath   TEXT NOT NULL,
                downloaded INTEGER NOT NULL,
                fmt        TEXT
            );
        """)


def _key(text: str) -> str:
    return hashlib.md5(text.lower().strip().encode()).hexdigest()


def _enabled() -> bool:
    return cfg_get("cache", "enabled", True)


# ── Búsquedas ────────────────────────────────────────────────────────────────

def get_search(query: str) -> list[dict] | None:
    if not _enabled():
        return None
    ttl = cfg_get("cache", "ttl_search", 86400)
    with _connect() as con:
        row = con.execute(
            "SELECT data, created FROM search_cache WHERE key=?", (_key(query),)
        ).fetchone()
    if row and (time.time() - row["created"]) < ttl:
        return json.loads(row["data"])
    return None


def set_search(query: str, results: list[dict]) -> None:
    if not _enabled():
        return
    with _connect() as con:
        con.execute(
            "INSERT OR REPLACE INTO search_cache VALUES (?,?,?)",
            (_key(query), json.dumps(results, ensure_ascii=False), int(time.time()))
        )


# ── Metadata ─────────────────────────────────────────────────────────────────

def get_metadata(video_id: str) -> dict | None:
    if not _enabled():
        return None
    ttl = cfg_get("cache", "ttl_metadata", 604800)
    with _connect() as con:
        row = con.execute(
            "SELECT data, created FROM metadata_cache WHERE video_id=?", (video_id,)
        ).fetchone()
    if row and (time.time() - row["created"]) < ttl:
        return json.loads(row["data"])
    return None


def set_metadata(video_id: str, data: dict) -> None:
    if not _enabled():
        return
    with _connect() as con:
        con.execute(
            "INSERT OR REPLACE INTO metadata_cache VALUES (?,?,?)",
            (video_id, json.dumps(data, ensure_ascii=False), int(time.time()))
        )


# ── Letras ────────────────────────────────────────────────────────────────────

def get_lyrics(title: str, artist: str) -> dict | None:
    if not _enabled():
        return None
    ttl = cfg_get("cache", "ttl_lyrics", 2592000)
    key = _key(f"{title} {artist}")
    with _connect() as con:
        row = con.execute(
            "SELECT lrc, plain, source, created FROM lyrics_cache WHERE key=?", (key,)
        ).fetchone()
    if row and (time.time() - row["created"]) < ttl:
        return {"lrc": row["lrc"], "plain": row["plain"], "source": row["source"]}
    return None


def set_lyrics(title: str, artist: str, lyrics: dict) -> None:
    if not _enabled():
        return
    key = _key(f"{title} {artist}")
    with _connect() as con:
        con.execute(
            "INSERT OR REPLACE INTO lyrics_cache VALUES (?,?,?,?,?)",
            (key, lyrics.get("lrc"), lyrics.get("plain"), lyrics.get("source"), int(time.time()))
        )


# ── Descargas ─────────────────────────────────────────────────────────────────

def mark_downloaded(video_id: str, filepath: str, fmt: str) -> None:
    with _connect() as con:
        con.execute(
            "INSERT OR REPLACE INTO downloads VALUES (?,?,?,?)",
            (video_id, filepath, int(time.time()), fmt)
        )


def is_downloaded(video_id: str) -> str | None:
    """Retorna el filepath si ya fue descargado y el archivo existe."""
    with _connect() as con:
        row = con.execute(
            "SELECT filepath FROM downloads WHERE video_id=?", (video_id,)
        ).fetchone()
    if row and Path(row["filepath"]).exists():
        return row["filepath"]
    return None


# ── Estadísticas ──────────────────────────────────────────────────────────────

def stats() -> dict:
    with _connect() as con:
        searches  = con.execute("SELECT COUNT(*) FROM search_cache").fetchone()[0]
        metadata  = con.execute("SELECT COUNT(*) FROM metadata_cache").fetchone()[0]
        lyrics    = con.execute("SELECT COUNT(*) FROM lyrics_cache").fetchone()[0]
        downloads = con.execute("SELECT COUNT(*) FROM downloads").fetchone()[0]
    size_bytes = DB_PATH.stat().st_size if DB_PATH.exists() else 0
    return {
        "searches":  searches,
        "metadata":  metadata,
        "lyrics":    lyrics,
        "downloads": downloads,
        "db_size_kb": round(size_bytes / 1024, 1),
    }


def clear(older_than_days: int = 0) -> dict:
    """Limpia entradas del caché. Si older_than_days=0, limpia todo."""
    cutoff = int(time.time()) - (older_than_days * 86400) if older_than_days else int(time.time()) + 1
    deleted = {}
    with _connect() as con:
        for table in ("search_cache", "metadata_cache", "lyrics_cache"):
            cur = con.execute(f"DELETE FROM {table} WHERE created < ?", (cutoff,))
            deleted[table] = cur.rowcount
    return deleted
