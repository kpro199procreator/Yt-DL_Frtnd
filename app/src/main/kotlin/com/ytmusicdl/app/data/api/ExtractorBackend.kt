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
    suspend fun getPlaylistTracks(playlistIdOrUrl: String, limit: Int = 200): PlaylistTracksResult
    suspend fun getAlbumTracks(albumIdOrName: String, artist: String): AlbumTracksResult
}

data class PlaylistMeta(
    val id: String,
    val title: String,
    val author: String,
    val trackCount: Int,
)

data class PlaylistTracksResult(
    val playlist: PlaylistMeta,
    val tracks: List<Track>,
)

data class AlbumMeta(
    val id: String,
    val title: String,
    val year: String,
    val trackCount: Int,
)

data class AlbumTracksResult(
    val album: AlbumMeta,
    val tracks: List<Track>,
    val exactMatch: Boolean,
)

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
