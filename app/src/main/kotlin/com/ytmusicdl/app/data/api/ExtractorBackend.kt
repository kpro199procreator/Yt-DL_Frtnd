package com.ytmusicdl.app.data.api

import com.ytmusicdl.app.data.model.AudioFormatOption
import com.ytmusicdl.app.data.model.Track

interface ExtractorBackend {
    suspend fun searchSongs(query: String, limit: Int = 8): List<Track>
    suspend fun extractAudio(videoId: String, preferredFormatId: String? = null): AudioExtractionResult?
    suspend fun getTrackInfo(videoIdOrQuery: String): Track?
    suspend fun listAudioFormats(videoId: String): List<AudioFormatOption>
    suspend fun getLyrics(title: String, artist: String): String?
}

data class AudioExtractionResult(
    val audioUrl: String,
    val containerExt: String,
    val bitrate: Int,
    val artist: String,
    val title: String,
    val coverUrl: String,
)
