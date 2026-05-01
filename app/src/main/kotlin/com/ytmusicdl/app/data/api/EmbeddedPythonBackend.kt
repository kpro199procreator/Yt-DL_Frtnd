package com.ytmusicdl.app.data.api

import com.ytmusicdl.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class EmbeddedPythonBackend : ExtractorBackend {
    override val name: String = "EmbeddedPython"

    override fun isAvailable(): Boolean = PythonBridge.isReady()

    override suspend fun searchSongs(query: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext emptyList()
        runCatching {
            val json = PythonBridge.call("yt_backend", "search_tracks", query, limit).toString()
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    add(trackFromJson(arr.getJSONObject(i)))
                }
            }
        }.getOrElse { emptyList() }
    }

    override suspend fun extractAudioUrl(videoId: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext null
        runCatching {
            val json = PythonBridge.call("yt_backend", "extract_audio", videoId).toString()
            val obj = JSONObject(json)
            val url = obj.optString("url")
            if (url.isBlank()) null else Pair(url, obj.optString("ext", "m4a"))
        }.getOrNull()
    }

    override suspend fun getTrackInfo(videoIdOrQuery: String): Track? = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext null
        runCatching {
            val json = PythonBridge.call("yt_backend", "get_music_metadata", videoIdOrQuery).toString()
            trackFromJson(JSONObject(json))
        }.getOrNull()
    }

    override suspend fun getLyrics(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext null
        runCatching {
            val json = PythonBridge.call("yt_backend", "get_lyrics", title, artist).toString()
            JSONObject(json).optString("lyrics").ifBlank { null }
        }.getOrNull()
    }

    private fun trackFromJson(obj: JSONObject): Track = Track(
        videoId = obj.optString("videoId"),
        title = obj.optString("title"),
        artist = obj.optString("artist"),
        album = obj.optString("album"),
        year = obj.optString("year"),
        coverUrl = obj.optString("coverUrl"),
        duration = obj.optString("duration"),
    )
}
