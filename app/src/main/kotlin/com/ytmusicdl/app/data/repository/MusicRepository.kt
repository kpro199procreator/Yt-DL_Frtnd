package com.ytmusicdl.app.data.repository

import android.content.Context
import android.content.Intent
import com.ytmusicdl.app.data.api.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MusicRepository(private val context: Context) {

    companion object {
        const val BASE_URL = "http://127.0.0.1:8484/"
        const val TERMUX_PKG = "com.termux"
        const val TERMUX_SERVICE = "com.termux.app.RunCommandService"
        const val TERMUX_ACTION = "com.termux.RUN_COMMAND"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(YtmusicdlApi::class.java)
    private val gson = Gson()

    // ── Conexión ──────────────────────────────────────────────────────────────

    suspend fun isServerRunning(): Boolean {
        return try {
            api.health()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun startServer() {
        val intent = Intent().apply {
            setClassName(TERMUX_PKG, TERMUX_SERVICE)
            action = TERMUX_ACTION
            putExtra("com.termux.RUN_COMMAND_PATH",
                "/data/data/com.termux/files/usr/bin/ytmusicdl")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS",
                arrayOf("serve", "--host", "127.0.0.1", "--port", "8484"))
            putExtra("com.termux.RUN_COMMAND_WORKDIR",
                "/data/data/com.termux/files/home")
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
        }
        context.startService(intent)
    }

    // Espera hasta que el servidor responda (máx 15s)
    suspend fun waitForServer(timeoutMs: Long = 15_000): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (isServerRunning()) return true
            delay(500)
        }
        return false
    }

    // ── Search ────────────────────────────────────────────────────────────────

    suspend fun searchSongs(query: String, limit: Int = 8): List<Track> {
        val response = api.search(query, "song", limit)
        if (!response.isSuccessful) return emptyList()
        val body = response.body() ?: return emptyList()
        val resultsJson = gson.toJson(body["results"])
        val type = object : TypeToken<List<Track>>() {}.type
        return gson.fromJson(resultsJson, type) ?: emptyList()
    }

    suspend fun searchAlbums(query: String, limit: Int = 5): List<Track> {
        val response = api.search(query, "album", limit)
        if (!response.isSuccessful) return emptyList()
        val body = response.body() ?: return emptyList()
        val resultsJson = gson.toJson(body["results"])
        val type = object : TypeToken<List<Track>>() {}.type
        return gson.fromJson(resultsJson, type) ?: emptyList()
    }

    // ── Download ──────────────────────────────────────────────────────────────

    suspend fun download(track: Track, format: String? = null): DownloadResponse {
        val req = DownloadRequest(
            video_id  = track.video_id,
            url       = track.url,
            title     = track.title,
            artist    = track.artist,
            album     = track.album,
            year      = track.year,
            cover_url = track.cover_url,
            duration  = track.duration,
            format    = format,
        )
        return api.download(req)
    }

    suspend fun pollProgress(jobId: String): ProgressResponse {
        return api.progress(jobId)
    }

    // Polling hasta que el job termine
    suspend fun waitForDownload(
        jobId: String,
        onProgress: (ProgressResponse) -> Unit,
    ): ProgressResponse {
        var last: ProgressResponse? = null
        while (true) {
            val prog = pollProgress(jobId)
            onProgress(prog)
            last = prog
            if (prog.status in listOf("done", "skipped", "error")) break
            delay(500)
        }
        return last!!
    }

    // ── Cover URL ─────────────────────────────────────────────────────────────

    fun coverUrl(originalUrl: String): String {
        if (originalUrl.isEmpty()) return ""
        val encoded = java.net.URLEncoder.encode(originalUrl, "UTF-8")
        return "${BASE_URL}cover?url=$encoded"
    }
}
