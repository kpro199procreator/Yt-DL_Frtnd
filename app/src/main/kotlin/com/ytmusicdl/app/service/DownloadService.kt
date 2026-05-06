package com.ytmusicdl.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import com.ytmusicdl.app.data.api.ExtractorBackendProvider
import com.ytmusicdl.app.data.api.PythonBridge
import com.ytmusicdl.app.data.model.DownloadState
import com.ytmusicdl.app.data.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class DownloadService : Service() {

    companion object {
        const val CHANNEL_ID    = "ytmusicdl_downloads"
        const val NOTIF_ID      = 1001
        const val EXTRA_TRACK   = "track"

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
        .readTimeout(0, TimeUnit.SECONDS)
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
            if (PythonBridge.isAvailable()) {
                if (downloadWithPython(track)) return
            }
            downloadWithFallback(track)
        } catch (e: Exception) {
            downloadState.value = DownloadState.Error(e.message ?: "Error desconocido")
            updateNotification("Error: ${e.message}", 0)
        } finally {
            delay(3000)
            stopSelf()
        }
    }

    private suspend fun downloadWithPython(track: Track): Boolean = withContext(Dispatchers.IO) {
        downloadState.value = DownloadState.FetchingStream
        updateNotification("Descargando con backend Python…", 10)

        val outputDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "ytmusicdl"
        ).also { it.mkdirs() }

        val resultJson = PythonBridge.call(
            "download_track_full",
            track.videoId,
            outputDir.absolutePath,
            track.title,
            track.artist,
            track.album,
            track.year,
            track.coverUrl,
        )
        val obj = JSONObject(resultJson)
        if (obj.optString("status") == "ok" || obj.optString("status") == "skipped") {
            val path = obj.optString("path")
            if (path.isNotBlank()) {
                MediaScannerConnection.scanFile(this@DownloadService, arrayOf(path), null, null)
                downloadState.value = DownloadState.Done(path)
                updateNotification("✓ ${track.title.ifBlank { "Descarga" }} completado", 100)
                return@withContext true
            }
        }

        val err = obj.optString("error", "Error en backend Python")
        updateNotification("Python falló, usando fallback…", 0)
        downloadState.value = DownloadState.Error(err)
        false
    }

    private suspend fun downloadWithFallback(track: Track) {
        downloadState.value = DownloadState.FetchingStream
        updateNotification("Obteniendo stream…", 0)

        val extraction = ExtractorBackendProvider.backend.extractAudio(track.videoId)
            ?: throw Exception("No se pudo extraer el stream")

        val enrichedTrack = track.copy(
            title = track.title.ifBlank { extraction.title },
            artist = track.artist.ifBlank { extraction.artist },
        )

        val tempFile = File(cacheDir, "${track.videoId}_temp.${extraction.containerExt}")
        downloadAudio(extraction.audioUrl, tempFile) { progress, mbDone, mbTotal ->
            downloadState.value = DownloadState.Downloading(progress, mbDone, mbTotal)
            updateNotification("Descargando ${enrichedTrack.title}", progress)
        }

        val outputDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "ytmusicdl"
        ).also { it.mkdirs() }
        val outputFile = File(
            outputDir,
            sanitize("${enrichedTrack.artist} - ${enrichedTrack.title}.${extraction.containerExt}")
        )
        tempFile.copyTo(outputFile, overwrite = true)
        tempFile.delete()

        MediaScannerConnection.scanFile(this, arrayOf(outputFile.absolutePath), null, null)
        downloadState.value = DownloadState.Done(outputFile.absolutePath)
        updateNotification("✓ ${enrichedTrack.title} descargado (fallback)", 100)
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

        val total = body.contentLength()
        var downloaded = 0L
        val buffer = ByteArray(8192)

        FileOutputStream(dest).use { out ->
            body.byteStream().use { input ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                    downloaded += read
                    if (total > 0) {
                        val progress = (downloaded * 100 / total).toInt()
                        val mbDone = downloaded / 1_048_576f
                        val mbTotal = total / 1_048_576f
                        onProgress(progress, mbDone, mbTotal)
                    }
                }
            }
        }
    }

    private fun sanitize(name: String) =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()

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
