package com.ytmusicdl.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.ytmusicdl.app.data.api.LrcLibService
import com.ytmusicdl.app.data.api.ExtractorProvider
import com.ytmusicdl.app.data.model.DownloadState
import com.ytmusicdl.app.data.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Servicio de descarga en foreground.
 * Flujo:
 *   1. NewPipe extrae la URL de audio del video
 *   2. OkHttp descarga el stream de audio (m4a/webm)
 *   3. Si es webm/opus → ffmpeg-kit convierte a m4a
 *   4. JAudioTagger escribe los tags (título, artista, carátula, letras LRC)
 *   5. Guarda en ~/Music en almacenamiento externo
 */
class DownloadService : Service() {

    companion object {
        const val CHANNEL_ID    = "ytmusicdl_downloads"
        const val NOTIF_ID      = 1001
        const val EXTRA_TRACK   = "track"

        // StateFlow compartido para que la UI observe el estado
        val downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)

        fun start(context: Context, track: Track) {
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

        startForeground(NOTIF_ID, buildNotification("Preparando descarga…", 0))
        scope.launch { downloadTrack(track) }
        return START_NOT_STICKY
    }

    private suspend fun downloadTrack(track: Track) {
        try {
            // 1. Extraer URL de audio con NewPipe
            downloadState.value = DownloadState.FetchingStream
            updateNotification("Obteniendo stream…", 0)

            val (audioUrl, ext) = ExtractorProvider.backend.extractAudioUrl(track.videoId)
                ?: run {
                    downloadState.value = DownloadState.Error("No se pudo extraer el stream")
                    stopSelf(); return
                }

            // 2. Descargar el audio
            val tempFile = File(cacheDir, "${track.videoId}_temp.$ext")
            downloadAudio(audioUrl, tempFile) { progress, mbDone, mbTotal ->
                downloadState.value = DownloadState.Downloading(progress, mbDone, mbTotal)
                updateNotification("Descargando ${track.title}", progress)
            }

            // 3. Convertir a m4a si es necesario (webm/opus → m4a/aac)
            downloadState.value = DownloadState.Converting
            updateNotification("Procesando audio…", 100)
            val finalFile = convertIfNeeded(tempFile, track, ext)

            // 4. Escribir tags
            downloadState.value = DownloadState.WritingTags
            updateNotification("Escribiendo metadata…", 100)
            writeTags(finalFile, track)

            // 5. Mover a carpeta de música
            val outputDir  = File(
                android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_MUSIC
                ), "ytmusicdl"
            ).also { it.mkdirs() }
            val outputFile = File(outputDir, sanitize("${track.artist} - ${track.title}.m4a"))
            finalFile.copyTo(outputFile, overwrite = true)
            tempFile.delete()
            finalFile.delete()

            // Notificar al sistema de medios
            android.media.MediaScannerConnection.scanFile(
                this, arrayOf(outputFile.absolutePath), null, null
            )

            downloadState.value = DownloadState.Done(outputFile.absolutePath)
            updateNotification("✓ ${track.title} descargado", 100)

        } catch (e: Exception) {
            downloadState.value = DownloadState.Error(e.message ?: "Error desconocido")
            updateNotification("Error: ${e.message}", 0)
        } finally {
            delay(3000)
            stopSelf()
        }
    }

    private suspend fun downloadAudio(
        url: String,
        dest: File,
        onProgress: (Int, Float, Float) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val req  = Request.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (compatible)")
            .build()
        val resp = httpClient.newCall(req).execute()
        val body = resp.body ?: throw Exception("Response body vacío")

        val total     = body.contentLength()
        var downloaded = 0L
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
                        onProgress(progress, mbDone, mbTotal)
                    }
                }
            }
        }
    }

    private suspend fun convertIfNeeded(input: File, track: Track, ext: String): File =
        withContext(Dispatchers.IO) {
            if (ext == "m4a") return@withContext input

            // webm/opus → m4a con ffmpeg-kit
            val output = File(cacheDir, "${track.videoId}.m4a")
            val session = FFmpegKit.execute(
                "-i \"${input.absolutePath}\" -c:a aac -b:a 256k -vn \"${output.absolutePath}\""
            )
            if (!ReturnCode.isSuccess(session.returnCode)) {
                // Si ffmpeg falla, devolver el archivo original sin convertir
                return@withContext input
            }
            output
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
                        val artwork = org.jaudiotagger.tag.images.ArtworkFactory
                            .createArtworkFromFile(file)
                        artwork.binaryData = coverBytes
                        artwork.mimeType   = "image/jpeg"
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

    private fun updateNotification(text: String, progress: Int) {
        val notif = buildNotification(text, progress)
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, notif)
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
