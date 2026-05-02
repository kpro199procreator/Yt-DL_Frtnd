package com.ytmusicdl.app.data

import android.content.Context

object AppSettings {
    private const val PREFS = "ytmusicdl_prefs"
    private const val KEY_DEFAULT_FORMAT_ID = "default_format_id"
    private const val KEY_BOOTSTRAP_CLI_DONE = "bootstrap_cli_done"

    fun getDefaultFormatId(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_DEFAULT_FORMAT_ID, "140")
            ?.trim()
            ?.ifBlank { "140" }
            ?: "140"

    fun setDefaultFormatId(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DEFAULT_FORMAT_ID, value.trim().ifBlank { "140" })
            .apply()
    }

    fun shouldRunBootstrapCli(context: Context): Boolean =
        !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_BOOTSTRAP_CLI_DONE, false)

    fun markBootstrapCliDone(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BOOTSTRAP_CLI_DONE, true)
            .apply()
    }
}
