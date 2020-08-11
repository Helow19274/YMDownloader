package com.helow.ymdownloader.model

import androidx.annotation.Keep

@Keep
data class Info(
    val host: String,
    val path: String,
    val s: String,
    val ts: String
)