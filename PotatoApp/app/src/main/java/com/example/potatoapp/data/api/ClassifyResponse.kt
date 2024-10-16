package com.example.potatoapp.data.api

import com.google.gson.annotations.SerializedName

data class ClassifyResponse(
    @field:SerializedName("error")
    val error: Boolean,
    @field:SerializedName("message")
    val message: String
)