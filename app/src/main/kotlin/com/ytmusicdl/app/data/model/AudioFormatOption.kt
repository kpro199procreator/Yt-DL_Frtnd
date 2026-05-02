package com.ytmusicdl.app.data.model

data class AudioFormatOption(
    val formatId: String,
    val ext: String,
    val abr: Int,
    val acodec: String,
    val asr: Int,
    val note: String,
)
