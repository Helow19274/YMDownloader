package com.helow.ymdownloader.model

import androidx.annotation.Keep

@Keep
data class PlayListResp(
    val playlist: PlayList
)

@Keep
data class PlayList(
    val tracks: List<Track>,
    val trackCount: Int,
    val title: String,
    val owner: Owner
)

@Keep
data class Owner(
    val name: String
)