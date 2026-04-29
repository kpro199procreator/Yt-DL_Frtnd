package com.ytmusicdl.app.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Cliente para lrclib.net — letras sincronizadas LRC gratuitas.
 * Sin API key, sin rate limit, open source.
 * Endpoint: GET https://lrclib.net/api/get?track_name=...&artist_name=...
 */
object LrcLibService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private const val BASE = "https://lrclib.net/api"
    private const val UA   = "ytmusicdl/0.1.0-alpha (https://github.com/ytmusicdl)"

    data class LyricsResult(
        val syncedLyrics: String?,   // LRC con timestamps [00:27.93]
        val plainLyrics:  String?,   // Texto plano sin timestamps
        val source:       String = "lrclib",
    )

    /**
     * Busca letras por título y artista.
     * Intenta match exacto primero, luego búsqueda fuzzy.
     */
    suspend fun getLyrics(
        title: String,
        artist: String,
        album: String  = "",
        durationSec: Long = 0,
    ): LyricsResult? = withContext(Dispatchers.IO) {
        // Intento 1 — match exacto con /api/get
        val exactResult = fetchExact(title, artist, album, durationSec)
        if (exactResult != null) return@withContext exactResult

        // Intento 2 — búsqueda fuzzy con /api/search
        fetchSearch(title, artist)
    }

    private fun fetchExact(title: String, artist: String, album: String, duration: Long): LyricsResult? {
        val params = buildString {
            append("track_name=${encode(title)}")
            append("&artist_name=${encode(artist)}")
            if (album.isNotEmpty()) append("&album_name=${encode(album)}")
            if (duration > 0) append("&duration=$duration")
        }
        return get("$BASE/get?$params")
    }

    private fun fetchSearch(title: String, artist: String): LyricsResult? {
        val q = encode("$title $artist")
        val resp = getRaw("$BASE/search?q=$q") ?: return null
        return try {
            // /api/search retorna un array — tomamos el primer resultado
            val arr = org.json.JSONArray(resp)
            if (arr.length() == 0) return null
            parseResult(arr.getJSONObject(0))
        } catch (e: Exception) { null }
    }

    private fun get(url: String): LyricsResult? {
        val raw = getRaw(url) ?: return null
        return try {
            parseResult(JSONObject(raw))
        } catch (e: Exception) { null }
    }

    private fun getRaw(url: String): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .header("Accept", "application/json")
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return null
            resp.body?.string()
        } catch (e: Exception) { null }
    }

    private fun parseResult(json: JSONObject): LyricsResult? {
        // Si es instrumental no hay letras
        if (json.optBoolean("instrumental", false)) return null
        val synced = json.optString("syncedLyrics").takeIf { it.isNotEmpty() }
        val plain  = json.optString("plainLyrics").takeIf { it.isNotEmpty() }
        if (synced == null && plain == null) return null
        return LyricsResult(syncedLyrics = synced, plainLyrics = plain)
    }

    private fun encode(s: String) = URLEncoder.encode(s, "UTF-8")
}
