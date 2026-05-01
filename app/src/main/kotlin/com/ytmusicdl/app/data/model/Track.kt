package com.ytmusicdl.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Modelo unificado de canción.
 * Sirve tanto para resultados de búsqueda como para el historial Room.
 */
data class Track(
    val videoId:     String,
    val title:       String,
    val artist:      String,
    val album:       String  = "",
    val year:        String  = "",
    val coverUrl:    String  = "",
    val duration:    String  = "",
    val streamUrl:   String  = "",   // URL de audio extraída por yt-dlp/Python backend
    val trackNumber: Int     = 0,
    val totalTracks: Int     = 0,
)

/** Historial de descargas persistido en Room */
@Entity(tableName = "downloads")
data class DownloadRecord(
    @PrimaryKey val videoId:    String,
    val title:                  String,
    val artist:                 String,
    val album:                  String,
    val filepath:               String,
    val format:                 String,
    val downloadedAt:           Long = System.currentTimeMillis(),
)

/** Estado de un job de descarga activo */
sealed class DownloadState {
    object Idle                                          : DownloadState()
    object FetchingStream                                : DownloadState()
    data class Downloading(val progress: Int,
                           val mbDone: Float,
                           val mbTotal: Float)           : DownloadState()
    object Converting                                    : DownloadState()
    object WritingTags                                   : DownloadState()
    data class Done(val filepath: String)                : DownloadState()
    data class Error(val message: String)                : DownloadState()
}
