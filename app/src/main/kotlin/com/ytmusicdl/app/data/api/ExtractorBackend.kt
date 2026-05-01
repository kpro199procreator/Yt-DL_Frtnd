package com.ytmusicdl.app.data.api

import com.ytmusicdl.app.data.model.Track

interface ExtractorBackend {
    val name: String
    fun isAvailable(): Boolean

    suspend fun searchSongs(query: String, limit: Int = 8): List<Track>
    suspend fun extractAudioUrl(videoId: String): Pair<String, String>?
    suspend fun getTrackInfo(videoIdOrQuery: String): Track?
    suspend fun getLyrics(title: String, artist: String): String?
}
