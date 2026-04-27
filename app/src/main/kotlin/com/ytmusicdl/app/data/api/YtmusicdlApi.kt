package com.ytmusicdl.app.data.api

import retrofit2.http.*

data class HealthResponse(val status: String, val version: String)

data class Track(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val year: String = "",
    val video_id: String = "",
    val url: String = "",
    val cover_url: String = "",
    val duration: String = "",
    val track_number: Int = 0,
    val total_tracks: Int = 0,
    val browse_id: String = "",
)

data class DownloadRequest(
    val video_id: String,
    val url: String?,
    val title: String,
    val artist: String,
    val album: String,
    val year: String,
    val cover_url: String,
    val duration: String,
    val format: String? = null,
)

data class DownloadResponse(
    val job_id: String?,
    val status: String,
    val path: String? = null,
    val message: String? = null,
)

data class ProgressResponse(
    val id: String,
    val status: String,
    val progress: Int,
    val mb_done: Float,
    val mb_total: Float,
    val path: String?,
    val error: String?,
)

interface YtmusicdlApi {
    @GET("health")
    suspend fun health(): HealthResponse

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "song",
        @Query("limit") limit: Int = 8,
    ): retrofit2.Response<Map<String, Any>>

    @POST("download")
    suspend fun download(@Body request: DownloadRequest): DownloadResponse

    @GET("progress/{jobId}")
    suspend fun progress(@Path("jobId") jobId: String): ProgressResponse
}
