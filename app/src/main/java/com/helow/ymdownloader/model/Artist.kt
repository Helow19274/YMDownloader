package com.helow.ymdownloader.model

import androidx.annotation.Keep

@Keep
data class PartialArtist(
    val name: String
)

@Keep
data class ArtistResp(
    val artist: Artist,
    val albumIds: List<Int>
)

@Keep
data class Counts(
    val tracks: Int
)

@Keep
data class Artist(
    val name: String,
    val counts: Counts
)