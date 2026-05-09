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
        return AudioExtractionResult(
            audioUrl = obj.optString("audioUrl"),
            containerExt = obj.optString("containerExt", "m4a"),
            bitrate = obj.optInt("bitrate", 0),
            artist = obj.optString("artist"),
            title = obj.optString("title"),
            coverUrl = obj.optString("coverUrl"),
            selectedFormatId = obj.optString("selectedFormatId"),
            selectedAudioCodec = obj.optString("selectedAudioCodec"),
            selectedSampleRate = obj.optInt("selectedSampleRate", 0),
            selectedProtocol = obj.optString("selectedProtocol"),
            selectedFormatNote = obj.optString("selectedFormatNote"),
            selectedFileSize = obj.optLong("selectedFileSize", 0L),
            selectionReason = obj.optString("selectionReason"),
        )
    }


    fun parseAudioFormats(json: String): List<AudioFormatOption> {
        if (json.isBlank() || json == "null") return emptyList()
        val obj = JSONObject(json)
        val arr = obj.optJSONArray("formats") ?: JSONArray()
        return buildList {
            for (i in 0 until arr.length()) {
                val f = arr.optJSONObject(i) ?: continue
                add(
                    AudioFormatOption(
                        formatId = f.optString("format_id"),
                        ext = f.optString("ext"),
                        audioCodec = f.optString("acodec"),
                        bitrate = f.optInt("abr", 0),
                        sampleRate = f.optInt("asr", 0),
                        fileSize = f.optLong("filesize", 0L),
                        protocol = f.optString("protocol"),
                        formatNote = f.optString("format_note"),
                    )
                )
            }
        }
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
