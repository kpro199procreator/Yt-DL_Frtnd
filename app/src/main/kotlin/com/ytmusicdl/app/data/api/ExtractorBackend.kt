package com.ytmusicdl.app.data.api

import com.ytmusicdl.app.data.model.Track

data class AudioFormatOption(
    val formatId: String,
    val ext: String,
    val audioCodec: String,
    val bitrate: Int,
    val sampleRate: Int,
    val fileSize: Long,
    val protocol: String,
    val formatNote: String,
)

interface ExtractorBackend {
    suspend fun searchSongs(query: String, limit: Int = 8): List<Track>
    suspend fun listAudioFormats(videoId: String): List<AudioFormatOption>
    suspend fun extractAudio(videoId: String, preferredFormatId: String? = null): AudioExtractionResult?
    suspend fun getTrackInfo(videoIdOrQuery: String): Track?
    suspend fun getLyrics(title: String, artist: String): String?
}

data class AudioExtractionResult(
    val audioUrl: String,
    val containerExt: String,
    val bitrate: Int,
    val artist: String,
    val title: String,
    val coverUrl: String,
    val selectedFormatId: String = "",
    val selectedAudioCodec: String = "",
    val selectedSampleRate: Int = 0,
    val selectedProtocol: String = "",
    val selectedFormatNote: String = "",
    val selectedFileSize: Long = 0L,
    val selectionReason: String = "",
)
