package com.ytmusicdl.app.data.api

import com.ytmusicdl.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import okhttp3.OkHttpClient
import okhttp3.Request as OkRequest
import java.util.concurrent.TimeUnit

/**
 * Wrapper de NewPipe Extractor para búsqueda en YouTube Music
 * y extracción de URLs de audio.
 *
 * NewPipe hace reverse engineering de la API interna de YouTube
 * (InnerTube) — sin API key, sin cuenta, sin Python.
 */
object NewPipeService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Downloader que NewPipe necesita para hacer sus requests HTTP */
    private val downloader = object : Downloader() {
        override fun execute(request: Request): Response {
            val okReq = OkRequest.Builder()
                .url(request.url())
                .apply {
                    request.headers().forEach { (k, vs) ->
                        vs.forEach { v -> addHeader(k, v) }
                    }
                    if (request.httpMethod() == "POST") {
                        val body = request.dataToSend() ?: ByteArray(0)
                        post(okhttp3.RequestBody.create(null, body))
                    }
                }
                .build()

            val resp = httpClient.newCall(okReq).execute()
            return Response(
                resp.code,
                resp.message,
                resp.headers.toMultimap(),
                resp.body?.string(),
                request.url(),
            )
        }
    }

    fun init() {
        NewPipe.init(downloader)
    }

    /**
     * Busca canciones en YouTube Music.
     * Usa el servicio de YouTube con filtro de música.
     */
    suspend fun searchSongs(query: String, limit: Int = 8): List<Track> =
        withContext(Dispatchers.IO) {
            try {
                val ytService = ServiceList.YouTube
                // YouTube Music search — filtro "music songs"
                val extractor: SearchExtractor = ytService.getSearchExtractor(
                    query,
                    listOf("EgWKAQIIAWoKEAMQBBAKEAkQBQ%3D%3D"), // filtro YT Music songs
                    null,
                )
                extractor.fetchPage()

                val items = extractor.initialPage.items
                items.take(limit).mapNotNull { item ->
                    try {
                        Track(
                            videoId  = item.url.substringAfterLast("v=").substringBefore("&"),
                            title    = item.name,
                            artist   = (item as? org.schabi.newpipe.extractor.stream.StreamInfoItem)
                                ?.uploaderName ?: "",
                            duration = (item as? org.schabi.newpipe.extractor.stream.StreamInfoItem)
                                ?.duration?.let { formatDuration(it) } ?: "",
                            coverUrl = item.thumbnails.lastOrNull()?.url ?: "",
                        )
                    } catch (e: Exception) { null }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    /**
     * Extrae la URL de audio de mejor calidad para un video.
     * Esto es lo que normalmente hace yt-dlp — NewPipe lo hace en Kotlin puro.
     */
    suspend fun extractAudioUrl(videoId: String): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            try {
                val url  = "https://www.youtube.com/watch?v=$videoId"
                val info = StreamInfo.getInfo(ServiceList.YouTube, url)

                // Prioridad: m4a (aac) > opus > webm
                val audioStream = info.audioStreams
                    .filter { it.format?.name?.contains("M4A", ignoreCase = true) == true ||
                               it.mediaFormat?.mimeType?.contains("mp4") == true }
                    .maxByOrNull { it.averageBitrate }
                    ?: info.audioStreams
                        .filter { it.format?.name?.contains("WEBM", ignoreCase = true) == true ||
                                   it.mediaFormat?.mimeType?.contains("webm") == true }
                        .maxByOrNull { it.averageBitrate }
                    ?: info.audioStreams.maxByOrNull { it.averageBitrate }

                audioStream?.let { stream ->
                    val ext = when {
                        stream.mediaFormat?.mimeType?.contains("mp4")  == true -> "m4a"
                        stream.mediaFormat?.mimeType?.contains("webm") == true -> "opus"
                        else -> "m4a"
                    }
                    Pair(stream.content ?: return@withContext null, ext)
                }
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Obtiene metadata adicional de un video (álbum, año, etc.)
     * que no viene en los resultados de búsqueda.
     */
    suspend fun getTrackInfo(videoId: String): Track? =
        withContext(Dispatchers.IO) {
            try {
                val url  = "https://music.youtube.com/watch?v=$videoId"
                val info = StreamInfo.getInfo(ServiceList.YouTube, url)
                Track(
                    videoId  = videoId,
                    title    = info.name,
                    artist   = info.uploaderName,
                    album    = "", // YT Music a veces lo expone en el description
                    coverUrl = info.thumbnails.lastOrNull()?.url ?: "",
                    duration = formatDuration(info.duration),
                )
            } catch (e: Exception) { null }
        }

    private fun formatDuration(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m >= 60) {
            val h = m / 60
            "$h:${(m % 60).toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}"
        } else {
            "$m:${s.toString().padStart(2,'0')}"
        }
    }
}
