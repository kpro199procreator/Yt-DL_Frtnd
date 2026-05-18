package com.ytmusicdl.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import com.ytmusicdl.app.data.SettingsPrefs
import com.ytmusicdl.app.data.api.LrcLibService
import com.ytmusicdl.app.data.api.ExtractorBackendProvider
import com.ytmusicdl.app.data.model.DownloadState
import com.ytmusicdl.app.data.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Servicio de descarga en foreground.
 * Flujo:
 *   1. El backend extrae la URL de audio del video
 *   2. OkHttp descarga el stream de audio (m4a/webm)
 *   3. Mantiene contenedor de origen (sin conversión FFmpeg)
 *   4. JAudioTagger escribe los tags (título, artista, carátula, letras LRC)
 *   5. Guarda en ~/Music en almacenamiento externo
 */
class DownloadService : Service() {
    data class QueueItem(
        val videoId: String,
        val title: String,
        val album: String,
        val progress: Int = 0,
        val status: String = "queued",
        val format: String = "auto",
        val bitrateKbps: Int = 0,
        val speedMbps: Float = 0f,
        val etaSec: Int = -1,
        val cliOutput: String = "En cola…"
    )

    companion object {
        const val CHANNEL_ID    = "ytmusicdl_downloads"
        const val NOTIF_ID      = 1001
                // StateFlow compartido para que la UI observe el estado
        val downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
        val queueState = MutableStateFlow<List<QueueItem>>(emptyList())
        @Volatile private var limiter: Semaphore? = null

        fun start(context: Context, track: Track) {
            queueState.update { old ->
                if (old.any { it.videoId == track.videoId && (it.status == "queued" || it.status == "downloading") }) old
                else old + QueueItem(track.videoId, track.title, track.album)
            }
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_TRACK_TITLE,  track.title)
                putExtra(EXTRA_TRACK_ARTIST, track.artist)
                putExtra(EXTRA_TRACK_ALBUM,  track.album)
                putExtra(EXTRA_TRACK_ID,     track.videoId)
                putExtra(EXTRA_TRACK_COVER,  track.coverUrl)
                putExtra(EXTRA_TRACK_YEAR,   track.year)
                putExtra(EXTRA_TRACK_DUR,    track.duration)
            }
            context.startForegroundService(intent)
        }

        private const val EXTRA_TRACK_TITLE  = "title"
        private const val EXTRA_TRACK_ARTIST = "artist"
        private const val EXTRA_TRACK_ALBUM  = "album"
        private const val EXTRA_TRACK_ID     = "videoId"
        private const val EXTRA_TRACK_COVER  = "coverUrl"
        private const val EXTRA_TRACK_YEAR   = "year"
        private const val EXTRA_TRACK_DUR    = "duration"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)  // Sin timeout para descargas largas
        .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val track = Track(
            videoId  = intent?.getStringExtra(EXTRA_TRACK_ID)     ?: return START_NOT_STICKY,
            title    = intent.getStringExtra(EXTRA_TRACK_TITLE)   ?: "",
            artist   = intent.getStringExtra(EXTRA_TRACK_ARTIST)  ?: "",
            album    = intent.getStringExtra(EXTRA_TRACK_ALBUM)   ?: "",
            coverUrl = intent.getStringExtra(EXTRA_TRACK_COVER)   ?: "",
            year     = intent.getStringExtra(EXTRA_TRACK_YEAR)    ?: "",
            duration = intent.getStringExtra(EXTRA_TRACK_DUR)     ?: "",
        )
        val notifId = (track.videoId.hashCode() and 0x7fffffff) % 100000 + 1000
        startForeground(notifId, buildNotification("Preparando descarga…", 0))
        scope.launch { downloadTrack(track) }
        return START_NOT_STICKY
    }

    private suspend fun downloadTrack(track: Track) {
        val notifId = (track.videoId.hashCode() and 0x7fffffff) % 100000 + 1000
        val sem = limiter ?: Semaphore(SettingsPrefs.maxConcurrent(applicationContext)).also { limiter = it }
        sem.withPermit {
        try {
            queueState.update { list -> list.map { if (it.videoId == track.videoId) it.copy(status = "downloading", cliOutput = "[yt-dlp] preparando extracción", speedMbps = 0f, etaSec = -1) else it } }
            // 1. Extraer URL de audio
            downloadState.value = DownloadState.FetchingStream
            updateNotification(notifId, "Obteniendo stream…", 0)

            val extraction = ExtractorBackendProvider.backend.extractAudio(track.videoId)
                ?: run {
                    downloadState.value = DownloadState.Error("No se pudo extraer el stream")
                    stopSelf(); return
                }

            val audioUrl = extraction.audioUrl
            val ext = extraction.containerExt
            if (audioUrl.isBlank()) {
                downloadState.value = DownloadState.Error("No se encontró URL de audio (${extraction.selectionReason})")
                stopSelf(); return
            }
            val qualityLabel = buildQualityLabel(extraction)
            queueState.update { list -> list.map { if (it.videoId == track.videoId) it.copy(format = extraction.containerExt.ifBlank { "auto" }, bitrateKbps = extraction.bitrate, cliOutput = "[yt-dlp] format=${extraction.selectedFormatId.ifBlank { "auto" }} ${extraction.containerExt} ${extraction.bitrate}kbps") else it } }
            updateNotification(notifId, "Formato elegido: $qualityLabel", 0)

            val enrichedTrack = track.copy(
                title = track.title.ifBlank { extraction.title },
                artist = track.artist.ifBlank { extraction.artist },
                coverUrl = track.coverUrl.ifBlank { extraction.coverUrl },
                streamUrl = audioUrl,
            )

            // 2. Descargar el audio
            val tempFile = File(cacheDir, "${track.videoId}_temp.$ext")
            downloadAudio(audioUrl, tempFile) { progress, mbDone, mbTotal, speedMbps, etaSec ->
                downloadState.value = DownloadState.Downloading(progress, mbDone, mbTotal)
                queueState.update { list ->
                    list.map {
                        if (it.videoId == track.videoId) it.copy(
                            progress = progress,
                            status = "downloading",
                            speedMbps = speedMbps,
                            etaSec = etaSec,
                            cliOutput = "[download] ${"%.2f".format(mbDone)}/${"%.2f".format(mbTotal)}MB @ ${"%.2f".format(speedMbps)}MB/s"
                        ) else it
                    }
                }
                updateNotification(notifId, "Descargando $qualityLabel", progress)
            }

            // 3. Sin conversión FFmpeg: se conserva el archivo descargado
            val finalFile = tempFile

            // 4. Escribir tags
            queueState.update { list -> list.map { if (it.videoId == track.videoId) it.copy(cliOutput = "[postprocess] escribiendo tags", etaSec = 0) else it } }
            downloadState.value = DownloadState.WritingTags
            updateNotification(notifId, "Escribiendo metadata…", 100)
            writeTags(finalFile, enrichedTrack)

            // 5. Mover a carpeta de música
            val configuredPath = SettingsPrefs.downloadPath(applicationContext).trim().ifBlank { "/Music/ytmusicdl" }
            val outputDir = File(android.os.Environment.getExternalStorageDirectory(), configuredPath.removePrefix("/")).also { it.mkdirs() }
            val outputName = applyTemplate(SettingsPrefs.filenameTemplate(applicationContext), enrichedTrack) + ".${ext.ifBlank { "m4a" }}"
            val outputFile = File(outputDir, sanitize(outputName))
            finalFile.copyTo(outputFile, overwrite = true)
            tempFile.delete()
            finalFile.delete()

            // Notificar al sistema de medios
            android.media.MediaScannerConnection.scanFile(
                this, arrayOf(outputFile.absolutePath), null, null
            )

            downloadState.value = DownloadState.Done(outputFile.absolutePath)
            queueState.update { list -> list.map { if (it.videoId == track.videoId) it.copy(progress = 100, status = "done", speedMbps = 0f, etaSec = 0, cliOutput = "[done] guardado en ${outputFile.name}") else it } }
            updateNotification(notifId, "✓ ${enrichedTrack.title} descargado ($qualityLabel)", 100)

        } catch (e: Exception) {
            downloadState.value = DownloadState.Error(e.message ?: "Error desconocido")
            queueState.update { list -> list.map { if (it.videoId == track.videoId) it.copy(status = "error", cliOutput = "[error] ${e.message}") else it } }
            updateNotification(notifId, "Error: ${e.message}", 0)
        } finally {
            delay(1500)
            val pending = queueState.value.any { it.status == "queued" || it.status == "downloading" }
            if (!pending) stopSelf()
        }
        }
    }


    private fun buildQualityLabel(extraction: com.ytmusicdl.app.data.api.AudioExtractionResult): String {
        val ext = extraction.containerExt.ifBlank { "audio" }
        val bitrate = extraction.bitrate.takeIf { it > 0 }?.let { " ${it}kbps" } ?: ""
        val codec = extraction.selectedAudioCodec.takeIf { it.isNotBlank() }?.let { " ${it}" } ?: ""
        return "${ext}${bitrate}${codec}".trim()
    }

    private suspend fun downloadAudio(
        url: String,
        dest: File,
        onProgress: (Int, Float, Float, Float, Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val req  = Request.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (compatible)")
            .build()
        val resp = httpClient.newCall(req).execute()
        val body = resp.body ?: throw Exception("Response body vacío")

        val total     = body.contentLength()
        var downloaded = 0L
        var lastBytes = 0L
        var lastAt = SystemClock.elapsedRealtime()
        val buffer    = ByteArray(8192)

        FileOutputStream(dest).use { out ->
            body.byteStream().use { input ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                    downloaded += read
                    if (total > 0) {
                        val progress = (downloaded * 100 / total).toInt()
                        val mbDone   = downloaded / 1_048_576f
                        val mbTotal  = total / 1_048_576f
                        val now = SystemClock.elapsedRealtime()
                        val dt = (now - lastAt).coerceAtLeast(1L)
                        val dBytes = (downloaded - lastBytes).coerceAtLeast(0L)
                        val speedMbps = (dBytes / 1_048_576f) / (dt / 1000f)
                        val remainMb = (mbTotal - mbDone).coerceAtLeast(0f)
                        val etaSec = if (speedMbps > 0.01f) (remainMb / speedMbps).toInt() else -1
                        lastBytes = downloaded
                        lastAt = now
                        onProgress(progress, mbDone, mbTotal, speedMbps, etaSec)
                    }
                }
            }
        }
    }

    private suspend fun writeTags(file: File, track: Track) = withContext(Dispatchers.IO) {
        try {
            val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
            val tag       = audioFile.tagOrCreateAndSetDefault

            tag.setField(org.jaudiotagger.tag.FieldKey.TITLE,  track.title)
            tag.setField(org.jaudiotagger.tag.FieldKey.ARTIST, track.artist)
            if (track.album.isNotEmpty())
                tag.setField(org.jaudiotagger.tag.FieldKey.ALBUM, track.album)
            if (track.year.isNotEmpty())
                tag.setField(org.jaudiotagger.tag.FieldKey.YEAR, track.year)

            // Carátula
            if (track.coverUrl.isNotEmpty()) {
                try {
                    val coverBytes = fetchBytes(track.coverUrl)
                    if (coverBytes != null) {
                        val artwork = org.jaudiotagger.tag.images.StandardArtwork()
                        artwork.binaryData = coverBytes
                        artwork.mimeType = "image/jpeg"
                        tag.deleteArtworkField()
                        tag.setField(artwork)
                    }
                } catch (_: Exception) {}
            }

            // Letras LRC desde lrclib
            try {
                val lyrics = LrcLibService.getLyrics(track.title, track.artist, track.album)
                if (lyrics != null) {
                    val lrcText = lyrics.syncedLyrics ?: lyrics.plainLyrics ?: ""
                    if (lrcText.isNotEmpty()) {
                        tag.setField(org.jaudiotagger.tag.FieldKey.LYRICS, lrcText)
                    }
                }
            } catch (_: Exception) {}

            audioFile.commit()
        } catch (e: Exception) {
            // Tags son opcionales — no interrumpir la descarga si fallan
        }
    }

    private fun fetchBytes(url: String): ByteArray? {
        return try {
            val req  = Request.Builder().url(url).build()
            val resp = httpClient.newCall(req).execute()
            resp.body?.bytes()
        } catch (e: Exception) { null }
    }

    private fun sanitize(name: String) =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()

    private fun applyTemplate(template: String, track: Track): String {
        return template
            .replace("{album}", track.album.ifBlank { "Unknown Album" })
            .replace("{track}", track.title.ifBlank { "Unknown Track" })
            .replace("{year}", track.year.ifBlank { "0000" })
            .replace("{artist}", track.artist.ifBlank { "Unknown Artist" })
    }

    // ── Notificaciones ────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Descargas",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Estado de descargas de ytmusicdl" }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(text: String, progress: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("ytmusicdl")
            .setContentText(text)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(notifId: Int, text: String, progress: Int) {
        val notif = buildNotification(text, progress)
        getSystemService(NotificationManager::class.java)
            .notify(notifId, notif)
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
