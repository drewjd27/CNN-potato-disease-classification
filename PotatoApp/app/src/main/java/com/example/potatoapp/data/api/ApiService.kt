package com.example.potatoapp.data.api

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("detect")
    suspend fun classifyImage(
        @Part file: MultipartBody.Part,
    ): ClassifyResponse
}
