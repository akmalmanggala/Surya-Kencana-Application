package com.example.suryakencanaapp.model

import com.google.gson.annotations.SerializedName

data class Admin(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("created_at") val createdAt: String
)