package com.helow.ymdownloader.model

data class TrackResp(
    val track: Track
)

data class Track(
    val title: String,
    val storageDir: String,
    val version: String? = null,
    val artists: List<Artist>
)
