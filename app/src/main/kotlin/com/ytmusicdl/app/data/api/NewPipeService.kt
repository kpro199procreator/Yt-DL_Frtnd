package com.ytmusicdl.app.data.api

import com.ytmusicdl.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request as OkRequest
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.util.concurrent.TimeUnit

/**
 * Wrapper de NewPipe Extractor para búsqueda en YouTube Music
 * y extracción de URLs de audio.
 */
object NewPipeService {

    data class AudioExtractionResult(
        val audioUrl: String,
        val containerExt: String,
        val bitrate: Int,
        val artist: String,
        val title: String,
        val coverUrl: String,
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

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

    suspend fun searchSongs(query: String, limit: Int = 8): List<Track> = withContext(Dispatchers.IO) {
        runCatching {
            buildSearchExtractor(query, musicFilter = true).toTracks(limit)
        }.getOrElse {
            runCatching { buildSearchExtractor(query, musicFilter = false).toTracks(limit) }
                .getOrElse { emptyList() }
        }
    }

    private fun buildSearchExtractor(query: String, musicFilter: Boolean): SearchExtractor {
        val filters = if (musicFilter) listOf("EgWKAQIIAWoKEAMQBBAKEAkQBQ%3D%3D") else emptyList()
        return ServiceList.YouTube.getSearchExtractor(query, filters, null).apply { fetchPage() }
    }

    private fun SearchExtractor.toTracks(limit: Int): List<Track> =
        initialPage.items.take(limit).mapNotNull { item ->
            val streamItem = item as? org.schabi.newpipe.extractor.stream.StreamInfoItem ?: return@mapNotNull null
            val videoId = streamItem.url
                .substringAfter("v=", "")
                .substringBefore("&")
                .ifBlank { streamItem.url.substringAfterLast("/", "") }
            if (videoId.isBlank()) return@mapNotNull null

            Track(
                videoId = videoId,
                title = streamItem.name,
                artist = streamItem.uploaderName ?: "",
                duration = streamItem.duration?.let { formatDuration(it) } ?: "",
                coverUrl = streamItem.thumbnails.lastOrNull()?.url ?: "",
            )
        }

    suspend fun extractAudio(videoId: String): AudioExtractionResult? = withContext(Dispatchers.IO) {
        try {
            val info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$videoId")
            val stream = info.audioStreams
                .filter { it.format?.name?.contains("M4A", ignoreCase = true) == true || it.format?.name?.contains("MP4", ignoreCase = true) == true }
                .maxByOrNull { it.averageBitrate }
                ?: info.audioStreams
                    .filter { it.format?.name?.contains("WEBM", ignoreCase = true) == true || it.format?.name?.contains("OPUS", ignoreCase = true) == true }
                    .maxByOrNull { it.averageBitrate }
                ?: info.audioStreams.maxByOrNull { it.averageBitrate }
                ?: return@withContext null

            val ext = when {
                stream.format?.name?.contains("WEBM", ignoreCase = true) == true -> "webm"
                else -> "m4a"
            }

            AudioExtractionResult(
                audioUrl = stream.content ?: return@withContext null,
                containerExt = ext,
                bitrate = stream.averageBitrate,
                artist = info.uploaderName ?: "",
                title = info.name ?: "",
                coverUrl = info.thumbnails.lastOrNull()?.url ?: "",
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun extractAudioUrl(videoId: String): Pair<String, String>? =
        extractAudio(videoId)?.let { it.audioUrl to it.containerExt }

    suspend fun getTrackInfo(videoId: String): Track? = withContext(Dispatchers.IO) {
        try {
            val info = StreamInfo.getInfo(ServiceList.YouTube, "https://music.youtube.com/watch?v=$videoId")
            Track(
                videoId = videoId,
                title = info.name,
                artist = info.uploaderName,
                album = "",
                coverUrl = info.thumbnails.lastOrNull()?.url ?: "",
                duration = formatDuration(info.duration),
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun formatDuration(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m >= 60) {
            val h = m / 60
            "$h:${(m % 60).toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
        } else {
            "$m:${s.toString().padStart(2, '0')}"
        }
    }
}
