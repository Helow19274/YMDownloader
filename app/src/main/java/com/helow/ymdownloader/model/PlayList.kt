package com.helow.ymdownloader.model

data class PlayListResp(
    val playlist: PlayList
)

data class PlayList(
    val tracks: List<Track>,
    val trackCount: Int,
    val title: String,
    val owner: Owner
)

data class Owner(
    val name: String
)