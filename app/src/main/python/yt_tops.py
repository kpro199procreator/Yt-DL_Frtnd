import json
from ytmusicapi import YTMusic
from yt_common import track_from_search_item

_ytmusic = YTMusic()

def _search_seed(seed: str, limit: int):
    rows = _ytmusic.search(seed, filter="songs", limit=limit)
    return [track_from_search_item(x) for x in rows if x.get("videoId")]

def get_top_world(limit=10):
    return json.dumps(_search_seed("Top songs worldwide", int(limit or 10)))

def get_top_region(region="US", limit=20):
    return json.dumps(_search_seed(f"Top songs {region}", int(limit or 20)))
