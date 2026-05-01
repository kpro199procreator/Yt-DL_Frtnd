package com.ytmusicdl.app.data.api

import com.ytmusicdl.app.data.model.Track

object NewPipeFallbackBackend : ExtractorBackend {
    override suspend fun searchSongs(query: String, limit: Int): List<Track> =
        NewPipeService.searchSongs(query, limit)

    override suspend fun extractAudio(videoId: String, preferredFormatId: String?): AudioExtractionResult? =
        NewPipeService.extractAudio(videoId)?.let {
            AudioExtractionResult(
                audioUrl = it.audioUrl,
                containerExt = it.containerExt,
                bitrate = it.bitrate,
                artist = it.artist,
                title = it.title,
                coverUrl = it.coverUrl,
            )
        }

    override suspend fun getTrackInfo(videoIdOrQuery: String): Track? =
        NewPipeService.getTrackInfo(videoIdOrQuery)

    override suspend fun getLyrics(title: String, artist: String): String? = null
}
