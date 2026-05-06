"""
Búsqueda unificada en YT Music con caché SQLite.
"""

from ytmusicdl import cache
from ytmusicdl.apis import ytmusic


def songs(query: str, limit: int = 8) -> list[dict]:
    """Busca canciones. Devuelve resultados cacheados si están disponibles."""
    cached = cache.get_search(f"song:{query}")
    if cached is not None:
        return cached

    results = ytmusic.search_songs(query, limit=limit)
    if results:
        cache.set_search(f"song:{query}", results)
    return results


def albums(query: str, limit: int = 5) -> list[dict]:
    cached = cache.get_search(f"album:{query}")
    if cached is not None:
        return cached

    results = ytmusic.search_albums(query, limit=limit)
    if results:
        cache.set_search(f"album:{query}", results)
    return results


def album_tracks(browse_id: str) -> list[dict]:
    cached = cache.get_search(f"album_tracks:{browse_id}")
    if cached is not None:
        return cached

    tracks = ytmusic.get_album_tracks(browse_id)
    if tracks:
        cache.set_search(f"album_tracks:{browse_id}", tracks)
    return tracks


def playlist_tracks(url_or_id: str) -> list[dict]:
    # No cacheamos playlists — pueden cambiar frecuentemente
    return ytmusic.get_playlist_tracks(url_or_id)
