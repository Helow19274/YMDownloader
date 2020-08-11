package com.helow.ymdownloader.api

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

object Api {
    val service: ApiInterface by lazy {
        Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create())
            .baseUrl("https://a.b")
            .build()
            .create<ApiInterface>()
    }
}