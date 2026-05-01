package com.ytmusicdl.app.data.api

object ExtractorProvider {
    private val embedded = EmbeddedPythonBackend()

    val backend: ExtractorBackend
        get() = if (embedded.isAvailable()) embedded else NewPipeBackend
}
