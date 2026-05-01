package com.ytmusicdl.app.data.api

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONArray
import org.json.JSONObject

object PythonBridge {
    private const val MODULE_NAME = "yt_backend"

    @Volatile
    private var initialized = false
    @Volatile
    private var initError: String? = null

    fun initialize(context: Context) {
        if (initialized) return
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context.applicationContext))
            }
            // Smoke test to verify module is importable.
            Python.getInstance().getModule(MODULE_NAME)
            initialized = true
            initError = null
        } catch (e: Exception) {
            initialized = false
            initError = e.message ?: "No se pudo iniciar runtime Python"
        }
    }

    fun isAvailable(): Boolean = initialized && initError == null
    fun getInitError(): String? = initError

    fun call(functionName: String, vararg args: Any?): String {
        check(isAvailable()) { initError ?: "Python runtime no disponible" }
        val module = Python.getInstance().getModule(MODULE_NAME)
        val result: PyObject = module.callAttr(functionName, *args)
        return result.toString()
    }

    fun parseTrackList(json: String): List<com.ytmusicdl.app.data.model.Track> {
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                add(parseTrack(array.getJSONObject(i)))
            }
        }
    }

    fun parseTrack(json: String): com.ytmusicdl.app.data.model.Track? {
        if (json.isBlank() || json == "null") return null
        return parseTrack(JSONObject(json))
    }

    fun parseAudioResult(json: String): AudioExtractionResult? {
        if (json.isBlank() || json == "null") return null
        val obj = JSONObject(json)
        if (obj.optBoolean("error", false)) {
            throw IllegalStateException(obj.optString("message", "No se pudo extraer audio"))
        }
        return AudioExtractionResult(
            audioUrl = obj.optString("audioUrl"),
            containerExt = obj.optString("containerExt", "m4a"),
            bitrate = obj.optInt("bitrate", 0),
            artist = obj.optString("artist"),
            title = obj.optString("title"),
            coverUrl = obj.optString("coverUrl"),
        )
    }

    private fun parseTrack(obj: JSONObject) = com.ytmusicdl.app.data.model.Track(
        videoId = obj.optString("videoId"),
        title = obj.optString("title"),
        artist = obj.optString("artist"),
        album = obj.optString("album"),
        year = obj.optString("year"),
        coverUrl = obj.optString("coverUrl"),
        duration = obj.optString("duration"),
        streamUrl = obj.optString("streamUrl"),
    )
}
