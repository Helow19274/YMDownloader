package com.helow.ymdownloader.api

import com.helow.ymdownloader.model.Info
import com.helow.ymdownloader.model.TrackResp
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ApiInterface {
    @GET("https://music.yandex.ru/handlers/track.jsx")
    suspend fun getTrack(@Query("track") track: String): TrackResp

    @GET("https://storage.mds.yandex.net/download-info/{storageDir}/2?format=json")
    suspend fun getInfo(@Path("storageDir") storageDir: String): Info

    @Streaming
    @GET("https://{host}/get-mp3/{hash}/{ts}/{path}")
    suspend fun getFile(
        @Path("host") host: String,
        @Path("hash") hash: String,
        @Path("ts") ts: String,
        @Path("path") path: String
    ): ResponseBody
}