package com.ytmusicdl.app.data

import android.content.Context

object SettingsPrefs {
    private const val PREFS = "ytmusicdl_prefs"
    private const val KEY_DOWNLOAD_PATH = "download_path"
    private const val KEY_FILENAME_TEMPLATE = "filename_template"
    private const val KEY_MAX_CONCURRENT = "max_concurrent_downloads"

    fun downloadPath(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DOWNLOAD_PATH, "/Music/ytmusicdl") ?: "/Music/ytmusicdl"

    fun filenameTemplate(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_FILENAME_TEMPLATE, "{artist} - {track} ({year})") ?: "{artist} - {track} ({year})"

    fun maxConcurrent(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MAX_CONCURRENT, 3).coerceIn(1, 15)

    fun save(context: Context, path: String, template: String, concurrent: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_DOWNLOAD_PATH, path)
            .putString(KEY_FILENAME_TEMPLATE, template)
            .putInt(KEY_MAX_CONCURRENT, concurrent.coerceIn(1, 15))
            .apply()
    }
}

