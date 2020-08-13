package com.helow.ymdownloader.model

import androidx.annotation.Keep

@Keep
data class PlaylistResp(
    val playlist: Playlist
)

@Keep
data class Playlist(
    val tracks: List<Track>,
    val trackCount: Int,
    val title: String,
    val owner: Owner
)

@Keep
data class Owner(
    val name: String
)