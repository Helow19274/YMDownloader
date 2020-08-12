package com.helow.ymdownloader.model

import androidx.annotation.Keep

@Keep
data class Album(
    val trackCount: Int,
    val volumes: List<List<Track>>,
    val artists: List<Artist>,
    val title: String
)