package com.helow.ymdownloader.model

import androidx.annotation.Keep

@Keep
data class TrackResp(
    val track: Track
)

@Keep
data class Track(
    val title: String,
    val storageDir: String,
    val version: String? = null,
    val artists: List<Artist>
)
