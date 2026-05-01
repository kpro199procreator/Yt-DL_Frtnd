package com.ytmusicdl.app.data.api

import com.ytmusicdl.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EmbeddedPythonBackend : ExtractorBackend {
    override suspend fun searchSongs(query: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val json = PythonBridge.call("search_tracks", query, limit)
        PythonBridge.parseTrackList(json)
    }

    override suspend fun extractAudio(videoId: String, preferredFormatId: String?): AudioExtractionResult? = withContext(Dispatchers.IO) {
        val json = PythonBridge.call("extract_audio", videoId, preferredFormatId)
        PythonBridge.parseAudioResult(json)
    }

    override suspend fun getTrackInfo(videoIdOrQuery: String): Track? = withContext(Dispatchers.IO) {
        val json = PythonBridge.call("get_music_metadata", videoIdOrQuery)
        PythonBridge.parseTrack(json)
    }

    override suspend fun getLyrics(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        val result = PythonBridge.call("get_lyrics", title, artist)
        result.takeIf { it.isNotBlank() && it != "null" }
    }
}

object ExtractorBackendProvider {
    val backend: ExtractorBackend
        get() = if (PythonBridge.isAvailable()) EmbeddedPythonBackend else NewPipeFallbackBackend
}
