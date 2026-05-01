package com.ytmusicdl.app.data.api

import com.ytmusicdl.app.data.model.Track

object NewPipeBackend : ExtractorBackend {
    override val name: String = "NewPipeFallback"

    override fun isAvailable(): Boolean = true

    override suspend fun searchSongs(query: String, limit: Int): List<Track> =
        NewPipeService.searchSongs(query, limit)

    override suspend fun extractAudioUrl(videoId: String): Pair<String, String>? =
        NewPipeService.extractAudioUrl(videoId)

    override suspend fun getTrackInfo(videoIdOrQuery: String): Track? =
        NewPipeService.getTrackInfo(videoIdOrQuery)

    override suspend fun getLyrics(title: String, artist: String): String? = null
}
